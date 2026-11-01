"""Independent check of transmission through one rectangular barrier (ħ = m = 1).

Compares with quantumlab-physics: RectangularBarrierTest.

Transmission is found here by matching plane waves at the two edges and solving the four linear
equations, which shares nothing with the Java transfer-matrix code. The closed form from the
explanation catalogue is checked against it as well.

Run from this folder:  python barrier_transmission.py
"""

import numpy as np

from _check import check, check_true, finish


def transmission_by_matching(energy, height, width):
    """T for a barrier of the given height and width, for a wave arriving from the left.

    Left of the barrier:   exp(ikx) + r exp(-ikx)
    Inside the barrier:    A exp(iqx) + B exp(-iqx)      (q may be imaginary)
    Right of the barrier:  t exp(ikx)
    Continuity of the wavefunction and its derivative at x = 0 and x = a gives four equations
    in the four unknowns r, A, B, t.
    """
    k = np.sqrt(2.0 * energy + 0j)
    q = np.sqrt(2.0 * (energy - height) + 0j)
    a = width
    # Unknowns in this order: r, A, B, t.
    matrix = np.array(
        [
            # psi continuous at x = 0:         1 + r = A + B         ->  -r + A + B = 1
            [-1.0, 1.0, 1.0, 0.0],
            # psi' continuous at x = 0:        ik (1 - r) = iq (A - B) ->  -ik r - iq A + iq B = -ik
            [-1j * k, -1j * q, 1j * q, 0.0],
            # psi continuous at x = a:         A e^{iqa} + B e^{-iqa} = t e^{ika}
            [0.0, np.exp(1j * q * a), np.exp(-1j * q * a), -np.exp(1j * k * a)],
            # psi' continuous at x = a:        iq (A e^{iqa} - B e^{-iqa}) = ik t e^{ika}
            [0.0, 1j * q * np.exp(1j * q * a), -1j * q * np.exp(-1j * q * a), -1j * k * np.exp(1j * k * a)],
        ],
        dtype=complex,
    )
    rhs = np.array([1.0, -1j * k, 0.0, 0.0], dtype=complex)
    solution = np.linalg.solve(matrix, rhs)
    t = solution[3]
    return float(abs(t) ** 2)


def closed_form(energy, height, width):
    """Closed form from the explanation catalogue (E below, at or above the barrier top)."""
    if energy < height:
        kappa = np.sqrt(2.0 * (height - energy))
        return 1.0 / (1.0 + height**2 * np.sinh(kappa * width) ** 2 / (4.0 * energy * (height - energy)))
    if energy > height:
        k = np.sqrt(2.0 * (energy - height))
        return 1.0 / (1.0 + height**2 * np.sin(k * width) ** 2 / (4.0 * energy * (energy - height)))
    return 1.0 / (1.0 + width**2 * height / 2.0)


def main():
    print("Closed form against plane-wave matching, four barriers (height, width)")
    worst = 0.0
    count = 0
    for height, width in [(2.0, 1.5), (5.0, 0.5), (0.5, 3.0), (10.0, 0.2)]:
        fraction = 0.05
        while fraction < 3.0:
            energy = fraction * height
            if abs(energy - height) > 1e-9:  # the exact top is checked separately below
                matched = transmission_by_matching(energy, height, width)
                formula = closed_form(energy, height, width)
                worst = max(worst, abs(matched - formula) / max(formula, 1e-300))
                count += 1
            fraction += 0.0625
    check_true(
        f"closed form and matching agree at {count} energies (largest relative gap {worst:.2e})",
        worst < 1e-9,
        )

    print()
    print("Exactly at the top of the barrier, T = 1 / (1 + a^2 V0 / 2)")
    expected = 1.0 / (1.0 + 1.5 * 1.5 * 2.0 / 2.0)
    for offset in (-1e-6, 1e-6):
        check(
            f"V0 = 2, a = 1.5, E = V0 (1 {offset:+.0e})",
            transmission_by_matching(2.0 * (1.0 + offset), 2.0, 1.5),
            expected,
            1e-5,
        )

    print()
    print("Tunnelling: V0 = 2, a = 1.5, E = 1")
    check(
        "T(E = 1)",
        transmission_by_matching(1.0, 2.0, 1.5),
        0.055861437216915076,
        1e-13,
    )

    print()
    print("Resonances above the barrier: V0 = 3, a = 2, k a = n pi")
    for n in range(1, 5):
        energy = 3.0 + 0.5 * (n * np.pi / 2.0) ** 2
        check(f"T = 1 for n = {n}", transmission_by_matching(energy, 3.0, 2.0), 1.0, 1e-12)

    print()
    print("Thick barrier: V0 = 2, a = 6, E = 1")
    energy, height, width = 1.0, 2.0, 6.0
    kappa = np.sqrt(2.0 * (height - energy))
    exact = transmission_by_matching(energy, height, width)
    estimate = 16.0 * energy * (height - energy) / height**2 * np.exp(-2.0 * kappa * width)
    check_true("kappa a is above 8", kappa * width > 8.0, f"kappa a = {kappa * width:.4f}")
    check_true(
        "the estimate agrees with T to within 0.1 %",
        abs(estimate / exact - 1.0) < 1e-3,
        f"ratio - 1 = {estimate / exact - 1.0:.3e}",
        )

    print()
    print("Trends: V0 = 2, E = 1 falls with width; a = 1.5, V0 = 2 rises with energy")
    widths = np.arange(0.25, 3.0 + 1e-9, 0.25)
    values = [transmission_by_matching(1.0, 2.0, w) for w in widths]
    check_true("T falls as the barrier gets wider", all(b < a for a, b in zip([1.0] + values, values)))
    energies = np.arange(0.2, 2.0, 0.2)
    values = [transmission_by_matching(e, 2.0, 1.5) for e in energies]
    check_true("T rises with energy below the top", all(b > a for a, b in zip([0.0] + values, values)))

    finish()


if __name__ == "__main__":
    main()
