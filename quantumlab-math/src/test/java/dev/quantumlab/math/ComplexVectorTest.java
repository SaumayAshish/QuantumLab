package dev.quantumlab.math;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ComplexVectorTest {

    private static final double TOL = Tolerances.STRICT;

    @Test
    @DisplayName("dimension and element access")
    void basics() {
        ComplexVector v = ComplexVector.of(Complex.of(1, 2), Complex.of(3, 4));
        assertEquals(2, v.dimension());
        assertEquals(Complex.of(1, 2), v.get(0));
        assertEquals(3.0, v.re(1), 0.0);
        assertEquals(4.0, v.im(1), 0.0);
    }

    @Test
    @DisplayName("an empty vector is rejected")
    void rejectsEmpty() {
        assertThrows(IllegalArgumentException.class, () -> ComplexVector.of());
        assertThrows(IllegalArgumentException.class, () -> ComplexVector.zeros(0));
    }

    @Test
    @DisplayName("fromParts copies its input (vector is immutable)")
    void defensiveCopy() {
        double[] re = {1, 2};
        double[] im = {3, 4};
        ComplexVector v = ComplexVector.fromParts(re, im);
        re[0] = 99;
        im[1] = 99;
        assertEquals(1.0, v.re(0), 0.0);
        assertEquals(4.0, v.im(1), 0.0);
    }

    @Test
    @DisplayName("fromParts rejects mismatched lengths and non-finite values")
    void fromPartsValidation() {
        assertThrows(
                IllegalArgumentException.class,
                () -> ComplexVector.fromParts(new double[] {1}, new double[] {1, 2}));
        assertThrows(
                IllegalArgumentException.class,
                () -> ComplexVector.fromParts(new double[] {Double.NaN}, new double[] {0}));
    }

    @Test
    @DisplayName("standard basis vectors are orthonormal")
    void basisOrthonormal() {
        int n = 4;
        for (int a = 0; a < n; a++) {
            for (int b = 0; b < n; b++) {
                Complex ip = ComplexVector.basis(n, a).innerProduct(ComplexVector.basis(n, b));
                assertEquals(a == b ? 1.0 : 0.0, ip.re(), TOL);
                assertEquals(0.0, ip.im(), TOL);
            }
        }
    }

    @Test
    @DisplayName("basis index out of range is rejected")
    void basisRange() {
        assertThrows(IllegalArgumentException.class, () -> ComplexVector.basis(2, 2));
        assertThrows(IllegalArgumentException.class, () -> ComplexVector.basis(2, -1));
    }

    @Test
    @DisplayName("inner product is conjugate-linear in the FIRST argument")
    void innerProductConjugateLinearInFirst() {
        ComplexVector v = ComplexVector.of(Complex.of(1, 2), Complex.of(-1, 0.5));
        ComplexVector w = ComplexVector.of(Complex.of(0, 1), Complex.of(2, -3));
        Complex base = v.innerProduct(w);

        // <(i v) | w> = conj(i) <v|w> = -i <v|w>
        Complex lhs = v.scale(Complex.I).innerProduct(w);
        Complex rhs = Complex.I.conjugate().times(base);
        assertTrue(lhs.approxEquals(rhs, TOL));
    }

    @Test
    @DisplayName("inner product is LINEAR in the second argument")
    void innerProductLinearInSecond() {
        ComplexVector v = ComplexVector.of(Complex.of(1, 2), Complex.of(-1, 0.5));
        ComplexVector w = ComplexVector.of(Complex.of(0, 1), Complex.of(2, -3));
        Complex base = v.innerProduct(w);

        // <v | (i w)> = i <v|w>
        Complex lhs = v.innerProduct(w.scale(Complex.I));
        Complex rhs = Complex.I.times(base);
        assertTrue(lhs.approxEquals(rhs, TOL));
    }

    @Test
    @DisplayName("conjugate symmetry: <v|w> = conj(<w|v>)")
    void conjugateSymmetry() {
        ComplexVector v = ComplexVector.of(Complex.of(1, 2), Complex.of(-1, 0.5));
        ComplexVector w = ComplexVector.of(Complex.of(0, 1), Complex.of(2, -3));
        assertTrue(v.innerProduct(w).approxEquals(w.innerProduct(v).conjugate(), TOL));
    }

    @Test
    @DisplayName("<v|v> is real, non-negative, and equals the squared norm")
    void selfInnerProduct() {
        ComplexVector v = ComplexVector.of(Complex.of(3, 4), Complex.of(0, -12));
        Complex ip = v.innerProduct(v);
        assertEquals(0.0, ip.im(), TOL);
        assertEquals(25.0 + 144.0, ip.re(), TOL);
        assertEquals(ip.re(), v.normSquared(), TOL);
        assertEquals(13.0, v.norm(), TOL);
    }

    @Test
    @DisplayName("(|0> + i|1>)/sqrt(2) is normalised")
    void normalisedExample() {
        double s = 1.0 / Math.sqrt(2.0);
        ComplexVector v = ComplexVector.of(Complex.of(s, 0), Complex.of(0, s));
        assertTrue(v.isNormalized(Tolerances.NORMALISATION));
    }

    @Test
    @DisplayName("normalized() yields a unit vector in the same direction")
    void normalizing() {
        ComplexVector v = ComplexVector.of(Complex.of(3, 0), Complex.of(0, 4));
        ComplexVector u = v.normalized();
        assertTrue(u.isNormalized(TOL));
        assertEquals(0.6, u.re(0), TOL);
        assertEquals(0.8, u.im(1), TOL);
        assertFalse(v.isNormalized(Tolerances.NORMALISATION));
    }

    @Test
    @DisplayName("normalising the zero vector is rejected")
    void normalizeZero() {
        assertThrows(ArithmeticException.class, () -> ComplexVector.zeros(3).normalized());
    }

    @Test
    @DisplayName("plus, minus and scale")
    void algebra() {
        ComplexVector a = ComplexVector.of(Complex.of(1, 1), Complex.of(2, 0));
        ComplexVector b = ComplexVector.of(Complex.of(0, 1), Complex.of(-2, 5));
        assertEquals(ComplexVector.of(Complex.of(1, 2), Complex.of(0, 5)), a.plus(b));
        assertEquals(ComplexVector.of(Complex.of(1, 0), Complex.of(4, -5)), a.minus(b));
        assertEquals(ComplexVector.of(Complex.of(2, 2), Complex.of(4, 0)), a.scale(2.0));
        assertEquals(ComplexVector.of(Complex.of(-1, 1), Complex.of(0, 2)), a.scale(Complex.I));
    }

    @Test
    @DisplayName("dimension mismatch is rejected")
    void dimensionMismatch() {
        ComplexVector a = ComplexVector.zeros(2);
        ComplexVector b = ComplexVector.zeros(3);
        assertThrows(IllegalArgumentException.class, () -> a.plus(b));
        assertThrows(IllegalArgumentException.class, () -> a.innerProduct(b));
    }

    @Test
    @DisplayName("Cauchy-Schwarz: |<v|w>|^2 <= <v|v><w|w>")
    void cauchySchwarz() {
        ComplexVector v = ComplexVector.of(Complex.of(1, 2), Complex.of(-1, 0.5), Complex.of(0, 3));
        ComplexVector w = ComplexVector.of(Complex.of(0, 1), Complex.of(2, -3), Complex.of(1, 1));
        double lhs = v.innerProduct(w).abs2();
        double rhs = v.normSquared() * w.normSquared();
        assertTrue(lhs <= rhs + TOL);
    }

    @Test
    @DisplayName("approxEquals honours tolerance and dimension")
    void approxEquals() {
        ComplexVector a = ComplexVector.of(Complex.of(1, 0), Complex.of(0, 1));
        ComplexVector b = ComplexVector.of(Complex.of(1 + 1e-10, 0), Complex.of(0, 1));
        assertTrue(a.approxEquals(b, 1e-9));
        assertFalse(a.approxEquals(b, 1e-12));
        assertFalse(a.approxEquals(ComplexVector.zeros(3), 1.0));
    }
}
