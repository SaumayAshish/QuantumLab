package dev.quantumlab.physics.dynamics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.quantumlab.physics.schrodinger.Grid1D;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GaussianPacketTest {

    private static final Grid1D GRID = new Grid1D(-80.0, 80.0, 3199); // h = 0.05

    @Test
    @DisplayName("the packet is normalised and has the requested centre and width")
    void normalisedWithRequestedMoments() {
        WaveFunction1D psi = GaussianPacket.create(GRID, -20.0, 3.0, 1.5);
        assertEquals(1.0, psi.totalProbability(), 1e-13);
        assertEquals(-20.0, psi.meanPosition(), 1e-9);
        assertEquals(3.0, psi.positionSpread(), 1e-9);
    }

    @Test
    @DisplayName("the mean momentum is k0, up to the central-difference factor sin(k0 h)/h")
    void meanMomentum() {
        double h = GRID.spacing();
        WaveFunction1D psi = GaussianPacket.create(GRID, -20.0, 3.0, 1.5);
        assertEquals(Math.sin(1.5 * h) / h, psi.meanMomentum(), 1e-4);
        assertEquals(1.5, psi.meanMomentum(), 2e-3);
        assertEquals(-Math.sin(1.5 * h) / h, GaussianPacket.create(GRID, -20.0, 3.0, -1.5).meanMomentum(), 1e-4);
    }

    @Test
    @DisplayName("packets that do not fit or cannot be resolved are rejected")
    void validation() {
        assertThrows(IllegalArgumentException.class, () -> GaussianPacket.create(null, 0, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> GaussianPacket.create(GRID, 0.0, 0.0, 1.0));
        assertThrows(IllegalArgumentException.class, () -> GaussianPacket.create(GRID, 0.0, -1.0, 1.0));
        assertThrows(IllegalArgumentException.class, () -> GaussianPacket.create(GRID, Double.NaN, 1.0, 1.0));
        assertThrows(IllegalArgumentException.class, () -> GaussianPacket.create(GRID, 0.0, 1.0, Double.POSITIVE_INFINITY));
        // 6 widths of 3 reach beyond the left wall at −80
        assertThrows(IllegalArgumentException.class, () -> GaussianPacket.create(GRID, -70.0, 3.0, 1.0));
        assertThrows(IllegalArgumentException.class, () -> GaussianPacket.create(GRID, 70.0, 3.0, 1.0));
        // narrower than two grid spacings (h = 0.05)
        assertThrows(IllegalArgumentException.class, () -> GaussianPacket.create(GRID, 0.0, 0.09, 1.0));
        // |k0| h = 25 · 0.05 > 1
        assertThrows(IllegalArgumentException.class, () -> GaussianPacket.create(GRID, 0.0, 3.0, 25.0));
    }
}
