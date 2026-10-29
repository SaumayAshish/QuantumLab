# ADR-0003: Seeded, reproducible randomness

Status: accepted

## Context

An experiment that cannot be repeated cannot be checked. `RandomGenerator.getDefault()` may change
its algorithm between JDK releases, so a seed would not mean the same thing everywhere.

## Decision

- All randomness comes from `java.util.SplittableRandom` (SplitMix64), created in one place,
  `Reproducibility.newGenerator(seed)`. Its algorithm is part of the JDK specification.
- A measurement samples outcome 0 exactly when the next uniform draw is below its probability
  (inverse-CDF sampling over the outcomes in order).
- A regression test pins the first twenty outcomes for seed 2026. The expected values were computed
  independently, outside this code base, from the published algorithm.
- Each trial prepares a fresh copy of the state, so trials are independent.
- The seed and the generator name are recorded in every exported experiment.

## Consequences

- The same seed gives the same results on any JDK 25 platform, and "reset" replays a run exactly.
- Changing the generator or the sampling rule breaks replay of previously exported experiments.
  Such a change must be a deliberate decision recorded in a new ADR, and the pinned test updated.
