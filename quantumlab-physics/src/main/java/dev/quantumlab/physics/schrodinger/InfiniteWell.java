package dev.quantumlab.physics.schrodinger;

/**
 * The infinite square well on {@code [xMin, xMax]}, whose exact solution is the reference every
 * numerical result is checked against (ħ = m = 1).
 *
 * <p>With width {@code L}, the levels are numbered {@code n = 1, 2, …} (the physics convention, one
 * more than the index used by {@link Hamiltonian1D}) and
 * {@code E_n = n² π² / (2 L²)}, {@code ψ_n(x) = √(2/L) sin(nπ (x − xMin) / L)}.
 *
 * <p>The finite-difference version of the same problem is also solvable exactly:
 * {@code E_n(h) = (2/h²) sin²(nπh / 2L)}. It is always below {@code E_n}, and the gap shrinks like
 * {@code h²}.
 */
public final class InfiniteWell {

    private final double xMin;
    private final double xMax;

    public InfiniteWell(double xMin, double xMax) {
        if (!Double.isFinite(xMin) || !Double.isFinite(xMax) || !(xMax > xMin)) {
            throw new IllegalArgumentException("Need finite walls with xMin < xMax.");
        }
        this.xMin = xMin;
        this.xMax = xMax;
    }

    public double width() {
        return xMax - xMin;
    }

    /** A grid on exactly this well with the given number of unknowns. */
    public Grid1D grid(int interiorPoints) {
        return new Grid1D(xMin, xMax, interiorPoints);
    }

    /** The exact energy of level {@code n ≥ 1}. */
    public double energy(int n) {
        requireLevel(n);
        double L = width();
        return n * n * Math.PI * Math.PI / (2.0 * L * L);
    }

    /** The exact energy of level {@code n ≥ 1} for the finite-difference problem with the given grid size. */
    public double discreteEnergy(int n, int interiorPoints) {
        requireLevel(n);
        double h = width() / (interiorPoints + 1);
        double s = Math.sin(n * Math.PI * h / (2.0 * width()));
        return 2.0 * s * s / (h * h);
    }

    /** The exact wavefunction of level {@code n ≥ 1} at {@code x}; zero outside the well. */
    public double wavefunction(int n, double x) {
        requireLevel(n);
        if (x < xMin || x > xMax) {
            return 0.0;
        }
        return Math.sqrt(2.0 / width()) * Math.sin(n * Math.PI * (x - xMin) / width());
    }

    private static void requireLevel(int n) {
        if (n < 1) {
            throw new IllegalArgumentException("The level number starts at 1, got " + n);
        }
    }
}
