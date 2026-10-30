package dev.quantumlab.physics.dynamics;

import dev.quantumlab.physics.schrodinger.Grid1D;
import dev.quantumlab.physics.schrodinger.Potential;

/**
 * Advances a wavefunction in time with the Crank–Nicolson scheme
 * {@code (1 + i·dt/2·H) ψ(t+dt) = (1 − i·dt/2·H) ψ(t)}, where {@code H} is the same finite-difference
 * Hamiltonian as in {@code Hamiltonian1D} (ħ = m = 1, hard walls).
 *
 * <p>For a Hermitian {@code H} the step is exactly unitary, so the total probability is conserved up to
 * rounding for any time step; the time step only controls the accuracy of the phases (second order in
 * {@code dt}). The implicit system is tridiagonal and is solved with a complex Thomas algorithm whose
 * factors are computed once.
 *
 * <p>Instances are immutable and may be shared between threads.
 */
public final class CrankNicolson {

    private final Grid1D grid;
    private final double dt;
    private final double[] potential;
    /** Reciprocal pivots {@code 1 / den_j} and the forward-elimination factors, as real/imaginary parts. */
    private final double[] pivotInvRe;
    private final double[] pivotInvIm;
    private final double[] upperRe;
    private final double[] upperIm;
    private final double offDiagonal;

    public CrankNicolson(Grid1D grid, Potential v, double dt) {
        if (grid == null || v == null) {
            throw new IllegalArgumentException("The grid and the potential are required.");
        }
        if (!Double.isFinite(dt) || dt <= 0.0) {
            throw new IllegalArgumentException("The time step must be positive and finite.");
        }
        this.grid = grid;
        this.dt = dt;
        int n = grid.interiorPoints();
        double h = grid.spacing();
        this.potential = new double[n];
        for (int i = 0; i < n; i++) {
            double value = v.value(grid.x(i));
            if (!Double.isFinite(value)) {
                throw new IllegalArgumentException("The potential must be finite at every grid point.");
            }
            potential[i] = value;
        }
        this.offDiagonal = -0.5 / (h * h);
        double c = 0.5 * dt;

        // A has diagonal b_j = 1 + i c d_j and constant off-diagonals a = i c o (purely imaginary).
        double aIm = c * offDiagonal;
        pivotInvRe = new double[n];
        pivotInvIm = new double[n];
        upperRe = new double[n];
        upperIm = new double[n];
        double prevUpperRe = 0.0;
        double prevUpperIm = 0.0;
        for (int j = 0; j < n; j++) {
            double bIm = c * (1.0 / (h * h) + potential[j]);
            // denominator = b_j − a·upper_{j−1}, with a = i·aIm
            double denRe = 1.0;
            double denIm = bIm;
            if (j > 0) {
                // a·upper = i aIm (uR + i uI) = −aIm·uI + i aIm·uR
                denRe -= -aIm * prevUpperIm;
                denIm -= aIm * prevUpperRe;
            }
            double mod = denRe * denRe + denIm * denIm;
            pivotInvRe[j] = denRe / mod;
            pivotInvIm[j] = -denIm / mod;
            // upper_j = a / den = i aIm · conj(den)/|den|²
            upperRe[j] = -aIm * pivotInvIm[j];
            upperIm[j] = aIm * pivotInvRe[j];
            prevUpperRe = upperRe[j];
            prevUpperIm = upperIm[j];
        }
    }

    public Grid1D grid() {
        return grid;
    }

    public double timeStep() {
        return dt;
    }

    /** Advances {@code state} by one time step. */
    public WaveFunction1D step(WaveFunction1D state) {
        double[][] result = advance(state, 1);
        return new WaveFunction1D(grid, result[0], result[1]);
    }

    /** Advances {@code state} by {@code steps} time steps. */
    public WaveFunction1D evolve(WaveFunction1D state, int steps) {
        if (steps < 0) {
            throw new IllegalArgumentException("The number of steps cannot be negative.");
        }
        double[][] result = advance(state, steps);
        return new WaveFunction1D(grid, result[0], result[1]);
    }

    /** {@code ⟨ψ|H|ψ⟩} with the discrete Hamiltonian; conserved by the scheme (up to rounding). */
    public double energy(WaveFunction1D state) {
        requireSameGrid(state);
        int n = grid.interiorPoints();
        double[] re = state.realCopy();
        double[] im = state.imaginaryCopy();
        double sum = 0.0;
        for (int i = 0; i < n; i++) {
            double hRe = hamiltonianTimes(re, i);
            double hIm = hamiltonianTimes(im, i);
            sum += re[i] * hRe + im[i] * hIm;
        }
        return sum * grid.spacing();
    }

    private double[][] advance(WaveFunction1D state, int steps) {
        requireSameGrid(state);
        int n = grid.interiorPoints();
        double c = 0.5 * dt;
        double aIm = c * offDiagonal;
        double[] re = state.realCopy();
        double[] im = state.imaginaryCopy();
        double[] rhsRe = new double[n];
        double[] rhsIm = new double[n];
        double[] yRe = new double[n];
        double[] yIm = new double[n];
        for (int s = 0; s < steps; s++) {
            for (int j = 0; j < n; j++) {
                rhsRe[j] = re[j] + c * hamiltonianTimes(im, j);
                rhsIm[j] = im[j] - c * hamiltonianTimes(re, j);
            }
            // forward elimination: y_j = (r_j − a y_{j−1}) / den_j
            for (int j = 0; j < n; j++) {
                double tRe = rhsRe[j];
                double tIm = rhsIm[j];
                if (j > 0) {
                    // a·y_{j−1} = i aIm (yR + i yI) = −aIm·yI + i aIm·yR
                    tRe -= -aIm * yIm[j - 1];
                    tIm -= aIm * yRe[j - 1];
                }
                yRe[j] = tRe * pivotInvRe[j] - tIm * pivotInvIm[j];
                yIm[j] = tRe * pivotInvIm[j] + tIm * pivotInvRe[j];
            }
            // back substitution: x_j = y_j − upper_j x_{j+1}
            re[n - 1] = yRe[n - 1];
            im[n - 1] = yIm[n - 1];
            for (int j = n - 2; j >= 0; j--) {
                re[j] = yRe[j] - (upperRe[j] * re[j + 1] - upperIm[j] * im[j + 1]);
                im[j] = yIm[j] - (upperRe[j] * im[j + 1] + upperIm[j] * re[j + 1]);
            }
        }
        return new double[][] {re, im};
    }

    /** {@code (H u)_i} for a real vector {@code u}. */
    private double hamiltonianTimes(double[] u, int i) {
        double h = grid.spacing();
        double left = i > 0 ? u[i - 1] : 0.0;
        double right = i + 1 < u.length ? u[i + 1] : 0.0;
        return (1.0 / (h * h) + potential[i]) * u[i] + offDiagonal * (left + right);
    }

    private void requireSameGrid(WaveFunction1D state) {
        if (state == null || !grid.equals(state.grid())) {
            throw new IllegalArgumentException("The state must live on the same grid as the propagator.");
        }
    }
}
