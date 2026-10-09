package com.hireshield.ml.training;

import java.util.HashMap;
import java.util.Map;

import org.tribuo.Example;
import org.tribuo.Model;
import org.tribuo.Prediction;
import org.tribuo.classification.Label;
import org.tribuo.classification.sgd.linear.LinearSGDModel;
import org.tribuo.impl.ArrayExample;
import org.tribuo.math.la.DenseMatrix;

/**
 * Helpers around a Tribuo model used during training and evaluation.
 */
final class TribuoClassifier {

    static final String FRAUD = "fraud";
    static final String REAL = "real";
    private static final Label UNKNOWN = new Label("unknown");

    private final Model<Label> model;

    TribuoClassifier(Model<Label> model) {
        this.model = model;
    }

    static Example<Label> toExample(Map<String, Double> vector, Label label) {
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

    /** Score for the fraud class. For logistic regression this is a probability. */
    double fraudScore(Map<String, Double> vector) {
        if (vector.isEmpty()) {
            return 0.0;
        }
        Prediction<Label> prediction = model.predict(toExample(vector, UNKNOWN));
        Label fraud = prediction.getOutputScores().get(FRAUD);
        return fraud == null ? 0.0 : fraud.getScore();
    }

    record LinearExport(Map<String, Double> weights, double bias) {
    }

    /**
     * Two-class softmax equals a sigmoid over the difference of the two weight rows,
     * so a linear model can be served as one weight per term plus a bias.
     */
    LinearExport exportLinear() {
        if (!(model instanceof LinearSGDModel linear)) {
            throw new IllegalStateException("Only linear models can be exported, got " + model.getClass().getSimpleName());
        }
        DenseMatrix w = linear.getWeightsCopy();
        int fraudRow = model.getOutputIDInfo().getID(new Label(FRAUD));
        int realRow = model.getOutputIDInfo().getID(new Label(REAL));
        var featureMap = model.getFeatureIDMap();
        Map<String, Double> weights = new HashMap<>();
        for (var info : featureMap) {
            int id = featureMap.getID(info.getName());
            weights.put(info.getName(), w.get(fraudRow, id) - w.get(realRow, id));
        }
        // The bias is stored in the last column.
        int biasColumn = w.getDimension2Size() - 1;
        double bias = w.get(fraudRow, biasColumn) - w.get(realRow, biasColumn);
        return new LinearExport(weights, bias);
    }
}
