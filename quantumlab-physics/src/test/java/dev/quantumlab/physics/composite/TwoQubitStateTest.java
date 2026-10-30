package dev.quantumlab.physics.composite;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.quantumlab.math.Complex;
import dev.quantumlab.math.ComplexMatrix;
import dev.quantumlab.math.ComplexVector;
import dev.quantumlab.physics.BornRule;
import dev.quantumlab.physics.MeasurementBasis;
import dev.quantumlab.physics.QubitOperators;
import dev.quantumlab.physics.QubitState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TwoQubitStateTest {

    private static final double TOL = 1e-12;
    private static final MeasurementBasis Z = MeasurementBasis.computational();
    private static final MeasurementBasis X = MeasurementBasis.hadamard();
    private static final MeasurementBasis Y = MeasurementBasis.circular();

    private static final TwoQubitState BELL = TwoQubitState.correlated(QubitState.plus());

    private static double sum(double[][] p) {
        return p[0][0] + p[0][1] + p[1][0] + p[1][1];
    }

    @Test
    @DisplayName("a state must be a normalised vector of dimension 4")
    void validation() {
        assertThrows(IllegalArgumentException.class, () -> TwoQubitState.of(ComplexVector.basis(2, 0)));
        assertThrows(IllegalArgumentException.class, () -> TwoQubitState.of(null));
        assertThrows(IllegalArgumentException.class, () ->
            TwoQubitState.of(ComplexVector.of(Complex.ONE, Complex.ONE, Complex.ZERO, Complex.ZERO)));
        assertThrows(IllegalArgumentException.class, () -> BELL.amplitude(2, 0));
        assertThrows(IllegalArgumentException.class, () -> BELL.jointProbabilities(null, Z));
    }

    @Test
    @DisplayName("amplitudes are ordered |00>, |01>, |10>, |11>")
    void ordering() {
        TwoQubitState s = TwoQubitState.product(QubitState.basis1(), QubitState.basis0());
        assertEquals(Complex.ONE, s.amplitude(1, 0));
        assertEquals(Complex.ZERO, s.amplitude(0, 0));
        assertEquals(Complex.ZERO, s.amplitude(0, 1));
        assertEquals(Complex.ZERO, s.amplitude(1, 1));
    }

    @Test
    @DisplayName("a product state is unentangled: C = 0, S = 0, purity 1")
    void productIsUnentangled() {
        TwoQubitState s = TwoQubitState.product(QubitState.plus(), QubitState.plusI());
        assertEquals(0.0, s.concurrence(), 1e-12);
        assertEquals(0.0, s.entanglementEntropy(), 1e-12);
        assertEquals(1.0, s.reducedPurity(), 1e-12);
        assertTrue(s.isProduct(1e-9));
    }

    @Test
    @DisplayName("(|00> + |11>)/sqrt2 is maximally entangled and each qubit alone is I/2")
    void bellState() {
        assertEquals(1.0, BELL.concurrence(), 1e-12);
        assertEquals(1.0, BELL.entanglementEntropy(), 1e-12);
        assertEquals(0.5, BELL.reducedPurity(), 1e-12);
        assertFalse(BELL.isProduct(1e-6));
        ComplexMatrix half = ComplexMatrix.identity(2).scale(Complex.of(0.5));
        assertTrue(BELL.reducedFirst().approxEquals(half, 1e-12));
        assertTrue(BELL.reducedSecond().approxEquals(half, 1e-12));
    }

    @Test
    @DisplayName("alpha|00> + beta|11> has concurrence 2|alpha beta| and entropy equal to the binary entropy")
    void partiallyEntangled() {
        QubitState n = QubitState.of(Complex.of(0.8, 0), Complex.of(0, 0.6));
        TwoQubitState s = TwoQubitState.correlated(n);
        assertEquals(2 * 0.8 * 0.6, s.concurrence(), 1e-12);
        double p = 0.64;
        double h = -(p * Math.log(p) + (1 - p) * Math.log(1 - p)) / Math.log(2);
        assertEquals(h, s.entanglementEntropy(), 1e-12);
        assertEquals(1.0 - 0.5 * s.concurrence() * s.concurrence(), s.reducedPurity(), 1e-12);
        // the reduced state is the diagonal mixture diag(|alpha|^2, |beta|^2)
        assertEquals(0.64, s.reducedFirst().get(0, 0).re(), 1e-12);
        assertEquals(0.36, s.reducedFirst().get(1, 1).re(), 1e-12);
        assertEquals(0.0, s.reducedFirst().get(0, 1).abs(), 1e-12);
    }

    @Test
    @DisplayName("correlating with a basis state is not entanglement")
    void basisStateStaysProduct() {
        assertEquals(0.0, TwoQubitState.correlated(QubitState.basis0()).concurrence(), 1e-12);
        assertEquals(0.0, TwoQubitState.correlated(QubitState.basis1()).concurrence(), 1e-12);
    }

    @Test
    @DisplayName("in the computational basis the two qubits always agree")
    void perfectAgreementInZ() {
        double[][] p = BELL.jointProbabilities(Z, Z);
        assertArrayEquals(new double[] {0.5, 0.0}, p[0], TOL);
        assertArrayEquals(new double[] {0.0, 0.5}, p[1], TOL);
    }

    @Test
    @DisplayName("in the Hadamard basis the entangled state also agrees; in the circular basis it always disagrees")
    void correlationsInOtherBases() {
        double[][] xx = BELL.jointProbabilities(X, X);
        assertEquals(0.5, xx[0][0], TOL);
        assertEquals(0.5, xx[1][1], TOL);
        assertEquals(0.0, xx[0][1], TOL);
        double[][] yy = BELL.jointProbabilities(Y, Y);
        assertEquals(0.0, yy[0][0], TOL);
        assertEquals(0.0, yy[1][1], TOL);
        assertEquals(0.5, yy[0][1], TOL);
        assertEquals(0.5, yy[1][0], TOL);
    }

    @Test
    @DisplayName("joint probabilities sum to 1 for any state and any pair of bases")
    void probabilitiesSumToOne() {
        ComplexVector raw = ComplexVector.of(
            Complex.of(1, 2), Complex.of(-0.5, 1), Complex.of(0.3, -0.7), Complex.of(2, 0.1));
        TwoQubitState s = TwoQubitState.of(raw.normalized());
        MeasurementBasis[] bases = {Z, X, Y};
        for (MeasurementBasis a : bases) {
            for (MeasurementBasis b : bases) {
                assertEquals(1.0, sum(s.jointProbabilities(a, b)), 1e-12);
            }
        }
    }

    @Test
    @DisplayName("a product state's joint distribution is the product of the single-qubit ones")
    void productFactorises() {
        QubitState a = QubitState.fromBloch(1.1, 0.4);
        QubitState b = QubitState.fromBloch(2.0, -1.0);
        double[][] p = TwoQubitState.product(a, b).jointProbabilities(X, Y);
        double[] pa = BornRule.probabilities(a, X);
        double[] pb = BornRule.probabilities(b, Y);
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2; j++) {
                assertEquals(pa[i] * pb[j], p[i][j], 1e-12);
            }
        }
    }

    @Test
    @DisplayName("measuring one qubit gives the same statistics whatever the other is doing (no signalling)")
    void marginalsDoNotDependOnTheOtherBasis() {
        QubitState n = QubitState.of(Complex.of(0.8, 0), Complex.of(0.6, 0));
        TwoQubitState s = TwoQubitState.correlated(n);
        double[][] withZ = s.jointProbabilities(X, Z);
        double[][] withY = s.jointProbabilities(X, Y);
        assertEquals(withZ[0][0] + withZ[0][1], withY[0][0] + withY[0][1], 1e-12);
    }

    @Test
    @DisplayName("correlators of the Bell state: <ZZ> = 1, <XX> = 1, <YY> = -1")
    void expectations() {
        ComplexMatrix zm = QubitOperators.PAULI_Z;
        ComplexMatrix xm = QubitOperators.PAULI_X;
        ComplexMatrix ym = QubitOperators.PAULI_Y;
        assertEquals(1.0, BELL.expectation(zm, zm), 1e-12);
        assertEquals(1.0, BELL.expectation(xm, xm), 1e-12);
        assertEquals(-1.0, BELL.expectation(ym, ym), 1e-12);
        assertEquals(0.0, BELL.expectation(zm, xm), 1e-12);
        assertThrows(IllegalArgumentException.class, () -> BELL.expectation(ComplexMatrix.identity(3), zm));
    }

    @Test
    @DisplayName("the expectation equals the sum of outcome-sign times joint probability")
    void expectationMatchesProbabilities() {
        QubitState n = QubitState.of(Complex.of(0.6, 0), Complex.of(0, 0.8));
        TwoQubitState s = TwoQubitState.correlated(n);
        double[][] p = s.jointProbabilities(Z, Z);
        double viaP = p[0][0] - p[0][1] - p[1][0] + p[1][1];
        assertEquals(viaP, s.expectation(QubitOperators.PAULI_Z, QubitOperators.PAULI_Z), 1e-12);
    }

    @Test
    @DisplayName("correlated() keeps the nucleus's relative phase: for (|00> + i|11>)/sqrt2, <XY> = <YX> = 1")
    void relativePhaseIsKept() {
        TwoQubitState s = TwoQubitState.correlated(QubitState.plusI());
        assertEquals(Complex.of(0, Math.sqrt(0.5)).im(), s.amplitude(1, 1).im(), 1e-12);
        assertEquals(1.0, s.expectation(QubitOperators.PAULI_X, QubitOperators.PAULI_Y), 1e-12);
        assertEquals(1.0, s.expectation(QubitOperators.PAULI_Y, QubitOperators.PAULI_X), 1e-12);
        assertEquals(0.0, s.expectation(QubitOperators.PAULI_X, QubitOperators.PAULI_X), 1e-12);
    }
}
