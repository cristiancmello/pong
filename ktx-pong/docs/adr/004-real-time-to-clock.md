# ADR 004: Passing from real time to the Game Clock

## Status
Proposed

## Context
The design has a "loose end" regarding the transition from real time to the Game Clock. When starting a match, there needs to be a way to initialize the Clock based on real time, and then keep it synchronized.

## Decision
The Clock advances from a fixed offset determined at match start. The approach is:
- At match start, the Clock is initialized to 0
- The Clock then advances based on game events, not real time passage
- The offset between real time and Clock time is fixed at match start
- The Game Mode or Clock module is responsible for advancing the Clock

This ensures that the simulation is deterministic and not dependent on real time fluctuations, while still allowing the application to track real time for purposes like VSYNC, FPS limiting, etc.

## Consequences
- **Positive:** Deterministic simulation - the Clock is independent of real time
- **Positive:** Easy to implement replay/demo functionality
- **Negative:** Real time tracking must be separate from the Game Clock
- **Negative:** Requires careful initialization at match start

## Related
- EN-04: Determinism and FPS-independence tests
- Game Clock: advances only through an explicit call from the Simulation
- Simulation: relays game state changes