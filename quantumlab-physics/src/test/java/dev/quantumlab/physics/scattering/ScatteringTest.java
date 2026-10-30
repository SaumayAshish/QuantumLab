package dev.quantumlab.physics.scattering;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ScatteringTest {

    private static final List<Layer> DOUBLE_BARRIER =
        List.of(new Layer(5.0, 0.5), new Layer(0.0, 1.0), new Layer(5.0, 0.5));
    private static final List<Layer> STAIRS =
        List.of(new Layer(1.0, 0.5), new Layer(3.0, 0.3), new Layer(0.5, 0.8));

    @Test
    @DisplayName("no layers, or layers with zero potential, transmit everything")
    void freeParticle() {
        assertEquals(1.0, Scattering.transmission(List.of(), 1.3), 1e-15);
        assertEquals(1.0, Scattering.transmission(List.of(new Layer(0.0, 2.0)), 0.4), 1e-14);
    }

    @Test
    @DisplayName("transfer-matrix results match independent plane-wave matching done in NumPy")
    void numpyReferences() {
        // Reference values: linear system for plane-wave amplitudes, solved with numpy.linalg.solve.
        assertEquals(0.055861437216915076, Scattering.transmission(List.of(new Layer(2.0, 1.5)), 1.0), 1e-13);
        assertEquals(0.8050814749000138, Scattering.transmission(List.of(new Layer(2.0, 1.5)), 3.0), 1e-13);
        assertEquals(0.0014797089248717486, Scattering.transmission(DOUBLE_BARRIER, 0.5), 1e-13);
        assertEquals(0.3005447578900813, Scattering.transmission(DOUBLE_BARRIER, 2.0), 1e-13);
        assertEquals(0.12535479645499475, Scattering.transmission(DOUBLE_BARRIER, 4.0), 1e-13);
        assertEquals(0.9804524186390046, Scattering.transmission(DOUBLE_BARRIER, 7.0), 1e-13);
        assertEquals(0.22703698980792894, Scattering.transmission(STAIRS, 0.8), 1e-13);
        assertEquals(0.6944092809059497, Scattering.transmission(STAIRS, 2.0), 1e-13);
        assertEquals(0.8369604903457447, Scattering.transmission(List.of(new Layer(-1.0, 1.0)), 0.7), 1e-13);
    }

    @Test
    @DisplayName("transmission is the same read from the other side (reciprocity)")
    void reciprocity() {
        List<Layer> reversed = new ArrayList<>(STAIRS);
        Collections.reverse(reversed);
        for (double e : new double[] {0.3, 0.8, 1.7, 2.9, 6.0}) {
            assertEquals(
                Scattering.transmission(STAIRS, e), Scattering.transmission(reversed, e), 1e-12);
        }
    }

    @Test
    @DisplayName("T and R are probabilities that add to 1")
    void probabilities() {
        for (double e = 0.05; e < 12.0; e += 0.37) {
            double t = Scattering.transmission(DOUBLE_BARRIER, e);
            assertTrue(t > 0.0 && t <= 1.0 + 1e-12, "T=" + t + " at E=" + e);
            assertEquals(1.0, t + Scattering.reflection(DOUBLE_BARRIER, e), 1e-15);
        }
    }

    @Test
    @DisplayName("a double barrier has a transmission peak that the single barrier lacks")
    void resonance() {
        double best = 0.0;
        for (double e = 0.2; e < 4.9; e += 0.005) {
            best = Math.max(best, Scattering.transmission(DOUBLE_BARRIER, e));
        }
        double single = Scattering.transmission(List.of(new Layer(5.0, 0.5)), 2.0);
        assertTrue(best > 0.9, "peak " + best);
        assertTrue(single < 0.5, "single barrier at E=2: " + single);
    }

    @Test
    @DisplayName("invalid inputs are rejected")
    void validation() {
        assertThrows(IllegalArgumentException.class, () -> Scattering.transmission(null, 1.0));
        assertThrows(IllegalArgumentException.class, () -> Scattering.transmission(List.of(), 0.0));
        assertThrows(IllegalArgumentException.class, () -> Scattering.transmission(List.of(), -1.0));
        assertThrows(IllegalArgumentException.class, () -> Scattering.transmission(List.of(), Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> new Layer(Double.NaN, 1.0));
        assertThrows(IllegalArgumentException.class, () -> new Layer(1.0, 0.0));
        assertThrows(IllegalArgumentException.class, () -> new Layer(1.0, Double.POSITIVE_INFINITY));
    }
}
