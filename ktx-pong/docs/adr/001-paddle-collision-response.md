# ADR 001: Paddle-edge collision and the Ball's Collision Response

## Status
Proposed

## Context
The design defines the Collision Response as the effect on the ball when it collides with a paddle. However, during implementation, it was discovered that the paddle edge clamp also affects the paddle's position. This creates a situation where the Response needs to address both the ball's behavior and the paddle's boundary constraints.

## Decision
The Collision Response is addressed to the *other participant* in the collision. Each object knows how to apply its part of the response:
- The **Ball** knows how to apply a Collision Response to itself (change its velocity/direction)
- The **Paddle** knows how to clamp its position within the Arena bounds

Both the Ball and Paddle react through the same collision channel, but each is responsible for its own part of the response. The Collision System simply facts the collision event, and each participant interprets and applies the response relevant to it.

## Consequences
- **Positive:** Clear separation of concerns - each module owns its own logic
- **Positive:** No information leakage - the Collision System doesn't need to know about paddle clamping
- **Negative:** Requires coordination between Ball and Paddle modules through the collision event
- **Negative:** The Game Mode still needs to interpret collision events to determine scoring or other game-state changes

## Related
- Collision System interface: "Given the current state, what is the next collision?"
- Paddle module: "Move according to the Command; tell me what Collision Response you produce at this point."
- Ball module: "Tell me where I will be from here; apply this Collision Response."