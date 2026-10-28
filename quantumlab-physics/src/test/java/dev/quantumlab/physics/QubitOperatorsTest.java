package dev.quantumlab.physics;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.quantumlab.math.Complex;
import dev.quantumlab.math.ComplexMatrix;
import dev.quantumlab.math.Tolerances;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class QubitOperatorsTest {

    private static final double TOL = Tolerances.STRICT;
    private static final ComplexMatrix I2 = ComplexMatrix.identity(2);
    private static final ComplexMatrix X = QubitOperators.PAULI_X;
    private static final ComplexMatrix Y = QubitOperators.PAULI_Y;
    private static final ComplexMatrix Z = QubitOperators.PAULI_Z;
    private static final ComplexMatrix H = QubitOperators.HADAMARD;

    @Test
    @DisplayName("Pauli matrices are Hermitian and unitary")
    void paulisHermitianUnitary() {
        for (ComplexMatrix m : new ComplexMatrix[] {X, Y, Z}) {
            assertTrue(m.isHermitian(TOL));
            assertTrue(m.isUnitary(TOL));
        }
    }

    @Test
    @DisplayName("Pauli matrices square to the identity")
    void paulisSquareToIdentity() {
        for (ComplexMatrix m : new ComplexMatrix[] {X, Y, Z}) {
            assertTrue(m.multiply(m).approxEquals(I2, TOL));
        }
    }

    @Test
    @DisplayName("XY = iZ, YZ = iX, ZX = iY")
    void paulisMultiplicationTable() {
        assertTrue(X.multiply(Y).approxEquals(Z.scale(Complex.I), TOL));
        assertTrue(Y.multiply(Z).approxEquals(X.scale(Complex.I), TOL));
        assertTrue(Z.multiply(X).approxEquals(Y.scale(Complex.I), TOL));
    }

    @Test
    @DisplayName("Paulis anticommute: XY = -YX")
    void paulisAnticommute() {
        assertTrue(X.multiply(Y).approxEquals(Y.multiply(X).scale(Complex.of(-1)), TOL));
    }

    @Test
    @DisplayName("Hadamard is unitary, Hermitian, self-inverse, and H = (X + Z)/sqrt(2)")
    void hadamard() {
        assertTrue(H.isUnitary(TOL));
        assertTrue(H.isHermitian(TOL));
        assertTrue(H.multiply(H).approxEquals(I2, TOL));
        assertTrue(H.approxEquals(X.plus(Z).scale(Complex.of(1 / Math.sqrt(2))), TOL));
    }

    @Test
    @DisplayName("H Z H = X: Hadamard swaps the Z and X bases")
    void hadamardConjugation() {
        assertTrue(H.multiply(Z).multiply(H).approxEquals(X, TOL));
        assertTrue(H.multiply(X).multiply(H).approxEquals(Z, TOL));
    }

    @Test
    @DisplayName("phase(phi) is unitary, and Hermitian only for phi = 0 or pi")
    void phaseGate() {
        for (double phi = -3; phi <= 3; phi += 0.5) {
            assertTrue(QubitOperators.phase(phi).isUnitary(TOL));
        }
        assertTrue(QubitOperators.phase(0).isHermitian(TOL));
        assertTrue(QubitOperators.phase(Math.PI).isHermitian(TOL));
        assertFalse(QubitOperators.phase(1.0).isHermitian(TOL));
    }

    @Test
    @DisplayName("phase(phi) rotates the relative phase of |+> by phi and leaves probabilities in Z alone")
    void phaseGateOnPlus() {
        double phi = 0.9;
        QubitState rotated =
                QubitState.fromVector(QubitOperators.phase(phi).apply(QubitState.plus().vector()));
        org.junit.jupiter.api.Assertions.assertEquals(
                phi, rotated.relativePhase().getAsDouble(), TOL);
        org.junit.jupiter.api.Assertions.assertEquals(0.5, rotated.probability0(), TOL);
    }
}
