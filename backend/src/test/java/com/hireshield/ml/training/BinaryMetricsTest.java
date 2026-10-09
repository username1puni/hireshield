package com.hireshield.ml.training;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.Test;

class BinaryMetricsTest {

    private final double[] scores = {0.9, 0.8, 0.7, 0.4, 0.3, 0.1};
    private final boolean[] actual = {true, true, false, true, false, false};

    @Test
    void confusionMatrixAtThreshold() {
        BinaryMetrics m = BinaryMetrics.of(scores, actual, 0.5);
        assertThat(m.truePositive()).isEqualTo(2);
        assertThat(m.falsePositive()).isEqualTo(1);
        assertThat(m.falseNegative()).isEqualTo(1);
        assertThat(m.trueNegative()).isEqualTo(2);
        assertThat(m.precision()).isCloseTo(2 / 3.0, within(1e-9));
        assertThat(m.recall()).isCloseTo(2 / 3.0, within(1e-9));
    }

    @Test
    void perfectRankingHasAucOfOne() {
        double[] s = {0.9, 0.8, 0.2, 0.1};
        boolean[] a = {true, true, false, false};
        assertThat(BinaryMetrics.rocAuc(s, a)).isEqualTo(1.0);
        assertThat(BinaryMetrics.averagePrecision(s, a)).isEqualTo(1.0);
    }

    @Test
    void aucMatchesHandCalculation() {
        // 9 positive/negative pairs, 8 ranked correctly
        assertThat(BinaryMetrics.rocAuc(scores, actual)).isCloseTo(8 / 9.0, within(1e-9));
    }
}
