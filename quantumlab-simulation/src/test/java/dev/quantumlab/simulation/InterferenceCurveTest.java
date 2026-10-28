package dev.quantumlab.simulation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.quantumlab.physics.StandardBasis;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class InterferenceCurveTest {

    private static final double TOL = 1e-12;

    private static final double A = Math.sqrt(0.7);
    private static final double B = Math.sqrt(0.3);

    @Test
    @DisplayName("the sweep covers [-pi, pi] with the requested number of points")
    void sweepShape() {
        List<SeriesPoint> c = InterferenceCurve.sweep(A, B, StandardBasis.HADAMARD, 0, 121);
        assertEquals(121, c.size());
        assertEquals(-Math.PI, c.get(0).x(), TOL);
        assertEquals(Math.PI, c.get(120).x(), TOL);
        assertEquals(0.0, c.get(60).x(), TOL);
    }

    @Test
    @DisplayName("Z basis: the probability does not depend on the relative phase")
    void zBasisFlat() {
        for (SeriesPoint p : InterferenceCurve.sweep(A, B, StandardBasis.COMPUTATIONAL, 0, 73)) {
            assertEquals(0.7, p.y(), TOL);
        }
        for (SeriesPoint p : InterferenceCurve.sweep(A, B, StandardBasis.COMPUTATIONAL, 1, 73)) {
            assertEquals(0.3, p.y(), TOL);
        }
    }

    @Test
    @DisplayName("X basis follows P(+) = (1 + 2|a||b| cos phi)/2 (closed form derived by hand)")
    void xBasisClosedForm() {
        for (SeriesPoint p : InterferenceCurve.sweep(A, B, StandardBasis.HADAMARD, 0, 97)) {
            assertEquals((1 + 2 * A * B * Math.cos(p.x())) / 2, p.y(), TOL);
        }
    }

    @Test
    @DisplayName("Y basis follows P(+i) = (1 + 2|a||b| sin phi)/2")
    void yBasisClosedForm() {
        for (SeriesPoint p : InterferenceCurve.sweep(A, B, StandardBasis.CIRCULAR, 0, 97)) {
            assertEquals((1 + 2 * A * B * Math.sin(p.x())) / 2, p.y(), TOL);
        }
    }

    @Test
    @DisplayName("fringe visibility is 2|a||b|: full for equal magnitudes, small for lopsided ones")
    void visibility() {
        double s = 1 / Math.sqrt(2);
        assertEquals(1.0, visibility(InterferenceCurve.sweep(s, s, StandardBasis.HADAMARD, 0, 361)), 1e-9);
        assertEquals(
                2 * A * B,
                visibility(InterferenceCurve.sweep(A, B, StandardBasis.HADAMARD, 0, 361)),
                1e-9);
        double lopsided = Math.sqrt(0.99);
        assertTrue(
                visibility(InterferenceCurve.sweep(lopsided, Math.sqrt(0.01), StandardBasis.HADAMARD, 0, 361))
                        < 0.25);
    }

    @Test
    @DisplayName("with a basis state (|b| = 0) there is no interference at all")
    void noInterferenceWithoutSuperposition() {
        assertEquals(0.0, visibility(InterferenceCurve.sweep(1.0, 0.0, StandardBasis.HADAMARD, 0, 100)), 1e-12);
    }

    @Test
    @DisplayName("the two outcomes' curves are complementary")
    void complementary() {
        List<SeriesPoint> plus = InterferenceCurve.sweep(A, B, StandardBasis.HADAMARD, 0, 50);
        List<SeriesPoint> minus = InterferenceCurve.sweep(A, B, StandardBasis.HADAMARD, 1, 50);
        for (int i = 0; i < 50; i++) {
            assertEquals(1.0, plus.get(i).y() + minus.get(i).y(), TOL);
        }
    }

    @Test
    @DisplayName("invalid arguments are rejected")
    void validation() {
        assertThrows(
                IllegalArgumentException.class,
                () -> InterferenceCurve.sweep(A, B, StandardBasis.HADAMARD, 0, 1));
        assertThrows(
                IllegalArgumentException.class,
                () -> InterferenceCurve.sweep(A, B, StandardBasis.HADAMARD, 2, 10));
        assertThrows(
                IllegalArgumentException.class,
                () -> InterferenceCurve.sweep(1.0, 1.0, StandardBasis.HADAMARD, 0, 10)); // not normalised
    }

    private static double visibility(List<SeriesPoint> curve) {
        double max = Double.NEGATIVE_INFINITY;
        double min = Double.POSITIVE_INFINITY;
        for (SeriesPoint p : curve) {
            max = Math.max(max, p.y());
            min = Math.min(min, p.y());
        }
        return max - min;
    }
}
