# ADR-0001: JDK 25 (LTS) and a Maven multi-module build

Status: accepted

## Context

The project needs a long-supported Java version, one place to set the language level, and clear
dependency direction between mathematics, physics, simulation and the user interface.

## Decision

- The language level is Java 25, an LTS release, set once through `maven.compiler.release` in the
  parent POM.
- The build is a multi-module Maven project: `quantumlab-math`, `quantumlab-physics`,
  `quantumlab-simulation`, `quantumlab-desktop`. Dependencies point one way:
  math ← physics ← simulation ← desktop.
- Only the desktop module depends on JavaFX. The other modules have no UI dependency and run
  headless.
- Tests use JUnit 5, with versions managed by the JUnit BOM.

## Consequences

- Developed and tested locally on Microsoft OpenJDK 25.0.5 with Maven 3.9.16. Other JDK 25
  distributions are expected to work but have not been tried locally.
- Raising the language level later is a one-line change.
- The physics and simulation code can be reused by a future service without pulling in JavaFX.
