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

## Run the application

Requirements: Clojure CLI, Node.js/npm, and Bend 2.0.5 or newer.

```bash
npm install
npm run build
clojure -M:run
```

Open <http://127.0.0.1:8080>. The production build is served by the same
http-kit process as the Reitit API.

For live frontend development, run the backend and shadow-cljs separately:

```bash
clojure -M:run
npm run dev
```

Then open <http://127.0.0.1:3000>; shadow-cljs proxies API requests to port
8080. Run the complete suite with `make check`.

The current map offers four curated Aegean demo corridors and four optimization
modes through a real REST/GeoJSON contract. The corridor geometry is explicitly
labeled as demonstration data until Bend CSR Dijkstra and the H3 coastline
pipeline replace the fixtures.

## Target capabilities

- H3 multi-resolution water grid with coastline and land-crossing exclusion
- Weather-aware travel-time and fuel costs using the Kwon speed-loss model
- Time, fuel, weighted, and safety-penalized objectives
- Dynamic re-routing as forecast windows change
- Bend Dijkstra and parallel delta-stepping
- REST/JSON and GeoJSON responses
- UIx/React and Leaflet route visualization
- Reproducible benchmarks and differential parity tests
- CPU first, with Bend GPU execution evaluated for suitable batch kernels

The detailed sequence, acceptance gates, risks, and old-to-new component map are
in [MIGRATION.md](docs/MIGRATION.md). Architectural boundaries are documented in
[ARCHITECTURE.md](docs/ARCHITECTURE.md).

## Repository layout

```text
bend/                  Bend 2 physics, algorithms, and future laws/proofs
docs/                  architecture, migration, and decision records
resources/public/      compiled UI, page shell, styles, and Leaflet CSS
src/maritime/          Reitit/http-kit backend and Bend process boundary
src/maritime/frontend/ ClojureScript UIx route console
test/maritime/         boundary, API, and security-header tests
```

## Status

This is the migration foundation, not yet a navigational product. Results must
not be used for vessel navigation until real hydrographic constraints, forecast
provenance, safety validation, and operational review are complete.
