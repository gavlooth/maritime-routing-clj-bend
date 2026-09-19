# ADR 0001: Start with a supervised Bend process

Status: accepted for migration foundation

## Context

Clojure must call Bend kernels, while Bend 2 and its generated ABI may evolve.
Direct native interop is lower latency but tightly couples memory layout,
lifecycle, and crash behavior.

## Decision

Begin with a supervised Bend worker and a versioned request/response protocol.
Use readable EDN only for development; use framed binary data for production
graph payloads. Physics and routing remain exclusively in Bend; immutable
fixtures from the predecessor system provide the external correctness oracle.

## Consequences

- Compiler upgrades and worker crashes are isolated from the JVM.
- Requests can be captured and replayed against legacy golden fixtures.
- Serialization adds overhead, measured before considering a C ABI.
- Large static topology may later use memory-mapped files identified by hash.
