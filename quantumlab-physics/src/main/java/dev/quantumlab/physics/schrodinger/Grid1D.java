package dev.quantumlab.physics.schrodinger;

/**
 * A uniform one-dimensional grid on {@code [xMin, xMax]} where the wavefunction is forced to zero at
 * both ends (hard walls).
 *
 * <p>Only the {@code interiorPoints} points strictly between the walls carry unknowns. With spacing
 * {@code h = (xMax − xMin) / (interiorPoints + 1)}, the unknowns sit at
 * {@code x_i = xMin + (i + 1)·h} for {@code i = 0 … interiorPoints − 1}.
 *
 * @param xMin the left wall
 * @param xMax the right wall
 * @param interiorPoints the number of unknowns, between 1 and {@link #MAX_POINTS}
 */
public record Grid1D(double xMin, double xMax, int interiorPoints) {

    /** Large enough for any screen, small enough that eigenvalue bisection stays fast. */
    public static final int MAX_POINTS = 100_000;

    public Grid1D {
        if (!Double.isFinite(xMin) || !Double.isFinite(xMax)) {
            throw new IllegalArgumentException("The walls must be finite numbers.");
        }
        if (!(xMax > xMin)) {
            throw new IllegalArgumentException("The right wall must be to the right of the left wall.");
        }
        if (interiorPoints < 1 || interiorPoints > MAX_POINTS) {
            throw new IllegalArgumentException(
                "interiorPoints must be between 1 and " + MAX_POINTS + ", got " + interiorPoints);
        }
        if (!Double.isFinite(xMax - xMin)) {
            throw new IllegalArgumentException("The interval is too long.");
        }
    }

    public double length() {
        return xMax - xMin;
    }

    /** The distance between neighbouring points. */
    public double spacing() {
        return length() / (interiorPoints + 1);
    }

    /** The position of the {@code i}-th unknown. */
    public double x(int i) {
        if (i < 0 || i >= interiorPoints) {
            throw new IllegalArgumentException("Index must be in [0, " + interiorPoints + "), got " + i);
        }
        return xMin + (i + 1) * spacing();
    }

    /** All unknown positions, left to right. */
    public double[] points() {
        double[] p = new double[interiorPoints];
        for (int i = 0; i < p.length; i++) {
            p[i] = x(i);
        }
        return p;
    }
}
