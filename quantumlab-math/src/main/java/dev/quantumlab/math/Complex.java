package dev.quantumlab.math;

/**
 * An immutable complex number {@code re + im·i} in double precision.
 *
 * <p>Both components must be finite. Rejecting NaN and infinity at construction means a corrupted
 * amplitude is caught where it is created rather than propagating silently into a probability.
 *
 * <p>{@link #equals(Object)} is exact (component-wise). Use {@link #approxEquals(Complex, double)}
 * for numerical comparison.
 */
public record Complex(double re, double im) {

    public static final Complex ZERO = new Complex(0.0, 0.0);
    public static final Complex ONE = new Complex(1.0, 0.0);
    public static final Complex I = new Complex(0.0, 1.0);

    public Complex {
        if (!Double.isFinite(re) || !Double.isFinite(im)) {
            throw new IllegalArgumentException(
                    "Complex components must be finite, got re=" + re + ", im=" + im);
        }
    }

    public static Complex of(double re) {
        return new Complex(re, 0.0);
    }

    public static Complex of(double re, double im) {
        return new Complex(re, im);
    }

    /** {@code r·(cos θ + i sin θ)}. */
    public static Complex polar(double r, double theta) {
        return new Complex(r * Math.cos(theta), r * Math.sin(theta));
    }

    /** The unit-modulus phase factor {@code e^{iθ} = cos θ + i sin θ}. */
    public static Complex cis(double theta) {
        return polar(1.0, theta);
    }

    public Complex plus(Complex o) {
        return new Complex(re + o.re, im + o.im);
    }

    public Complex minus(Complex o) {
        return new Complex(re - o.re, im - o.im);
    }

    public Complex negate() {
        return new Complex(-re, -im);
    }

    public Complex times(Complex o) {
        return new Complex(re * o.re - im * o.im, re * o.im + im * o.re);
    }

    public Complex times(double s) {
        return new Complex(re * s, im * s);
    }

    /**
     * Complex division using the textbook formula {@code (a+bi)/(c+di) = ((ac+bd) + (bc−ad)i) /
     * (c²+d²)}. Adequate for the magnitudes that occur in QuantumLab; it can overflow for
     * components near {@code 1e154}.
     *
     * @throws ArithmeticException if the divisor is exactly zero
     */
    public Complex dividedBy(Complex o) {
        double d = o.re * o.re + o.im * o.im;
        if (d == 0.0) {
            throw new ArithmeticException("Division by complex zero");
        }
        return new Complex((re * o.re + im * o.im) / d, (im * o.re - re * o.im) / d);
    }

    public Complex conjugate() {
        return new Complex(re, -im);
    }

    /** The squared modulus {@code |z|²}. This is the quantity the Born rule uses. */
    public double abs2() {
        return re * re + im * im;
    }

    /** The modulus {@code |z|}, computed with {@link Math#hypot} to avoid overflow. */
    public double abs() {
        return Math.hypot(re, im);
    }

    /** The principal argument in {@code (−π, π]}. By convention {@code arg(0) = 0}. */
    public double arg() {
        return Math.atan2(im, re);
    }

    /** True if {@code |this − other| ≤ tol}. */
    public boolean approxEquals(Complex other, double tol) {
        return minus(other).abs() <= tol;
    }

    @Override
    public String toString() {
        return im < 0 || (im == 0.0 && 1.0 / im < 0)
                ? re + " - " + (-im) + "i"
                : re + " + " + im + "i";
    }
}
