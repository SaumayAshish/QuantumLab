package dev.quantumlab.physics.scattering;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RectangularBarrierTest {

    @Test
    @DisplayName("the closed form and the transfer matrix agree below, at and above the barrier")
    void closedFormMatchesTransferMatrix() {
        double[][] barriers = {{2.0, 1.5}, {5.0, 0.5}, {0.5, 3.0}, {10.0, 0.2}};
        for (double[] b : barriers) {
            RectangularBarrier barrier = new RectangularBarrier(b[0], b[1]);
            for (double fraction = 0.05; fraction < 3.0; fraction += 0.0625) {
                double e = fraction * b[0];
                double expected = barrier.transmission(e);
                assertEquals(expected, barrier.transmissionByTransferMatrix(e), 1e-12 * Math.max(1, 1 / expected));
            }
        }
    }

    @Test
    @DisplayName("exactly at the barrier height T = 1 / (1 + a²V0/2), found by both methods")
    void atTheTop() {
        RectangularBarrier barrier = new RectangularBarrier(2.0, 1.5);
        double expected = 1.0 / (1.0 + 1.5 * 1.5 * 2.0 / 2.0);
        assertEquals(expected, barrier.transmission(2.0), 1e-15);
        assertEquals(expected, barrier.transmissionByTransferMatrix(2.0), 1e-14);
    }

    @Test
    @DisplayName("a classically forbidden barrier still transmits: 0 < T < 1 for E < V0")
    void tunnelling() {
        RectangularBarrier barrier = new RectangularBarrier(2.0, 1.5);
        assertEquals(0.055861437216915076, barrier.transmission(1.0), 1e-13);
        for (double e = 0.1; e < 2.0; e += 0.1) {
            double t = barrier.transmission(e);
            assertTrue(t > 0.0 && t < 1.0, "T=" + t);
        }
    }

    @Test
    @DisplayName("above the barrier T reaches exactly 1 when ka is a multiple of π")
    void transmissionResonances() {
        RectangularBarrier barrier = new RectangularBarrier(3.0, 2.0);
        for (int n = 1; n <= 4; n++) {
            double e = 3.0 + 0.5 * Math.pow(n * Math.PI / 2.0, 2);
            assertEquals(1.0, barrier.transmission(e), 1e-12);
            assertEquals(1.0, barrier.transmissionByTransferMatrix(e), 1e-12);
        }
    }

    @Test
    @DisplayName("a thick barrier follows 16E(V0−E)/V0² · exp(−2κa) to within 0.1 %")
    void thickBarrier() {
        RectangularBarrier barrier = new RectangularBarrier(2.0, 6.0);
        double e = 1.0;
        double exact = barrier.transmission(e);
        assertTrue(barrier.decayConstant(e) * 6.0 > 8.0);
        assertEquals(1.0, barrier.thickBarrierEstimate(e) / exact, 1e-3);
    }

    @Test
    @DisplayName("the tunnelling probability falls steeply with width and rises with energy")
    void trends() {
        double previous = 1.0;
        for (double width = 0.25; width <= 3.0; width += 0.25) {
            double t = new RectangularBarrier(2.0, width).transmission(1.0);
            assertTrue(t < previous, "width " + width);
            previous = t;
        }
        previous = 0.0;
        for (double e = 0.2; e < 2.0; e += 0.2) {
            double t = new RectangularBarrier(2.0, 1.5).transmission(e);
            assertTrue(t > previous, "energy " + e);
            previous = t;
        }
    }

    @Test
    @DisplayName("invalid barriers and energies are rejected")
    void validation() {
        assertThrows(IllegalArgumentException.class, () -> new RectangularBarrier(0.0, 1.0));
        assertThrows(IllegalArgumentException.class, () -> new RectangularBarrier(1.0, -1.0));
        assertThrows(IllegalArgumentException.class, () -> new RectangularBarrier(Double.NaN, 1.0));
        RectangularBarrier b = new RectangularBarrier(2.0, 1.0);
        assertThrows(IllegalArgumentException.class, () -> b.transmission(0.0));
        assertThrows(IllegalArgumentException.class, () -> b.decayConstant(2.0));
        assertThrows(IllegalArgumentException.class, () -> b.thickBarrierEstimate(3.0));
    }
}
