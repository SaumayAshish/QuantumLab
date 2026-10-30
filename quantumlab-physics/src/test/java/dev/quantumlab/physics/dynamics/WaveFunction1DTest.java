package dev.quantumlab.physics.dynamics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.quantumlab.physics.schrodinger.Grid1D;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class WaveFunction1DTest {

    /** Three points at 0.25, 0.5, 0.75 with densities 1, 2, 1, which sum to 4 so that Σ|ψ|²h = 1. */
    private static WaveFunction1D small() {
        return new WaveFunction1D(
            new Grid1D(0.0, 1.0, 3), new double[] {1.0, Math.sqrt(2.0), 1.0}, new double[] {0.0, 0.0, 0.0});
    }

    @Test
    @DisplayName("total probability, mean and spread of a small hand-computed state")
    void handComputedObservables() {
        WaveFunction1D psi = small();
        assertEquals(1.0, psi.totalProbability(), 1e-14);
        assertEquals(0.5, psi.meanPosition(), 1e-14);
        // variance = (0.25² · 1 + 0 + 0.25² · 1) · 0.25 = 0.03125
        assertEquals(Math.sqrt(0.03125), psi.positionSpread(), 1e-14);
    }

    @Test
    @DisplayName("an interval includes its left end and excludes its right end")
    void halfOpenInterval() {
        WaveFunction1D psi = small();
        assertEquals(0.25, psi.probabilityBetween(0.25, 0.5), 1e-14);
        assertEquals(0.5, psi.probabilityBetween(0.5, 0.75), 1e-14);
        assertEquals(1.0, psi.probabilityBetween(0.0, 1.0), 1e-14);
        assertEquals(0.0, psi.probabilityBetween(0.8, 0.9), 0.0);
    }

    @Test
    @DisplayName("the density combines both components")
    void densityUsesBothComponents() {
        WaveFunction1D psi = new WaveFunction1D(new Grid1D(0.0, 1.0, 2), new double[] {3.0, 0.0}, new double[] {4.0, 2.0});
        assertEquals(25.0, psi.density(0), 0.0);
        assertEquals(4.0, psi.density(1), 0.0);
        assertEquals(2, psi.densities().length);
    }

    @Test
    @DisplayName("a real wavefunction has zero mean momentum")
    void realStateHasNoMomentum() {
        WaveFunction1D psi = GaussianPacket.create(new Grid1D(-30, 30, 1199), 0.0, 2.0, 0.0);
        assertEquals(0.0, psi.meanMomentum(), 1e-12);
    }

    @Test
    @DisplayName("input arrays are copied and checked")
    void validationAndCopies() {
        Grid1D grid = new Grid1D(0.0, 1.0, 3);
        double[] re = {1.0, 0.0, 0.0};
        double[] im = {0.0, 0.0, 0.0};
        WaveFunction1D psi = new WaveFunction1D(grid, re, im);
        re[0] = 99.0;
        assertEquals(1.0, psi.real(0), 0.0);
        psi.densities()[0] = 99.0;
        assertEquals(1.0, psi.density(0), 0.0);

        assertThrows(IllegalArgumentException.class, () -> new WaveFunction1D(null, re, im));
        assertThrows(IllegalArgumentException.class, () -> new WaveFunction1D(grid, new double[2], new double[3]));
        assertThrows(IllegalArgumentException.class, () -> new WaveFunction1D(grid, new double[3], new double[2]));
        assertThrows(
            IllegalArgumentException.class,
            () -> new WaveFunction1D(grid, new double[] {Double.NaN, 0, 0}, new double[3]));
        assertThrows(IllegalArgumentException.class, () -> small().probabilityBetween(0.6, 0.4));
    }
}
