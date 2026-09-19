import Base

# All physical models and objective calculations live in Bend. F32 is used for
# the first parity implementation; the worker protocol returns scaled U32 costs.

type Condition is Data:
  Normal{}
  Laden{}
  Ballast{}

type CostMode is Data:
  TimeOnly{}
  FuelOnly{}
  Weighted{alpha: F32}
  Safety{alpha: F32, threshold: F32, penalty: F32}

type Ship is Data:
  Ship{
    design_speed_knots: F32,
    waterline_length_m: F32,
    displacement_m3: F32,
    block_coefficient: F32,
    condition: Condition,
    fuel_rate_base: F32,
    fuel_rate_per_knot: F32
  }

type Weather is Data:
  Weather{
    wind_speed_ms: F32,
    wind_direction_deg: F32,
    current_speed_ms: F32,
    current_direction_deg: F32
  }

type Edge is Data:
  Edge{distance_m: F32, heading_deg: F32}

type Metrics is Data:
  Metrics{cost: F32, hours: F32, fuel_kg: F32, speed_knots: F32, beaufort: F32}

def wind_to_beaufort(+speed_ms: F32) -> F32:
  Bool.pick(F32, F32.is_gt(speed_ms, 0.0), F32.pow((speed_ms / 0.836 : F32), 0.6666667), 0.0)

def froude_number(speed_knots: F32, waterline_length_m: F32) -> F32:
  ((speed_knots * 0.5144 : F32) / F32.sqrt((9.81 * waterline_length_m : F32)) : F32)

def relative_angle.finish(+delta: F32) -> F32:
  Bool.pick(F32, F32.is_le(delta, 180.0), delta, (360.0 - delta : F32))

def relative_angle(a: F32, b: F32) -> F32:
  relative_angle.finish(F32.mod(F32.abs((a - b : F32)), 360.0))

def normal_alpha.pick_70(+f: F32, high: Bool) -> F32:
  Bool.pick(F32, high,
    (3.1 - 5.3 * f - 12.4 * f * f : F32),
    (2.6 - 3.7 * f - 11.6 * f * f : F32))

def normal_alpha.pick_65(+cb: F32, +f: F32, high: Bool) -> F32:
  Bool.pick(F32, high,
    normal_alpha.pick_70(f, F32.is_ge(cb, 0.675)),
    (2.2 - 2.5 * f - 9.7 * f * f : F32))

def normal_alpha(+cb: F32, +f: F32) -> F32:
  Bool.pick(F32, F32.is_lt(cb, 0.575),
    (1.7 - 1.4 * f - 7.4 * f * f : F32),
    normal_alpha.pick_65(cb, f, F32.is_ge(cb, 0.625)))

def laden_alpha.pick(+cb: F32, +f: F32, high: Bool) -> F32:
  Bool.pick(F32, high,
    (3.1 - 18.7 * f + 28.0 * f * f : F32),
    (2.6 - 13.1 * f - 15.1 * f * f : F32))

def laden_alpha(+cb: F32, +f: F32) -> F32:
  Bool.pick(F32, F32.is_lt(cb, 0.775),
    (2.4 - 10.6 * f - 9.5 * f * f : F32),
    laden_alpha.pick(cb, f, F32.is_ge(cb, 0.825)))

def ballast_alpha.pick(+cb: F32, +f: F32, high: Bool) -> F32:
  Bool.pick(F32, high,
    (3.4 - 20.9 * f + 31.8 * f * f : F32),
    (3.0 - 16.3 * f - 21.6 * f * f : F32))

def ballast_alpha(+cb: F32, +f: F32) -> F32:
  Bool.pick(F32, F32.is_lt(cb, 0.775),
    (2.6 - 12.5 * f - 13.5 * f * f : F32),
    ballast_alpha.pick(cb, f, F32.is_ge(cb, 0.825)))

def correction_factor.go(cb: F32, f: F32, condition: Condition) -> F32:
  match condition:
    case Normal{}:
      normal_alpha(cb, f)
    case Laden{}:
      laden_alpha(cb, f)
    case Ballast{}:
      ballast_alpha(cb, f)

def correction_factor(cb: F32, speed: F32, length: F32, condition: Condition) -> F32:
  correction_factor.go(cb, froude_number(speed, length), condition)

def head_loss.go(+bn: F32, +displacement: F32, condition: Condition) -> F32:
  match condition:
    case Normal{}:
      ((0.7 * bn + F32.pow(bn, 6.5) / (22.0 * F32.pow(displacement, 0.6666667) : F32) : F32) / 100.0 : F32)
    case Laden{}:
      ((0.5 * bn + F32.pow(bn, 6.5) / (2.7 * F32.pow(displacement, 0.6666667) : F32) : F32) / 100.0 : F32)
    case Ballast{}:
      ((0.7 * bn + F32.pow(bn, 6.5) / (2.7 * F32.pow(displacement, 0.6666667) : F32) : F32) / 100.0 : F32)

def direction_factor.tail(+angle: F32, +bn: F32, following: Bool) -> F32:
  Bool.pick(F32, following,
    ((0.4 - 0.03 * (bn - 8.0 : F32) * (bn - 8.0 : F32) : F32) / 2.0 : F32),
    ((0.9 - 0.06 * (bn - 6.0 : F32) * (bn - 6.0 : F32) : F32) / 2.0 : F32))

def direction_factor.mid(+angle: F32, +bn: F32, broad: Bool) -> F32:
  Bool.pick(F32, broad,
    direction_factor.tail(angle, bn, F32.is_gt(angle, 150.0)),
    ((1.7 - 0.03 * (bn - 4.0 : F32) * (bn - 4.0 : F32) : F32) / 2.0 : F32))

def direction_factor(+angle: F32, +bn: F32) -> F32:
  Bool.pick(F32, F32.is_le(angle, 30.0), 1.0,
    direction_factor.mid(angle, bn, F32.is_gt(angle, 60.0)))

def objective(+hours: F32, +fuel: F32, +bn: F32, mode: CostMode) -> F32:
  match mode:
    case TimeOnly{}:
      hours
    case FuelOnly{}:
      fuel
    case Weighted{+alpha}:
      (alpha * hours + (1.0 - alpha : F32) * fuel : F32)
    case Safety{+alpha, threshold, penalty}:
      +base = (alpha * hours + (1.0 - alpha : F32) * fuel : F32)
      Bool.pick(F32, F32.is_gt(bn, threshold), (base + penalty : F32), base)

def edge_cost(ship: Ship, weather: Weather, edge: Edge, mode: CostMode) -> Metrics:
  Ship{+design, +length, +displacement, +cb, +condition, +fuel_base, +fuel_per_knot} = ship
  Weather{+wind_speed, +wind_direction, +current_speed, +current_direction} = weather
  Edge{+distance, +heading} = edge
  +bn = wind_to_beaufort(wind_speed)
  loss = (correction_factor(cb, design, length, condition) * head_loss.go(bn, displacement, condition) * direction_factor(relative_angle(wind_direction, heading), bn) : F32)
  current = (current_speed * 1.94384 * F32.cos((relative_angle(current_direction, heading) * 0.0174532925 : F32)) : F32)
  +speed = F32.max(0.5, (design * (1.0 - loss : F32) + current : F32))
  +hours = ((distance / 1852.0 : F32) / speed : F32)
  +fuel = (hours * (fuel_base + fuel_per_knot * speed : F32) : F32)
  Metrics{objective(hours, fuel, bn, mode), hours, fuel, speed, bn}

def scale_cost(cost: F32, scale: F32) -> U32:
  F32.to_u32(F32.round((cost * scale : F32)))
