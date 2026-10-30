package dev.quantumlab.physics.schrodinger;

import dev.quantumlab.math.SymmetricTridiagonal;

/**
 * The one-dimensional time-independent Schrödinger equation {@code −½ ψ″ + V ψ = E ψ} (ħ = m = 1)
 * on a {@link Grid1D}, solved by finite differences.
 *
 * <p>The second derivative is replaced by the three-point formula
 * {@code ψ″(x_i) ≈ (ψ_{i−1} − 2ψ_i + ψ_{i+1}) / h²}, which is second-order accurate. The
 * Hamiltonian then becomes a symmetric tridiagonal matrix with
 * {@code diagonal_i = 1/h² + V(x_i)} and {@code offDiagonal = −1/(2h²)}, whose eigenvalues are the
 * energies and whose eigenvectors are the states. The walls impose {@code ψ = 0} just outside the
 * first and last unknown.
 *
 * <p>Levels are numbered from 0 (the ground state). All energies are for the discretised problem;
 * they approach the continuum values as the grid is refined, with an error that shrinks like
 * {@code h²} for smooth potentials, and they are only meaningful for levels whose wavefunctions
 * vary slowly compared with the grid spacing.
 */
public final class Hamiltonian1D {

    private final Grid1D grid;
    private final SymmetricTridiagonal matrix;

    /**
     * @throws IllegalArgumentException if the potential is not finite at some grid point
     */
    public Hamiltonian1D(Grid1D grid, Potential potential) {
        if (grid == null || potential == null) {
            throw new IllegalArgumentException("The grid and the potential must not be null.");
        }
        this.grid = grid;
        int n = grid.interiorPoints();
        double h = grid.spacing();
        double kinetic = 1.0 / (h * h);
        double[] diagonal = new double[n];
        double[] offDiagonal = new double[n - 1];
        for (int i = 0; i < n; i++) {
            double v = potential.value(grid.x(i));
            if (!Double.isFinite(v)) {
                throw new IllegalArgumentException(
                    "The potential is not finite at x = " + grid.x(i) + ".");
            }
            diagonal[i] = kinetic + v;
        }
        java.util.Arrays.fill(offDiagonal, -0.5 * kinetic);
        this.matrix = new SymmetricTridiagonal(diagonal, offDiagonal);
    }

    public Grid1D grid() {
        return grid;
    }

    /** The Hamiltonian as a matrix. */
    public SymmetricTridiagonal matrix() {
        return matrix;
    }

    /** The energy of level {@code n}, {@code n = 0} being the ground state. */
    public double energy(int n) {
        return matrix.eigenvalue(n);
    }

    /** The {@code count} lowest energies in ascending order. */
    public double[] energies(int count) {
        return matrix.eigenvalues(count);
    }

    /** The state of level {@code n}, normalised so that {@code Σ ψ_i² h = 1}. */
    public StationaryState state(int n) {
        double[] unit = matrix.eigenvector(n);
        double scale = 1.0 / Math.sqrt(grid.spacing());
        for (int i = 0; i < unit.length; i++) {
            unit[i] *= scale;
        }
        return new StationaryState(matrix.eigenvalue(n), grid, unit);
    }
}
