package dev.quantumlab.math;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SymmetricTridiagonalTest {

    /** The matrix with 2 on the diagonal and −1 beside it: eigenvalues and vectors in closed form. */
    private static SymmetricTridiagonal secondDifference(int n) {
        double[] d = new double[n];
        double[] e = new double[n - 1];
        java.util.Arrays.fill(d, 2.0);
        java.util.Arrays.fill(e, -1.0);
        return new SymmetricTridiagonal(d, e);
    }

    private static double dot(double[] a, double[] b) {
        double s = 0.0;
        for (int i = 0; i < a.length; i++) {
            s += a[i] * b[i];
        }
        return s;
    }

    @Test
    @DisplayName("the (2, −1) matrix has eigenvalues 4 sin²(kπ / 2(n+1)), found to machine precision")
    void closedFormEigenvalues() {
        int n = 10;
        SymmetricTridiagonal t = secondDifference(n);
        for (int k = 0; k < n; k++) {
            double s = Math.sin((k + 1) * Math.PI / (2.0 * (n + 1)));
            assertEquals(4.0 * s * s, t.eigenvalue(k), 1e-13);
        }
    }

    @Test
    @DisplayName("the (2, −1) matrix has eigenvectors sqrt(2/(n+1)) sin(jkπ/(n+1))")
    void closedFormEigenvectors() {
        int n = 10;
        SymmetricTridiagonal t = secondDifference(n);
        for (int k = 0; k < n; k++) {
            double[] v = t.eigenvector(k);
            for (int j = 0; j < n; j++) {
                double expected =
                    Math.sqrt(2.0 / (n + 1)) * Math.sin((k + 1) * (j + 1) * Math.PI / (n + 1));
                assertEquals(expected, v[j], 1e-10);
            }
        }
    }

    @Test
    @DisplayName("a general 6×6 matrix agrees with eigenvalues and vectors computed independently by NumPy")
    void numpyReference() {
        // Reference values from numpy.linalg.eigh, outside this code base.
        double[] d = {2.0, -1.0, 0.5, 3.0, 1.25, -0.75};
        double[] e = {1.0, -0.5, 0.8, 2.0, -1.5};
        double[] eigenvalues = {
            -1.7974452377297268, -1.4259209525775256, 0.22999505679893537,
            1.1282901592762598, 2.314431535960096, 4.550649438271963
        };
        double[][] vectors = {
            {0.026573762766773847, -0.10091240886724481, -0.10779671422449119,
                0.24650105413043372, -0.548168968426907, -0.7850085360286181},
            {0.26865974959596217, -0.9204070652550386, -0.24672180879309877,
                0.018703960481170373, 0.05729759822234253, 0.12715450971858383},
            {0.12133212412398817, -0.214758459468544, 0.7709679353521041,
                -0.39443047916107377, 0.23790001437182814, -0.3641345117835191},
            {0.09609468145774817, -0.08376667946793218, 0.5487489620891937,
                0.37861279132466974, -0.5738262285088122, 0.45825685584961845},
            {0.9503279321720004, 0.2988130713786246, -0.08013506992481662,
                0.005008672101696962, 0.030337134150106776, -0.014849638731087862},
            {0.006076451615466438, 0.015498897899676366, -0.15990499461041183,
                -0.7999620345571333, -0.5562683419182929, 0.15741514744454765}
        };
        SymmetricTridiagonal t = new SymmetricTridiagonal(d, e);
        for (int k = 0; k < 6; k++) {
            assertEquals(eigenvalues[k], t.eigenvalue(k), 1e-13);
            double[] v = t.eigenvector(k);
            for (int j = 0; j < 6; j++) {
                assertEquals(vectors[k][j], v[j], 1e-10);
            }
        }
    }

    @Test
    @DisplayName("the Sturm count is the number of eigenvalues below a value")
    void sturmCount() {
        SymmetricTridiagonal t = secondDifference(10);
        assertEquals(0, t.countEigenvaluesBelow(0.0));
        assertEquals(10, t.countEigenvaluesBelow(4.0 + 1e-9));
        for (int k = 0; k < 10; k++) {
            double lambda = t.eigenvalue(k);
            assertEquals(k, t.countEigenvaluesBelow(lambda - 1e-9));
            assertEquals(k + 1, t.countEigenvaluesBelow(lambda + 1e-9));
        }
    }

    @Test
    @DisplayName("on a larger varying matrix every pair satisfies T v = λ v, and the vectors are orthonormal")
    void residualsAndOrthonormality() {
        int n = 200;
        double[] d = new double[n];
        double[] e = new double[n - 1];
        for (int i = 0; i < n; i++) {
            d[i] = 2.0 + Math.sin(0.3 * i);
        }
        for (int i = 0; i < n - 1; i++) {
            e[i] = -1.0 + 0.4 * Math.cos(0.17 * i);
        }
        SymmetricTridiagonal t = new SymmetricTridiagonal(d, e);
        int m = 12;
        double[][] v = new double[m][];
        for (int k = 0; k < m; k++) {
            double lambda = t.eigenvalue(k);
            v[k] = t.eigenvector(k);
            double[] tv = t.multiply(v[k]);
            double worst = 0.0;
            for (int j = 0; j < n; j++) {
                worst = Math.max(worst, Math.abs(tv[j] - lambda * v[k][j]));
            }
            assertTrue(worst < 1e-10, "residual " + worst + " for k=" + k);
        }
        for (int a = 0; a < m; a++) {
            for (int b = 0; b < m; b++) {
                assertEquals(a == b ? 1.0 : 0.0, dot(v[a], v[b]), 1e-9);
            }
        }
    }

    @Test
    @DisplayName("eigenvalues ascend and sum to the trace")
    void ascendingAndTrace() {
        int n = 60;
        double[] d = new double[n];
        double[] e = new double[n - 1];
        double trace = 0.0;
        for (int i = 0; i < n; i++) {
            d[i] = Math.cos(i) * 3.0;
            trace += d[i];
        }
        java.util.Arrays.fill(e, 0.7);
        SymmetricTridiagonal t = new SymmetricTridiagonal(d, e);
        double[] all = t.eigenvalues(n);
        double sum = 0.0;
        for (int k = 0; k < n; k++) {
            sum += all[k];
            if (k > 0) {
                assertTrue(all[k] >= all[k - 1]);
            }
        }
        assertEquals(trace, sum, 1e-10);
    }

    @Test
    @DisplayName("the lowest eigenvalue of a large (2, −1) matrix is accurate in absolute terms")
    void largeMatrixLowestEigenvalue() {
        int n = 2000;
        double s = Math.sin(Math.PI / (2.0 * (n + 1)));
        assertEquals(4.0 * s * s, secondDifference(n).eigenvalue(0), 1e-14);
    }

    @Test
    @DisplayName("a 1×1 matrix has its entry as eigenvalue and (1) as eigenvector")
    void oneByOne() {
        SymmetricTridiagonal t = new SymmetricTridiagonal(new double[] {-3.5}, new double[0]);
        assertEquals(-3.5, t.eigenvalue(0), 1e-15);
        assertEquals(1.0, t.eigenvector(0)[0], 1e-15);
    }

    @Test
    @DisplayName("a diagonal matrix has its sorted diagonal as eigenvalues; eigenvectors are refused")
    void decoupledMatrix() {
        SymmetricTridiagonal t =
            new SymmetricTridiagonal(new double[] {3.0, -1.0, 2.0}, new double[] {0.0, 0.0});
        assertEquals(-1.0, t.eigenvalue(0), 1e-14);
        assertEquals(2.0, t.eigenvalue(1), 1e-14);
        assertEquals(3.0, t.eigenvalue(2), 1e-14);
        assertThrows(IllegalStateException.class, () -> t.eigenvector(0));
    }

    @Test
    @DisplayName("each eigenvector is signed so that its first non-negligible component is positive")
    void signConvention() {
        SymmetricTridiagonal t = secondDifference(25);
        for (int k = 0; k < 25; k++) {
            assertTrue(t.eigenvector(k)[0] > 0.0, "k=" + k);
        }
    }

    @Test
    @DisplayName("invalid matrices and indices are rejected")
    void validation() {
        assertThrows(IllegalArgumentException.class, () -> new SymmetricTridiagonal(null, new double[0]));
        assertThrows(IllegalArgumentException.class, () -> new SymmetricTridiagonal(new double[0], new double[0]));
        assertThrows(
            IllegalArgumentException.class,
            () -> new SymmetricTridiagonal(new double[] {1, 2}, new double[] {1, 1}));
        assertThrows(
            IllegalArgumentException.class,
            () -> new SymmetricTridiagonal(new double[] {1, Double.NaN}, new double[] {1}));
        assertThrows(
            IllegalArgumentException.class,
            () -> new SymmetricTridiagonal(new double[] {1, 2}, new double[] {Double.POSITIVE_INFINITY}));
        SymmetricTridiagonal t = secondDifference(3);
        assertThrows(IllegalArgumentException.class, () -> t.eigenvalue(-1));
        assertThrows(IllegalArgumentException.class, () -> t.eigenvalue(3));
        assertThrows(IllegalArgumentException.class, () -> t.eigenvector(3));
        assertThrows(IllegalArgumentException.class, () -> t.eigenvalues(0));
        assertThrows(IllegalArgumentException.class, () -> t.eigenvalues(4));
        assertThrows(IllegalArgumentException.class, () -> t.multiply(new double[2]));
    }

    @Test
    @DisplayName("the matrix does not change if the arrays it was built from are changed")
    void immutable() {
        double[] d = {2.0, 2.0};
        double[] e = {-1.0};
        SymmetricTridiagonal t = new SymmetricTridiagonal(d, e);
        double before = t.eigenvalue(0);
        d[0] = 100.0;
        e[0] = 50.0;
        assertEquals(before, t.eigenvalue(0), 0.0);
        assertEquals(2.0, t.diagonal(0), 0.0);
        assertEquals(-1.0, t.offDiagonal(0), 0.0);
    }
}
