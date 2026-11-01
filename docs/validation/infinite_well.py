"""Independent check of the infinite square well (ħ = m = 1).

Compares with quantumlab-physics: InfiniteWellTest.

The finite-difference matrix is built here from scratch and diagonalised by NumPy's dense
symmetric eigensolver, so nothing is shared with the Java tridiagonal solver.

Run from this folder:  python infinite_well.py
"""

import numpy as np

from _check import check, check_true, finish


def exact_energy(n, width):
    """E_n = n² π² / (2 L²)."""
    return n * n * np.pi**2 / (2.0 * width**2)


def fd_matrix(width, interior_points):
    """Hamiltonian -1/2 d²/dx² on a grid of N interior points, ψ = 0 at both walls."""
    h = width / (interior_points + 1)
    main = np.full(interior_points, 1.0 / h**2)
    off = np.full(interior_points - 1, -0.5 / h**2)
    return np.diag(main) + np.diag(off, 1) + np.diag(off, -1), h


def fd_energy_dense(level, width, interior_points):
    """Level `level` (1 = lowest) of the finite-difference matrix, by dense diagonalisation."""
    matrix, _ = fd_matrix(width, interior_points)
    return np.linalg.eigvalsh(matrix)[level - 1]


def fd_energy_formula(level, width, interior_points):
    """Closed form for the same matrix: (1 - cos(n π h / L)) / h²."""
    h = width / (interior_points + 1)
    return (1.0 - np.cos(level * np.pi * h / width)) / h**2


def simpson(values, spacing):
    """Composite Simpson's rule on an odd number of equally spaced samples."""
    return spacing / 3.0 * (
        values[0] + values[-1] + 4.0 * values[1:-1:2].sum() + 2.0 * values[2:-1:2].sum()
    )


def main():
    print("Exact levels")
    check("E_1 on a unit well = pi^2 / 2", exact_energy(1, 1.0), np.pi**2 / 2.0, 1e-14)
    check("E_2 = 4 E_1", exact_energy(2, 1.0), 4.0 * exact_energy(1, 1.0), 1e-13)
    check("E_3 = 9 E_1", exact_energy(3, 1.0), 9.0 * exact_energy(1, 1.0), 1e-13)
    check("well on [-1, 1] has E_1 = (unit-well E_1) / 4", exact_energy(1, 2.0), exact_energy(1, 1.0) / 4.0, 1e-14)

    print()
    print("Finite-difference energies (unit well, level 1)")
    exact = exact_energy(1, 1.0)
    previous = -np.inf
    all_below = True
    all_rising = True
    for points in (10, 40, 160, 640):
        value = fd_energy_dense(1, 1.0, points)
        all_below &= value < exact
        all_rising &= value > previous
        previous = value
        print(f"       N = {points:4d}   E_numeric = {value:.12f}   error = {exact - value:.3e}")
    check_true("every finite-difference energy lies below the exact one", all_below)
    check_true("the energies rise as the grid is refined", all_rising)

    # The closed form must reproduce the dense numbers before it can be trusted at N = 100000.
    for points in (10, 40, 160, 640):
        check(
            f"closed form matches dense diagonalisation, N = {points}",
            fd_energy_formula(1, 1.0, points),
            fd_energy_dense(1, 1.0, points),
            1e-8,
            labels=("closed form", "dense diagonalisation"),
        )
    big = fd_energy_formula(1, 1.0, 100_000)
    check_true(
        "N = 100000 is within 1e-6 of the exact level",
        abs(big - exact) < 1e-6,
        f"difference {abs(big - exact):.2e}",
        )

    print()
    print("Observed order of convergence (each step halves h; level 1, unit well)")
    errors = []
    sizes = [9, 19, 39, 79, 159]  # h = 1/10, 1/20, 1/40, 1/80, 1/160
    for points in sizes:
        errors.append(abs(exact - fd_energy_dense(1, 1.0, points)))
    for k in range(1, len(sizes)):
        order = np.log2(errors[k - 1] / errors[k])
        print(f"       h = 1/{sizes[k] + 1:<4d}  error = {errors[k]:.4e}  observed order = {order:.4f}")
    check_true("the observed order is 2 to three decimals", abs(np.log2(errors[-2] / errors[-1]) - 2.0) < 1e-3)

    print()
    print("Exact wavefunctions on the well [-1, 3]")
    left, right = -1.0, 3.0
    width = right - left
    n_intervals = 4000  # even, as Simpson's rule needs
    x = np.linspace(left, right, n_intervals + 1)
    spacing = (right - left) / n_intervals

    def psi(level):
        return np.sqrt(2.0 / width) * np.sin(level * np.pi * (x - left) / width)

    for level in range(1, 5):
        norm = simpson(psi(level) ** 2, spacing)
        check(f"level {level} is normalised", norm, 1.0, 1e-9)
    check("levels 1 and 2 are orthogonal", simpson(psi(1) * psi(2), spacing), 0.0, 1e-9)
    check("levels 1 and 3 are orthogonal", simpson(psi(1) * psi(3), spacing), 0.0, 1e-9)

    finish()


if __name__ == "__main__":
    main()
