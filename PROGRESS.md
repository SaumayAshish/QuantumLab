# Progress

This file records what is built, what was actually run, and what has not been verified. Nothing
here is claimed as working unless it was run.

## Phases

| Phase | Scope | Status |
|-------|-------|--------|
| 0 | Inspect the environment, plan | Done |
| 1 | Mathematics: complex linear algebra, qubit, Born rule, measurement, decay model | Done |
| 2 | First working cat laboratory with desktop UI (modes A, B, C) | Screens, README, ADRs and a CI definition done; the CI workflow has not run |
| 3 | Finite-difference 1D Schrödinger solver | In progress: solver, reference tests and presenter done; no screen yet |
| 4 | Barriers, tunnelling, composite systems, decoherence | Not started |
| 5 | Desktop polish | Not started |
| 6 | Optional Spring Boot service | Not started |
| 7 | Release quality | Not started |

## Last verified run

- Date: 2026-10-29.
- Command: `mvn test` from the repository root.
- Environment: Windows, Microsoft OpenJDK 25.0.5, Maven 3.9.16, JavaFX 25.0.1.
- Result: BUILD SUCCESS. math 54, physics 79, simulation 91, desktop 49, 273 tests in total,
  no failures.
- Desktop application started with `mvn -pl quantumlab-desktop javafx:run`; both screens were
  exercised by hand (preparation, single and batch runs, invalid input, explanation panels).

## Not yet verified

- JSON export from either screen has not been inspected by hand (the exporter itself is covered
  by automated tests).
- Reset followed by the same batch reproducing identical counts has been checked in tests, not yet
  by hand in the window.
- The CI workflow in `.github/workflows/ci.yml` is written but has never run. It uses Temurin 25,
  not the Microsoft build used locally.


## Known issues and polish

- On small windows the measurement results table can be squeezed; a minimum height is being added.
- The convergence chart uses a linear trial axis, so early trials are compressed; a logarithmic
  axis is planned.
- "Observed" and "Theory" bar colours in the measurement chart are too similar.
- The convergence chart title is truncated at the default window width.
- The classical screen has no charts yet.
- The two views duplicate their worker-thread plumbing; it should be extracted.
- The footer scope notes are hard-coded text rather than taken from the explanation catalogue.
- The interference curve exists in the simulation module but has no screen.
