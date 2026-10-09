package com.hireshield.ml;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.tribuo.Example;
import org.tribuo.Model;
import org.tribuo.Prediction;
import org.tribuo.classification.Label;
import org.tribuo.classification.sgd.linear.LinearSGDModel;
import org.tribuo.impl.ArrayExample;
import org.tribuo.math.la.DenseMatrix;

/**
 * Wraps a trained Tribuo model together with the vectorizer it was trained with.
 */
public class ScamClassifier {

    public static final String FRAUD = "fraud";
    public static final String REAL = "real";
    private static final Label UNKNOWN = new Label("unknown");

    private final Model<Label> model;
    private final TfidfVectorizer vectorizer;
    private final double threshold;

    public ScamClassifier(Model<Label> model, TfidfVectorizer vectorizer, double threshold) {
        this.model = model;
        this.vectorizer = vectorizer;
        this.threshold = threshold;
    }

    public static Example<Label> toExample(Map<String, Double> vector, Label label) {
        // Build from arrays: ArrayExample.add() re-sorts on every call, which is very slow for text.
        String[] names = new String[vector.size()];
        double[] values = new double[vector.size()];
        int i = 0;
        for (Map.Entry<String, Double> entry : vector.entrySet()) {
            names[i] = entry.getKey();
            values[i] = entry.getValue();
            i++;
        }
        return new ArrayExample<>(label, names, values);
    }

    /** Raw score for the fraud class. For logistic regression this is a probability. */
    public double fraudScore(Map<String, Double> vector) {
        if (vector.isEmpty()) {
            return 0.0;
        }
        Prediction<Label> prediction = model.predict(toExample(vector, UNKNOWN));
        Label fraud = prediction.getOutputScores().get(FRAUD);
        return fraud == null ? 0.0 : fraud.getScore();
    }

    public MlResult predict(String text, int topTerms) {
        Map<String, Double> vector = vectorizer.transform(text);
        double probability = fraudScore(vector);
        return new MlResult(probability, probability >= threshold, contributions(vector, topTerms));
    }

    /**
     * For a linear model each term pushes the score by weight * tf-idf value, so the
     * biggest positive products are the words that made the post look like a scam.
     */
    private List<TermContribution> contributions(Map<String, Double> vector, int limit) {
        if (!(model instanceof LinearSGDModel linear) || vector.isEmpty()) {
            return List.of();
        }
        DenseMatrix weights = linear.getWeightsCopy();
        int fraudRow = model.getOutputIDInfo().getID(new Label(FRAUD));
        int realRow = model.getOutputIDInfo().getID(new Label(REAL));
        var featureIds = model.getFeatureIDMap();

        List<TermContribution> result = new ArrayList<>();
        for (Map.Entry<String, Double> entry : vector.entrySet()) {
            var info = featureIds.get(entry.getKey());
            if (info == null) {
                continue;
            }
            int id = info.getID();
            double weight = weights.get(fraudRow, id) - weights.get(realRow, id);
            double contribution = weight * entry.getValue();
            if (contribution > 0) {
                result.add(new TermContribution(readable(entry.getKey()), contribution));
            }
        }
        result.sort(Comparator.comparingDouble(TermContribution::weight).reversed());
        return result.size() > limit ? result.subList(0, limit) : result;
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

    public record TermContribution(String term, double weight) {
    }

    public record MlResult(double probability, boolean flagged, List<TermContribution> topTerms) {
    }
}
