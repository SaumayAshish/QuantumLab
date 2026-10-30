package dev.quantumlab.physics.dynamics;

import dev.quantumlab.physics.schrodinger.Grid1D;

/**
 * Builds a Gaussian wave packet
 * {@code ψ(x) ∝ exp(−(x − x0)² / 4σ²) · e^{i k0 x}}.
 *
 * <p>{@code σ} is the standard deviation of {@code |ψ|²}, {@code k0} the mean wave number (so the mean
 * momentum is {@code k0} and the mean kinetic energy is about {@code k0²/2 + 1/(8σ²)} with ħ = m = 1).
 * The packet is normalised on the grid, so {@code Σ |ψ_i|² h = 1} exactly (to rounding).
 */
public final class GaussianPacket {

    /** The packet must fit inside the grid with at least this many standard deviations on each side. */
    public static final double MARGIN_IN_SIGMAS = 6.0;

    private GaussianPacket() {}

    public static WaveFunction1D create(Grid1D grid, double x0, double sigma, double k0) {
        if (grid == null) {
            throw new IllegalArgumentException("The grid is required.");
        }
        if (!Double.isFinite(x0) || !Double.isFinite(sigma) || !Double.isFinite(k0)) {
            throw new IllegalArgumentException("The centre, width and wave number must be finite numbers.");
        }
        if (sigma <= 0.0) {
            throw new IllegalArgumentException("The packet width must be positive.");
        }
        if (x0 - MARGIN_IN_SIGMAS * sigma < grid.xMin() || x0 + MARGIN_IN_SIGMAS * sigma > grid.xMax()) {
            throw new IllegalArgumentException(
                "The packet must lie at least " + (int) MARGIN_IN_SIGMAS
                    + " widths inside the walls; move it or make it narrower.");
        }
        double h = grid.spacing();
        if (sigma < 2.0 * h) {
            throw new IllegalArgumentException("The packet is narrower than two grid spacings; use a finer grid.");
        }
        if (Math.abs(k0) * h > 1.0) {
            throw new IllegalArgumentException(
                "The wave number is too large for this grid: |k0|·h must not exceed 1.");
        }
        int n = grid.interiorPoints();
        double[] re = new double[n];
        double[] im = new double[n];
        double sum = 0.0;
        for (int i = 0; i < n; i++) {
            double x = grid.x(i);
            double envelope = Math.exp(-(x - x0) * (x - x0) / (4.0 * sigma * sigma));
            re[i] = envelope * Math.cos(k0 * x);
            im[i] = envelope * Math.sin(k0 * x);
            sum += envelope * envelope;
        }
        double scale = 1.0 / Math.sqrt(sum * h);
        for (int i = 0; i < n; i++) {
            re[i] *= scale;
            im[i] *= scale;
        }
        return new WaveFunction1D(grid, re, im);
    }
}
