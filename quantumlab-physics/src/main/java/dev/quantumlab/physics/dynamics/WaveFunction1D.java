package dev.quantumlab.physics.dynamics;

import dev.quantumlab.physics.schrodinger.Grid1D;

/**
 * A complex wavefunction {@code ψ(x_i) = re_i + i·im_i} on the unknown points of a {@link Grid1D}.
 *
 * <p>Probabilities are grid sums: {@code Σ |ψ_i|² · h} approximates {@code ∫ |ψ|² dx}. Instances are
 * immutable; every operation that changes the state returns a new object.
 */
public final class WaveFunction1D {

    private final Grid1D grid;
    private final double[] re;
    private final double[] im;

    public WaveFunction1D(Grid1D grid, double[] re, double[] im) {
        if (grid == null) {
            throw new IllegalArgumentException("The grid is required.");
        }
        if (re == null || im == null || re.length != grid.interiorPoints() || im.length != re.length) {
            throw new IllegalArgumentException(
                "Both component arrays must have one entry per grid point (" + grid.interiorPoints() + ").");
        }
        for (int i = 0; i < re.length; i++) {
            if (!Double.isFinite(re[i]) || !Double.isFinite(im[i])) {
                throw new IllegalArgumentException("The wavefunction must be finite at every point.");
            }
        }
        this.grid = grid;
        this.re = re.clone();
        this.im = im.clone();
    }

    public Grid1D grid() {
        return grid;
    }

    public double real(int i) {
        return re[i];
    }

    public double imaginary(int i) {
        return im[i];
    }

    /** {@code |ψ(x_i)|²}. */
    public double density(int i) {
        return re[i] * re[i] + im[i] * im[i];
    }

    /** All densities, left to right. */
    public double[] densities() {
        double[] d = new double[re.length];
        for (int i = 0; i < d.length; i++) {
            d[i] = density(i);
        }
        return d;
    }

    /** {@code Σ |ψ_i|² · h}, which should stay at 1 for a normalised state. */
    public double totalProbability() {
        return sumDensity(0, re.length) * grid.spacing();
    }

    /** The probability of finding the particle at positions {@code x} with {@code from ≤ x < to}. */
    public double probabilityBetween(double from, double to) {
        if (Double.isNaN(from) || Double.isNaN(to) || from > to) {
            throw new IllegalArgumentException("Invalid interval [" + from + ", " + to + ").");
        }
        double sum = 0.0;
        for (int i = 0; i < re.length; i++) {
            double x = grid.x(i);
            if (x >= from && x < to) {
                sum += density(i);
            }
        }
        return sum * grid.spacing();
    }

    /** {@code ⟨x⟩ = Σ x_i |ψ_i|² h}, for a normalised state. */
    public double meanPosition() {
        double sum = 0.0;
        for (int i = 0; i < re.length; i++) {
            sum += grid.x(i) * density(i);
        }
        return sum * grid.spacing();
    }

    /** The standard deviation of position, {@code sqrt(⟨x²⟩ − ⟨x⟩²)}. */
    public double positionSpread() {
        double mean = meanPosition();
        double sum = 0.0;
        for (int i = 0; i < re.length; i++) {
            double d = grid.x(i) - mean;
            sum += d * d * density(i);
        }
        return Math.sqrt(sum * grid.spacing());
    }

    /**
     * The mean momentum {@code ⟨p⟩ = Im ∫ ψ* ψ′ dx}, using central differences (and zero beyond the
     * walls).
     */
    public double meanMomentum() {
        int n = re.length;
        double h = grid.spacing();
        double sum = 0.0;
        for (int i = 0; i < n; i++) {
            double reNext = i + 1 < n ? re[i + 1] : 0.0;
            double imNext = i + 1 < n ? im[i + 1] : 0.0;
            double rePrev = i > 0 ? re[i - 1] : 0.0;
            double imPrev = i > 0 ? im[i - 1] : 0.0;
            double dRe = (reNext - rePrev) / (2.0 * h);
            double dIm = (imNext - imPrev) / (2.0 * h);
            sum += re[i] * dIm - im[i] * dRe;
        }
        return sum * h;
    }

    double[] realCopy() {
        return re.clone();
    }

    double[] imaginaryCopy() {
        return im.clone();
    }

    private double sumDensity(int from, int to) {
        double sum = 0.0;
        for (int i = from; i < to; i++) {
            sum += density(i);
        }
        return sum;
    }
}
