package dev.quantumlab.physics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.quantumlab.math.Complex;
import dev.quantumlab.math.Tolerances;
import dev.quantumlab.physics.ProjectiveMeasurement.MeasurementResult;
import java.util.random.RandomGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ProjectiveMeasurementTest {

    private static final MeasurementBasis Z = MeasurementBasis.computational();
    private static final MeasurementBasis X = MeasurementBasis.hadamard();

    /** Five standard deviations of a binomial proportion: a failure here is a ~6e-7 event. */
    private static double fiveSigma(double p, int n) {
        return 5.0 * Math.sqrt(p * (1.0 - p) / n);
    }

    private static int[] outcomes(QubitState s, MeasurementBasis b, long seed, int n) {
        RandomGenerator rng = Reproducibility.newGenerator(seed);
        int[] out = new int[n];
        for (int i = 0; i < n; i++) {
            out[i] = ProjectiveMeasurement.measure(s, b, rng).outcomeIndex();
        }
        return out;
    }

    private static double frequencyOfOne(int[] outcomes) {
        long ones = 0;
        for (int o : outcomes) {
            ones += o;
        }
        return (double) ones / outcomes.length;
    }

    // ------------------------------------------------------------ result contents

    @Test
    @DisplayName("a result records the pre-state, a consistent label, and the Born probability")
    void resultContents() {
        QubitState s = QubitState.fromBloch(1.0, 0.5);
        double[] born = BornRule.probabilities(s, Z);
        MeasurementResult r =
                ProjectiveMeasurement.measure(s, Z, Reproducibility.newGenerator(1));
        assertTrue(r.preState().approxEquals(s.vector(), 0.0));
        assertEquals(Z.label(r.outcomeIndex()), r.outcomeLabel());
        assertEquals(born[r.outcomeIndex()], r.probability(), Tolerances.STRICT);
    }

    @Test
    @DisplayName("the post-measurement state is the basis vector that was found")
    void collapse() {
        QubitState s = QubitState.fromBloch(1.0, 0.5);
        for (long seed = 0; seed < 20; seed++) {
            MeasurementResult r =
                    ProjectiveMeasurement.measure(s, X, Reproducibility.newGenerator(seed));
            assertTrue(r.postState().approxEquals(X.vector(r.outcomeIndex()), 0.0));
        }
    }

    @Test
    @DisplayName("measuring a collapsed state again repeats the same outcome")
    void repeatMeasurementAgrees() {
        QubitState s = QubitState.fromBloch(1.3, 2.0);
        RandomGenerator rng = Reproducibility.newGenerator(99);
        for (int i = 0; i < 50; i++) {
            MeasurementResult first = ProjectiveMeasurement.measure(s, Z, rng);
            MeasurementResult second =
                    ProjectiveMeasurement.measure(
                            QubitState.fromVector(first.postState()), Z, rng);
            assertEquals(first.outcomeIndex(), second.outcomeIndex());
            assertEquals(1.0, second.probability(), Tolerances.STRICT);
        }
    }

    // ------------------------------------------------------------ determinism of certain outcomes

    @Test
    @DisplayName("|0> always gives outcome 0 and |1> always gives outcome 1 in the Z basis")
    void certainOutcomes() {
        for (int o : outcomes(QubitState.basis0(), Z, 5, 2000)) {
            assertEquals(0, o);
        }
        for (int o : outcomes(QubitState.basis1(), Z, 5, 2000)) {
            assertEquals(1, o);
        }
    }

    @Test
    @DisplayName("|+> always gives |+> in the X basis")
    void certainInX() {
        for (int o : outcomes(QubitState.plus(), X, 8, 2000)) {
            assertEquals(0, o);
        }
    }

    // ------------------------------------------------------------ reproducibility

    @Test
    @DisplayName("the same seed reproduces the identical outcome sequence")
    void sameSeedSameSequence() {
        QubitState s = QubitState.fromBloch(1.2, 0.3);
        int[] a = outcomes(s, Z, 12345L, 5000);
        int[] b = outcomes(s, Z, 12345L, 5000);
        assertTrue(java.util.Arrays.equals(a, b));
    }

    @Test
    @DisplayName("different seeds give different sequences")
    void differentSeedDifferentSequence() {
        QubitState s = QubitState.plus();
        int[] a = outcomes(s, Z, 1L, 5000);
        int[] b = outcomes(s, Z, 2L, 5000);
        assertFalse(java.util.Arrays.equals(a, b));
    }

    @Test
    @DisplayName("a pinned seed gives a pinned prefix (guards against RNG changes)")
    void pinnedRegression() {
        // Expected value computed INDEPENDENTLY of this code base, in Python, from the published
        // SplitMix64 algorithm (seed 2026; outcome 0 iff nextDouble() < |<0|+>|^2). So this checks
        // both the JDK generator and our inverse-CDF sampling rule, not merely that the code is
        // unchanged. If it fails, previously exported experiments no longer replay: treat any
        // change as a deliberate, documented decision (ADR-0003).
        int[] firstTwenty = java.util.Arrays.copyOf(outcomes(QubitState.plus(), Z, 2026L, 20), 20);
        StringBuilder sb = new StringBuilder();
        for (int o : firstTwenty) {
            sb.append(o);
        }
        assertEquals(PINNED_PREFIX, sb.toString());
    }

    /** First 20 outcomes for seed 2026 on |+> in the Z basis; see pinnedRegression. */
    private static final String PINNED_PREFIX = "10101111000100100011";

    // ------------------------------------------------------------ statistics

    @Test
    @DisplayName("empirical frequency approaches the Born probability (several seeds, 5 sigma)")
    void frequencyMatchesBornZ() {
        QubitState s = QubitState.of(Complex.of(Math.sqrt(0.7)), Complex.of(0, Math.sqrt(0.3)));
        int n = 200_000;
        double p1 = BornRule.probabilities(s, Z)[1];
        for (long seed : new long[] {1, 2, 3, 42, 2026}) {
            double f = frequencyOfOne(outcomes(s, Z, seed, n));
            assertTrue(
                    Math.abs(f - p1) <= fiveSigma(p1, n),
                    "seed " + seed + ": observed " + f + " vs expected " + p1);
        }
    }

    @Test
    @DisplayName("empirical frequency matches the interference prediction in the X basis")
    void frequencyMatchesBornX() {
        double phi = 1.1;
        QubitState s =
                QubitState.of(
                        Complex.of(1 / Math.sqrt(2)), Complex.cis(phi).times(1 / Math.sqrt(2)));
        int n = 200_000;
        double expectedMinus = (1 - Math.cos(phi)) / 2;
        for (long seed : new long[] {11, 12, 13}) {
            double f = frequencyOfOne(outcomes(s, X, seed, n));
            assertTrue(
                    Math.abs(f - expectedMinus) <= fiveSigma(expectedMinus, n),
                    "seed " + seed + ": observed " + f + " vs expected " + expectedMinus);
        }
    }

    @Test
    @DisplayName("a skewed distribution (p = 0.01) is also reproduced")
    void rareOutcome() {
        QubitState s = QubitState.fromDecay(new DecayModel(0.01005033585), 1.0, 0.0);
        int n = 300_000;
        double p1 = BornRule.probabilities(s, Z)[1];
        double f = frequencyOfOne(outcomes(s, Z, 77L, n));
        assertTrue(Math.abs(f - p1) <= fiveSigma(p1, n));
    }

    // ------------------------------------------------------------ sampleIndex

    @Test
    @DisplayName("an outcome with probability zero is never sampled")
    void zeroProbabilityNeverSampled() {
        double[] p = {0.5, 0.0, 0.5};
        RandomGenerator rng = Reproducibility.newGenerator(3);
        for (int i = 0; i < 100_000; i++) {
            assertFalse(ProjectiveMeasurement.sampleIndex(p, rng) == 1);
        }
    }

    @Test
    @DisplayName("degenerate distributions pick the only possible outcome")
    void degenerate() {
        RandomGenerator rng = Reproducibility.newGenerator(4);
        for (int i = 0; i < 1000; i++) {
            assertEquals(1, ProjectiveMeasurement.sampleIndex(new double[] {0.0, 1.0}, rng));
            assertEquals(0, ProjectiveMeasurement.sampleIndex(new double[] {1.0, 0.0}, rng));
        }
    }

    @Test
    @DisplayName("rounding shortfall falls back to the last positive outcome")
    void roundingFallback() {
        // Probabilities sum to slightly below 1; a draw above the cumulative sum must not fail.
        RandomGenerator alwaysHigh =
                new RandomGenerator() {
                    @Override
                    public long nextLong() {
                        return -1L;
                    }

                    @Override
                    public double nextDouble() {
                        return Math.nextDown(1.0);
                    }
                };
        double[] p = {0.5, 0.4999999, 0.0};
        assertEquals(1, ProjectiveMeasurement.sampleIndex(p, alwaysHigh));
    }

    @Test
    @DisplayName("an all-zero distribution is rejected")
    void allZero() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        ProjectiveMeasurement.sampleIndex(
                                new double[] {0.0, 0.0}, Reproducibility.newGenerator(1)));
    }
}
