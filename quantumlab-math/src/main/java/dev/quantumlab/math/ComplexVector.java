package dev.quantumlab.math;

import java.util.Arrays;

/**
 * An immutable vector in {@code ℂⁿ}.
 *
 * <p>Storage is two parallel {@code double[]} arrays (real and imaginary parts) rather than an
 * array of {@link Complex} objects, so numerical loops allocate nothing. {@link #get(int)}
 * materialises a {@link Complex} on demand for convenience.
 *
 * <p><b>Inner-product convention.</b> {@link #innerProduct(ComplexVector)} computes
 * {@code ⟨this|other⟩ = Σᵢ conj(thisᵢ)·otherᵢ}: conjugate-linear in the first argument and linear in
 * the second, matching Dirac notation as used in most physics texts.
 */
public final class ComplexVector {

    private final double[] re;
    private final double[] im;

    /** Takes ownership of the arrays; callers must not retain references. */
    private ComplexVector(double[] re, double[] im) {
        this.re = re;
        this.im = im;
    }

    // ---------------------------------------------------------------- factories

    /** Builds a vector from the given entries. The vector must have dimension ≥ 1. */
    public static ComplexVector of(Complex... entries) {
        if (entries.length == 0) {
            throw new IllegalArgumentException("A vector must have dimension >= 1");
        }
        double[] r = new double[entries.length];
        double[] i = new double[entries.length];
        for (int k = 0; k < entries.length; k++) {
            r[k] = entries[k].re();
            i[k] = entries[k].im();
        }
        return new ComplexVector(r, i);
    }

    /** Builds a vector from separate real and imaginary arrays (copied, and checked finite). */
    public static ComplexVector fromParts(double[] re, double[] im) {
        if (re.length == 0 || re.length != im.length) {
            throw new IllegalArgumentException(
                    "re and im must be non-empty and of equal length, got "
                            + re.length + " and " + im.length);
        }
        for (int k = 0; k < re.length; k++) {
            if (!Double.isFinite(re[k]) || !Double.isFinite(im[k])) {
                throw new IllegalArgumentException("Entry " + k + " is not finite");
            }
        }
        return new ComplexVector(re.clone(), im.clone());
    }

    public static ComplexVector zeros(int n) {
        if (n < 1) {
            throw new IllegalArgumentException("Dimension must be >= 1, got " + n);
        }
        return new ComplexVector(new double[n], new double[n]);
    }

    /** The standard basis vector {@code eₖ} of {@code ℂⁿ} (a 1 in position {@code k}). */
    public static ComplexVector basis(int n, int k) {
        if (k < 0 || k >= n) {
            throw new IllegalArgumentException("Index " + k + " out of range for dimension " + n);
        }
        double[] r = new double[n];
        r[k] = 1.0;
        return new ComplexVector(r, new double[n]);
    }

    // ---------------------------------------------------------------- accessors

    public int dimension() {
        return re.length;
    }

    public Complex get(int i) {
        return new Complex(re[i], im[i]);
    }

    public double re(int i) {
        return re[i];
    }

    public double im(int i) {
        return im[i];
    }

    // ---------------------------------------------------------------- algebra

    public ComplexVector plus(ComplexVector o) {
        requireSameDimension(o);
        int n = re.length;
        double[] r = new double[n];
        double[] i = new double[n];
        for (int k = 0; k < n; k++) {
            r[k] = re[k] + o.re[k];
            i[k] = im[k] + o.im[k];
        }
        return new ComplexVector(r, i);
    }

    public ComplexVector minus(ComplexVector o) {
        requireSameDimension(o);
        int n = re.length;
        double[] r = new double[n];
        double[] i = new double[n];
        for (int k = 0; k < n; k++) {
            r[k] = re[k] - o.re[k];
            i[k] = im[k] - o.im[k];
        }
        return new ComplexVector(r, i);
    }

    public ComplexVector scale(Complex c) {
        int n = re.length;
        double[] r = new double[n];
        double[] i = new double[n];
        for (int k = 0; k < n; k++) {
            r[k] = c.re() * re[k] - c.im() * im[k];
            i[k] = c.re() * im[k] + c.im() * re[k];
        }
        return new ComplexVector(r, i);
    }

    public ComplexVector scale(double s) {
        int n = re.length;
        double[] r = new double[n];
        double[] i = new double[n];
        for (int k = 0; k < n; k++) {
            r[k] = s * re[k];
            i[k] = s * im[k];
        }
        return new ComplexVector(r, i);
    }

    /**
     * {@code ⟨this|other⟩ = Σᵢ conj(thisᵢ)·otherᵢ}. With {@code conj(a)·b = (ar·br + ai·bi) + i(ar·bi −
     * ai·br)}.
     */
    public Complex innerProduct(ComplexVector other) {
        requireSameDimension(other);
        double sr = 0.0;
        double si = 0.0;
        for (int k = 0; k < re.length; k++) {
            sr += re[k] * other.re[k] + im[k] * other.im[k];
            si += re[k] * other.im[k] - im[k] * other.re[k];
        }
        return new Complex(sr, si);
    }

    /** {@code ‖v‖² = ⟨v|v⟩ = Σ |vᵢ|²}, always real and non-negative. */
    public double normSquared() {
        double s = 0.0;
        for (int k = 0; k < re.length; k++) {
            s += re[k] * re[k] + im[k] * im[k];
        }
        return s;
    }

    public double norm() {
        return Math.sqrt(normSquared());
    }

    /**
     * Returns {@code v/‖v‖}.
     *
     * @throws ArithmeticException if the vector is exactly zero
     */
    public ComplexVector normalized() {
        double n = norm();
        if (n == 0.0) {
            throw new ArithmeticException("Cannot normalise the zero vector");
        }
        return scale(1.0 / n);
    }

    /** True if {@code |‖v‖² − 1| ≤ tol}. */
    public boolean isNormalized(double tol) {
        return Math.abs(normSquared() - 1.0) <= tol;
    }

    /** True if the vectors have equal dimension and every entry differs by at most {@code tol}. */
    public boolean approxEquals(ComplexVector o, double tol) {
        if (re.length != o.re.length) {
            return false;
        }
        for (int k = 0; k < re.length; k++) {
            if (Math.hypot(re[k] - o.re[k], im[k] - o.im[k]) > tol) {
                return false;
            }
        }
        return true;
    }

    // ---------------------------------------------------------------- object

    @Override
    public boolean equals(Object obj) {
        return obj instanceof ComplexVector o
                && Arrays.equals(re, o.re)
                && Arrays.equals(im, o.im);
    }

    @Override
    public int hashCode() {
        return 31 * Arrays.hashCode(re) + Arrays.hashCode(im);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("[");
        for (int k = 0; k < re.length; k++) {
            if (k > 0) {
                sb.append(", ");
            }
            sb.append(get(k));
        }
        return sb.append(']').toString();
    }

    private void requireSameDimension(ComplexVector o) {
        if (re.length != o.re.length) {
            throw new IllegalArgumentException(
                    "Dimension mismatch: " + re.length + " vs " + o.re.length);
        }
    }
    /**
     * The tensor (Kronecker) product {@code this ⊗ o}. Entry {@code a·n + b} is
     * {@code this[a] · o[b]} with {@code n = o.dimension()}, so the first factor is the more
     * significant index.
     */
    public ComplexVector tensor(ComplexVector o) {
        int m = re.length;
        int n = o.re.length;
        double[] r = new double[m * n];
        double[] i = new double[m * n];
        for (int a = 0; a < m; a++) {
            for (int b = 0; b < n; b++) {
                r[a * n + b] = re[a] * o.re[b] - im[a] * o.im[b];
                i[a * n + b] = re[a] * o.im[b] + im[a] * o.re[b];
            }
        }
        return new ComplexVector(r, i);
    }
}
