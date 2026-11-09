# ADR 005: Deterministic serve in R1

## Status
Proposed

## Context
In Release 1 (R1), the serve is fixed to ensure deterministic testing and rally start. When the serve becomes random in R2, the source of randomness needs to be defined.

## Decision
In R1, the serve is fixed and defined by the Game Mode. The Game Mode specifies exactly where the ball starts and in which direction it serves. This allows for:
- Deterministic testing of the rally start
- Consistent behavior across runs
- Predictable debugging of the initial rally

When the serve becomes random in R2, the seed for randomness should come from the Game Mode, ensuring that the randomness is still deterministic from the Game Mode's perspective (i.e., the Game Mode controls what "random" means, not the underlying system).

## Consequences
- **Positive:** Deterministic tests in R1 - analytic position tests can pass
- **Positive:** Clear progression from fixed serve (R1) to variable serve (R2)
- **Negative:** R1 lacks the variety of a random serve
- **Negative:** Requires Game Mode to define both fixed and random serve behaviors

## Related
- US-01: Fixed serve
- Phase 1: Domain vocabulary
- EN-01: Project free of template leftovers, with minimal dependencies