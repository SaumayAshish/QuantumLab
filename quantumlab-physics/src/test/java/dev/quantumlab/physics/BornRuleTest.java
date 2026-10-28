package dev.quantumlab.physics;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.quantumlab.math.Complex;
import dev.quantumlab.math.ComplexVector;
import dev.quantumlab.math.Tolerances;
import java.util.SplittableRandom;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BornRuleTest {

    private static final double TOL = Tolerances.STRICT;
    private static final MeasurementBasis Z = MeasurementBasis.computational();
    private static final MeasurementBasis X = MeasurementBasis.hadamard();
    private static final MeasurementBasis Y = MeasurementBasis.circular();

    private static double[] p(QubitState s, MeasurementBasis b) {
        return BornRule.probabilities(s, b);
    }

    @Test
    @DisplayName("basis states are deterministic in their own basis")
    void deterministicInOwnBasis() {
        assertArrayEquals(new double[] {1, 0}, p(QubitState.basis0(), Z), TOL);
        assertArrayEquals(new double[] {0, 1}, p(QubitState.basis1(), Z), TOL);
        assertArrayEquals(new double[] {1, 0}, p(QubitState.plus(), X), TOL);
        assertArrayEquals(new double[] {0, 1}, p(QubitState.minus(), X), TOL);
        assertArrayEquals(new double[] {1, 0}, p(QubitState.plusI(), Y), TOL);
        assertArrayEquals(new double[] {0, 1}, p(QubitState.minusI(), Y), TOL);
    }

    @Test
    @DisplayName("mutually unbiased bases: any state of one basis is 50/50 in the others")
    void mutuallyUnbiased() {
        double[] half = {0.5, 0.5};
        assertArrayEquals(half, p(QubitState.basis0(), X), TOL);
        assertArrayEquals(half, p(QubitState.basis0(), Y), TOL);
        assertArrayEquals(half, p(QubitState.plus(), Z), TOL);
        assertArrayEquals(half, p(QubitState.plus(), Y), TOL);
        assertArrayEquals(half, p(QubitState.plusI(), Z), TOL);
        assertArrayEquals(half, p(QubitState.plusI(), X), TOL);
    }

    @Test
    @DisplayName("Z-basis probabilities are |alpha|^2, |beta|^2 regardless of relative phase")
    void zBasisIgnoresRelativePhase() {
        double a = Math.sqrt(0.3);
        double b = Math.sqrt(0.7);
        for (double phi = -3; phi <= 3; phi += 0.5) {
            QubitState s = QubitState.of(Complex.of(a), Complex.cis(phi).times(b));
            assertArrayEquals(new double[] {0.3, 0.7}, p(s, Z), TOL);
        }
    }

    @Test
    @DisplayName("interference: (|0> + e^{i phi}|1>)/sqrt2 gives P(+) = (1 + cos phi)/2 in X")
    void interferenceInXBasis() {
        for (double phi = -Math.PI; phi <= Math.PI; phi += Math.PI / 12) {
            QubitState s = QubitState.of(Complex.of(1 / Math.sqrt(2)), Complex.cis(phi).times(1 / Math.sqrt(2)));
            double[] probs = p(s, X);
            assertEquals((1 + Math.cos(phi)) / 2, probs[0], TOL);
            assertEquals((1 - Math.cos(phi)) / 2, probs[1], TOL);
        }
    }

    @Test
    @DisplayName("interference: the same state gives P(+i) = (1 + sin phi)/2 in Y")
    void interferenceInYBasis() {
        for (double phi = -Math.PI; phi <= Math.PI; phi += Math.PI / 12) {
            QubitState s = QubitState.of(Complex.of(1 / Math.sqrt(2)), Complex.cis(phi).times(1 / Math.sqrt(2)));
            double[] probs = p(s, Y);
            assertEquals((1 + Math.sin(phi)) / 2, probs[0], TOL);
            assertEquals((1 - Math.sin(phi)) / 2, probs[1], TOL);
        }
    }

    @Test
    @DisplayName("a coherent superposition is NOT a classical 50/50 mixture")
    void superpositionDiffersFromMixture() {
        // A classical mixture of |0> and |1> with weights 1/2, 1/2 gives P(+) = 1/2*|<+|0>|^2 +
        // 1/2*|<+|1>|^2 = 1/2 in the X basis. The superposition |+> gives P(+) = 1.
        double mixtureP =
                0.5 * p(QubitState.basis0(), X)[0] + 0.5 * p(QubitState.basis1(), X)[0];
        double superpositionP = p(QubitState.plus(), X)[0];
        assertEquals(0.5, mixtureP, TOL);
        assertEquals(1.0, superpositionP, TOL);
    }

    @Test
    @DisplayName("probabilities sum to 1 for many random states in every basis")
    void probabilitiesSumToOne() {
        SplittableRandom rng = new SplittableRandom(42);
        for (int i = 0; i < 500; i++) {
            QubitState s =
                    QubitState.fromBloch(
                            rng.nextDouble() * Math.PI, (rng.nextDouble() - 0.5) * 4 * Math.PI);
            for (StandardBasis sb : StandardBasis.values()) {
                double[] probs = p(s, sb.basis());
                assertEquals(1.0, probs[0] + probs[1], TOL);
                assertTrue(probs[0] >= 0.0 && probs[1] >= 0.0);
            }
        }
    }

    @Test
    @DisplayName("probabilities are invariant under a global phase")
    void globalPhaseInvariant() {
        QubitState s = QubitState.fromBloch(0.9, 1.7);
        QubitState g =
                QubitState.of(s.alpha().times(Complex.cis(0.77)), s.beta().times(Complex.cis(0.77)));
        for (StandardBasis sb : StandardBasis.values()) {
            assertArrayEquals(p(s, sb.basis()), p(g, sb.basis()), TOL);
        }
    }

    @Test
    @DisplayName("measuring in the X basis equals applying H then measuring in the Z basis")
    void basisChangeConsistency() {
        SplittableRandom rng = new SplittableRandom(7);
        for (int i = 0; i < 200; i++) {
            QubitState s =
                    QubitState.fromBloch(rng.nextDouble() * Math.PI, rng.nextDouble() * 6.0 - 3.0);
            ComplexVector rotated = QubitOperators.HADAMARD.apply(s.vector());
            assertArrayEquals(p(s, X), BornRule.probabilities(rotated, Z), TOL);
        }
    }

    @Test
    @DisplayName("decay-prepared state reproduces the decay law in the Z basis")
    void decayStateBornProbabilities() {
        DecayModel m = new DecayModel(0.8);
        QubitState s = QubitState.fromDecay(m, 1.5, 0.4);
        double[] probs = p(s, Z);
        assertEquals(m.survivalProbability(1.5), probs[0], TOL);
        assertEquals(m.decayProbability(1.5), probs[1], TOL);
    }

    @Test
    @DisplayName("an un-normalised vector is rejected")
    void rejectsUnnormalised() {
        ComplexVector v = ComplexVector.of(Complex.of(2), Complex.ZERO);
        assertThrows(IllegalArgumentException.class, () -> BornRule.probabilities(v, Z));
    }

    @Test
    @DisplayName("a dimension mismatch is rejected")
    void rejectsDimensionMismatch() {
        assertThrows(
                IllegalArgumentException.class,
                () -> BornRule.probabilities(ComplexVector.basis(3, 0), Z));
    }
}
