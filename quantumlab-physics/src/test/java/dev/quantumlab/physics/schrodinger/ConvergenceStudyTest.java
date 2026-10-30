package dev.quantumlab.physics.schrodinger;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ConvergenceStudyTest {

    private static List<ConvergenceStudy.Row> wellRows(int level) {
        InfiniteWell well = new InfiniteWell(0.0, 1.0);
        return ConvergenceStudy.run(
            Potential.zero(),
            0.0,
            1.0,
            level,
            well.energy(level + 1),
            ConvergenceStudy.halvingGrids(15, 6));
    }

    @Test
    @DisplayName("halving grids double the number of intervals each time")
    void halvingGrids() {
        assertArrayEquals(new int[] {15, 31, 63}, ConvergenceStudy.halvingGrids(15, 3));
        assertArrayEquals(new int[] {1}, ConvergenceStudy.halvingGrids(1, 1));
        assertThrows(IllegalArgumentException.class, () -> ConvergenceStudy.halvingGrids(0, 3));
        assertThrows(IllegalArgumentException.class, () -> ConvergenceStudy.halvingGrids(15, 0));
        assertThrows(IllegalArgumentException.class, () -> ConvergenceStudy.halvingGrids(15, 20));
    }

    @Test
    @DisplayName("the infinite well error falls with observed order 2 at every refinement")
    void wellIsSecondOrder() {
        List<ConvergenceStudy.Row> rows = wellRows(0);
        assertTrue(Double.isNaN(rows.get(0).observedOrder()));
        for (int k = 1; k < rows.size(); k++) {
            assertEquals(2.0, rows.get(k).observedOrder(), 0.02, "row " + k);
        }
    }

    @Test
    @DisplayName("the well error is negative and close to −(nπh)²/12 of the exact energy")
    void wellErrorMatchesTheory() {
        InfiniteWell well = new InfiniteWell(0.0, 1.0);
        for (int level = 0; level < 3; level++) {
            double exact = well.energy(level + 1);
            for (ConvergenceStudy.Row row : wellRows(level).subList(2, 6)) {
                double x = (level + 1) * Math.PI * row.spacing();
                assertTrue(row.error() < 0.0);
                assertEquals(-x * x / 12.0, row.error() / exact, 0.02 * x * x / 12.0);
            }
        }
    }

    @Test
    @DisplayName("each numerical energy equals the exact discrete value (2/h²) sin²(nπh/2L)")
    void matchesDiscreteFormula() {
        InfiniteWell well = new InfiniteWell(0.0, 1.0);
        for (ConvergenceStudy.Row row : wellRows(1)) {
            assertEquals(well.discreteEnergy(2, row.interiorPoints()), row.numerical(), 1e-8);
        }
    }

    @Test
    @DisplayName("the harmonic oscillator ground state also converges at order 2")
    void oscillatorIsSecondOrder() {
        List<ConvergenceStudy.Row> rows =
            ConvergenceStudy.run(
                Potential.harmonic(1.0),
                -10.0,
                10.0,
                0,
                0.5,
                ConvergenceStudy.halvingGrids(99, 4));
        for (int k = 2; k < rows.size(); k++) {
            assertEquals(2.0, rows.get(k).observedOrder(), 0.1, "row " + k);
        }
        assertTrue(Math.abs(rows.get(3).error()) < Math.abs(rows.get(0).error()) / 30.0);
    }

    @Test
    @DisplayName("a zero error gives no order instead of infinity or NaN arithmetic")
    void zeroErrorHasNoOrder() {
        // Use the first grid's own energy as the reference, so its error is exactly zero.
        double exactOnTwoPoints = new Hamiltonian1D(new Grid1D(0, 1, 3), Potential.zero()).energy(0);
        List<ConvergenceStudy.Row> rows =
            ConvergenceStudy.run(
                Potential.zero(), 0, 1, 0, exactOnTwoPoints, new int[] {3, 7});
        assertEquals(0.0, rows.get(0).error(), 0.0);
        assertTrue(rows.get(1).error() != 0.0);
        assertTrue(Double.isNaN(rows.get(1).observedOrder()));
    }

    @Test
    @DisplayName("invalid studies are rejected")
    void validation() {
        int[] grids = {15, 31};
        assertThrows(
            IllegalArgumentException.class,
            () -> ConvergenceStudy.run(null, 0, 1, 0, 1.0, grids));
        assertThrows(
            IllegalArgumentException.class,
            () -> ConvergenceStudy.run(Potential.zero(), 0, 1, 0, 1.0, new int[0]));
        assertThrows(
            IllegalArgumentException.class,
            () -> ConvergenceStudy.run(Potential.zero(), 0, 1, -1, 1.0, grids));
        assertThrows(
            IllegalArgumentException.class,
            () -> ConvergenceStudy.run(Potential.zero(), 0, 1, 15, 1.0, grids));
        assertThrows(
            IllegalArgumentException.class,
            () -> ConvergenceStudy.run(Potential.zero(), 0, 1, 0, Double.NaN, grids));
    }
}
