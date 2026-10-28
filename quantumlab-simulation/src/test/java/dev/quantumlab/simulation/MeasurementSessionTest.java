package dev.quantumlab.simulation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.quantumlab.math.Complex;
import dev.quantumlab.physics.CatState;
import dev.quantumlab.physics.QubitState;
import dev.quantumlab.physics.StandardBasis;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MeasurementSessionTest {

    private static QuantumMeasurementConfig config(QubitState s, StandardBasis b, long seed) {
        return QuantumMeasurementConfig.of(s, b, seed);
    }

    private static String outcomeString(MeasurementSession s, int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) {
            sb.append(s.outcomeAt(i));
        }
        return sb.toString();
    }

    // ------------------------------------------------------------ reproducibility

    @Test
    @DisplayName("the same config reproduces the identical outcome sequence")
    void sameConfigSameSequence() {
        QuantumMeasurementConfig c = config(QubitState.fromBloch(1.2, 0.4), StandardBasis.COMPUTATIONAL, 99);
        MeasurementSession a = new MeasurementSession(c);
        MeasurementSession b = new MeasurementSession(c);
        a.measure(5000);
        b.measure(5000);
        assertEquals(outcomeString(a, 5000), outcomeString(b, 5000));
    }

    @Test
    @DisplayName("reset() rewinds to the seed and replays exactly")
    void resetReplays() {
        MeasurementSession s =
                new MeasurementSession(config(QubitState.plus(), StandardBasis.COMPUTATIONAL, 7));
        s.measure(3000);
        String first = outcomeString(s, 3000);
        long[] firstCounts = s.counts();

        s.reset();
        assertEquals(0, s.trialCount());
        s.measure(3000);
        assertEquals(first, outcomeString(s, 3000));
        assertEquals(firstCounts[0], s.counts()[0]);
        assertEquals(firstCounts[1], s.counts()[1]);
    }

    @Test
    @DisplayName("running in one batch equals running the same trials one at a time")
    void batchEqualsSingles() {
        QuantumMeasurementConfig c = config(QubitState.fromBloch(0.8, 1.9), StandardBasis.HADAMARD, 31);
        MeasurementSession batch = new MeasurementSession(c);
        MeasurementSession singles = new MeasurementSession(c);
        batch.measure(500);
        for (int i = 0; i < 500; i++) {
            singles.measureOnce();
        }
        assertEquals(outcomeString(batch, 500), outcomeString(singles, 500));
    }

    @Test
    @DisplayName("different seeds give different sequences")
    void differentSeeds() {
        MeasurementSession a =
                new MeasurementSession(config(QubitState.plus(), StandardBasis.COMPUTATIONAL, 1));
        MeasurementSession b =
                new MeasurementSession(config(QubitState.plus(), StandardBasis.COMPUTATIONAL, 2));
        a.measure(2000);
        b.measure(2000);
        assertFalse(outcomeString(a, 2000).equals(outcomeString(b, 2000)));
    }

    @Test
    @DisplayName("seed 2026 on |+> in Z gives the independently computed prefix")
    void pinnedPrefixThroughSession() {
        // Same independently computed value as in the physics module (SplitMix64 in Python), now
        // checked through the config -> session path.
        MeasurementSession s =
                new MeasurementSession(config(QubitState.plus(), StandardBasis.COMPUTATIONAL, 2026));
        s.measure(20);
        assertEquals("10101111000100100011", outcomeString(s, 20));
    }

    // ------------------------------------------------------------ records and bookkeeping

    @Test
    @DisplayName("measureOnce returns a complete, consistent record")
    void trialRecordContents() {
        QubitState prepared = QubitState.fromBloch(1.0, 0.5);
        MeasurementSession s =
                new MeasurementSession(config(prepared, StandardBasis.COMPUTATIONAL, 3));
        double[] born = s.theoreticalProbabilities();
        for (int i = 1; i <= 50; i++) {
            TrialRecord t = s.measureOnce();
            assertEquals(i, t.trialNumber());
            assertTrue(t.preState().sameRayAs(prepared, 1e-12));
            assertEquals(s.basis().label(t.outcomeIndex()), t.outcomeLabel());
            assertEquals(born[t.outcomeIndex()], t.theoreticalProbability(), 1e-15);
        }
        assertEquals(50, s.trialCount());
    }

    @Test
    @DisplayName("in the Z basis the apparatus chain follows the outcome")
    void apparatusInZ() {
        MeasurementSession s =
                new MeasurementSession(config(QubitState.plus(), StandardBasis.COMPUTATIONAL, 5));
        boolean sawAlive = false;
        boolean sawDead = false;
        for (int i = 0; i < 200; i++) {
            TrialRecord t = s.measureOnce();
            assertTrue(t.apparatus().isPresent());
            CatState cat = t.apparatus().get().cat();
            assertEquals(t.outcomeIndex() == 1, cat == CatState.DEAD);
            assertEquals(t.outcomeIndex() == 1, t.apparatus().get().nucleusDecayed());
            sawAlive |= cat == CatState.ALIVE;
            sawDead |= cat == CatState.DEAD;
        }
        assertTrue(sawAlive && sawDead);
    }

    @Test
    @DisplayName("outside the Z basis no apparatus consequence is defined")
    void noApparatusInXAndY() {
        for (StandardBasis b : new StandardBasis[] {StandardBasis.HADAMARD, StandardBasis.CIRCULAR}) {
            MeasurementSession s = new MeasurementSession(config(QubitState.plus(), b, 5));
            for (int i = 0; i < 20; i++) {
                assertFalse(s.measureOnce().apparatus().isPresent());
            }
        }
    }

    @Test
    @DisplayName("counts add up to the number of trials")
    void countsAddUp() {
        MeasurementSession s =
                new MeasurementSession(config(QubitState.fromBloch(2.0, 1.0), StandardBasis.CIRCULAR, 8));
        s.measure(12345);
        long[] c = s.counts();
        assertEquals(12345, c[0] + c[1]);
        assertEquals(12345, s.trialCount());
        long ones = 0;
        for (int i = 0; i < 12345; i++) {
            ones += s.outcomeAt(i);
        }
        assertEquals(ones, c[1]);
    }

    @Test
    @DisplayName("recentTrials returns the last k trials, oldest first")
    void recentTrials() {
        MeasurementSession s =
                new MeasurementSession(config(QubitState.plus(), StandardBasis.COMPUTATIONAL, 4));
        s.measure(100);
        List<TrialRecord> recent = s.recentTrials(10);
        assertEquals(10, recent.size());
        assertEquals(91, recent.get(0).trialNumber());
        assertEquals(100, recent.get(9).trialNumber());
        assertEquals(100, s.recentTrials(1000).size());
        assertEquals(0, s.recentTrials(0).size());
    }

    @Test
    @DisplayName("invalid arguments are rejected")
    void validation() {
        MeasurementSession s =
                new MeasurementSession(config(QubitState.plus(), StandardBasis.COMPUTATIONAL, 4));
        assertThrows(IllegalArgumentException.class, () -> s.measure(0));
        assertThrows(IllegalArgumentException.class, () -> s.recentTrials(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> s.trial(0));
    }

    @Test
    @DisplayName("theoreticalProbabilities returns a defensive copy")
    void defensiveCopy() {
        MeasurementSession s =
                new MeasurementSession(config(QubitState.plus(), StandardBasis.COMPUTATIONAL, 4));
        double[] p = s.theoreticalProbabilities();
        p[0] = 99.0;
        assertEquals(0.5, s.theoreticalProbabilities()[0], 1e-12);
    }

    // ------------------------------------------------------------ limits

    @Test
    @DisplayName("the trial limit is enforced")
    void trialLimit() {
        MeasurementSession s =
                new MeasurementSession(config(QubitState.plus(), StandardBasis.COMPUTATIONAL, 4));
        int done = s.measure(MeasurementSession.MAX_TRIALS + 500);
        assertEquals(MeasurementSession.MAX_TRIALS, done);
        assertEquals(MeasurementSession.MAX_TRIALS, s.trialCount());
        assertEquals(0, s.remainingCapacity());
        assertEquals(0, s.measure(10));
        assertThrows(IllegalStateException.class, s::measureOnce);
        s.reset();
        assertEquals(MeasurementSession.MAX_TRIALS, s.remainingCapacity());
    }

    // ------------------------------------------------------------ statistics

    @Test
    @DisplayName("certain outcomes have zero scatter and zero z-score")
    void certainOutcomeStatistics() {
        MeasurementSession s =
                new MeasurementSession(config(QubitState.basis1(), StandardBasis.COMPUTATIONAL, 1));
        s.measure(1000);
        StatisticalSummary sum = s.summary();
        assertEquals(1000, sum.trials());
        assertEquals(1.0, sum.outcomes().get(1).observed(), 0.0);
        assertEquals(0.0, sum.outcomes().get(1).zScore(), 0.0);
        assertEquals(0.0, sum.outcomes().get(0).zScore(), 0.0);
        assertTrue(sum.withinFiveSigma());
    }

    @Test
    @DisplayName("observed frequencies agree with Born probabilities within 5 sigma, in every basis")
    void frequenciesMatchTheory() {
        QubitState s =
                QubitState.of(Complex.of(Math.sqrt(0.7)), Complex.cis(0.9).times(Math.sqrt(0.3)));
        for (StandardBasis b : StandardBasis.values()) {
            for (long seed : new long[] {1, 2, 3, 2026}) {
                MeasurementSession session = new MeasurementSession(config(s, b, seed));
                session.measure(200_000);
                StatisticalSummary sum = session.summary();
                assertTrue(
                        sum.withinFiveSigma(),
                        b + " seed " + seed + ": " + sum.outcomes());
                // Observed frequencies of all outcomes must themselves sum to 1.
                assertEquals(
                        1.0,
                        sum.outcomes().get(0).observed() + sum.outcomes().get(1).observed(),
                        1e-12);
            }
        }
    }

    @Test
    @DisplayName("scatter shrinks roughly like 1/sqrt(N): standard error is 10x smaller at 100x the trials")
    void standardErrorScaling() {
        MeasurementSession s =
                new MeasurementSession(config(QubitState.plus(), StandardBasis.COMPUTATIONAL, 6));
        s.measure(1_000);
        double seSmall = s.summary().outcomes().get(0).standardError();
        s.measure(99_000);
        double seLarge = s.summary().outcomes().get(0).standardError();
        assertEquals(10.0, seSmall / seLarge, 1e-9);
    }

    // ------------------------------------------------------------ running frequency

    @Test
    @DisplayName("the running frequency ends at the final observed frequency")
    void runningFrequencyEndpoint() {
        MeasurementSession s =
                new MeasurementSession(config(QubitState.fromBloch(1.0, 0), StandardBasis.COMPUTATIONAL, 12));
        s.measure(50_000);
        List<SeriesPoint> series = s.runningFrequency(1, 200);
        SeriesPoint last = series.get(series.size() - 1);
        assertEquals(50_000.0, last.x(), 0.0);
        assertEquals(s.summary().outcomes().get(1).observed(), last.y(), 1e-12);
        assertTrue(series.size() <= 200);
    }

    @Test
    @DisplayName("running-frequency points are strictly increasing in trial number")
    void runningFrequencyMonotoneX() {
        MeasurementSession s =
                new MeasurementSession(config(QubitState.plus(), StandardBasis.COMPUTATIONAL, 13));
        s.measure(100_000);
        List<SeriesPoint> series = s.runningFrequency(0, 300);
        for (int i = 1; i < series.size(); i++) {
            assertTrue(series.get(i).x() > series.get(i - 1).x());
        }
        assertEquals(1.0, series.get(0).x(), 0.0);
    }

    @Test
    @DisplayName("with no trials the running frequency is empty")
    void runningFrequencyEmpty() {
        MeasurementSession s =
                new MeasurementSession(config(QubitState.plus(), StandardBasis.COMPUTATIONAL, 1));
        assertTrue(s.runningFrequency(0, 50).isEmpty());
    }
}
