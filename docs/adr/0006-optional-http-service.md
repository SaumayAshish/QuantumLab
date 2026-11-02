# ADR-0006: An optional, read-only HTTP service

Status: accepted

## Context

The mathematics, physics and simulation modules have no user-interface dependency (ADR-0001), so
other programs can use them without JavaFX. A small HTTP service shows this and lets a script read
results. It must not change the desktop application, and it must not make scientific claims of its
own.

## Decision

- A fifth module, `quantumlab-service`, depends on `quantumlab-simulation` (and through it on physics
  and mathematics) and on Spring Boot 4.1.2, whose documentation lists JDK 25 as supported. Nothing
  depends on the service, and the desktop module does not use it.
- Spring's versions are imported inside this module only, so the other modules keep their own
  dependency management.
- The service only reads. It has no database, no login and no state between requests, and it listens
  on 127.0.0.1 only.
- Endpoints: `GET /api/status`, `/api/scope`, `/api/decay`, `/api/measurement` and `/api/levels`.
  Each returns JSON.
- The service never writes its own scope wording. `/api/scope`, `/api/decay` and `/api/levels`
  return explanations from `ExplanationCatalog`; `/api/measurement` returns the experiment exporter's
  JSON, which carries its own scope notes.
- Input is parsed strictly: plain decimals and whole numbers only, with the same limits as the
  desktop screens. A rejected input gives HTTP 400 and `{"error": "..."}`.
- A number JSON cannot represent is written as null, as in ADR-0004: a stable nucleus has no
  half-life.
- Results are repeatable. The measurement endpoint takes a seed, and the same inputs give the same
  answer (ADR-0003).

## Consequences

- There is no authentication and no rate limiting. The service is meant for the local machine and
  should not be exposed to a network as it stands.
- The limits in `LevelsController` copy those in the desktop `SchrodingerPresenter`, because that
  class cannot be used from here. If one changes, the other must follow.
- A measurement answer can be about a megabyte, because the exporter lists up to 10,000 trials. The
  counts and statistics are always exact, and the answer says when the list was cut short.
- The controllers are tested by calling them directly, and one test starts the real server on a free
  port and calls it over HTTP.
