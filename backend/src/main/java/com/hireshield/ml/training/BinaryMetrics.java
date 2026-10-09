package com.hireshield.ml.training;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Precision, recall and friends for the fraud class. Kept separate from Tribuo's
 * evaluator because we pick our own decision threshold instead of argmax.
 */
record BinaryMetrics(int truePositive, int falsePositive, int trueNegative, int falseNegative,
                     double rocAuc, double averagePrecision) {

    static BinaryMetrics of(double[] scores, boolean[] actual, double threshold) {
        int tp = 0, fp = 0, tn = 0, fn = 0;
        for (int i = 0; i < scores.length; i++) {
            boolean predicted = scores[i] >= threshold;
            if (predicted && actual[i]) tp++;
            else if (predicted) fp++;
            else if (actual[i]) fn++;
            else tn++;
        }
        return new BinaryMetrics(tp, fp, tn, fn, rocAuc(scores, actual), averagePrecision(scores, actual));
    }

    double precision() {
        return truePositive + falsePositive == 0 ? 0 : (double) truePositive / (truePositive + falsePositive);
    }

    double recall() {
        return truePositive + falseNegative == 0 ? 0 : (double) truePositive / (truePositive + falseNegative);
    }

    double f1() {
        double p = precision();
        double r = recall();
        return p + r == 0 ? 0 : 2 * p * r / (p + r);
    }

    double accuracy() {
        int total = truePositive + falsePositive + trueNegative + falseNegative;
        return (double) (truePositive + trueNegative) / total;
    }

    /** Threshold with the best F1 on the given scores. Used on cross-validation predictions only. */
    static double bestThreshold(double[] scores, boolean[] actual) {
        double best = 0.5;
        double bestF1 = -1;
        for (int step = 2; step <= 98; step++) {
            double t = step / 100.0;
            double f1 = of(scores, actual, t).f1();
            if (f1 > bestF1) {
                bestF1 = f1;
                best = t;
            }
        }
        return best;
    }

    private static List<Integer> rankedIndexes(double[] scores) {
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < scores.length; i++) {
            order.add(i);
        }
        order.sort(Comparator.comparingDouble((Integer i) -> scores[i]).reversed());
        return order;
    }

    static double averagePrecision(double[] scores, boolean[] actual) {
        int positives = 0;
        for (boolean a : actual) {
            if (a) positives++;
        }
        if (positives == 0) {
            return 0;
        }
        double sum = 0;
        int hits = 0;
        int seen = 0;
        for (int i : rankedIndexes(scores)) {
            seen++;
            if (actual[i]) {
                hits++;
                sum += (double) hits / seen;
            }
        }
        return sum / positives;
    }

    static double rocAuc(double[] scores, boolean[] actual) {
        // Mann-Whitney U: chance that a random scam scores higher than a random real ad.
        List<Integer> order = rankedIndexes(scores);
        long positives = 0, negatives = 0;
        for (boolean a : actual) {
            if (a) positives++;
            else negatives++;
        }
        if (positives == 0 || negatives == 0) {
            return 0;
        }
        double negativesBelow = negatives;
        double area = 0;
        int i = 0;
        while (i < order.size()) {
            int j = i;
            int pos = 0, neg = 0;
            while (j < order.size() && scores[order.get(j)] == scores[order.get(i)]) {
                if (actual[order.get(j)]) pos++;
                else neg++;
                j++;
            }
            negativesBelow -= neg;
            area += pos * (negativesBelow + neg / 2.0);
            i = j;
        }
        return area / (positives * negatives);
    }

    Map<String, Object> toMap(double threshold) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("threshold", threshold);
        map.put("precision", round(precision()));
        map.put("recall", round(recall()));
        map.put("f1", round(f1()));
        map.put("accuracy", round(accuracy()));
        map.put("rocAuc", round(rocAuc));
        map.put("prAuc", round(averagePrecision));
        map.put("confusionMatrix", Map.of(
                "truePositive", truePositive, "falsePositive", falsePositive,
                "trueNegative", trueNegative, "falseNegative", falseNegative));
        return map;
    }

    private static double round(double value) {
        return Math.round(value * 10000) / 10000.0;
    }
}
