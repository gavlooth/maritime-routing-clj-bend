import Base

# Algorithmic code is Bend-owned from day one. This module is the checked seed
# for CSR Dijkstra and delta-stepping: parallel minimum reduction over candidate
# route costs using the same scaled U32 representation as the future worker.

def min_u32.pick(a: U32, b: U32, take_a: Bool) -> U32:
  match take_a:
    case True{}:
      a
    case False{}:
      b

def min_u32(+a: U32, +b: U32) -> U32:
  min_u32.pick(a, b, U32.is_le(a, b))

def best_cost(xs: List<U32>) -> U32:
  match xs:
    case Nil{}:
      4294967295
    case Con{head, tail}:
      min_u32(head, best_cost(tail))

def best_pair(left: List<U32>, right: List<U32>) -> U32:
  a b = best_cost(left) best_cost(right)
  min_u32(a, b)

