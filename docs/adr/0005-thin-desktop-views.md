# ADR-0005: Thin views, presenters without JavaFX

Status: accepted (updated 2026-11-01)

## Context

JavaFX code is slow to test and easy to get subtly wrong. Experiment logic must be testable without
a window.

## Decision

- Presenters contain all screen logic and use no JavaFX types. There is one per screen:
  `LabPresenter` (superposition and measurement), `ClassicalPresenter`, `InterferencePresenter`,
  `SchrodingerPresenter`, `ConvergencePresenter`, `TunnellingPresenter`, `WavePacketPresenter`,
  `CompositePresenter` and `DecoherencePresenter`. They take typed text in and return immutable
  views.
- JavaFX views only lay out controls and display those views.
- Each presenter is confined to one worker thread. A view runs work there, receives a UI update to
  apply, and applies it on the JavaFX thread. Controls are disabled while work runs.
- Input parsing accepts plain decimals only and rejects NaN, infinity, hexadecimal and suffixes.
  A failed input never destroys the running experiment.
- The collapsible explanation panel and the scope note in each footer are built from the tested
  `ExplanationCatalog`, through `ExplanationText` and `ScopeNote`. A view never contains its own
  scope wording.

## Consequences

- Screen behaviour is covered by ordinary unit tests.
- Layout and appearance still need to be checked by hand.
- Update 2026-11-01: the worker-thread code that the first two views duplicated is now one class,
  `Worker`, shared by all nine views, and table columns are built by one helper, `Tables`.
