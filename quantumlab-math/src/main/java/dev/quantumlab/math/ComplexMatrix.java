package dev.quantumlab.math;

import java.util.Arrays;

/**
 * An immutable dense complex matrix, stored row-major as two parallel {@code double[]} arrays.
 *
 * <p>Entry {@code (r, c)} lives at index {@code r·cols + c}. Rows and columns are zero-based.
 */
public final class ComplexMatrix {

    private final int rows;
    private final int cols;
    private final double[] re;
    private final double[] im;

    /** Takes ownership of the arrays. */
    private ComplexMatrix(int rows, int cols, double[] re, double[] im) {
        this.rows = rows;
        this.cols = cols;
        this.re = re;
        this.im = im;
    }

    // ---------------------------------------------------------------- factories

    /** Builds a matrix from a rectangular array of rows. */
    public static ComplexMatrix of(Complex[][] entries) {
        int r = entries.length;
        if (r == 0 || entries[0].length == 0) {
            throw new IllegalArgumentException("A matrix must have at least one row and column");
        }
        int c = entries[0].length;
        double[] re = new double[r * c];
        double[] im = new double[r * c];
        for (int i = 0; i < r; i++) {
            if (entries[i].length != c) {
                throw new IllegalArgumentException(
                        "Ragged input: row " + i + " has " + entries[i].length
                                + " entries, expected " + c);
            }
            for (int j = 0; j < c; j++) {
                re[i * c + j] = entries[i][j].re();
                im[i * c + j] = entries[i][j].im();
            }
        }
        return new ComplexMatrix(r, c, re, im);
    }

    public static ComplexMatrix zeros(int rows, int cols) {
        if (rows < 1 || cols < 1) {
            throw new IllegalArgumentException("Dimensions must be >= 1");
        }
        return new ComplexMatrix(rows, cols, new double[rows * cols], new double[rows * cols]);
    }

    public static ComplexMatrix identity(int n) {
        ComplexMatrix m = zeros(n, n);
        for (int i = 0; i < n; i++) {
            m.re[i * n + i] = 1.0;
        }
        return m;
    }

    // ---------------------------------------------------------------- accessors

    public int rows() {
        return rows;
    }

    public int cols() {
        return cols;
    }

    public boolean isSquare() {
        return rows == cols;
    }

    public Complex get(int r, int c) {
        checkIndex(r, c);
        return new Complex(re[r * cols + c], im[r * cols + c]);
    }

    // ---------------------------------------------------------------- algebra

    public ComplexMatrix plus(ComplexMatrix o) {
        requireSameShape(o);
        double[] r = new double[re.length];
        double[] i = new double[re.length];
        for (int k = 0; k < re.length; k++) {
            r[k] = re[k] + o.re[k];
            i[k] = im[k] + o.im[k];
        }
        return new ComplexMatrix(rows, cols, r, i);
    }

    public ComplexMatrix scale(Complex c) {
        double[] r = new double[re.length];
        double[] i = new double[re.length];
        for (int k = 0; k < re.length; k++) {
            r[k] = c.re() * re[k] - c.im() * im[k];
            i[k] = c.re() * im[k] + c.im() * re[k];
        }
        return new ComplexMatrix(rows, cols, r, i);
    }

    /** Matrix product {@code this · o}. Requires {@code this.cols == o.rows}. */
    public ComplexMatrix multiply(ComplexMatrix o) {
        if (cols != o.rows) {
            throw new IllegalArgumentException(
                    "Shape mismatch: (" + rows + "x" + cols + ") * (" + o.rows + "x" + o.cols + ")");
        }
        double[] r = new double[rows * o.cols];
        double[] i = new double[rows * o.cols];
        // i-k-j loop order keeps the inner loop contiguous in both o and the result.
        for (int a = 0; a < rows; a++) {
            for (int k = 0; k < cols; k++) {
                double ar = re[a * cols + k];
                double ai = im[a * cols + k];
                for (int b = 0; b < o.cols; b++) {
                    double br = o.re[k * o.cols + b];
                    double bi = o.im[k * o.cols + b];
                    r[a * o.cols + b] += ar * br - ai * bi;
                    i[a * o.cols + b] += ar * bi + ai * br;
                }
            }
        }
        return new ComplexMatrix(rows, o.cols, r, i);
    }

    /** Matrix-vector product {@code this · v}. Requires {@code this.cols == v.dimension()}. */
    public ComplexVector apply(ComplexVector v) {
        if (cols != v.dimension()) {
            throw new IllegalArgumentException(
                    "Shape mismatch: (" + rows + "x" + cols + ") * vector of dimension "
                            + v.dimension());
        }
        double[] r = new double[rows];
        double[] i = new double[rows];
        for (int a = 0; a < rows; a++) {
            double sr = 0.0;
            double si = 0.0;
            for (int k = 0; k < cols; k++) {
                double mr = re[a * cols + k];
                double mi = im[a * cols + k];
                sr += mr * v.re(k) - mi * v.im(k);
                si += mr * v.im(k) + mi * v.re(k);
            }
            r[a] = sr;
            i[a] = si;
        }
        return ComplexVector.fromParts(r, i);
    }

    /** The conjugate transpose {@code A†}, with {@code (A†)ᵢⱼ = conj(Aⱼᵢ)}. */
    public ComplexMatrix dagger() {
        double[] r = new double[re.length];
        double[] i = new double[re.length];
        for (int a = 0; a < rows; a++) {
            for (int b = 0; b < cols; b++) {
                r[b * rows + a] = re[a * cols + b];
                i[b * rows + a] = -im[a * cols + b];
            }
        }
        return new ComplexMatrix(cols, rows, r, i);
    }

    // ---------------------------------------------------------------- predicates

    /** True if the matrices have equal shape and every entry differs by at most {@code tol}. */
    public boolean approxEquals(ComplexMatrix o, double tol) {
        if (rows != o.rows || cols != o.cols) {
            return false;
        }
        for (int k = 0; k < re.length; k++) {
            if (Math.hypot(re[k] - o.re[k], im[k] - o.im[k]) > tol) {
                return false;
            }
        }
        return true;
    }

    /** True if the matrix is square and {@code A ≈ A†} entry-wise within {@code tol}. */
    public boolean isHermitian(double tol) {
        return isSquare() && approxEquals(dagger(), tol);
    }

    /** True if the matrix is square and {@code A†A ≈ I} entry-wise within {@code tol}. */
    public boolean isUnitary(double tol) {
        return isSquare() && dagger().multiply(this).approxEquals(identity(rows), tol);
    }

    // ---------------------------------------------------------------- object

    @Override
    public boolean equals(Object obj) {
        return obj instanceof ComplexMatrix o
                && rows == o.rows
                && cols == o.cols
                && Arrays.equals(re, o.re)
                && Arrays.equals(im, o.im);
    }

    @Override
    public int hashCode() {
        return 31 * (31 * (31 * rows + cols) + Arrays.hashCode(re)) + Arrays.hashCode(im);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int a = 0; a < rows; a++) {
            sb.append(a == 0 ? "[[" : " [");
            for (int b = 0; b < cols; b++) {
                if (b > 0) {
                    sb.append(", ");
                }
                sb.append(get(a, b));
            }
            sb.append(a == rows - 1 ? "]]" : "]\n");
        }
        return sb.toString();
    }

    private void checkIndex(int r, int c) {
        if (r < 0 || r >= rows || c < 0 || c >= cols) {
            throw new IndexOutOfBoundsException(
                    "(" + r + ", " + c + ") out of range for " + rows + "x" + cols);
        }
    }

    private void requireSameShape(ComplexMatrix o) {
        if (rows != o.rows || cols != o.cols) {
            throw new IllegalArgumentException(
                    "Shape mismatch: " + rows + "x" + cols + " vs " + o.rows + "x" + o.cols);
        }
    }
}
