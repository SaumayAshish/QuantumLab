package dev.quantumlab.math;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ComplexTest {

    private static final double TOL = Tolerances.STRICT;

    @Test
    @DisplayName("i * i = -1")
    void imaginaryUnitSquared() {
        Complex p = Complex.I.times(Complex.I);
        assertEquals(-1.0, p.re(), TOL);
        assertEquals(0.0, p.im(), TOL);
    }

    @Test
    @DisplayName("addition, subtraction and negation")
    void additiveStructure() {
        Complex a = Complex.of(1, 2);
        Complex b = Complex.of(3, -5);
        assertEquals(Complex.of(4, -3), a.plus(b));
        assertEquals(Complex.of(-2, 7), a.minus(b));
        assertEquals(Complex.of(-1, -2), a.negate());
        assertEquals(Complex.ZERO, a.plus(a.negate()));
    }

    @Test
    @DisplayName("multiplication follows (a+bi)(c+di) = (ac-bd) + (ad+bc)i")
    void multiplication() {
        Complex p = Complex.of(1, 2).times(Complex.of(3, 4));
        assertEquals(-5.0, p.re(), TOL);
        assertEquals(10.0, p.im(), TOL);
    }

    @Test
    @DisplayName("scalar multiplication")
    void scalarMultiplication() {
        assertEquals(Complex.of(2, -4), Complex.of(1, -2).times(2.0));
    }

    @Test
    @DisplayName("division: (1+2i)/(3-i) = 0.1 + 0.7i")
    void division() {
        Complex q = Complex.of(1, 2).dividedBy(Complex.of(3, -1));
        assertEquals(0.1, q.re(), TOL);
        assertEquals(0.7, q.im(), TOL);
    }

    @Test
    @DisplayName("z / z = 1 and (z * w) / w = z")
    void divisionInvertsMultiplication() {
        Complex z = Complex.of(-2.5, 0.75);
        Complex w = Complex.of(0.3, -4.0);
        assertTrue(z.dividedBy(z).approxEquals(Complex.ONE, TOL));
        assertTrue(z.times(w).dividedBy(w).approxEquals(z, TOL));
    }

    @Test
    @DisplayName("division by zero is rejected")
    void divisionByZero() {
        assertThrows(ArithmeticException.class, () -> Complex.ONE.dividedBy(Complex.ZERO));
    }

    @Test
    @DisplayName("conjugate flips the sign of the imaginary part")
    void conjugate() {
        assertEquals(Complex.of(3, -4), Complex.of(3, 4).conjugate());
        assertEquals(Complex.of(3, 4), Complex.of(3, 4).conjugate().conjugate());
    }

    @Test
    @DisplayName("z * conj(z) = |z|^2 and is real")
    void productWithConjugateIsModulusSquared() {
        Complex z = Complex.of(3, 4);
        Complex p = z.times(z.conjugate());
        assertEquals(25.0, p.re(), TOL);
        assertEquals(0.0, p.im(), TOL);
        assertEquals(25.0, z.abs2(), TOL);
    }

    @Test
    @DisplayName("modulus of 3+4i is 5")
    void modulus() {
        assertEquals(5.0, Complex.of(3, 4).abs(), TOL);
    }

    @Test
    @DisplayName("modulus does not overflow for large components")
    void modulusLargeComponents() {
        double big = 1e200;
        assertEquals(big * Math.sqrt(2.0), Complex.of(big, big).abs(), big * 1e-12);
    }

    @Test
    @DisplayName("principal argument")
    void argument() {
        assertEquals(0.0, Complex.ONE.arg(), TOL);
        assertEquals(Math.PI / 2, Complex.I.arg(), TOL);
        assertEquals(Math.PI, Complex.of(-1, 0).arg(), TOL);
        assertEquals(-Math.PI / 2, Complex.of(0, -1).arg(), TOL);
        assertEquals(0.0, Complex.ZERO.arg(), TOL);
    }

    @Test
    @DisplayName("polar form")
    void polar() {
        Complex z = Complex.polar(2.0, Math.PI / 2);
        assertEquals(0.0, z.re(), TOL);
        assertEquals(2.0, z.im(), TOL);
    }

    @Test
    @DisplayName("Euler's identity: e^{i pi} = -1")
    void eulerIdentity() {
        assertTrue(Complex.cis(Math.PI).approxEquals(Complex.of(-1, 0), TOL));
    }

    @Test
    @DisplayName("cis(theta) has unit modulus for any theta")
    void cisUnitModulus() {
        for (double t = -10; t <= 10; t += 0.37) {
            assertEquals(1.0, Complex.cis(t).abs(), TOL);
        }
    }

    @Test
    @DisplayName("cis is a homomorphism: cis(a) * cis(b) = cis(a+b)")
    void cisAddsAngles() {
        double a = 0.7;
        double b = -2.1;
        assertTrue(Complex.cis(a).times(Complex.cis(b)).approxEquals(Complex.cis(a + b), TOL));
    }

    @Test
    @DisplayName("non-finite components are rejected")
    void rejectsNonFinite() {
        assertThrows(IllegalArgumentException.class, () -> Complex.of(Double.NaN, 0));
        assertThrows(IllegalArgumentException.class, () -> Complex.of(0, Double.POSITIVE_INFINITY));
    }

    @Test
    @DisplayName("approxEquals honours its tolerance")
    void approxEquals() {
        assertTrue(Complex.of(1, 1).approxEquals(Complex.of(1 + 1e-10, 1), 1e-9));
        assertFalse(Complex.of(1, 1).approxEquals(Complex.of(1 + 1e-6, 1), 1e-9));
    }

    @Test
    @DisplayName("toString shows the sign of the imaginary part")
    void toStringSign() {
        assertEquals("1.0 + 2.0i", Complex.of(1, 2).toString());
        assertEquals("1.0 - 2.0i", Complex.of(1, -2).toString());
    }
}
