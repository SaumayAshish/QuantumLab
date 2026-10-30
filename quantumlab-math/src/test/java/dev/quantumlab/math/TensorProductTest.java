package dev.quantumlab.math;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TensorProductTest {

    private static final double TOL = Tolerances.STRICT;

    private static ComplexMatrix pauliX() {
        return ComplexMatrix.of(new Complex[][] {
            {Complex.ZERO, Complex.ONE},
            {Complex.ONE, Complex.ZERO}
        });
    }

    private static ComplexMatrix pauliZ() {
        return ComplexMatrix.of(new Complex[][] {
            {Complex.ONE, Complex.ZERO},
            {Complex.ZERO, Complex.of(-1, 0)}
        });
    }

    @Test
    @DisplayName("tensor of basis vectors puts the 1 at index 2a+b")
    void tensorBasisOrdering() {
        ComplexVector t = ComplexVector.basis(2, 1).tensor(ComplexVector.basis(2, 0));
        assertEquals(4, t.dimension());
        assertTrue(t.approxEquals(ComplexVector.basis(4, 2), TOL));
    }

    @Test
    @DisplayName("tensor multiplies complex amplitudes")
    void tensorComplexValues() {
        ComplexVector a = ComplexVector.of(Complex.of(1, 1), Complex.of(0, 2));
        ComplexVector b = ComplexVector.of(Complex.of(3, 0), Complex.of(1, -1));
        ComplexVector t = a.tensor(b);
        // (1+i)(1-i) = 2 ; (0+2i)(3) = 6i
        assertEquals(Complex.of(3, 3), t.get(0));
        assertEquals(Complex.of(2, 0), t.get(1));
        assertEquals(Complex.of(0, 6), t.get(2));
        assertEquals(Complex.of(2, 2), t.get(3));
    }

    @Test
    @DisplayName("tensor squared norm is the product of squared norms")
    void tensorNorm() {
        ComplexVector a = ComplexVector.of(Complex.of(1, 2), Complex.of(-3, 0.5), Complex.of(0, 1));
        ComplexVector b = ComplexVector.of(Complex.of(2, -1), Complex.of(0.5, 0.5));
        assertEquals(a.normSquared() * b.normSquared(), a.tensor(b).normSquared(), 1e-10);
        assertEquals(6, a.tensor(b).dimension());
    }

    @Test
    @DisplayName("kronecker of identities is the larger identity")
    void kroneckerIdentity() {
        ComplexMatrix k = ComplexMatrix.identity(2).kronecker(ComplexMatrix.identity(3));
        assertEquals(6, k.rows());
        assertEquals(6, k.cols());
        assertTrue(k.approxEquals(ComplexMatrix.identity(6), TOL));
    }

    @Test
    @DisplayName("kronecker has the right shape and entries, and (A⊗B)(u⊗v) = Au⊗Bv")
    void kroneckerEntriesAndMixedProduct() {
        ComplexMatrix zx = pauliZ().kronecker(pauliX());
        // Z⊗X = [[X, 0], [0, -X]]
        assertEquals(Complex.ONE, zx.get(0, 1));
        assertEquals(Complex.ONE, zx.get(1, 0));
        assertEquals(Complex.of(-1, 0), zx.get(2, 3));
        assertEquals(Complex.of(-1, 0), zx.get(3, 2));
        assertEquals(Complex.ZERO, zx.get(0, 3));

        ComplexMatrix a = ComplexMatrix.of(new Complex[][] {
            {Complex.of(1, 1), Complex.of(0, 2), Complex.of(1, 0)},
            {Complex.of(2, 0), Complex.of(-1, 1), Complex.of(0, 0)}
        });
        ComplexMatrix b = ComplexMatrix.of(new Complex[][] {
            {Complex.of(0, 1), Complex.of(2, -1)},
            {Complex.of(1, 0), Complex.of(3, 3)}
        });
        ComplexMatrix k = a.kronecker(b);
        assertEquals(4, k.rows());
        assertEquals(6, k.cols());

        ComplexVector u = ComplexVector.of(Complex.of(1, 0), Complex.of(0, 1), Complex.of(2, -1));
        ComplexVector v = ComplexVector.of(Complex.of(1, 2), Complex.of(-1, 0));
        ComplexVector left = k.apply(u.tensor(v));
        ComplexVector right = a.apply(u).tensor(b.apply(v));
        assertTrue(left.approxEquals(right, 1e-10));
    }
}
