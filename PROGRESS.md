# Progress

This file records what is built, what was actually run, and what has not been verified. Nothing
here is claimed as working unless it was run.

## Phases

| Phase | Scope | Status |
|-------|-------|--------|
| 0 | Inspect the environment, plan | Done |
| 1 | Mathematics: complex linear algebra, qubit, Born rule, measurement, decay model | Done |
| 2 | First working cat laboratory with desktop UI (modes A, B, C) | Screens, README, ADRs and a CI definition done; the CI workflow has not run |
| 3 | Finite-difference 1D Schrödinger solver | Done: solver, convergence study and two screens (Quantum well, Convergence) |
| 4 | Barriers, tunnelling, wave packet, composite systems, decoherence | In progress. Done: stationary tunnelling through one or two barriers (Tunnelling screen), time-dependent wave packet (Crank–Nicolson), two-qubit composite system with CHSH (Composite system screen). Remaining: decoherence (mode E) |
| 5 | Desktop polish | Not started |
| 6 | Optional Spring Boot service | Not started |
| 7 | Release quality | Not started |

## Last verified run

- Date: 2026-10-30.
- Command: `mvn test` from the repository root.
- Environment: Windows, Microsoft OpenJDK 25.0.5, Maven 3.9.16, JavaFX 25.0.1.
- Result: BUILD SUCCESS, 464 tests (math 71, physics 171, simulation 104, desktop 118), no failures.
- The application starts with seven tabs.
- Composite system tab exercised by hand: default Z/Z, X/X, Y/Y, a rounded phase of 1.5708, an
  unequal split (P = 0.8), P = 1 (not entangled) and invalid input (P = 1.5).
- Earlier hand checks: Quantum well, Convergence and Tunnelling tabs (single and double barrier,
  probe energy, narrow oscillator box, invalid input).

## Not yet verified

- The Wave packet tab has automated tests (presenter and physics) but has not been exercised by
  hand since it was added; this list should be updated once it has.
- JSON export from the screens has not been inspected by hand (the exporter itself is covered by
  automated tests).
- Reset followed by the same batch reproducing identical counts has been checked in tests, not yet
  by hand in the window.
- The CI workflow in `.github/workflows/ci.yml` is written but has never run. It uses Temurin 25,
  not the Microsoft build used locally.
- The numerical code was cross-checked against independent NumPy calculations during development,
  but those scripts are not part of the repository.

## Known issues and polish

- On small windows the measurement results table can be squeezed; a minimum height is planned.
- The convergence chart uses a linear trial axis, so early trials are compressed; a logarithmic
  axis is planned.
- "Observed" and "Theory" bar colours in the measurement chart are too similar.
- On the Composite system tab the two chart curves are similar in colour, and identical curves
  hide each other.
- The convergence chart title is truncated at the default window width.
- The classical screen has no charts yet.
- The views duplicate their worker-thread plumbing; it should be extracted.
- The footer scope notes are hard-coded text rather than taken from the explanation catalogue.
- The interference curve exists in the simulation module but has no screen.
- Switching the potential changes the input fields but not the table until Solve is pressed.
- The grid-point error message says "use a decimal point, e.g. 0.5", which is wrong for whole numbers.
- The Convergence tables show empty rows below the data because the table height is fixed.
- The Tunnelling table layout may need the same fix; this has not been confirmed.
