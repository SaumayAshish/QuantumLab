package dev.quantumlab.physics.composite;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.quantumlab.math.Complex;
import dev.quantumlab.math.ComplexMatrix;
import dev.quantumlab.physics.QubitOperators;
import dev.quantumlab.physics.QubitState;
import java.util.SplittableRandom;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ChshTest {

    private static final TwoQubitState BELL = TwoQubitState.correlated(QubitState.plus());

    private static double chsh(Chsh.Correlator c, double[] s) {
        return Chsh.value(c, s[0], s[1], s[2], s[3]);
    }

    @Test
    @DisplayName("the observable cos(t) Z + sin(t) X is Hermitian with eigenvalues +-1")
    void observable() {
        for (double t : new double[] {0.0, 0.7, Math.PI / 2, 2.4, -1.3}) {
            ComplexMatrix a = Chsh.observable(t);
            assertTrue(a.isHermitian(1e-12));
            assertTrue(a.multiply(a).approxEquals(ComplexMatrix.identity(2), 1e-12), "A^2 = I at t=" + t);
        }
        assertTrue(Chsh.observable(0).approxEquals(QubitOperators.PAULI_Z, 1e-12));
        assertTrue(Chsh.observable(Math.PI / 2).approxEquals(QubitOperators.PAULI_X, 1e-12));
        assertThrows(IllegalArgumentException.class, () -> Chsh.observable(Double.NaN));
    }

    @Test
    @DisplayName("for (|00> + |11>)/sqrt2 the correlation is cos(thetaA - thetaB)")
    void bellCorrelation() {
        for (double a : new double[] {0.0, 0.5, 1.9}) {
            for (double b : new double[] {-0.4, 0.0, 2.2}) {
                double e = BELL.expectation(Chsh.observable(a), Chsh.observable(b));
                assertEquals(Math.cos(a - b), e, 1e-12);
            }
        }
    }

    @Test
    @DisplayName("the entangled state reaches Tsirelson's bound 2 sqrt 2 > 2")
    void bellViolates() {
        double[] s = Chsh.optimalSettings(1.0);
        double value = chsh(BELL::expectation, s);
        assertEquals(Chsh.TSIRELSON_BOUND, value, 1e-12);
        assertTrue(value > Chsh.CLASSICAL_BOUND);
    }

    @Test
    @DisplayName("the classical mixture with the same Z statistics never exceeds 2, even with the best settings")
    void mixtureDoesNotViolate() {
        ClassicalCorrelation mix = new ClassicalCorrelation(0.5);
        assertEquals(2.0, chsh(mix::expectation, Chsh.optimalSettings(0.0)), 1e-12);
        SplittableRandom rng = new SplittableRandom(7);
        for (int k = 0; k < 2000; k++) {
            double[] s = new double[4];
            for (int i = 0; i < 4; i++) {
                s[i] = rng.nextDouble(-Math.PI, Math.PI);
            }
            double p0 = rng.nextDouble();
            double v = chsh(new ClassicalCorrelation(p0)::expectation, s);
            assertTrue(Math.abs(v) <= Chsh.CLASSICAL_BOUND + 1e-12, "|S| = " + v);
        }
    }

    @Test
    @DisplayName("product states never exceed 2")
    void productDoesNotViolate() {
        SplittableRandom rng = new SplittableRandom(11);
        for (int k = 0; k < 2000; k++) {
            TwoQubitState s = TwoQubitState.product(
                QubitState.fromBloch(rng.nextDouble(0, Math.PI), rng.nextDouble(0, 2 * Math.PI)),
                QubitState.fromBloch(rng.nextDouble(0, Math.PI), rng.nextDouble(0, 2 * Math.PI)));
            double v = Chsh.value(s::expectation,
                rng.nextDouble(-Math.PI, Math.PI), rng.nextDouble(-Math.PI, Math.PI),
                rng.nextDouble(-Math.PI, Math.PI), rng.nextDouble(-Math.PI, Math.PI));
            assertTrue(Math.abs(v) <= Chsh.CLASSICAL_BOUND + 1e-12, "|S| = " + v);
        }
    }

    @Test
    @DisplayName("partial entanglement: the optimal in-plane value is 2 sqrt(1 + c^2) and no setting beats it")
    void partialEntanglement() {
        QubitState n = QubitState.of(Complex.of(0.8, 0), Complex.of(0.6, 0));
        TwoQubitState s = TwoQubitState.correlated(n);
        double c = 2 * 0.8 * 0.6;
        double[] best = Chsh.optimalSettings(c);
        assertEquals(Chsh.maximumInPlane(c), chsh(s::expectation, best), 1e-12);
        assertTrue(Chsh.maximumInPlane(c) > 2.0);
        assertTrue(Chsh.maximumInPlane(c) < Chsh.TSIRELSON_BOUND);

        SplittableRandom rng = new SplittableRandom(3);
        for (int k = 0; k < 5000; k++) {
            double v = Chsh.value(s::expectation,
                rng.nextDouble(-Math.PI, Math.PI), rng.nextDouble(-Math.PI, Math.PI),
                rng.nextDouble(-Math.PI, Math.PI), rng.nextDouble(-Math.PI, Math.PI));
            assertTrue(Math.abs(v) <= Chsh.maximumInPlane(c) + 1e-12, "|S| = " + v);
        }
    }

    @Test
    @DisplayName("optimal settings are a = 0, a' = pi/2, b = atan c, b' = -atan c")
    void optimalSettingsValues() {
        assertArrayEquals(new double[] {0.0, Math.PI / 2, Math.PI / 4, -Math.PI / 4}, Chsh.optimalSettings(1.0), 1e-15);
        assertEquals(Chsh.TSIRELSON_BOUND, Chsh.maximumInPlane(1.0), 1e-15);
        assertEquals(2.0, Chsh.maximumInPlane(0.0), 1e-15);
        assertThrows(IllegalArgumentException.class, () -> Chsh.optimalSettings(Double.POSITIVE_INFINITY));
    }

    @Test
    @DisplayName("the observable with an azimuth is Hermitian with eigenvalues +-1 and reduces to Y, X, Z at the axes")
    void observableWithAzimuth() {
        for (double t : new double[] {0.0, 0.9, 2.2}) {
            for (double az : new double[] {0.0, 1.1, -2.5}) {
                ComplexMatrix a = Chsh.observable(t, az);
                assertTrue(a.isHermitian(1e-12));
                assertTrue(a.multiply(a).approxEquals(ComplexMatrix.identity(2), 1e-12));
            }
        }
        assertTrue(Chsh.observable(Math.PI / 2, Math.PI / 2).approxEquals(QubitOperators.PAULI_Y, 1e-12));
        assertTrue(Chsh.observable(Math.PI / 2, 0).approxEquals(QubitOperators.PAULI_X, 1e-12));
        assertTrue(Chsh.observable(0.7).approxEquals(Chsh.observable(0.7, 0.0), 1e-15));
        assertThrows(IllegalArgumentException.class, () -> Chsh.observable(0.1, Double.NaN));
    }

    @Test
    @DisplayName("with the detector's axes turned by the relative phase, any phase reaches 2 sqrt(1 + C^2), and nothing exceeds it")
    void phasedStateReachesTheBoundWithMatchedAzimuth() {
        double[][] cases = {{0.5, 0.0}, {0.5, 1.2}, {0.8, 2.5}, {0.64, -0.7}, {0.9, 3.0}};
        SplittableRandom rng = new SplittableRandom(5);
        for (double[] c : cases) {
            double p0 = c[0];
            double phi = c[1];
            QubitState n = QubitState.of(
                Complex.of(Math.sqrt(p0), 0),
                Complex.of(Math.sqrt(1 - p0) * Math.cos(phi), Math.sqrt(1 - p0) * Math.sin(phi)));
            TwoQubitState s = TwoQubitState.correlated(n);
            double conc = s.concurrence();
            assertEquals(2 * Math.sqrt(p0 * (1 - p0)), conc, 1e-12);
            double t = Math.atan(conc);
            double matched = Chsh.value(s::expectation,
                Chsh.observable(0), Chsh.observable(Math.PI / 2),
                Chsh.observable(t, phi), Chsh.observable(-t, phi));
            assertEquals(Chsh.maximumInPlane(conc), matched, 1e-12, "p0=" + p0 + " phi=" + phi);

            for (int k = 0; k < 3000; k++) {
                double v = Chsh.value(s::expectation,
                    Chsh.observable(rng.nextDouble(0, Math.PI), rng.nextDouble(0, 2 * Math.PI)),
                    Chsh.observable(rng.nextDouble(0, Math.PI), rng.nextDouble(0, 2 * Math.PI)),
                    Chsh.observable(rng.nextDouble(0, Math.PI), rng.nextDouble(0, 2 * Math.PI)),
                    Chsh.observable(rng.nextDouble(0, Math.PI), rng.nextDouble(0, 2 * Math.PI)));
                assertTrue(Math.abs(v) <= Chsh.maximumInPlane(conc) + 1e-12, "|S| = " + v);
            }
        }
    }

    @Test
    @DisplayName("the classical mixture never exceeds 2 for settings in any directions")
    void mixtureNeverExceedsTwoInThreeDimensions() {
        SplittableRandom rng = new SplittableRandom(13);
        for (int k = 0; k < 3000; k++) {
            ClassicalCorrelation mix = new ClassicalCorrelation(rng.nextDouble());
            double v = Chsh.value(mix::expectation,
                Chsh.observable(rng.nextDouble(0, Math.PI), rng.nextDouble(0, 2 * Math.PI)),
                Chsh.observable(rng.nextDouble(0, Math.PI), rng.nextDouble(0, 2 * Math.PI)),
                Chsh.observable(rng.nextDouble(0, Math.PI), rng.nextDouble(0, 2 * Math.PI)),
                Chsh.observable(rng.nextDouble(0, Math.PI), rng.nextDouble(0, 2 * Math.PI)));
            assertTrue(Math.abs(v) <= Chsh.CLASSICAL_BOUND + 1e-12, "|S| = " + v);
        }
    }
}
