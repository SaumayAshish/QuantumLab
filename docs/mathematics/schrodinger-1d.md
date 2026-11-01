# One-dimensional Schrödinger equation by finite differences

Units: ħ = 1 and particle mass m = 1, so H = −½ d²/dx² + V (x). Energies and lengths are in these
natural units; nothing here is tied to a particular physical system.

## Discretisation

Unknowns sit at x_i = x_min + (i + 1)h, i = 0 … N − 1, with h = (x_max − x_min)/ (N + 1).
The wavefunction is zero at the walls, x_min and x_max, which are one spacing outside the first
and last unknown.

Taylor expansion of ψ (x ± h) gives

    (ψ_{i−1} − 2ψ_i + ψ_{i+1}) / h² = ψ″(x_i) + (h²/12) ψ⁗(x_i) + O(h⁴),

so the three-point formula is second-order accurate. The eigenproblem becomes Tψ = Eψ with the
symmetric tridiagonal matrix

    T_ii = 1/h² + V(x_i),    T_{i,i±1} = −1/(2h²).

## Exact reference: the infinite well

With V = 0 on [0, L] the continuum solution is E_n = n²π²/ (2L²), ψ_n = √ (2/L) sin (nπx/L), n = 1, 2, …

The discrete matrix with V = 0 can also be solved exactly. Its eigenvectors are sin (nπ x_i / L), and

    E_n(h) = (1/h²)(1 − cos θ) = (2/h²) sin²(θ/2),    θ = nπh/L.

Expanding, E_n (h) = E_n (1 − θ²/12 + …). So the discrete level lies below the exact one, with a
relative error of about (nπh/L)²/12. This is the h² behaviour the tests measure, and it shows why
high levels (large n at fixed h) are less accurate than low ones.

For this matrix, Σ sin² (nπ x_i/L) = (N + 1)/2 exactly, so the normalised discrete eigenvectors
coincide with the exact wavefunctions at the grid points, up to rounding.

## Normalisation

An eigenvector v with Σ v_i² = 1 becomes a wavefunction ψ_i = v_i/√h, so that
Σ ψ_i² h = 1, the grid form of ∫|ψ|² dx = 1. Signs are fixed so that the first non-negligible
component is positive.

## What the numbers mean, and what they do not

- The results are for the discretised problem. They approximate the continuum problem only when the
  grid is fine enough for the level considered.
- Hard walls at both ends are part of the model. For the harmonic oscillator the walls are placed
  far enough out that the low levels do not feel them (±10 for ω = 1).
- Wavefunctions are real, because V is real and the problem is time-independent.
- Nothing here is time evolution, and nothing here says what a measurement does.

## Checks

- Infinite well: discrete energies equal the exact discrete formula; error against the continuum falls
  by a factor of 4 when h is halved; numerical states equal the exact wavefunctions. The formula
  E_n (h) = (1/h²)(1 − cos θ), the observed order of 2 and the normalisation of the exact states are
  also reproduced independently, with a dense NumPy diagonalisation, by
  `docs/validation/infinite_well.py`.
- Harmonic oscillator: the error against ω (n + ½) falls like h², and the ground state matches the
  Gaussian π^ (−1/4) e^ (−x²/2). During development the levels were also compared with an independent
  NumPy diagonalisation of the same matrix; that script is not in the repository.
- Level n has n nodes and parity (−1)ⁿ.

