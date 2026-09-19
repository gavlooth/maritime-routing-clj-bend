# Maritime Routing — Clojure + Bend

A clean successor to `gavlooth/hvm4-pathfinding`: weather-aware maritime route
optimization with Clojure at the system boundary and Bend 2 for parallel,
verifiable compute kernels.

The repository currently contains the migration specification, a Clojure
orchestration shell, and checked Bend 2 physics and routing kernel prototypes.
Golden fixtures exported from the previous implementation serve as the parity
oracle; physical formulas and graph algorithms are never duplicated in Clojure.

## Why this split?

- **Clojure** is responsible for HTTP, configuration, raw weather acquisition,
  GeoJSON, H3/coastline preparation, cache lifecycle, process supervision, and
  operations. It does not implement physical models or routing algorithms.
- **Bend** is the first and only implementation location for ship physics,
  weather interpolation, edge cost calculation, objective functions, batched
  edge evolution, Dijkstra/delta-stepping, and proof-backed invariants.
- A versioned, language-neutral protocol keeps either side replaceable and
  makes differential testing straightforward.

## Run the current vertical slice

Requirements: Clojure CLI and Bend 2.0.5 or newer.

```bash
clojure -M:test
clojure -M:run
bend bend/main.bend
```

The Clojure demo reports the prepared topology and Bend engine boundary. The
Bend demo evaluates Kwon-derived edge cost and parallel candidate reduction.

## Target capabilities

- H3 multi-resolution water grid with coastline and land-crossing exclusion
- Weather-aware travel-time and fuel costs using the Kwon speed-loss model
- Time, fuel, weighted, and safety-penalized objectives
- Dynamic re-routing as forecast windows change
- Bend Dijkstra and parallel delta-stepping
- REST/JSON and GeoJSON responses
- Leaflet route visualization
- Reproducible benchmarks and differential parity tests
- CPU first, with Bend GPU execution evaluated for suitable batch kernels

The detailed sequence, acceptance gates, risks, and old-to-new component map are
in [MIGRATION.md](docs/MIGRATION.md). Architectural boundaries are documented in
[ARCHITECTURE.md](docs/ARCHITECTURE.md).

## Repository layout

```text
bend/                  Bend 2 physics, algorithms, and future laws/proofs
docs/                  architecture, migration, and decision records
resources/             configuration and future static frontend assets
src/maritime/          Clojure orchestration and Bend process boundary
test/maritime/         boundary and contract tests
```

## Status

This is the migration foundation, not yet a navigational product. Results must
not be used for vessel navigation until real hydrographic constraints, forecast
provenance, safety validation, and operational review are complete.
