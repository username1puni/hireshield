package com.hireshield.ml;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.hireshield.ml.MlResult.TermContribution;

import tools.jackson.databind.ObjectMapper;

/**
 * The trained logistic regression in a compact form for serving.
 *
 * <p>Training uses Tribuo (see {@code TrainModel}). With two classes, Tribuo's softmax output
 * is the same as sigmoid(w . x + b) where w is the difference between the "fraud" and "real"
 * weight rows, so only that one vector is exported. Loading the full Tribuo model needed
 * well over 200 MB of heap because of its feature statistics; this needs about 15 MB, which
 * matters on a small server. The training job checks that both give the same probabilities.
 */
public class LinearScamModel {

    private final TfidfVectorizer vectorizer;
    private final Map<String, Double> weights;
    private final double bias;
    private final double threshold;

    public LinearScamModel(TfidfVectorizer vectorizer, Map<String, Double> weights, double bias, double threshold) {
        this.vectorizer = vectorizer;
        this.weights = weights;
        this.bias = bias;
        this.threshold = threshold;
    }

    public double probability(Map<String, Double> vector) {
        if (vector.isEmpty()) {
            return 0.0;
        }
        double z = bias;
        for (Map.Entry<String, Double> entry : vector.entrySet()) {
            Double w = weights.get(entry.getKey());
            if (w != null) {
                z += w * entry.getValue();
            }
        }
        return 1.0 / (1.0 + Math.exp(-z));
    }

    public MlResult predict(String text, int topTerms) {
        Map<String, Double> vector = vectorizer.transform(text);
        double p = probability(vector);
        return new MlResult(p, p >= threshold, contributions(vector, topTerms));
    }

    /** The terms with the biggest positive weight * tf-idf, i.e. what made the text look like a scam. */
    private List<TermContribution> contributions(Map<String, Double> vector, int limit) {
        List<TermContribution> result = new ArrayList<>();
        for (Map.Entry<String, Double> entry : vector.entrySet()) {
            Double w = weights.get(entry.getKey());
            if (w != null && w * entry.getValue() > 0) {
                result.add(new TermContribution(readable(entry.getKey()), w * entry.getValue()));
            }
        }
        result.sort(Comparator.comparingDouble(TermContribution::weight).reversed());
        return result.size() > limit ? List.copyOf(result.subList(0, limit)) : result;
    }

    /** The normalizer replaces amounts, emails and links with placeholder words; show them nicely. */
    static String readable(String term) {
        return term.replace("moneytoken", "[amount]")
                .replace("emailtoken", "[email]")
                .replace("urltoken", "[link]")
                .replace("phonetoken", "[phone]");
    }

    public double threshold() {
        return threshold;
    }

    public int vocabularySize() {
        return vectorizer.vocabularySize();
    }

    public static void saveWeights(OutputStream out, ObjectMapper mapper, Map<String, Double> weights, double bias,
                                   double threshold) throws IOException {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("type", "logistic-regression");
        json.put("bias", bias);
        json.put("threshold", threshold);
        json.put("weights", weights);
        mapper.writeValue(out, json);
    }

    @SuppressWarnings("unchecked")
    public static LinearScamModel load(InputStream weightsJson, InputStream vectorizerJson, ObjectMapper mapper)
            throws IOException {
        Map<String, Object> json = mapper.readValue(weightsJson, Map.class);
        Map<String, Number> raw = (Map<String, Number>) json.get("weights");
        Map<String, Double> weights = new HashMap<>(raw.size() * 2);
        raw.forEach((term, value) -> weights.put(term, value.doubleValue()));
        TfidfVectorizer vectorizer = TfidfVectorizer.load(vectorizerJson, mapper);
        return new LinearScamModel(vectorizer, weights,
                ((Number) json.get("bias")).doubleValue(), ((Number) json.get("threshold")).doubleValue());
    }
}
