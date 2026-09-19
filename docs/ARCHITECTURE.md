# Architecture

## System boundary

```text
Browser / API client
        |
        v
Clojure service
  - request validation and route jobs
  - H3/coastline graph preparation
  - raw weather adapters and forecast cache
  - ship/cost input transport (no formulas)
  - GeoJSON and diagnostics
        |
        | versioned CSR request/response
        v
Bend worker
  - weather interpolation and ship physics
  - Kwon speed-loss and objective functions
  - parallel edge-weight batches
  - shortest-path kernels
  - predecessor/path reconstruction
  - proof-backed invariants
```

The ownership rule is strict: if code expresses a physical formula, numerical
model, optimization policy, graph traversal, search, reduction, or route
algorithm, it belongs in Bend. Clojure may validate shapes and safety limits but
must not reproduce the computation as a fallback or reference implementation.

The integration begins as a supervised native process using length-delimited
binary messages. During development, newline-delimited EDN is allowed as a
human-readable debug transport. Native C ABI integration is deferred until
profiling demonstrates that process transport is material.

## Data model

The compute contract uses compact graph data and keeps geospatial concerns out
of Bend:

- `row-offsets`: CSR offsets, `node-count + 1` unsigned integers
- `columns`: destination node IDs
- `weights`: scaled non-negative integer costs
- `source`, `target`: node IDs
- `scale`: units per hour or kilogram
- edge metadata in Clojure: distance, heading, and midpoint
- node metadata in Clojure: H3 ID, latitude, longitude, and resolution
- raw weather arrays and ship parameters transported unchanged to Bend

Bend returns a status, total scaled cost, path node IDs, kernel name, duration,
and diagnostic counters. It never receives shapefiles, GRIB files, HTTP values,
or mutable application state.

## Core invariants

1. Every CSR column is a valid node ID.
2. Row offsets are monotonic and end at the edge count.
3. Routing weights are finite, non-negative integers.
4. A successful path starts at `source`, ends at `target`, and every adjacent
   pair corresponds to an input edge.
5. Reported route cost equals the sum of path-edge costs.
6. An unreachable target is represented explicitly, never as a fabricated path.
7. Weather and graph snapshots are immutable and identified in every result.

## Runtime modes

- `bend-cpu`: Bend native worker on configured CPU threads.
- `bend-gpu`: opt-in only for kernels shown by benchmarks to benefit from GPU
  execution. Irregular graph traversal is not assumed to be GPU-efficient.
- `bend-shadow`: run a candidate Bend kernel beside the stable Bend kernel and
  compare both against immutable legacy fixtures.

There is no Clojure computational fallback. Worker failure returns a typed
service-unavailable result; it never silently changes the physics or algorithm.

## Observability

Each route receives a correlation ID. Logs and metrics include engine version,
graph/weather snapshot IDs, node/edge counts, objective, queue/relaxation counts,
kernel duration, serialization duration, total duration, and parity outcome.
Development defaults to verbose logging; production logging is configurable.
