# QuantumLab

Explore quantum mechanics through mathematics, simulation and scientific computing.

QuantumLab is a Java desktop application and library for building intuition about quantum
measurement. Every number on screen comes from tested code, every experiment can be repeated
exactly from its seed, and every screen says what it models and what it does not.

> **Status: early development (version 0.1.0-SNAPSHOT).** Only the features listed under
> [Implemented](#implemented) exist. Everything under [Planned](#planned) is not built yet.

## Implemented

**Classical decay (mode A).** A nucleus decays with probability 1 − e^(−λt). A detector, a
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

**Quantum well (one dimension).** Solve −½ψ″ + Vψ = Eψ (ħ = m = 1) by finite differences for an
infinite square well or a harmonic oscillator in a hard-walled box. The screen lists the lowest
levels with the numerical energy, the analytic reference, the error and the relative error, and
plots the wavefunction and probability density of the selected level (with the exact wavefunction
for the well). Stationary states only: nothing evolves in time and nothing is measured.

**Across both screens.**

- Seeded, reproducible random numbers: the same seed gives the same results, and "reset" replays
  them exactly.
- Statistics with a 5σ consistency check between observed counts and theory.
- Input validation with plain-language error messages. A failed input never destroys the running
  experiment.
- A collapsible panel on every screen listing what the experiment models and what it does not.
- JSON export of an experiment: configuration, seed, counts and history, with a schema identifier.

**Libraries (tested, partly without a screen yet).**

- `quantumlab-math`: immutable complex numbers, vectors and matrices.
- `quantumlab-physics`: qubit states and operators, the Born rule, projective measurement in the
  Z, X and Y bases, an exponential decay model, and the classical apparatus chain.
- `quantumlab-simulation`: experiment sessions, outcome logs, statistics, JSON/CSV export, and the
  explanation catalogue. It also contains a phase-dependence (interference) curve for a qubit,
  which has no screen yet.

## Planned

Not implemented yet:

- Barriers and tunnelling for the one-dimensional solver.
- An entanglement-inspired two-qubit mode (tensor products, correlations).
- A decoherence mode.
- Charts for the classical decay screen.
- Desktop polish: layout, colours, a logarithmic axis for the convergence chart.
- An optional Spring Boot service.

## Scientific scope

QuantumLab shows measurement statistics for a two-level system and a classical thought experiment
around it. It does not solve the measurement problem, it does not model a whole cat, and
consciousness plays no role in anything it computes. "Superposition" here means a normalised vector
of complex amplitudes and nothing more. The in-app explanation panels and the tests that guard
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
quantumlab-physics      qubits, Born rule, measurement, decay model, apparatus chain
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
