package dev.quantumlab.physics.scattering;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PacketTransmissionTest {

    private static final RectangularBarrier BARRIER = new RectangularBarrier(2.0, 1.0);

    @Test
    @DisplayName("matches the value from an independent NumPy integration")
    void matchesReference() {
        // NumPy: trapezoid over k in (0.001, 6) with 200001 points gave 0.25053026896
        assertEquals(0.25053026896, PacketTransmission.average(BARRIER, 3.0, 1.5), 1e-6);
    }

    @Test
    @DisplayName("a very narrow momentum distribution gives T at the mean energy")
    void sharpMomentumLimit() {
        double atMean = BARRIER.transmission(1.5 * 1.5 / 2.0);
        assertEquals(0.24452751863, atMean, 1e-10);
        assertEquals(atMean, PacketTransmission.average(BARRIER, 60.0, 1.5), 2e-4);
    }

    @Test
    @DisplayName("the average of a probability is a probability, and a thick barrier blocks a slow packet")
    void range() {
        double thick = PacketTransmission.average(new RectangularBarrier(5.0, 4.0), 3.0, 1.0);
        assertTrue(thick >= 0.0 && thick < 1e-3, "thick = " + thick);
        double fast = PacketTransmission.average(BARRIER, 3.0, 4.0);
        assertTrue(fast > 0.5 && fast <= 1.0, "fast = " + fast);
    }

    @Test
    @DisplayName("packets with a visible left-moving part and bad numbers are rejected")
    void validation() {
        assertThrows(IllegalArgumentException.class, () -> PacketTransmission.average(null, 3.0, 1.5));
        assertThrows(IllegalArgumentException.class, () -> PacketTransmission.average(BARRIER, 0.0, 1.5));
        assertThrows(IllegalArgumentException.class, () -> PacketTransmission.average(BARRIER, 3.0, -1.5));
        assertThrows(IllegalArgumentException.class, () -> PacketTransmission.average(BARRIER, Double.NaN, 1.5));
        assertThrows(IllegalArgumentException.class, () -> PacketTransmission.average(BARRIER, 1.0, 1.0));
    }
}
