package dev.quantumlab.math;

import java.util.Arrays;

/**
 * A real symmetric tridiagonal matrix, with eigenvalues and eigenvectors.
 *
 * <p>The matrix has {@code diagonal} entries {@code d₀ … d_{n−1}} and {@code offDiagonal} entries
 * {@code e₀ … e_{n−2}}, where {@code e_i} sits between rows {@code i} and {@code i+1} (and, by
 * symmetry, between columns {@code i+1} and {@code i}). It is what a second-order finite-difference
 * Hamiltonian in one dimension looks like.
 *
 * <p><b>Eigenvalues</b> are found by bisection on the Sturm count (the number of eigenvalues below
 * a trial value), which is slow compared with the best algorithms but very robust: it needs no
 * starting guess, always converges, and is accurate to about machine precision relative to the
 * largest eigenvalue magnitude. They are numbered from 0 in ascending order.
 *
 * <p><b>Eigenvectors</b> use inverse iteration with a tridiagonal solver that pivots. They are
 * normalised to unit length, and signed so that the first component that is not negligible is
 * positive. They require every off-diagonal entry to be non-zero: then the spectrum is simple, so
 * each eigenvector is unique up to sign. Without that condition eigenvalues can repeat and the
 * eigenvectors are not unique.
 *
 * <p>Instances are immutable.
 */
public final class SymmetricTridiagonal {

    private static final int INVERSE_ITERATIONS = 3;
    private static final double NEGLIGIBLE = 1e-3;

    private final double[] diagonal;
    private final double[] offDiagonal;
    private final double lowerBound;
    private final double upperBound;

    /**
     * @param diagonal the {@code n ≥ 1} diagonal entries
     * @param offDiagonal the {@code n − 1} off-diagonal entries
     * @throws IllegalArgumentException if the lengths do not match, or an entry is NaN or infinite
     */
    public SymmetricTridiagonal(double[] diagonal, double[] offDiagonal) {
        if (diagonal == null || offDiagonal == null) {
            throw new IllegalArgumentException("The diagonals must not be null.");
        }
        if (diagonal.length < 1) {
            throw new IllegalArgumentException("A matrix needs at least one row.");
        }
        if (offDiagonal.length != diagonal.length - 1) {
            throw new IllegalArgumentException(
                "For " + diagonal.length + " rows the off-diagonal needs "
                    + (diagonal.length - 1) + " entries, not " + offDiagonal.length + ".");
        }
        requireFinite(diagonal, "diagonal");
        requireFinite(offDiagonal, "off-diagonal");
        this.diagonal = diagonal.clone();
        this.offDiagonal = offDiagonal.clone();

        // Gershgorin: every eigenvalue lies in some disc centred on d_i with radius |e_{i-1}|+|e_i|.
        double lo = Double.POSITIVE_INFINITY;
        double hi = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < diagonal.length; i++) {
            double radius =
                (i > 0 ? Math.abs(offDiagonal[i - 1]) : 0.0)
                    + (i < offDiagonal.length ? Math.abs(offDiagonal[i]) : 0.0);
            lo = Math.min(lo, diagonal[i] - radius);
            hi = Math.max(hi, diagonal[i] + radius);
        }
        this.lowerBound = lo;
        this.upperBound = hi;
    }

    public int size() {
        return diagonal.length;
    }

    public double diagonal(int i) {
        return diagonal[i];
    }

    public double offDiagonal(int i) {
        return offDiagonal[i];
    }

    /** The matrix-vector product {@code T·x}. */
    public double[] multiply(double[] x) {
        if (x.length != size()) {
            throw new IllegalArgumentException("Expected " + size() + " entries, got " + x.length + ".");
        }
        double[] y = new double[size()];
        for (int i = 0; i < size(); i++) {
            double s = diagonal[i] * x[i];
            if (i > 0) {
                s += offDiagonal[i - 1] * x[i - 1];
            }
            if (i < size() - 1) {
                s += offDiagonal[i] * x[i + 1];
            }
            y[i] = s;
        }
        return y;
    }

    /** The number of eigenvalues strictly below {@code x}, counted with multiplicity. */
    public int countEigenvaluesBelow(double x) {
        int n = size();
        double tiny = Double.MIN_NORMAL + Math.ulp(Math.max(Math.abs(lowerBound), Math.abs(upperBound)));
        int count = 0;
        double q = diagonal[0] - x;
        if (q == 0.0) {
            q = -tiny;
        }
        if (q < 0.0) {
            count++;
        }
        for (int i = 1; i < n; i++) {
            q = diagonal[i] - x - offDiagonal[i - 1] * offDiagonal[i - 1] / q;
            if (q == 0.0) {
                q = -tiny;
            }
            if (q < 0.0) {
                count++;
            }
        }
        return count;
    }

    /**
     * The {@code k}-th smallest eigenvalue, {@code k = 0} being the smallest.
     *
     * @throws IllegalArgumentException if {@code k} is outside {@code [0, n)}
     */
    public double eigenvalue(int k) {
        requireIndex(k);
        double lo = lowerBound;
        double hi = upperBound;
        // Invariant: fewer than k+1 eigenvalues are below lo, at least k+1 are below hi.
        while (true) {
            double mid = 0.5 * (lo + hi);
            if (mid <= lo || mid >= hi) {
                break;
            }
            if (countEigenvaluesBelow(mid) >= k + 1) {
                hi = mid;
            } else {
                lo = mid;
            }
        }
        return 0.5 * (lo + hi);
    }

    /** The {@code count} smallest eigenvalues in ascending order. */
    public double[] eigenvalues(int count) {
        if (count < 1 || count > size()) {
            throw new IllegalArgumentException("count must be in [1, " + size() + "], got " + count);
        }
        double[] values = new double[count];
        for (int k = 0; k < count; k++) {
            values[k] = eigenvalue(k);
        }
        return values;
    }

    /**
     * The unit-length eigenvector belonging to {@link #eigenvalue(int) eigenvalue(k)}.
     *
     * @throws IllegalArgumentException if {@code k} is outside {@code [0, n)}
     * @throws IllegalStateException if any off-diagonal entry is zero
     */
    public double[] eigenvector(int k) {
        requireIndex(k);
        for (double e : offDiagonal) {
            if (e == 0.0) {
                throw new IllegalStateException(
                    "Eigenvectors need every off-diagonal entry to be non-zero.");
            }
        }
        int n = size();
        double scale = Math.max(Math.abs(lowerBound), Math.abs(upperBound));
        double shift = eigenvalue(k) + 4.0 * Math.ulp(Math.max(scale, Double.MIN_NORMAL));

        // A fixed, irregular start vector: deterministic, and not orthogonal to any eigenvector
        // by symmetry (a constant vector would be orthogonal to every antisymmetric one).
        double[] x = new double[n];
        long state = 0x9E3779B97F4A7C15L;
        for (int i = 0; i < n; i++) {
            state = state * 6364136223846793005L + 1442695040888963407L;
            x[i] = 0.5 + ((state >>> 11) * 0x1.0p-53);
        }
        normalise(x);

        for (int iteration = 0; iteration < INVERSE_ITERATIONS; iteration++) {
            solveShifted(shift, x);
            normalise(x);
        }

        double biggest = 0.0;
        for (double v : x) {
            biggest = Math.max(biggest, Math.abs(v));
        }
        for (double v : x) {
            if (Math.abs(v) > NEGLIGIBLE * biggest) {
                if (v < 0.0) {
                    for (int i = 0; i < n; i++) {
                        x[i] = -x[i];
                    }
                }
                break;
            }
        }
        return x;
    }

    /** Solves {@code (T − shift·I) y = b} in place, using Gaussian elimination with row pivoting. */
    private void solveShifted(double shift, double[] b) {
        int n = size();
        double[] d = new double[n];
        for (int i = 0; i < n; i++) {
            d[i] = diagonal[i] - shift;
        }
        double[] du = offDiagonal.clone(); // super-diagonal, then second super-diagonal fill-in
        double[] dl = offDiagonal.clone(); // sub-diagonal, reused for the second super-diagonal
        double tiny = Math.ulp(Math.max(Math.max(Math.abs(lowerBound), Math.abs(upperBound)), 1.0));

        for (int i = 0; i < n - 1; i++) {
            if (Math.abs(d[i]) >= Math.abs(dl[i])) {
                if (d[i] == 0.0) {
                    d[i] = tiny; // exactly singular: perturb, as inverse iteration expects
                }
                double fact = dl[i] / d[i];
                d[i + 1] -= fact * du[i];
                b[i + 1] -= fact * b[i];
                dl[i] = 0.0;
            } else {
                double fact = d[i] / dl[i];
                d[i] = dl[i];
                double temp = d[i + 1];
                d[i + 1] = du[i] - fact * temp;
                if (i < n - 2) {
                    dl[i] = du[i + 1];
                    du[i + 1] = -fact * dl[i];
                }
                du[i] = temp;
                double t = b[i];
                b[i] = b[i + 1];
                b[i + 1] = t - fact * b[i + 1];
            }
        }
        if (d[n - 1] == 0.0) {
            d[n - 1] = tiny;
        }

        b[n - 1] /= d[n - 1];
        if (n > 1) {
            b[n - 2] = (b[n - 2] - du[n - 2] * b[n - 1]) / d[n - 2];
        }
        for (int i = n - 3; i >= 0; i--) {
            b[i] = (b[i] - du[i] * b[i + 1] - dl[i] * b[i + 2]) / d[i];
        }
    }

    private static void normalise(double[] x) {
        double max = 0.0;
        for (double v : x) {
            max = Math.max(max, Math.abs(v));
        }
        if (max == 0.0 || Double.isInfinite(max) || Double.isNaN(max)) {
            throw new ArithmeticException("Inverse iteration broke down.");
        }
        double sum = 0.0;
        for (double v : x) {
            double s = v / max;
            sum += s * s;
        }
        double norm = max * Math.sqrt(sum);
        for (int i = 0; i < x.length; i++) {
            x[i] /= norm;
        }
    }

    private void requireIndex(int k) {
        if (k < 0 || k >= size()) {
            throw new IllegalArgumentException("Index must be in [0, " + size() + "), got " + k);
        }
    }

    private static void requireFinite(double[] values, String name) {
        for (double v : values) {
            if (Double.isNaN(v) || Double.isInfinite(v)) {
                throw new IllegalArgumentException("The " + name + " contains a non-finite entry.");
            }
        }
    }

    @Override
    public String toString() {
        return "SymmetricTridiagonal{n=" + size() + ", d=" + Arrays.toString(diagonal) + "}";
    }
}
