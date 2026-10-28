package dev.quantumlab.math;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ComplexMatrixTest {

    private static final double TOL = Tolerances.STRICT;

    private static ComplexMatrix pauliY() {
        return ComplexMatrix.of(
                new Complex[][] {
                        {Complex.ZERO, Complex.of(0, -1)},
                        {Complex.of(0, 1), Complex.ZERO}
                });
    }

    private static ComplexMatrix hadamard() {
        double s = 1.0 / Math.sqrt(2.0);
        return ComplexMatrix.of(
                new Complex[][] {
                        {Complex.of(s), Complex.of(s)},
                        {Complex.of(s), Complex.of(-s)}
                });
    }

    private static ComplexMatrix sample3x2() {
        return ComplexMatrix.of(
                new Complex[][] {
                        {Complex.of(1, 1), Complex.of(2, 0)},
                        {Complex.of(0, -1), Complex.of(3, 2)},
                        {Complex.of(-1, 0.5), Complex.of(0, 4)}
                });
    }

    @Test
    @DisplayName("shape and element access")
    void shape() {
        ComplexMatrix m = sample3x2();
        assertEquals(3, m.rows());
        assertEquals(2, m.cols());
        assertFalse(m.isSquare());
        assertEquals(Complex.of(3, 2), m.get(1, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> m.get(3, 0));
    }

    @Test
    @DisplayName("ragged and empty inputs are rejected")
    void rejectsBadShapes() {
        assertThrows(
                IllegalArgumentException.class,
                () -> ComplexMatrix.of(new Complex[][] {{Complex.ONE}, {Complex.ONE, Complex.ONE}}));
        assertThrows(IllegalArgumentException.class, () -> ComplexMatrix.of(new Complex[0][0]));
    }

    @Test
    @DisplayName("identity is neutral for multiplication")
    void identityIsNeutral() {
        ComplexMatrix m = sample3x2();
        assertTrue(ComplexMatrix.identity(3).multiply(m).approxEquals(m, TOL));
        assertTrue(m.multiply(ComplexMatrix.identity(2)).approxEquals(m, TOL));
    }

    @Test
    @DisplayName("2x2 product matches a hand calculation")
    void productByHand() {
        ComplexMatrix a =
                ComplexMatrix.of(
                        new Complex[][] {
                                {Complex.of(1, 0), Complex.of(0, 1)},
                                {Complex.of(2, 0), Complex.of(1, 1)}
                        });
        ComplexMatrix b =
                ComplexMatrix.of(
                        new Complex[][] {
                                {Complex.of(1, 1), Complex.of(0, 0)},
                                {Complex.of(0, 0), Complex.of(1, -1)}
                        });
        // row0: [1*(1+i), i*(1-i)] = [1+i, i+1]; row1: [2*(1+i), (1+i)(1-i)] = [2+2i, 2]
        ComplexMatrix expected =
                ComplexMatrix.of(
                        new Complex[][] {
                                {Complex.of(1, 1), Complex.of(1, 1)},
                                {Complex.of(2, 2), Complex.of(2, 0)}
                        });
        assertTrue(a.multiply(b).approxEquals(expected, TOL));
    }

    @Test
    @DisplayName("incompatible shapes are rejected")
    void multiplyShapeMismatch() {
        assertThrows(
                IllegalArgumentException.class,
                () -> sample3x2().multiply(sample3x2()));
    }

    @Test
    @DisplayName("dagger is the conjugate transpose")
    void dagger() {
        ComplexMatrix m =
                ComplexMatrix.of(
                        new Complex[][] {
                                {Complex.of(1, 0), Complex.of(0, 1)},
                                {Complex.of(0, 0), Complex.of(2, 0)}
                        });
        ComplexMatrix expected =
                ComplexMatrix.of(
                        new Complex[][] {
                                {Complex.of(1, 0), Complex.of(0, 0)},
                                {Complex.of(0, -1), Complex.of(2, 0)}
                        });
        assertTrue(m.dagger().approxEquals(expected, TOL));
    }

    @Test
    @DisplayName("dagger changes shape for non-square matrices and is an involution")
    void daggerShapeAndInvolution() {
        ComplexMatrix m = sample3x2();
        ComplexMatrix d = m.dagger();
        assertEquals(2, d.rows());
        assertEquals(3, d.cols());
        assertTrue(d.dagger().approxEquals(m, TOL));
    }

    @Test
    @DisplayName("(AB)† = B† A†")
    void daggerReversesProducts() {
        ComplexMatrix a = sample3x2();
        ComplexMatrix b =
                ComplexMatrix.of(
                        new Complex[][] {
                                {Complex.of(1, -1), Complex.of(0, 2), Complex.of(3, 0)},
                                {Complex.of(0, 1), Complex.of(-2, 2), Complex.of(1, 1)}
                        });
        ComplexMatrix lhs = a.multiply(b).dagger();
        ComplexMatrix rhs = b.dagger().multiply(a.dagger());
        assertTrue(lhs.approxEquals(rhs, TOL));
    }

    @Test
    @DisplayName("Pauli-Y is Hermitian and unitary")
    void pauliYProperties() {
        assertTrue(pauliY().isHermitian(TOL));
        assertTrue(pauliY().isUnitary(TOL));
    }

    @Test
    @DisplayName("Hadamard is Hermitian, unitary and self-inverse")
    void hadamardProperties() {
        ComplexMatrix h = hadamard();
        assertTrue(h.isHermitian(TOL));
        assertTrue(h.isUnitary(TOL));
        assertTrue(h.multiply(h).approxEquals(ComplexMatrix.identity(2), TOL));
    }

    @Test
    @DisplayName("diag(1, i) is unitary but NOT Hermitian (and A*A != I, so isUnitary must use A†A)")
    void unitaryButNotHermitian() {
        ComplexMatrix s =
                ComplexMatrix.of(
                        new Complex[][] {
                                {Complex.ONE, Complex.ZERO},
                                {Complex.ZERO, Complex.I}
                        });
        assertTrue(s.isUnitary(TOL));
        assertFalse(s.isHermitian(TOL));
        // Guards against an implementation that checks A*A = I instead of A†A = I.
        assertFalse(s.multiply(s).approxEquals(ComplexMatrix.identity(2), TOL));
    }

    @Test
    @DisplayName("a Hermitian matrix that is not unitary is classified correctly")
    void hermitianButNotUnitary() {
        ComplexMatrix m =
                ComplexMatrix.of(
                        new Complex[][] {
                                {Complex.of(2), Complex.ZERO},
                                {Complex.ZERO, Complex.of(1)}
                        });
        assertTrue(m.isHermitian(TOL));
        assertFalse(m.isUnitary(TOL));
    }

    @Test
    @DisplayName("a non-Hermitian, non-unitary matrix is classified correctly")
    void negativeClassification() {
        ComplexMatrix m =
                ComplexMatrix.of(
                        new Complex[][] {
                                {Complex.of(1, 0), Complex.of(1, 0)},
                                {Complex.of(0, 0), Complex.of(1, 0)}
                        });
        assertFalse(m.isHermitian(TOL));
        assertFalse(m.isUnitary(TOL));
        assertFalse(sample3x2().isHermitian(TOL)); // not square
    }

    @Test
    @DisplayName("apply: Hadamard maps |0> to (|0>+|1>)/sqrt(2)")
    void applyHadamard() {
        double s = 1.0 / Math.sqrt(2.0);
        ComplexVector out = hadamard().apply(ComplexVector.basis(2, 0));
        assertTrue(out.approxEquals(ComplexVector.of(Complex.of(s), Complex.of(s)), TOL));
    }

    @Test
    @DisplayName("apply preserves the norm for a unitary matrix")
    void unitaryPreservesNorm() {
        ComplexVector v = ComplexVector.of(Complex.of(0.3, -1.2), Complex.of(2.0, 0.7));
        assertEquals(v.norm(), pauliY().apply(v).norm(), TOL);
        assertEquals(v.norm(), hadamard().apply(v).norm(), TOL);
    }

    @Test
    @DisplayName("apply agrees with multiplying by a one-column matrix")
    void applyMatchesMatrixProduct() {
        ComplexMatrix m = sample3x2();
        ComplexVector v = ComplexVector.of(Complex.of(1, -1), Complex.of(0.5, 2));
        ComplexMatrix column =
                ComplexMatrix.of(new Complex[][] {{v.get(0)}, {v.get(1)}});
        ComplexMatrix viaProduct = m.multiply(column);
        ComplexVector viaApply = m.apply(v);
        for (int r = 0; r < 3; r++) {
            assertTrue(viaApply.get(r).approxEquals(viaProduct.get(r, 0), TOL));
        }
    }

    @Test
    @DisplayName("apply rejects a vector of the wrong dimension")
    void applyDimensionMismatch() {
        assertThrows(IllegalArgumentException.class, () -> sample3x2().apply(ComplexVector.zeros(3)));
    }

    @Test
    @DisplayName("plus and scale")
    void plusAndScale() {
        ComplexMatrix i2 = ComplexMatrix.identity(2);
        ComplexMatrix twice = i2.plus(i2);
        assertTrue(twice.approxEquals(i2.scale(Complex.of(2, 0)), TOL));
        assertEquals(Complex.of(0, 1), i2.scale(Complex.I).get(0, 0));
    }
}
