package dev.quantumlab.physics.schrodinger;

/**
 * An energy eigenstate on a grid: its energy and its real amplitude {@code ψ(x_i)} at every unknown
 * point.
 *
 * <p>The amplitude is normalised so that {@code Σ ψ_i² · h = 1}, the grid version of
 * {@code ∫ |ψ|² dx = 1}. The overall sign is fixed so that the first non-negligible value is
 * positive. Instances are immutable.
 */
public final class StationaryState {

    private final double energy;
    private final Grid1D grid;
    private final double[] amplitude;

    StationaryState(double energy, Grid1D grid, double[] amplitude) {
        this.energy = energy;
        this.grid = grid;
        this.amplitude = amplitude.clone();
    }

    public double energy() {
        return energy;
    }

    public Grid1D grid() {
        return grid;
    }

    /** {@code ψ(x_i)}. */
    public double amplitude(int i) {
        return amplitude[i];
    }

    public double[] amplitudes() {
        return amplitude.clone();
    }

    /** The probability density {@code |ψ(x_i)|²}. */
    public double density(int i) {
        return amplitude[i] * amplitude[i];
    }

    /** The probability of finding the particle in the grid cells {@code from … to − 1}. */
    public double probability(int from, int to) {
        if (from < 0 || to > amplitude.length || from > to) {
            throw new IllegalArgumentException("Invalid range [" + from + ", " + to + ").");
        }
        double sum = 0.0;
        for (int i = from; i < to; i++) {
            sum += amplitude[i] * amplitude[i];
        }
        return sum * grid.spacing();
    }

    /** The number of sign changes along the amplitude, which equals the level index. */
    public int nodes() {
        int count = 0;
        double previous = 0.0;
        for (double v : amplitude) {
            if (Math.abs(v) < 1e-12) {
                continue;
            }
            if (previous != 0.0 && (v > 0.0) != (previous > 0.0)) {
                count++;
            }
            previous = v;
        }
        return count;
    }
}
