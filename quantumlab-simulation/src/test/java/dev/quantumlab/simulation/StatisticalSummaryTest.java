package dev.quantumlab.simulation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class StatisticalSummaryTest {

    private static final List<String> LABELS = List.of("a", "b");

    @Test
    @DisplayName("hand-computed example: 60/40 against 50/50 over 100 trials is +2 sigma")
    void handComputed() {
        StatisticalSummary s =
                StatisticalSummary.of(LABELS, new long[] {60, 40}, new double[] {0.5, 0.5});
        assertEquals(100, s.trials());
        OutcomeStatistics a = s.outcomes().get(0);
        assertEquals(60, a.count());
        assertEquals(0.6, a.observed(), 1e-15);
        assertEquals(0.05, a.standardError(), 1e-15); // sqrt(0.25 / 100)
        assertEquals(2.0, a.zScore(), 1e-12);
        assertEquals(-2.0, s.outcomes().get(1).zScore(), 1e-12);
        assertTrue(s.withinFiveSigma());
    }

    @Test
    @DisplayName("a 6-sigma deviation is flagged")
    void sixSigmaFlagged() {
        StatisticalSummary s =
                StatisticalSummary.of(LABELS, new long[] {80, 20}, new double[] {0.5, 0.5});
        assertEquals(6.0, s.outcomes().get(0).zScore(), 1e-12);
        assertFalse(s.withinFiveSigma());
    }

    @Test
    @DisplayName("the 5-sigma threshold separates 4.8 sigma (accepted) from 5.2 sigma (flagged)")
    void fiveSigmaThreshold() {
        // Chosen away from the boundary itself so the test does not depend on rounding at z = 5.
        StatisticalSummary inside =
                StatisticalSummary.of(LABELS, new long[] {74, 26}, new double[] {0.5, 0.5});
        assertEquals(4.8, inside.outcomes().get(0).zScore(), 1e-9);
        assertTrue(inside.withinFiveSigma());

        StatisticalSummary outside =
                StatisticalSummary.of(LABELS, new long[] {76, 24}, new double[] {0.5, 0.5});
        assertEquals(5.2, outside.outcomes().get(0).zScore(), 1e-9);
        assertFalse(outside.withinFiveSigma());
    }

    @Test
    @DisplayName("zero trials gives zeros, never NaN")
    void zeroTrials() {
        StatisticalSummary s =
                StatisticalSummary.of(LABELS, new long[] {0, 0}, new double[] {0.5, 0.5});
        assertEquals(0, s.trials());
        for (OutcomeStatistics o : s.outcomes()) {
            assertEquals(0.0, o.observed(), 0.0);
            assertEquals(0.0, o.standardError(), 0.0);
            assertEquals(0.0, o.zScore(), 0.0);
        }
    }

    @Test
    @DisplayName("probability 0 or 1 has zero standard error and zero z-score")
    void degenerateProbabilities() {
        StatisticalSummary s =
                StatisticalSummary.of(LABELS, new long[] {0, 500}, new double[] {0.0, 1.0});
        assertEquals(0.0, s.outcomes().get(0).standardError(), 0.0);
        assertEquals(0.0, s.outcomes().get(1).zScore(), 0.0);
        assertTrue(s.withinFiveSigma());
    }

    @Test
    @DisplayName("the outcome list is immutable")
    void immutable() {
        StatisticalSummary s =
                StatisticalSummary.of(LABELS, new long[] {1, 1}, new double[] {0.5, 0.5});
        assertThrows(UnsupportedOperationException.class, () -> s.outcomes().clear());
    }
}
