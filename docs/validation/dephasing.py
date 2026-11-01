"""Independent check of pure dephasing of one qubit.

Compares with quantumlab-physics: DephasingTest.

The state is (0.6, 0.8 i). The density matrix, the Born probabilities in the X and Y bases, the
purity, the entropy, the Bloch components and the fringe are all computed here with plain NumPy
matrix algebra (traces with the Pauli matrices, eigenvalues of the density matrix), not with the
formulas the Java classes use.

Run from this folder:  python dephasing.py
"""

import numpy as np

from _check import check, check_true, finish

PAULI_X = np.array([[0, 1], [1, 0]], dtype=complex)
PAULI_Y = np.array([[0, -1j], [1j, 0]], dtype=complex)
PAULI_Z = np.array([[1, 0], [0, -1]], dtype=complex)

Z_BASIS = [np.array([1, 0], dtype=complex), np.array([0, 1], dtype=complex)]
X_BASIS = [np.array([1, 1], dtype=complex) / np.sqrt(2), np.array([1, -1], dtype=complex) / np.sqrt(2)]
Y_BASIS = [np.array([1, 1j], dtype=complex) / np.sqrt(2), np.array([1, -1j], dtype=complex) / np.sqrt(2)]


def start():
    psi = np.array([0.6, 0.8j], dtype=complex)
    return np.outer(psi, psi.conj())


def dephase(rho, rate, time):
    """Multiply the off-diagonal entries by exp(-rate * time); the diagonal is unchanged."""
    factor = np.exp(-rate * time)
    out = rho.copy()
    out[0, 1] *= factor
    out[1, 0] *= factor
    return out


def dephase_by_flips(rho, rate, time):
    """The same channel as a random Z flip with probability q = (1 - exp(-rate t)) / 2."""
    q = (1.0 - np.exp(-rate * time)) / 2.0
    return (1.0 - q) * rho + q * PAULI_Z @ rho @ PAULI_Z


def probabilities(rho, basis):
    return np.array([float((b.conj() @ rho @ b).real) for b in basis])


def entropy_bits(rho):
    eigenvalues = np.linalg.eigvalsh(rho)
    return float(-sum(v * np.log2(v) for v in eigenvalues if v > 1e-300))


def equatorial(rho, phase):
    """Probability of the outcome (|0> + e^{i phase}|1>)/sqrt(2)."""
    b = np.array([1.0, np.exp(1j * phase)], dtype=complex) / np.sqrt(2.0)
    return float((b.conj() @ rho @ b).real)


def main():
    rho0 = start()
    after = dephase(rho0, 0.5, 2.0)

    print("Coherence")
    check("exp(-gamma t) for gamma = 0.5, t = 2", np.exp(-0.5 * 2.0), np.exp(-1.0), 1e-12)
    check("Re rho01 after dephasing", after[0, 1].real, 0.0, 1e-12)
    check("Im rho01 after dephasing", after[0, 1].imag, -0.17658213176229232, 1e-15)

    print()
    print("Populations never change")
    for t in (0.0, 0.3, 2.0, 40.0):
        rho = dephase(rho0, 0.7, t)
        check(f"rho00 at t = {t}", rho[0, 0].real, 0.36, 1e-12)
        check(f"rho11 at t = {t}", rho[1, 1].real, 0.64, 1e-12)

    print()
    print("Statistics in each basis, gamma = 0.5, t = 2")
    zb, zb_after = probabilities(rho0, Z_BASIS), probabilities(after, Z_BASIS)
    check_true("Z statistics unchanged", np.allclose(zb, zb_after, atol=1e-12))
    x_after = probabilities(after, X_BASIS)
    check("P(+) in X after dephasing", x_after[0], 0.5, 1e-15)
    check("P(-) in X after dephasing", x_after[1], 0.5, 1e-15)
    y_after = probabilities(after, Y_BASIS)
    check("P(+i) in Y after dephasing", y_after[0], 0.6765821317622922, 1e-14)
    check("P(-i) in Y after dephasing", y_after[1], 0.3234178682377077, 1e-14)
    y_before = probabilities(rho0, Y_BASIS)
    check("P(+i) in Y before dephasing", y_before[0], 0.98, 1e-14)
    check("P(-i) in Y before dephasing", y_before[1], 0.02, 1e-14)

    print()
    print("Derived quantities after dephasing")
    check("purity Tr rho^2", float(np.trace(after @ after).real), 0.6015624985154313, 1e-14)
    check("entropy in bits", entropy_bits(after), 0.8480625116545679, 1e-14)
    bloch_y = float(np.trace(after @ PAULI_Y).real)
    bloch_z = float(np.trace(after @ PAULI_Z).real)
    check("Bloch <Y>", bloch_y, 0.35316426352458463, 1e-14)
    check("Bloch <Z>", bloch_z, -0.28, 1e-14)
    # Fringe visibility 2|rho01|: the full swing of the equatorial probability over its phase.
    phases = np.linspace(-np.pi, np.pi, 100_001)
    fringe = np.array([equatorial(after, p) for p in phases])
    check("fringe visibility (max - min of the swept curve)", float(fringe.max() - fringe.min()), 0.35316426352458463, 1e-9)
    check("equatorial probability at phi = 0.7", equatorial(after, 0.7), 0.6137573325314052, 1e-14)

    print()
    print("Structural properties")
    twice = dephase(dephase(rho0, 0.9, 0.8), 0.9, 1.7)
    once = dephase(rho0, 0.9, 2.5)
    check_true("two halves equal the whole (semigroup)", np.allclose(twice, once, atol=1e-14))
    for t in (0.0, 0.4, 2.0, 9.0):
        check_true(
            f"exp(-gamma t) decay equals the random Z flip, t = {t}",
            np.allclose(dephase(rho0, 0.5, t), dephase_by_flips(rho0, 0.5, t), atol=1e-14),
        )
    purity = [float(np.trace(dephase(rho0, 0.4, 0.25 * k) @ dephase(rho0, 0.4, 0.25 * k)).real) for k in range(61)]
    entropy = [entropy_bits(dephase(rho0, 0.4, 0.25 * k)) for k in range(61)]
    check_true("purity only falls over 0 <= t <= 15", all(b <= a + 1e-12 for a, b in zip(purity, purity[1:])))
    check_true("entropy only rises over 0 <= t <= 15", all(b >= a - 1e-12 for a, b in zip(entropy, entropy[1:])))

    limit = np.diag(np.diag(rho0))
    check_true("at gamma t = 60 the state is the classical mixture", np.allclose(dephase(rho0, 1.0, 60.0), limit, atol=1e-12))
    y_limit = probabilities(limit, Y_BASIS)
    check("long-time P(+i) in Y", y_limit[0], 0.5, 1e-12)
    # The coherence falls to one half at t = ln 2 / gamma; check by evaluating the decay there.
    half_time = np.log(2.0) / 0.5
    check("coherence left at t = ln 2 / gamma", abs(dephase(rho0, 0.5, half_time)[0, 1]) / abs(rho0[0, 1]), 0.5, 1e-12)

    finish()


if __name__ == "__main__":
    main()
