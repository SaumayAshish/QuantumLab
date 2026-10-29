# ADR-0005: Thin views, presenters without JavaFX

Status: accepted

## Context

JavaFX code is slow to test and easy to get subtly wrong. Experiment logic must be testable without
a window.

## Decision

- Presenters (`LabPresenter`, `ClassicalPresenter`) contain all screen logic and use no JavaFX
  types. They take typed text in and return immutable views.
- JavaFX views only lay out controls and display those views.
- Each presenter is confined to one worker thread. A view runs work there, receives a UI update to
  apply, and applies it on the JavaFX thread. Controls are disabled while work runs.
- Input parsing accepts plain decimals only and rejects NaN, infinity, hexadecimal and suffixes.
  A failed input never destroys the running experiment.

## Consequences

- Screen behaviour is covered by ordinary unit tests.
- Layout and appearance still need to be checked by hand.
- The two views currently duplicate the worker-thread code. Extracting it is on the polish list.
