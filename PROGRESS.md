# Progress

This file records what is built, what was actually run, and what has not been verified. Nothing
here is claimed as working unless it was run.

## Phases

| Phase | Scope                                                                           | Status                                                                                                                                                                                                                                                                                                                                                                                      |
|-------|---------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 0     | Inspect the environment, plan                                                   | Done                                                                                                                                                                                                                                                                                                                                                                                        |
| 1     | Mathematics: complex linear algebra, qubit, Born rule, measurement, decay model | Done                                                                                                                                                                                                                                                                                                                                                                                        |
| 2     | First working cat laboratory with desktop UI (modes A, B, C)                    | Screens, README, ADRs and a CI definition done; the CI workflow has not run                                                                                                                                                                                                                                                                                                                 |
| 3     | Finite-difference 1D Schrödinger solver                                         | Done: solver, convergence study and two screens (Quantum well, Convergence)                                                                                                                                                                                                                                                                                                                 |
| 4     | Barriers, tunnelling, wave packet, composite systems, decoherence               | Done: stationary tunnelling through one or two barriers (Tunnelling screen), time-dependent wave packet (Crank–Nicolson), two-qubit composite system with CHSH (Composite system screen), and dephasing of a single qubit (Decoherence screen)                                                                                                                                              |
| 5     | Desktop polish                                                                  | Done, apart from the small items under Known issues: tables sized to their rows, logarithmic trial axis, distinct series colours, untruncated titles, one shared worker and table-column helper for all screens, footer scope notes taken from the explanation catalogue, re-solve when the potential is switched, charts for the classical screen, and a screen for the interference curve |
| 6     | Optional Spring Boot service                                                    | Not started                                                                                                                                                                                                                                                                                                                                                                                 |
| 7     | Release quality                                                                 | In progress. Done: line-ending policy (`.gitattributes`), independent NumPy checks in `docs/validation/`, code pushed to GitHub. Remaining: the first CI run has to be read and fixed, then a final review of the docs                                                                                                                                                                      |

## Last verified run

- Dates: automated tests 2026-10-31; screens checked by hand 2026-10-31 and 2026-11-01.
- Command: `mvn test` from the repository root, on the code of commit c2c9fe7.
- Environment: Windows, Microsoft OpenJDK 25.0.5, Maven 3.9.16, JavaFX 25.0.1.
- Result: BUILD SUCCESS, 553 tests (math 71, physics 193, simulation 109, desktop 180), no failures.
- `mvn install -DskipTests` also succeeded in the same session.
- The application starts with nine tabs.
- Checked by hand after the shared-worker and scope-note changes: the scope note in the footer of
  each tab; buttons greying out while a computation runs and coming back; an invalid input showing
  the red message and keeping the previous result; Export JSON on the Superposition tab reporting
  the file it wrote.
- Interference tab checked by hand: the default (P (not decayed) = 0.5, X basis: a full cosine
  fringe, 100 % / 50 % / 0 % at 0° / 90° / 180°, flat 50 % classical mixture), P = 0.1 (fringe
  height 0.6000, equal to the closed form 2|α||β|), P = 1 (no fringe, stated in the verdict) and
  an invalid input (`abc`: red message, previous result kept).
- Classical tab checked by hand after the charts were added: the start screen (Theory bars only,
  empty running-frequency chart), 1000 runs (bars and curve agree with the table, the curve settles
  on the Theory line), Reset runs (both charts empty, no NaN), an invalid seed (red message, charts
  kept), and a changed law (λ = 1, t = 2: the Theory bars move to 86.47 % and 13.53 %).
- Independent NumPy checks (`docs/validation/`) run on 2026-11-01 with Python 3.12 and NumPy 1.26.4:
  `infinite_well.py` 18 of 18, `barrier_transmission.py` 12 of 12, `dephasing.py` 34 of 34
  comparisons agree. They compare independent calculations with the values and claims in the Java
  tests; they do not run the Java classes.
- Decoherence tab exercised by hand: defaults in the X basis, the Z basis (statistics unchanged),
  strong dephasing (γ = 10, t = 1), and an invalid rate (−1).
- Composite system tab exercised by hand: default Z/Z, X/X, Y/Y, a rounded phase of 1.5708, an
  unequal split (P = 0.8), P = 1 (not entangled) and invalid input (P = 1.5).
- Earlier hand checks: Quantum well, Convergence and Tunnelling tabs (single and double barrier,
  probe energy, narrow oscillator box, invalid input).
- Measurement and Convergence tabs checked by hand after the polish step: no empty table rows, a
  logarithmic trial axis, distinct colours for the two series.
- Quantum well tab checked by hand: switching between the infinite well and the harmonic
  oscillator re-solves at once (table, setup panel and charts follow the box), and editing a
  field alone changes nothing until Solve is pressed.

## Not yet verified

- The Wave packet tab has automated tests (presenter and physics) but has not been exercised by
  hand since it was added; this list should be updated once it has.
- On the Interference tab, the Y and Z bases have been checked by automated tests, not yet by hand
  in the window.
- On the Classical tab, repeated "Run once" with the charts, and scrolling the results pane in a
  small window, have not been checked by hand.
- The contents of an exported JSON file have not been inspected by hand (the exporter itself is
  covered by automated tests), and export from the Classical tab has not been run by hand.
- Reset followed by the same batch reproducing identical counts has been checked in tests, not yet
  by hand in the window.
- The CI workflow in `.github/workflows/ci.yml` is written but has never run. It uses Temurin 25,
  not the Microsoft build used locally.
- The independent NumPy checks cover the infinite well, a single rectangular barrier and pure
  dephasing only. The convergence study, the wave packet, the composite system and CHSH have no
  independent script yet.


## Known issues and polish

- On small windows the measurement results table can be squeezed; a minimum height is planned.
- After Reset runs on the Classical tab, the empty running-frequency chart keeps the trial axis of
  the previous batch.
- On the Interference tab, when there is no fringe the superposition curve lies exactly under the
  classical mixture line, so only one line is visible. The "not a number" message there ends with
  a whole-number example ("e.g. 12 or 0.5"), which suits a probability less well.
- The Tunnelling table layout may need the same fix as the other tables; this has not been confirmed.
