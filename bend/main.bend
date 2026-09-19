import Base

# Migration kernel prototype. Costs are scaled unsigned integers; production
# graph kernels will consume CSR arrays over the same representation.

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

def main() -> U32:
  best_pair([12000, 9700, 15100], [8300, 11200, 10400])
