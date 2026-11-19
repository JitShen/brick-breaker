# Brick Breaker — Design

Date: 2026-11-19
Status: Draft, awaiting approval

## 1. Intent and why

A brick-breaker arcade game in Java, written from scratch, to replace an old university coursework repo
(COMP2042_CW_hfyjg6) as part of tidying up the owner's GitHub. It is **not** a job-hunting showcase:
the aim is a clean, readable, well-structured game the owner is happy to have public, following good
Java practices, without portfolio-level extras.

The README keeps a short "Remembrance" section about the coursework (already written, keep it).

## 2. Goals

- Simple but solid, fun gameplay: aimable paddle bounces, a few brick types, levels, power-ups.
- Clean, readable modern Java with a clear split between game rules and JavaFX presentation.
- Key game logic covered by unit tests; simple CI that builds and tests on every push.
- Builds and runs from a fresh clone with one command.

## 3. Non-goals

- Packaged releases/installers, code signing, web build.
- Sound, screen shake, formatting/lint enforcement, coverage reports.
- Mobile, networking, online leaderboards, level editor.

## 4. Anti-goals

- Any code, structure or naming carried over from the old coursework, or any reference to what that
  coursework was based on. The only link to the past is the README Remembrance section.
- Using trademarked game names or recreating a specific commercial game's look or levels.
- Over-engineering: interfaces with one implementation "for the future", deep inheritance, patterns
  without a reason, a separate Gradle module just for show.
- Ball tunnelling through bricks, or bounces that feel random/unfair.

## 5. Constraints

- **No JDK on the dev machine** (Windows 11). Step one: install JDK 21 (Eclipse Temurin, e.g.
  `winget install EclipseAdoptium.Temurin.21.JDK`). Gradle comes via the wrapper.
- Repo: `github.com/JitShen/brick-breaker`, branch `main`, no commits yet; `README.md` exists.
- Commits carry **no** AI co-author trailer (owner's decision). Commits are made locally; the owner
  reviews and pushes.
- Any image assets must be original or CC0 (shapes drawn in code is the default — no assets needed).

## 6. Approaches considered

| Approach | Trade-off | Outcome |
|---|---|---|
| **A. JavaFX, hand-written loop and collision, game rules kept free of UI code** | A bit more of our own code; it's small and testable. Modern standard Java UI. | **Chosen** by owner. |
| B. libGDX | Fastest polish; framework does most of the work, heavier setup. | Rejected. |
| C. Swing / Java2D | Zero deps; dated, poor animation, resembles the old coursework. | Rejected. |

Physics engines (Box2D etc.) are rejected: arcade bounces must be exact and predictable.

Scope was then cut from a "portfolio" version to this "tidy" version (owner's choice), dropping the
separate `core` module, sound, screen shake, formatting checks, coverage and releases.

## 7. Decided design

### 7.1 Tech stack (builder's calls)

- Java 21 (records, sealed interfaces, switch pattern matching where they help).
- Gradle (Kotlin DSL) with wrapper, **single module**, version catalog (`gradle/libs.versions.toml`).
- JavaFX 21 via `org.openjfx.javafxplugin`; `./gradlew run` launches the game.
- JUnit 5 + AssertJ.
- GitHub Actions: `./gradlew build` (compile + test) on push/PR, ubuntu, Temurin 21.
- Base package: `io.github.jitshen.brickbreaker`. Licence: MIT.

### 7.2 Package layout

```
io.github.jitshen.brickbreaker
├── BrickBreakerApp          # JavaFX entry point, screen switching
├── model/                   # pure Java game rules — must not import javafx.*
│   ├── Vec2, Collision      # vector record, swept circle-vs-rectangle collision
│   ├── Ball, Paddle, Brick, BrickType, PowerUp, PowerUpType
│   ├── Level, LevelLoader   # text-file levels
│   └── Game, GameState, InputState, GameEvent
└── ui/                      # JavaFX: loop, rendering, input, screens, particles
    ├── GameLoop             # AnimationTimer + fixed-step accumulator
    ├── GameView             # Canvas renderer + HUD
    ├── TitleScreen, PauseOverlay, EndScreen
    └── Particles
```

The `model` package never imports JavaFX, so it is unit-testable without a UI. A small unit test that
scans `model` sources for `javafx` imports guards this.

### 7.3 Game model behaviour

- **`Game`** owns paddle, balls, bricks, falling power-ups, lives, score, level and state; exposes
  `update(double dt, InputState input)` returning the `GameEvent`s from that step. The UI reads state
  to draw and reacts to events (particles, brick flash).
- **`GameState`**: `SERVING` (ball on paddle until launch), `PLAYING`, `PAUSED`, `LEVEL_CLEARED`,
  `GAME_OVER`, `WON`.
- Power-up drop randomness goes through an injected `RandomGenerator` so tests are deterministic.
- Fixed logical playfield (e.g. 800 × 600) scaled to the window.

### 7.4 Physics and collision

- Fixed timestep (120 Hz) with an accumulator; render at display rate.
- Swept (continuous) circle-vs-rectangle collision so a fast ball never skips through a brick.
- Paddle aiming: bounce angle depends on hit position (centre = straight up, edges ≈ 60°); enforce a
  minimum vertical speed so the ball never travels nearly horizontally forever.
- Ball speeds up slightly during a level, capped.

### 7.5 Gameplay content

- **Controls**: A/D or ←/→, or mouse; Space/click to launch; Esc/P to pause.
- **Lives**: 3.
- **Brick types**: 1-hit, 2-hit, 3-hit, unbreakable (not needed to clear a level). Damaged bricks
  change shade.
- **Levels**: 3–5 text files in `src/main/resources/levels/`, one char per cell (`.` empty, `1`–`3`
  hit points, `#` unbreakable). Malformed files throw a clear exception.
- **Power-ups** (random drop from destroyed bricks, caught by paddle): wide paddle (timed), multi-ball,
  slow ball (timed), extra life.
- **Score** per brick; high score saved locally (e.g. `~/.brick-breaker/highscore.properties`).

### 7.6 Presentation

- Own visual style: dark background, flat bright shapes on a JavaFX `Canvas`, palette in one place.
- Simple particle burst when a brick breaks; brief flash when a brick is hit.
- Screens: title (play / quit, high score), game with HUD (score, lives, level), pause overlay,
  game-over / win screen.
- Auto-pause when the window loses focus.

### 7.7 Tests (key ones, not exhaustive)

Collision maths incl. the high-speed tunnelling case; paddle bounce angles; brick damage/destroy;
level parsing (valid + malformed); a power-up effect and its expiry; life lost → game over; level
cleared → next level → win.

### 7.8 Milestones (builder's call)

1. **v0.1** — playable: paddle, ball, collision, bricks, 1 level, lives, score, pause; tests; CI.
2. **v1.0** — all levels and brick types, power-ups, high score, screens, particles, final README
   (screenshot/GIF, controls, how to run, short architecture note, Remembrance).

## 8. Left to the builder

- Exact numbers (speeds, sizes, drop rates, timers, scores) — tune for feel.
- Level designs and colours.
- Class decomposition inside the packages, as long as `model` stays free of JavaFX.
