package dev.quantumlab.physics.schrodinger;

import java.util.ArrayList;
import java.util.List;

/**
 * Measures how the error of one numerical energy level shrinks as the grid is refined.
 *
 * <p>For the second-order three-point scheme the error should fall like {@code h²}, so halving the
 * spacing should divide the error by about four: an observed order of about 2. This class computes
 * that order from the numbers instead of assuming it.
 */
public final class ConvergenceStudy {

    private ConvergenceStudy() {}

    /**
     * One grid size.
     *
     * @param interiorPoints the number of unknowns
     * @param spacing the grid spacing h
     * @param numerical the computed energy
     * @param error numerical minus reference
     * @param observedOrder {@code ln(|e_prev| / |e|) / ln(h_prev / h)} against the previous row, or
     *     NaN for the first row or when either error is zero
     */
    public record Row(
            int interiorPoints,
            double spacing,
            double numerical,
            double error,
            double observedOrder) {}

    /**
     * Interior point counts that halve the spacing at every step: {@code N → 2(N + 1) − 1}.
     *
     * @param first the first count, at least 1
     * @param count how many counts to return, at least 1
     * @throws IllegalArgumentException if a count would exceed {@link Grid1D#MAX_POINTS}
     */
    public static int[] halvingGrids(int first, int count) {
        if (first < 1 || count < 1) {
            throw new IllegalArgumentException("first and count must be at least 1.");
        }
        int[] grids = new int[count];
        long n = first;
        for (int k = 0; k < count; k++) {
            if (n > Grid1D.MAX_POINTS) {
                throw new IllegalArgumentException(
                        "Grid " + n + " exceeds the maximum of " + Grid1D.MAX_POINTS + " points.");
            }
            grids[k] = (int) n;
            n = 2 * (n + 1) - 1;
        }
        return grids;
    }

    /**
     * Solves the same problem on each grid and compares the chosen level with a reference energy.
     *
     * @param level zero-based level index (0 is the lowest)
     * @param reference the exact energy the numerical level approximates
     * @param pointCounts interior point counts, in the order to be reported
     */
    public static List<Row> run(
            Potential potential,
            double xMin,
            double xMax,
            int level,
            double reference,
            int[] pointCounts) {
        if (potential == null || pointCounts == null || pointCounts.length == 0) {
            throw new IllegalArgumentException("A potential and at least one grid size are required.");
        }
        if (level < 0) {
            throw new IllegalArgumentException("level must not be negative.");
        }
        if (!Double.isFinite(reference)) {
            throw new IllegalArgumentException("The reference energy must be finite.");
        }
        List<Row> rows = new ArrayList<>(pointCounts.length);
        Row previous = null;
        for (int n : pointCounts) {
            if (level >= n) {
                throw new IllegalArgumentException(
                        "Level " + level + " does not exist on a grid of " + n + " points.");
            }
            Grid1D grid = new Grid1D(xMin, xMax, n);
            double numerical = new Hamiltonian1D(grid, potential).energy(level);
            double error = numerical - reference;
            double order = Double.NaN;
            if (previous != null && error != 0.0 && previous.error() != 0.0) {
                order =
                        Math.log(Math.abs(previous.error()) / Math.abs(error))
                                / Math.log(previous.spacing() / grid.spacing());
            }
            previous = new Row(n, grid.spacing(), numerical, error, order);
            rows.add(previous);
        }
        return List.copyOf(rows);
    }
}
