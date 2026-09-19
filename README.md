# Maritime Routing — Clojure + Bend

A clean successor to `gavlooth/hvm4-pathfinding`: weather-aware maritime route
optimization with Clojure at the system boundary and Bend 2 for parallel,
verifiable compute kernels.

The repository currently contains the migration specification, a runnable
Clojure reference implementation of edge costing and Dijkstra routing, and a
checked Bend 2 kernel prototype. The reference engine is intentionally retained
as an oracle while graph construction and routing move into Bend.

## Why this split?

- **Clojure** is responsible for HTTP, configuration, weather providers,
  GeoJSON, H3/coastline preparation, cache lifecycle, and operations.
- **Bend** is responsible for pure numeric kernels: edge cost calculation,
  batched edge evolution, Dijkstra/delta-stepping, and later proof-backed
  invariants.
- A versioned, language-neutral protocol keeps either side replaceable and
  makes differential testing straightforward.

## Run the current vertical slice

Requirements: Clojure CLI and Bend 2.0.5 or newer.

```bash
clojure -M:test
clojure -M:run
bend bend/main.bend
```

The Clojure demo calculates a weather-weighted route across a tiny graph. The
Bend demo evaluates candidate paths with parallel recursion and returns the
minimum cost.

## Target capabilities

- H3 multi-resolution water grid with coastline and land-crossing exclusion
- Weather-aware travel-time and fuel costs using the Kwon speed-loss model
- Time, fuel, weighted, and safety-penalized objectives
- Dynamic re-routing as forecast windows change
- Clojure reference Dijkstra plus Bend Dijkstra and parallel delta-stepping
- REST/JSON and GeoJSON responses
- Leaflet route visualization
- Reproducible benchmarks and differential parity tests
- CPU first, with Bend GPU execution evaluated for suitable batch kernels

The detailed sequence, acceptance gates, risks, and old-to-new component map are
in [MIGRATION.md](docs/MIGRATION.md). Architectural boundaries are documented in
[ARCHITECTURE.md](docs/ARCHITECTURE.md).

## Repository layout

```text
bend/                  Bend 2 kernels and future laws/proofs
docs/                  architecture, migration, and decision records
resources/             configuration and future static frontend assets
src/maritime/          Clojure application and reference engine
test/maritime/         reference and contract tests
```

## Status

This is the migration foundation, not yet a navigational product. Results must
not be used for vessel navigation until real hydrographic constraints, forecast
provenance, safety validation, and operational review are complete.

