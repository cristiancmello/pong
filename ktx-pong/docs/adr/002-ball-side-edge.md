# ADR 002: Ball behavior at side edges before reset

## Status
Proposed

## Context
When the ball reaches the side edge (left or right of the Arena), the game needs to decide what happens before a point reset. The design mentions that hit offset is part of R1, which exercises the Collision Response, but the behavior at the side edge before reset needs to be defined.

## Decision
The Arena acts as an "absorber" at the side edges - the ball does not bounce off the side edges. Instead:
- The Arena returns an "absorb" collision event when the ball reaches the side edge
- The Game Mode interprets this event as a "round reset"
- The Game Mode then initiates a new serve

This ensures that the Collision System's responsibility is purely collision detection/response, while the Game Mode handles the game logic (scoring, serve reset).

## Consequences
- **Positive:** Clear separation - Collision System detects collisions, Game Mode decides the outcome
- **Positive:** Deterministic behavior - the ball always gets absorbed at side edges
- **Negative:** Requires the Game Mode to handle the side-edge collision as a reset event
- **Negative:** Removes the possibility of side-wall bounces (intentional design choice for R1)

## Related
- US-08: Point on side edge
- Collision System: "Given the current state, what is the next collision?"
- Game Mode: "Evaluate this Collision Event; is this Command valid now?"