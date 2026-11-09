# ADR 003: Command timestamp policy relative to Game Clock

## Status
Proposed

## Context
The Command module translates real input (keyboard) into timestamped Commands in Game Clock time. A question arose: what happens if a Command has a timestamp earlier than the current Game Clock time? This could happen if input is processed out of order or if there's a time discrepancy.

## Decision
The Command module only delivers Commands with a timestamp greater than or equal to the current Game Clock. If a Command's timestamp is earlier than the Clock:
- The Command is rejected/dropped
- The Simulation continues with the current state
- No backwards time travel is allowed

Alternatively, the Simulation can apply Commands at the current instant if the timestamp is equal to or slightly before the Clock, but never allow time travel.

## Consequences
- **Positive:** Deterministic simulation - no time travel or state corruption
- **Positive:** Clear policy for handling out-of-order inputs
- **Negative:** Inputs arriving late may be dropped if significantly out of sync
- **Negative:** Requires careful handling in replay/resume scenarios

## Related
- EN-04: Determinism and FPS-independence tests
- Game Clock: advances only through an explicit call from the Simulation
- Command: "Receive the key; deliver a timestamped Command."