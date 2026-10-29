# ADR-0004: A small JSON writer instead of a library

Status: accepted

## Context

The application only ever writes JSON (experiment export). It never reads it.

## Decision

- A minimal pretty-printing writer (`JsonWriter`, package-private) produces the output. No JSON
  library is added.
- Strings are escaped per RFC 8259. Non-finite numbers are written as `null`, because JSON cannot
  represent them.
- Every export carries a schema identifier (for example `quantumlab.quantum-measurement/1`).
- The history in an export is capped, so a very large run cannot produce an unbounded file.

## Consequences

- No third-party dependency in the simulation module.
- Reading exports back is out of scope. If it becomes necessary, adopting a library is revisited in
  a new ADR.
- The writer is covered by its own tests, and the tests parse the output with a small independent
  reader.
