# Design Document

Architecture decisions through information hiding, inspired by *A Philosophy of Software Design* (Ousterhout). This Technical Design Document describes how the software is divided so that every decision has one owner, with no implementation details. Gameplay vision, art direction, audio, and screen flow belong to a Game Design Document and are out of scope. Terminology follows common game-development usage (Box2D, Unity, Unreal, *Game Programming Patterns*).

## 1. Premise

Instead of decomposing by **time** (read, compute, update, draw), we decompose by **knowledge**: what information exists, and whose secret each piece is. Pong was chosen because it is the leanest domain.

**Quality test:** a change in requirements should require editing **a single module**. If it requires two, there is information leakage.

## 2. Principles That Guided the Decisions

- **Collision event versus outcome.** The Collision System returns facts ("the ball collided with the left edge at point P"). The Game Mode interprets them ("the right player scored").
- **Interfaces in domain terms.** Nobody exposes internal representation (for example, `y = ax + b` versus a vector).
- **Deep modules.** Small interface, complexity concentrated inside the module.
- **One-way flow between simulation and Renderer.** The simulation informs, the Renderer consumes. Nothing visual flows back into the physics.
- **Fairness through access structure**, not through extra verification. Game Mode is read-only after start, and only it writes Game State.
- **"Collision" is the only word used** for what happens when two things meet (no "contact").

## 3. Modules

| # | Module | Secret | Interface in one sentence |
|---|---|---|---|
| 1 | **Ball** | Kinematic State representation, its Collider, how it applies a Collision Response | "Tell me where I will be from here; apply this Collision Response." |
| 2 | **Arena** | Boundary shape (its Collider), paddles added to it, movement bounds | "Tell me where this Kinematic State crosses my boundary; tell me how far a paddle can go." |
| 3 | **Paddle** | Kinematic State, its Collider, how the hit position becomes a hit offset | "Move according to the Command; tell me what Collision Response you produce at this point." |
| 4 | **Collision System** | How to find the next collision between moving Colliders (time of impact, continuous collision detection) | "Given the current state, what is the next collision?" |
| 5 | **Game Mode** | The rules: scoring, win condition, cooldown, serve | "Evaluate this Collision Event; is this Command valid now?" |
| 6 | **Game State** | Mutable match state: score, current round, who serves | "Tell me the current score and round." |
| 7 | **Command** | How real input (keyboard) becomes a timestamped Command in Game Clock time | "Receive the key; deliver a timestamped Command." |
| 8 | **Game Clock** | How simulation time advances | "Advance to T." |
| 9 | **Renderer** | How the game is drawn: Sprite/Mesh, VFX, juice, HUD | "Given an instant, draw the game." |
| + | **Simulation** | The order of the cycle | "Run the match by relaying questions and answers." |

### Concepts (data owned by modules, not modules themselves)

| Concept | Owner | Meaning |
|---|---|---|
| **Kinematic State** | Ball, Paddle | Position and motion of an object. Representation is the owner's secret. |
| **Collider** | Ball, Paddle, Arena | The **physical** shape used for collisions. |
| **Sprite/Mesh** | Renderer | The **visual** shape used for drawing. Never read by physics. |
| **Collision Response** | The collided object | What a collision does to the ball (bounce direction, hit offset). A semantic description, never raw data. |
| **Collision Event** | Collision System | The fact that a collision happened, where, and between which objects. |
| **Collision Listener** | Collision System (interface) | The single channel through which Collision Events are dispatched. |

## 4. Decisions by Area

### Movement and Collision
- Simulation **collision to collision** (no fixed time step). Time becomes derived data.
- Ball and Arena **do not know each other**. Only the Collision System knows both.
- The Collision System **never stores predictions**: it always recomputes. There is nothing to invalidate. Caching, if needed, stays inside it.
- **Ball and Paddle have independent Kinematic States.** The paddle's single-axis restriction is not knowledge the Collision System has.
- Its question is uniform: "given these moving Colliders, when and where is there a collision?". It does not know which is a ball or a paddle.
- It returns **only the next collision in time**, which resolves the paddle versus edge precedence.

### Collision System: Detects Collisions and Reports Facts
- **Detects** the collision: that it happened, where, and between which objects.
- **Asks the collided object** for its Collision Response and passes the answer to the Ball. It never computes the effect itself.
- **Dispatches Collision Events only**, through the Collision Listener. Listeners (Game Mode, Renderer) take what interests them and ignore the rest.
- **Never interprets.** It does not know score, point, or cooldown. What an event means belongs to the Game Mode.
- **Does not know who listens.** Simulation wires the listeners; adding or removing one does not change the Collision System.

### Colliders, Sprite/Mesh and Paddles
- **Collider and Sprite/Mesh are separate knowledge.** The Collider changes for gameplay reasons, the Sprite/Mesh for visual taste.
- Each object **owns its Collider**. The Renderer owns every Sprite/Mesh.
- The Renderer **queries the Collider** as a visual anchor and never keeps a copy (single source of truth), so the visuals cannot silently diverge from collision.
- Paddles are **added to the Arena**.
- Paddle movement: straight line, constant speed, commanded by the player, with only a **movement bound** enforced by **clamping** (no inertia or friction).
- Stopping at the bound is a paddle-edge collision, handled by the same mechanism.
- The Paddle translates "collision point + movement" into a **Collision Response** (hit offset). It never passes raw data to the Ball.
- The first version may restrict Colliders to convex shapes and translation only, without exposing this in the interface.

### Time and Input
- **Game Clock** is the only clock, advanced only by the simulation. This isolates the game from FPS.
- The **Command** module is the only point that reads the system clock.
- Commands are accepted **at any moment**: the Game Clock advances to the earlier of the next collision and the next Command's instant.
- The Paddle updates its own Kinematic State from the Command's instant.

### Game Mode and Game State
- **Game Mode holds the rules**: scoring, win condition, cooldown, serve. It is **fixed at start and read-only**, with a single value identical for both players.
- **Game State holds what changes**: score, round, who serves. **Only Game Mode writes it**; everyone else reads.
- **Cooldown is a Game Mode rule.** The question "is this Command valid now?" belongs to it.
- **Serve** (ball position and direction at the start of each round) is defined by the Game Mode. It can be fixed in the first version. After a point, a **round reset** returns the match to a serve.

### Renderer
- **Sprite/Mesh** is the elaborate look drawn on top of the Collider (glow, depth, deformation).
- **VFX** are effects such as trails and glow. They need past positions, which the Renderer stores itself.
- **Juice** is the impact feedback that makes the game feel good (flash, squash and stretch, screen shake). It reacts to Collision Events.
- **HUD** shows the score. It receives a Game State snapshot through the Simulation and never queries the Game Mode.
- The Renderer stores only its own visual state (recent positions, a glow that fades). Nothing else reads it.

### Simulation
- Its own module, owner of the secret "who asks whom, and in what sequence".
- **Relays, does not interpret.** Delivers state, Collision Events, and Game State to the Renderer in the format received, without assembling packages specific to the screen.
- It is not temporal decomposition, because the order is the secret of a single module, and no other module needs to know its place in the sequence.

## 5. Simulation Flow (without imposing order on the modules)

1. What is the next instant of interest (pending Command or next collision)?
2. Advance the Game Clock to that instant.
3. If it was a **collision**: the Collision System detects it, asks the collided object for its Collision Response, passes it to the Ball, and dispatches the Collision Event to the Collision Listeners.
4. The Game Mode evaluates the Collision Event, updates the Game State, and returns the outcome (for example, "round reset").
5. If it was a **Command**: the Game Mode validates it and the Paddle updates its Kinematic State.
6. On "round reset": the Game Mode supplies the serve and the Simulation relays it to the Ball and Paddles.
7. State, Collision Events, and Game State are relayed to the Renderer.

## 6. Change Test

| Change | Affected module |
|---|---|
| Change the scoring rule | Game Mode |
| Change the cooldown value | Game Mode (configuration) |
| Change what is stored per match (for example, add a round counter) | Game State |
| Change the Kinematic State representation | Ball and Paddle |
| Change the paddle's Collider | Paddle (the Renderer follows by querying) |
| Change the Arena's shape | Arena |
| Replace keyboard with another device | Command |
| Change the cycle order | Simulation |
| Change the visual style, add VFX, or tune juice | Renderer |
| Run at 30 or 144 FPS | Renderer (game outcome identical) |
| Play without a screen | None (Simulation with no listener to relay to) |

## 7. First Version Scope

**In:** rectangular Arena, Ball on a straight line, two constant-speed Paddles, own Colliders, Collision Response with hit offset, point for a side collision, scoring, win condition, single cooldown, Game Clock, Renderer with Sprite/Mesh, VFX, juice, and HUD.

**Left for later, each item with an identified owner:**

| Extension | Owner |
|---|---|
| Paddle inertia and friction | Paddle and Arena |
| Acceleration effect on bounce (extra field in the Collision Response) | Paddle and Ball |
| Discard versus delay a Command that arrives during cooldown (starts with discard) | Game Mode |
| Order of simultaneous Commands (fair to both sides) | Simulation and Game Mode |
| Non-rectangular Arenas | Arena and Collider |
| Rotation and concave Colliders | Collider and Collision System |

## 8. Loose Ends

All closed. Implementation details are left for later, without altering the interfaces:

- Ball Collider as a point or a circle, and how thickness enters the computation.
- Where the cooldown history lives. The natural home is Game State (timestamps per player), with Game Mode reading the value and judging validity.
- The mapping between real time and Game Clock time inside the Command module.

## 9. Pitfalls to Watch

- **Collision System becoming a "god":** if it starts to know point, score, or cooldown, the Game Mode's secret has leaked into it.
- **Simulation becoming a "GameManager":** if it changes along with the scoring rule, the physics, or the Arena's shape, some secret has leaked into it.
- **Game State with many writers:** if anything besides the Game Mode writes it, the fairness guarantee is lost.
- **Visuals feeding the physics:** any dependence of the simulation on something visual reintroduces the FPS problem.
- **Sprite/Mesh diverging from the Collider:** keep the Collider as the single source, and have the Renderer query it.

## 10. Glossary

| Term | Meaning |
|---|---|
| **Arena** | The bounded playing area. Owns its boundary (a Collider), the Paddles added to it, and their movement bounds. |
| **Ball** | The object that travels across the Arena and bounces. Owns its Kinematic State and its Collider. |
| **Bounce** | The change in the Ball's direction after a collision, produced by applying a Collision Response. |
| **Clamp** | Enforcing a movement bound by stopping an object at the limit instead of letting it go past. |
| **Collider** | The physical shape used for collisions. Each object owns its own, independent of how it looks. |
| **Collision** | The moment two Colliders meet. The only word used for this idea in the document. |
| **Collision Event** | The fact that a collision happened, where, and between which objects. It carries no meaning about points or rules. |
| **Collision Listener** | The interface through which Collision Events are dispatched. Whoever listens takes what interests it and ignores the rest. |
| **Collision Response** | A semantic description, produced by the collided object, of what a collision does to the Ball (for example, bounce direction and hit offset). Never raw data. |
| **Collision System** | The module that finds the next collision between moving Colliders, asks the collided object for its Collision Response, and dispatches Collision Events. It never interprets them. |
| **Command** | A player's intention (move up, stop) timestamped in Game Clock time. Also the name of the module that translates real input into Commands. |
| **Continuous collision detection (CCD) / time of impact** | The technique of computing *when* two moving shapes will collide, instead of moving a step and checking for overlap. It is internal to the Collision System. |
| **Cooldown** | The minimum interval between Commands from the same player. A Game Mode rule, with one value for both players. |
| **Event dispatch** | Sending a Collision Event to all listeners without knowing who they are. |
| **Game Clock** | The only clock of the simulation. It advances to the next collision or the next Command, and is independent of FPS. |
| **Game Mode** | The rules of the match: scoring, win condition, cooldown, serve. Fixed at start and read-only. The only writer of the Game State. |
| **Game State** | The mutable data of the match: score, current round, who serves. Everyone reads it, only the Game Mode writes it. |
| **Hit offset** | The angle deviation of a bounce that depends on where on the Paddle the Ball hit. Part of the Collision Response. |
| **HUD** | Heads-up display: information drawn over the game, such as the score. Owned by the Renderer, fed by a Game State snapshot. |
| **Juice** | Impact feedback that makes the game feel good (flash, squash and stretch, screen shake). Reacts to Collision Events. |
| **Kinematic State** | The position and motion of an object, driven by velocity rather than forces. Its representation is the owner's secret. |
| **Movement bound** | The limit within which a Paddle may move, defined by the Arena. |
| **Outcome** | The result of the Game Mode evaluating an event or Command (for example, "round reset"). |
| **Paddle** | The player-controlled object. Owns its Kinematic State and Collider, and turns the hit position into a hit offset. |
| **Renderer** | The module that draws the game: Sprite/Mesh, VFX, juice, and HUD. It only consumes information and never feeds the physics. |
| **Round reset** | Returning the match to a serve after a point. |
| **Serve** | The ball's starting position and direction at the beginning of a round. Defined by the Game Mode. |
| **Simulation** | The module that runs the cycle, relaying questions and answers between modules without knowing their secrets. |
| **Sprite/Mesh** | The visual shape used for drawing, elaborate and owned by the Renderer. Never read by physics. |
| **VFX** | Visual effects such as trails and glow. They need past positions, which the Renderer stores itself. |
