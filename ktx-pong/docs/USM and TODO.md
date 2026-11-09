# User Story Map and TODO: Release 1

Project: ktx-pong. Based on the Design Document (information hiding, inspired by *A Philosophy of Software Design*).

## Scope adjustments to section 7 of the Design Document

To keep R1 lean without hurting the design:

- **VFX, juice and a dedicated Sprite/Mesh move to R2.** In R1 the Renderer draws simple shapes by querying the Collider, which already validates the rule "visuals never feed the physics".
- **Cooldown stays as an interface, without a rule.** The question "is this Command valid now?" exists in the Game Mode, but always answers "yes". This avoids rework on the interfaces later.
- **Hit offset is part of R1.** Without it, rallies are deterministic and boring, and it exercises the Collision Response, which is the central idea of the design.

## 1. Story Map

**Persona:** Player. Technical enablers get their own row, because in this project the architecture is part of the value.

|  | 1. Start match | 2. Control paddle | 3. Play the rally | 4. Score a point | 5. End match | 6. See the game |
| --- | --- | --- | --- | --- | --- | --- |
| **R1: Playable game** | US-01 Fixed serve | US-03 Move with keyboard | US-05 Ball bounces off walls | US-08 Point on side edge | US-10 First to N wins | US-12 Scoreboard (HUD) |
|  | US-02 Restart match | US-04 Paddle stops at its bound | US-06 Ball bounces off paddle | US-09 New serve after a point | US-11 Victory screen | US-13 Basic shapes via Collider |
|  |  |  | US-07 Angle from hit position |  |  | US-14 Same result at 30 and 144 FPS |
| **R2: Feel** | Variable serve | Cooldown (discard) |  |  | Pause | Sprite/Mesh, trail, glow, juice |
| **R3: Depth** |  | Inertia and friction; ordering of simultaneous Commands; delayed cooldown | Acceleration on bounce |  |  |  |
|  |  |  | Non-rectangular Arenas; concave Colliders and rotation |  |  |  |

### Enablers (R1 technical row)

| ID | Enabler | Owner module |
| --- | --- | --- |
| EN-01 | Project free of template leftovers, with minimal dependencies | Build |
| EN-02 | Pure Kotlin domain module, no libGDX (guarantees "play without a screen") | Build |
| EN-03 | Headless tests with a full simulated match | Simulation |
| EN-04 | Determinism and FPS-independence tests | Simulation / Game Clock |

## 2. R1 stories with acceptance criteria

| ID | Story | Acceptance criterion (summary) | Modules |
| --- | --- | --- | --- |
| US-01 | As a player, I want the ball to start from the center, so the rally begins. | Fixed serve defined by the Game Mode. | Game Mode, Ball |
| US-02 | As a player, I want to restart the match, so I can play again. | Score reset and initial serve. | Game Mode, Game State |
| US-03 | As a player, I want to move my paddle with the keyboard. | Straight-line movement, constant speed, timestamped Command. | Command, Paddle, Game Clock |
| US-04 | As a player, I want the paddle to stop at its limits. | The bound comes from the Arena and the clamp is handled as a paddle-edge collision. | Arena, Paddle, Collision System |
| US-05 | As a player, I want the ball to bounce at the top and bottom. | Continuous collision, never crossing the wall at any speed. | Collision System, Arena, Ball |
| US-06 | As a player, I want to hit the ball with the paddle. | The Collision Response comes from the Paddle and is applied to the Ball. | Paddle, Ball, Collision System |
| US-07 | As a player, I want to control the angle by where I hit. | Hit offset inside the Collision Response. | Paddle |
| US-08 | As a player, I want a point when the ball reaches the opponent's side. | The Collision System only emits the event, and the Game Mode interprets it. | Collision System (event), Game Mode |
| US-09 | As a player, I want a new serve after each point. | "Round reset" comes from the Game Mode and the Simulation relays it. | Game Mode, Simulation |
| US-10 | As a player, I want the match to end when someone reaches N points (suggestion: 5). | Win condition in the rules, with N configurable in the Game Mode. | Game Mode, Game State |
| US-11 | As a player, I want to see who won and be able to restart. | "Finished" state read from the Game State. | Game State, Renderer |
| US-12 | As a player, I want to see the score. | HUD receives a Game State snapshot via the Simulation. | Renderer, Simulation |
| US-13 | As a player, I want to see the ball, paddles and arena. | The Renderer queries the Collider as an anchor and keeps no copy. | Renderer |
| US-14 | As a player, I want the game to behave the same on any monitor. | Same sequence of events and score at 30 and 144 FPS. | Game Clock, Simulation |

## 3. TODO in implementation order

The order is by vertical slices: each phase ends with something demonstrable and testable. The domain comes first and headless, and libGDX only enters as a shell.

### Phase 0: Groundwork (EN-01, EN-02)

- [DONE] Remove the template's `FirstScreen`, `logo.png` and the `ktx-gdx-demo-1` title.
- [DONE] Prune `core` dependencies. Ashley, Artemis, Box2D, gdx-ai, scene2d, vis and tiled are not part of the design. Box2D in particular is a temptation to leak the Collision System's secret. Keep `ktx-app`, `ktx-graphics`, `ktx-math` and `ktx-log`.
- [DONE] Align toolchains. The root `build.gradle` uses `jvmToolchain(8)`, `core` uses 25 and `lwjgl3` uses 25.
- [DONE] Create the Gradle module `domain` (pure Kotlin, only `kotlin-test`). `core` depends on it, never the reverse. The compiler now enforces the one-way flow.
- [DONE] Record the open decisions (section 4) in `docs/adr/`.

**Done when:** `./gradlew test` runs in `domain` with no libGDX class on the classpath.

### Phase 1: Domain vocabulary

- [ ] Value types: own `Vec2`, `GameTime` (value class) and `Duration`.
- [ ] `KinematicState` with a "where will I be after Δt" interface. Exact linear position, no numerical integration.
- [ ] `GameClock`: advances only through an explicit call from the Simulation.

**Done when:** analytic position tests pass.

### Phase 2: Colliders and Collision System (the hard core)

- [ ] `Collider` interface with `CircleCollider` and `BoxCollider` (AABB). The first cut is convex and translation only.
- [ ] Time of impact for a moving circle against a moving box, treating the Arena as an inner box.
- [ ] `CollisionSystem.next(state)` returns only the next collision (time, point, participants), storing no prediction.
- [ ] `CollisionEvent` and `CollisionListener`.
- [ ] Edge-case tests: tangent, corner, high speed, collision at Δt=0 (avoid infinite loops) and simultaneous ties.

**Done when:** the collision suite passes, including the edge cases.

### Phase 3: Slice 1, bouncing ball (US-05, US-13 partial)

- [ ] `Arena` with a boundary Collider and a wall Collision Response (reflect).
- [ ] `Ball` with Kinematic State and application of the Collision Response.
- [ ] `Simulation`, the cycle from section 5 of the Design Document, still without Commands.
- [ ] Headless test: 10,000 collisions and the ball never leaves the Arena.
- [ ] libGDX shell: a `Screen` that advances the Clock to the frame's instant and draws. The drawing places the ball exactly at that instant, since the motion is linear and needs no interpolation.

**Done when:** the ball bounces in the 640x480 window.

### Phase 4: Slice 2, paddles (US-03, 04, 06, 07)

- [ ] `Command` module: a key becomes a Command timestamped at Game Clock time. It is the only point that reads the system clock.
- [ ] `Paddle` with its own Kinematic State, its own Collider, and updates starting at the Command's instant.
- [ ] Arena provides the movement bound and the clamp becomes a paddle-edge collision.
- [ ] Paddle Collision Response with hit offset.
- [ ] Simulation advances to the earlier of the next collision and the next Command.

**Done when:** two players on the same keyboard can hit the ball (W/S and ↑/↓).

### Phase 5: Slice 3, rules (US-01, 02, 08, 09, 10)

- [ ] Immutable `GameMode`, with `evaluate(event)`, `isValid(command)` (always `true` in R1), serve and N.
- [ ] `GameState` written exclusively by the Game Mode (`internal` visibility / read-only interface).
- [ ] Round reset, win condition and restart.
- [ ] Headless test of a full match through to victory.

**Done when:** a full match runs without a screen.

### Phase 6: Slice 4, final presentation (US-11, 12, 13, 14)

- [ ] HUD with a Game State snapshot received from the Simulation.
- [ ] Victory screen and restart.
- [ ] Renderer with simple shapes, querying the Colliders.

**Done when:** the game is playable from start to finish.

### Phase 7: Hardening (EN-03, EN-04)

- [ ] FPS-independence test: the same Command input yields the same events and the same score with simulated frames at 30 and 144 FPS.
- [ ] Determinism test with a fixed serve.
- [ ] Review of the "Pitfalls" list (section 9 of the Design Document), with one checklist item per pitfall.
- [ ] Run the "change test" table (section 6) as an exercise. For each row, confirm that only one module would be edited.

## 4. Open decisions to resolve before Phase 2

These surfaced when crossing the design with the implementation, and each is worth a short ADR:

1. **Paddle-edge collision and the "Ball's Collision Response".** The design defines the Response as the effect on the ball, but the clamp affects the paddle. Suggestion: the Response is addressed to the other participant in the collision, and each object knows how to apply it. Ball and paddle react through the same channel.
2. **What the ball does at the side edge before the reset.** Suggestion: the Arena answers "absorb" (no bounce) and the Game Mode returns "round reset".
3. **Command with a timestamp earlier than the Clock.** Define the policy: the Command module only delivers Commands with a timestamp greater than or equal to the Clock, or the Simulation applies them at the current instant.
4. **Passing from real time to the Clock.** This is the design's "loose end". Suggestion: the Clock advances to `now - offset`, with the offset fixed at match start.
5. **Deterministic serve.** In R1 it is fixed. When it becomes random in R2, the seed should come from the Game Mode.
