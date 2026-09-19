import Base
import ./physics.bend as Physics
import ./routing.bend as Routing

def finish(metrics: Physics.Metrics) -> U32:
  match metrics:
    case Physics.Metrics{cost, hours, fuel, speed, bn}:
      Routing.best_pair([Physics.scale_cost(cost, 1000.0), 15100], [8300, 11200, 10400])

def main() -> U32:
  ship = {Physics.Ship{15.0, 200.0, 30000.0, 0.65, Physics.Normal{}, 50.0, 5.0} : Physics.Ship}
  weather = {Physics.Weather{10.0, 20.0, 0.2, 90.0} : Physics.Weather}
  edge = {Physics.Edge{8000.0, 70.0} : Physics.Edge}
  finish(Physics.edge_cost(ship, weather, edge, Physics.TimeOnly{}))
