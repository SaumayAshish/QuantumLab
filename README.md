# QuantumLab

Explore quantum mechanics through mathematics, simulation and scientific computing.

QuantumLab is a Java desktop application and library for building intuition about quantum
measurement. Every number on screen comes from tested code, every experiment can be repeated
exactly from its seed, and every screen says what it models and what it does not.

> **Status: early development (version 0.1.0-SNAPSHOT).** Only the features listed under
> [Implemented](#implemented) exist. Everything under [Planned](#planned) is not built yet.

## Implemented

**Classical decay (mode A).** A nucleus decays with probability 1 − e^ (−λt). A detector, a
trigger and a cat follow the nucleus deterministically. The screen shows the apparatus after each
run, the random draw that decided it, and the tally over many runs against the theoretical
probabilities. The nucleus and the cat always have a definite state; the randomness is ignorance of
the outcome, not a superposition.

**Superposition and measurement (modes B and C).** Prepare a qubit state α|0⟩ + β|1⟩ with complex
amplitudes (|0⟩ is "not decayed", |1⟩ is "decayed"), choose the Z, X or Y measurement basis, and
measure repeatedly. Each trial prepares a fresh copy of the state and samples the Born rule. The
screen shows the observed frequencies, the theoretical probabilities, standard errors, the
deviation in σ, an observed-versus-theory chart, the convergence of the running frequency, and the
trial history.

**Composite system (mode D).** A nucleus and an idealised detector as one two-qubit state
α|00⟩ + β|11⟩, compared with the classical mixture that has the same Z-basis chances. Choose a
measurement basis (Z, X or Y) for each qubit and a relative phase, and see the joint outcome
probabilities for both. In Z/Z the two are identical; in other bases they differ, which is what
makes the state entangled rather than merely correlated. The screen also reports concurrence,
entanglement entropy and purity, and a CHSH test: the best value for the entangled state (up to
2√2, above the classical bound of 2) against the best value for the mixture (never above 2). Each
qubit on its own looks the same in both cases; the difference is only in the correlations. The
detector is an idealised two-level system, not a model of a real apparatus. 

**Decoherence (mode E).** One qubit, such as
the nucleus, starts in a pure superposition
α|0⟩ + β|1⟩ and is dephased at a rate γ you choose: its coherence is multiplied by e^ (−γt) while the
chances of "not decayed" and "decayed" stay as they were. The screen shows the outcome probabilities
in the Z, X or Y basis at t = 0, at a chosen time and in the long-time limit, the visibility of the
interference fringe, the purity and entropy of the qubit, and charts of both over time. As the
coherence goes, the statistics approach those of a classical mixture of the two outcomes. The rate
is an input, not derived from a real environment, only pure dephasing is modelled, and losing
coherence is not the same as one outcome occurring.

**Wave packet.** A Gaussian wave packet moves towards a rectangular barrier. The time-dependent
Schrödinger equation is stepped forward with the Crank–Nicolson scheme, which conserves the norm
to rounding. The screen shows the density splitting into a reflected and a transmitted part and
compares the final split with the stationary transmission T (E) averaged over the packet's
energies. One particle in one dimension; nothing is measured.

**Quantum well (one dimension).** Solve −½ψ″ + Vψ = Eψ (ħ = m = 1) by finite differences for an
infinite square well or a harmonic oscillator in a hard-walled box. The screen lists the lowest
levels with the numerical energy, the analytic reference, the error and the relative error, and
plots the wavefunction and probability density of the selected level (with the exact wavefunction
for the well). Stationary states only: nothing evolves in time and nothing is measured.

**Convergence.** Refine the grid step by step (each step halves the spacing h) and see the error of
one energy level against its exact value. The screen shows the observed order, computed from the
numbers, and a log–log chart of the error with a slope-2 guide. For the infinite well and for the
oscillator in a wide box the order is 2. For an oscillator in a narrow box the fixed wall error
dominates, the error stops shrinking, and the summary says so.

**Tunnelling.** The probability T that a particle with energy E gets through one or two rectangular
barriers, computed by transfer matrices and checked against the closed-form result and against an
independent plane-wave calculation. The screen compares the quantum answer with the classical one (a classical particle
is always reflected below the barrier and always passes above it), marks the
barrier top, and for two barriers shows the resonances. Stationary only: no wave packet moves, and
nothing is measured.

**Across the screens.**

- Seeded, reproducible random numbers: the same seed gives the same results, and "reset" replays
  them exactly.
- Statistics with a 5σ consistency check between observed counts and theory.
- Input validation with plain-language error messages. A failed input never destroys the running
  experiment.
- A collapsible panel on every screen listing what the experiment models and what it does not.
- JSON export of an experiment: configuration, seed, counts and history, with a schema identifier.

**Libraries (tested, partly without a screen yet).**

- `quantumlab-math`: immutable complex numbers, vectors and matrices, including tensor products,
  and a symmetric tridiagonal eigenvalue solver.
- `quantumlab-physics`: qubit states and operators, the Born rule, projective measurement in the
  Z, X and Y bases, an exponential decay model, the classical apparatus chain, the finite-difference
  Schrödinger solver, barrier scattering, Crank–Nicolson time evolution, and two-qubit states with
  entanglement measures and CHSH, and single-qubit density matrices with pure dephasing.
- `quantumlab-simulation`: experiment sessions, outcome logs, statistics, JSON/CSV export, and the
  explanation catalogue. It also contains a phase-dependence (interference) curve for a qubit,
  which has no screen yet.

## Planned

Not implemented yet:

- Charts for the classical decay screen.
- Desktop polish: layout, colours, a logarithmic axis for the convergence chart.
- An optional Spring Boot service.

## Scientific scope

QuantumLab shows measurement statistics for small quantum systems and a classical thought
experiment around them. It does not solve the measurement problem, it does not model a whole cat,
and consciousness plays no role in anything it computes. "Superposition" here means a normalised
vector of complex amplitudes and nothing more. The CHSH numbers on the composite screen are the
quantum-mechanical predictions for the chosen state and settings; the application does not run or
simulate a loophole-free Bell experiment. The decoherence screen dephases one qubit at an assumed rate; it does not model a real
environment and does not explain why a single outcome is observed. The in-app explanation panels and the tests that guard
their wording are the authoritative statement of scope for each screen.

## Requirements

- JDK 25 (LTS). Developed and tested on Microsoft OpenJDK 25.0.5.
- Maven 3.9 or newer. Developed with Maven 3.9.16.
- JavaFX 25 is fetched by Maven; no separate installation is needed.

## Build and test

From the repository root:

```
mvn test
```

## Run the desktop application

```
mvn install -DskipTests
mvn -pl quantumlab-desktop javafx:run
```

In IntelliJ IDEA you can instead run `dev.quantumlab.desktop.QuantumLabLauncher`, adding the VM
option `--enable-native-access=ALL-UNNAMED`.

## Project layout

```
quantumlab-math         complex linear algebra
quantumlab-physics      qubits, Born rule, measurement, decay model, Schrödinger solver,
                        scattering, time evolution, two-qubit states
quantumlab-simulation   experiment sessions, statistics, export, explanations
quantumlab-desktop      JavaFX application
docs/                   architecture decisions, mathematics notes, validation notes
```

The desktop module is deliberately thin: presenters without any JavaFX types hold the logic and
are unit-tested; the views only lay out controls and display immutable results.

## Reproducibility

Every experiment is determined by its configuration and a 64-bit seed. Exported JSON records both,
so a run can be repeated exactly.

## License

MIT. See [LICENSE](LICENSE).
