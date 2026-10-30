package dev.quantumlab.physics.schrodinger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class InfiniteWellTest {

    /** Composite Simpson's rule for ∫ f over [a, b] with an even number of intervals. */
    private static double simpson(java.util.function.DoubleUnaryOperator f, double a, double b, int intervals) {
        double h = (b - a) / intervals;
        double sum = f.applyAsDouble(a) + f.applyAsDouble(b);
        for (int i = 1; i < intervals; i++) {
            sum += f.applyAsDouble(a + i * h) * (i % 2 == 1 ? 4.0 : 2.0);
        }
        return sum * h / 3.0;
    }

    @Test
    @DisplayName("the exact levels are n²π²/(2L²): π²/2 for n = 1 on a unit well, four times that for n = 2")
    void exactEnergies() {
        InfiniteWell w = new InfiniteWell(0.0, 1.0);
        assertEquals(Math.PI * Math.PI / 2.0, w.energy(1), 1e-14);
        assertEquals(4.0 * w.energy(1), w.energy(2), 1e-13);
        assertEquals(9.0 * w.energy(1), w.energy(3), 1e-13);
        InfiniteWell wide = new InfiniteWell(-1.0, 1.0);
        assertEquals(w.energy(1) / 4.0, wide.energy(1), 1e-14);
    }

    @Test
    @DisplayName("the discrete energies lie below the exact ones and approach them as the grid is refined")
    void discreteEnergiesApproachExact() {
        InfiniteWell w = new InfiniteWell(0.0, 1.0);
        double previous = Double.NEGATIVE_INFINITY;
        for (int points : new int[] {10, 40, 160, 640}) {
            double d = w.discreteEnergy(1, points);
            assertTrue(d < w.energy(1));
            assertTrue(d > previous);
            previous = d;
        }
        assertEquals(w.energy(1), w.discreteEnergy(1, 100_000), 1e-6);
    }

    @Test
    @DisplayName("the exact wavefunctions are normalised, orthogonal and vanish at and beyond the walls")
    void wavefunctions() {
        InfiniteWell w = new InfiniteWell(-1.0, 3.0);
        for (int n = 1; n <= 4; n++) {
            int level = n;
            assertEquals(1.0, simpson(x -> Math.pow(w.wavefunction(level, x), 2), -1.0, 3.0, 4000), 1e-9);
            assertEquals(0.0, w.wavefunction(n, -1.0), 1e-12);
            assertEquals(0.0, w.wavefunction(n, 3.0), 1e-12);
            assertEquals(0.0, w.wavefunction(n, 3.5), 0.0);
            assertEquals(0.0, w.wavefunction(n, -7.0), 0.0);
        }
        assertEquals(0.0, simpson(x -> w.wavefunction(1, x) * w.wavefunction(2, x), -1.0, 3.0, 4000), 1e-9);
        assertEquals(0.0, simpson(x -> w.wavefunction(1, x) * w.wavefunction(3, x), -1.0, 3.0, 4000), 1e-9);
    }

    @Test
    @DisplayName("a grid made by the well spans exactly the well")
    void gridMatchesWell() {
        InfiniteWell w = new InfiniteWell(-2.0, 6.0);
        Grid1D g = w.grid(79);
        assertEquals(-2.0, g.xMin(), 0.0);
        assertEquals(6.0, g.xMax(), 0.0);
        assertEquals(79, g.interiorPoints());
        assertEquals(8.0, w.width(), 0.0);
    }

    @Test
    @DisplayName("invalid wells and level numbers are rejected")
    void validation() {
        assertThrows(IllegalArgumentException.class, () -> new InfiniteWell(1.0, 1.0));
        assertThrows(IllegalArgumentException.class, () -> new InfiniteWell(Double.NaN, 1.0));
        InfiniteWell w = new InfiniteWell(0.0, 1.0);
        assertThrows(IllegalArgumentException.class, () -> w.energy(0));
        assertThrows(IllegalArgumentException.class, () -> w.wavefunction(0, 0.5));
        assertThrows(IllegalArgumentException.class, () -> w.discreteEnergy(0, 10));
    }
}
