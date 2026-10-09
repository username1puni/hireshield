package com.hireshield.ml;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import tools.jackson.databind.ObjectMapper;

/**
 * TF-IDF over unigrams and bigrams. Sublinear term frequency (1 + log tf) and
 * L2 normalisation, which is the usual setup for short and long documents mixed together.
 */
public class TfidfVectorizer {

    private final Map<String, Double> idf;
    private final int documentCount;

    private TfidfVectorizer(Map<String, Double> idf, int documentCount) {
        this.idf = idf;
        this.documentCount = documentCount;
    }

    /**
     * Builds the vocabulary from the terms of training documents only
     * (see {@link TextNormalizer#terms(String)}).
     *
     * @param minDocFrequency terms seen in fewer documents are dropped (mostly typos and names)
     * @param maxDocRatio     terms in more than this share of documents are dropped
     * @param maxFeatures     keep only this many of the most common remaining terms
     */
    public static TfidfVectorizer fit(Collection<List<String>> documents, int minDocFrequency, double maxDocRatio,
                                      int maxFeatures) {
        Map<String, Integer> docFrequency = new HashMap<>();
        for (List<String> terms : documents) {
            Set<String> seen = new HashSet<>(terms);
            for (String term : seen) {
                docFrequency.merge(term, 1, Integer::sum);
            }
        }
        int n = documents.size();
        int maxDocs = (int) (maxDocRatio * n);
        Map<String, Double> idf = new LinkedHashMap<>();
        docFrequency.entrySet().stream()
                .filter(e -> e.getValue() >= minDocFrequency && e.getValue() <= maxDocs)
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()))
                .limit(maxFeatures)
                .forEach(e -> idf.put(e.getKey(), Math.log((1.0 + n) / (1.0 + e.getValue())) + 1.0));
        return new TfidfVectorizer(idf, n);
    }

    public Map<String, Double> transform(String text) {
        return transform(TextNormalizer.terms(text));
    }

    public Map<String, Double> transform(List<String> terms) {
        Map<String, Integer> counts = new HashMap<>();
        for (String term : terms) {
            if (idf.containsKey(term)) {
                counts.merge(term, 1, Integer::sum);
            }
        }
        Map<String, Double> vector = new HashMap<>();
        double norm = 0;
        for (Map.Entry<String, Integer> e : counts.entrySet()) {
            double value = (1.0 + Math.log(e.getValue())) * idf.get(e.getKey());
            vector.put(e.getKey(), value);
            norm += value * value;
        }
        if (norm > 0) {
            double length = Math.sqrt(norm);
            vector.replaceAll((term, value) -> value / length);
        }
        return vector;
    }

    public int vocabularySize() {
        return idf.size();
    }

    public void save(OutputStream out, ObjectMapper mapper) throws IOException {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("documentCount", documentCount);
        json.put("idf", idf);
        mapper.writeValue(out, json);
    }

    @SuppressWarnings("unchecked")
    public static TfidfVectorizer load(InputStream in, ObjectMapper mapper) throws IOException {
        Map<String, Object> json = mapper.readValue(in, Map.class);
        Map<String, Number> raw = (Map<String, Number>) json.get("idf");
        Map<String, Double> idf = new HashMap<>(raw.size() * 2);
        raw.forEach((term, value) -> idf.put(term, value.doubleValue()));
        return new TfidfVectorizer(idf, ((Number) json.get("documentCount")).intValue());
    }

    public List<String> vocabulary() {
        return new ArrayList<>(idf.keySet());
    }
}
