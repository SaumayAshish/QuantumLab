# ADR-0002: Basis convention for the cat qubit

Status: accepted

## Context

A two-level system needs a fixed mapping between basis vectors and the story being told. Mixing
conventions between modules would silently flip results.

## Decision

- |0⟩ = (1, 0)ᵀ is the **not decayed** nucleus. |1⟩ = (0, 1)ᵀ is the **decayed** nucleus.
- A state is stored as the amplitude pair (α, β) in that order, with |α|² + |β|² = 1.
- Three measurement bases are offered, as the closed set `StandardBasis`:
  Z (computational, {|0⟩, |1⟩}), X (Hadamard, {|+⟩, |−⟩}) and Y (circular, {|+i⟩, |−i⟩}).
- A state is a ray: a global phase has no physical effect. Probabilities and the relative phase
  between α and β are unaffected by it.

## Consequences

- The classical decay screen and the quantum screens use the same labels ("not decayed",
  "decayed").
- Only the relative phase is shown to the user as a phase.
- Measuring in X or Y exposes the relative phase; measuring in Z does not.
