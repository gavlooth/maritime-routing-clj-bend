# Migration plan

## Objective

Replace the Julia/C3/Vine/HVM4 stack with a maintainable hybrid Clojure + Bend 2
application while preserving at least the existing user-visible and numerical
capabilities. Bend is the first and only home of physical models and routing
algorithms; Clojure is the system shell. Migration is incremental: exported
results from the old application remain the behavioral oracle until every
parity gate passes.

## Baseline capability map

| Existing capability | New owner | Migration strategy | Acceptance evidence |
|---|---|---|---|
| Julia REST server | Clojure | Ring-compatible service and explicit schemas | Contract tests for every endpoint |
| Leaflet single-page map | Clojure resources | Port unchanged first, then modernize | Browser smoke test and route rendering |
| H3 multi-resolution grid | Clojure | JVM H3 binding or data service; preserve IDs | Golden Aegean graph statistics |
| GSHHG land classification | Clojure | JTS/GeoTools ingestion and prepared geometry | Coastline and island fixture suite |
| Land-crossing edge filter | Clojure | Segment sampling initially; exact geometry later | Zero forbidden crossings in fixtures |
| Kwon speed loss | Bend | Implement directly in Bend; compare with legacy fixtures | Property and golden tests |
| Time/fuel/weighted/safety objectives | Shared contract | Scaled unsigned edge weights | Golden cost-mode routes |
| C3 Fibonacci-heap Dijkstra | Bend | Implement correctness-first Bend Dijkstra, then optimize | Exact cost/path parity |
| Vine delta-stepping | Bend | Reimplement with balanced parallel work | Correctness plus benchmark gate |
| Forecast-window rerouting | Clojure orchestration | Immutable snapshots and repeated kernel calls | Deterministic voyage simulation |
| Waypoint metadata | Clojure | Enrich returned Bend node path | GeoJSON/ETA schema tests |
| Existing preset trips | Clojure resources | Port as fixtures | Golden route suite |

## SPARC execution plan

### 1. Specification — parity contract

Deliverables:

- Freeze representative old-system inputs and outputs before modifying it.
- Capture the eight preset routes, calm and adverse weather, all four cost modes,
  reachable/unreachable voyages, and forecast updates.
- Define units, coordinate order, heading convention, rounding, tie-breaking,
  error semantics, and the exact meaning of ETA and fuel.
- Version the compute protocol independently of the HTTP API.

Exit gate: fixtures can be replayed without starting the old UI, and every
numeric field has a tolerance or exact-comparison rule.

### 2. Pseudocode — deterministic route pipeline

```text
route(request):
  validate coordinates, ship parameters, objective, and requested engine
  graph_snapshot <- graph_for(request.bounds, requested resolutions)
  weather_snapshot <- forecast_for(request.departure, graph_snapshot.bounds)
  for each directed edge in graph_snapshot, in parallel-friendly batches:
    weather <- interpolate(weather_snapshot, edge.midpoint, departure)
    effective_speed <- kwon(ship, weather, edge.heading)
    weight <- objective_cost(effective_speed, edge.distance, safety settings)
  result <- engine.shortest_path(CSR(weights), source, target)
  assert path and cost invariants
  enrich node IDs with coordinates, ETA, speed, wave, wind, and heading
  return GeoJSON route plus diagnostics and snapshot provenance

reroute(voyage, forecast_stream):
  while destination not reached:
    route from current position using current immutable forecast
    advance vessel for configured interval
    persist route decision and actual advancement
    load next forecast snapshot
```

### 3. Architecture — stable seams

Implement four explicit interfaces:

1. `GraphProvider`: bounds/resolution to immutable maritime CSR topology.
2. `WeatherProvider`: bounds/time to immutable forecast snapshot.
3. `CostModel`: edge + ship + weather + objective to scaled integer weight.
4. `RoutingEngine`: CSR + source/target to path and diagnostics.

No HTTP handler may call Bend directly. It calls an application service that is
parameterized by these interfaces. `CostModel` and `RoutingEngine` are Bend
worker operations, never Clojure implementations. This permits fixture tests,
worker supervision, Bend-to-Bend shadow execution, and engine replacement.

### 4. Refinement — phased implementation

#### Phase 0: foundation — included in this repository

- Clojure project and orchestration/process boundary only
- Bend 2 checked Kwon physics and parallel routing prototypes
- architecture and migration documents
- unit tests and local commands

Gate: `clojure -M:test` and `bend bend/main.bend` pass.

#### Phase 1: freeze old behavior

- Add an exporter to the old app for topology, weighted CSR, route, and waypoint
  metadata using a versioned fixture format.
- Produce small, medium, and full-Aegean fixture sets.
- Record old engine versions and benchmark hardware.

Gate: fixtures are committed or reproducibly downloadable with checksums.

#### Phase 2: Clojure vertical slice

- Add HTTP routes, JSON/GeoJSON schemas, configuration, and structured logs.
- Port preset voyages and the Leaflet UI.
- Implement graph/weather provider fakes and serve a complete synthetic route.

Current status: the Reitit/http-kit/Jsonista API and UIx/Leaflet production
bundle are implemented. Four curated Aegean corridors exercise route selection,
objective selection, GeoJSON rendering, responsive layout, and the single-origin
deployment model. The UI labels these routes as demonstration fixtures.

Gate: browser selects endpoints, requests all objectives, and renders a route.

#### Phase 3: real geospatial graph

- Integrate H3 and GSHHG/JTS.
- Build deterministic multi-resolution water topology and disk cache.
- Snap voyage endpoints while exposing snap distance in the response.

Gate: graph counts and coast-crossing fixtures match the baseline; no silent
land route is accepted.

#### Phase 4: weather and cost parity

- Implement raw forecast adapters in Clojure.
- Implement interpolation, Kwon physics, and every objective policy in Bend.
- Test randomized valid ship/weather/edge inputs against exported legacy cases
  and independently calculated specification examples.

Gate: integer weights match exactly after agreed scaling and rounding.

#### Phase 5: Bend routing

- Implement array/CSR Dijkstra in Bend 2.
- Add predecessor reconstruction and unreachable/error results.
- Add delta-stepping only after Dijkstra is correct.
- Introduce `LAWS.bend` and `PROOF.bend` for tractable invariants such as path
  endpoint preservation and minimum reduction; do not claim full shortest-path
  verification until the law actually states and proves it.

Gate: exact cost parity on all fixtures; tie-equivalent paths are validated by
edge existence and total cost rather than sequence identity.

#### Phase 6: Bend worker integration

- Compile a native worker pinned by Bend version and source hash.
- Add framed binary protocol, timeout, maximum graph size, crash supervision,
  and bounded concurrency. Do not add a Clojure computational fallback.
- Run stable-Bend versus candidate-Bend shadow mode in development and staging.

Gate: malformed or failed worker requests cannot crash the Clojure service;
shadow mismatch rate is zero for the golden suite.

#### Phase 7: dynamic voyages and parity completion

- Port forecast-window rerouting and waypoint enrichment.
- Preserve route-decision history and snapshot provenance.
- Complete all preset and API parity cases.

Gate: all baseline capabilities in the table have automated evidence.

#### Phase 8: performance and release

- Benchmark serialization, edge weighting, Dijkstra, and delta-stepping
  independently at 1k, 10k, 84k, and larger graphs.
- Tune CPU threads first. Evaluate Bend GPU mode for uniform edge-cost batches;
  retain it only if end-to-end latency improves on supported hardware.
- Add container/release build, SBOM, dependency scanning, load tests, and backup
  documentation for generated graph artifacts.

Gate: no regression against the agreed latency/memory envelope, with correctness
gates still enabled. Release is explicitly labeled non-navigational unless the
required maritime safety review has occurred.

### 5. Completion — cutover checklist

- Every capability row has automated acceptance evidence.
- Golden and randomized differential tests pass in CI.
- Bend binary/source hash and protocol version appear in diagnostics.
- Worker circuit breaker and typed unavailability responses are exercised by
  fault tests.
- Operational runbook covers graph rebuild, weather outage, worker crash, and
  rollback.
- Documentation states data licenses and forecast/coastline provenance.
- Old application is archived only after a measured dual-run period.

## Testing strategy

- Unit: formulas, rounding, headings, interpolation, objective policies.
- Property: non-negative costs, monotonic distance cost, valid reconstructed
  edges, reported cost equals path sum.
- Differential: stable Bend versus candidate Bend, plus legacy golden fixtures.
- Golden: exported old-system routes and metadata.
- Contract: HTTP and Clojure/Bend protocol compatibility.
- Geospatial: narrow channels, island coastlines, antimeridian, endpoint snap.
- Fault: timeout, truncated response, process exit, oversized graph, bad weight.
- Performance: fixed datasets, warmups, percentiles, allocation and peak memory.

## Principal risks and decisions

1. **Bend 2 is evolving.** Pin the exact compiler in CI and releases; upgrades
   require the complete differential suite.
2. **Graph traversal may not suit a GPU.** Bend is valuable for language-level
   parallelism and proofs even when CPU execution wins. GPU is an optimization,
   not an architectural requirement.
3. **Floating-point drift can fake parity failures.** Bend defines the canonical
   numeric representation and rounding rule. Legacy results are compared after
   applying the documented tolerance or scaled-integer conversion.
4. **Shortest paths may not be unique.** Compare optimal cost and path validity;
   require identical node sequences only after specifying tie-breaking.
5. **Maritime safety is broader than coastline avoidance.** Draft, bathymetry,
   traffic separation, restricted areas, and authoritative forecasts are future
   domain requirements, not implied by algorithmic correctness.

## Immediate next work

The next implementation task is Phase 1: add a read-only fixture exporter to
`hvm4-pathfinding`, then consume those fixtures here without changing the old
repository's routing behavior.
