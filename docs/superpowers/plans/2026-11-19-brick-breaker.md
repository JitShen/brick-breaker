# Brick Breaker Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** A from-scratch JavaFX brick-breaker game with tested game rules, run with `./gradlew run`.

**Architecture:** Single Gradle project. `model` package holds all game rules as plain Java (no JavaFX),
driven by `Game.update(dt, input)` which returns `GameEvent`s. `ui` package runs a fixed-step loop,
draws on a `Canvas`, maps keyboard/mouse to `InputState`, and reacts to events with particles/flashes.
`score` package persists the high score to a file.

**Tech Stack:** Java 21 (Temurin), Gradle 8.14 wrapper (Kotlin DSL, version catalog), JavaFX 21.0.5
via `org.openjfx.javafxplugin` 0.1.0, JUnit 5.11, AssertJ 3.26, GitHub Actions.

**Spec:** `docs/superpowers/specs/2026-11-19-brick-breaker-design.md`

## Global Constraints

- Base package `io.github.jitshen.brickbreaker`; licence MIT (copyright holder "JitShen").
- `io.github.jitshen.brickbreaker.model` must never import `javafx.*` (guarded by a test, Task 5).
- No code, names or structure from the old coursework; no mention of what it was based on. README
  Remembrance section stays exactly as currently written.
- No trademarked game names anywhere (code, README, window title).
- Commits: conventional-commit messages, **no** `Co-Authored-By` trailer. Commit locally only; never push.
- Logical playfield 800 × 600; all model units are playfield pixels and seconds.
- `./gradlew build` must pass on a fresh clone (compile + all tests).

## Review Focus

1. **Huge frame time** (window dragged / app stalled): loop clamps frame time to 0.25 s and the swept
   collision still never lets the ball skip a brick or leave the playfield → test in Task 5.
2. **Ball hits the seam between two adjacent bricks**: exactly one brick is damaged per contact and the
   ball ends outside both bricks → test in Task 5.
3. **Paddle moves into the ball from the side / ball below paddle top**: the ball must not stick to or
   jitter inside the paddle; paddle bounce only when the ball is moving down and its centre is above
   the paddle top → test in Task 5.
4. **Bad level files** (ragged rows, unknown char, no breakable bricks, empty): loading fails with
   `LevelFormatException` naming the level and problem → tests in Task 4.
5. **Missing, corrupt or unwritable high-score file**: `load()` returns 0, `save()` never throws → tests in
   Task 7.

---

### Task 1: Toolchain, project scaffold, CI

**Files:**
- Create: `settings.gradle.kts`, `build.gradle.kts`, `gradle/libs.versions.toml`, `gradlew`,
  `gradlew.bat`, `gradle/wrapper/*`, `.gitignore`, `.gitattributes`, `LICENSE`,
  `.github/workflows/build.yml`,
  `src/main/java/io/github/jitshen/brickbreaker/BrickBreakerApp.java` (placeholder window),
  `src/test/java/io/github/jitshen/brickbreaker/SmokeTest.java`

- [ ] **Step 1: Install JDK 21.** Try `winget install EclipseAdoptium.Temurin.21.JDK` (PowerShell). If
  winget is unavailable or needs admin, download the Temurin 21 Windows x64 JDK zip into
  `%USERPROFILE%\.jdks\` and use that path as `JAVA_HOME` for every command. Verify: `java -version`
  prints `21`.
- [ ] **Step 2: Generate the wrapper.** Download Gradle 8.14 binary zip to the scratchpad, run
  `gradle wrapper --gradle-version 8.14 --distribution-type bin` in the repo. Delete nothing else.
- [ ] **Step 3: Build files.** `settings.gradle.kts`: `rootProject.name = "brick-breaker"`.
  `build.gradle.kts`: plugins `application` + `org.openjfx.javafxplugin`; Java toolchain 21;
  `javafx { version = "21.0.5"; modules("javafx.controls") }`;
  `application.mainClass = "io.github.jitshen.brickbreaker.BrickBreakerApp"`; JUnit Platform;
  test deps junit-bom/jupiter + assertj from the catalog (plus `junit-platform-launcher` runtimeOnly).
  `.gitignore`: `.gradle/`, `build/`, `.idea/`, `*.iml`, `out/`, `.vscode/`. `.gitattributes`:
  `gradlew text eol=lf`, `*.bat text eol=crlf`. `LICENSE`: MIT, "Copyright (c) 2026 JitShen".
- [ ] **Step 4: Placeholder app + smoke test.** `BrickBreakerApp extends Application` opening an
  800 × 600 window titled `Brick Breaker`. `SmokeTest.passes()` asserts `1 + 1 == 2` (proves the test
  pipeline; deleted in Task 2).
- [ ] **Step 5: CI.** `.github/workflows/build.yml`: on `push` and `pull_request`; `ubuntu-latest`;
  `actions/checkout@v4`, `actions/setup-java@v4` (temurin, 21), `gradle/actions/setup-gradle@v4`,
  run `./gradlew build`.
- [ ] **Step 6: Verify.** `./gradlew build` → `BUILD SUCCESSFUL`; `./gradlew run` opens the window
  (close it manually / kill after a few seconds).
- [ ] **Step 7: Commit** (include README.md and `docs/`): `chore: scaffold Gradle JavaFX project with CI`.

### Task 2: Vector maths and swept collision

**Files:**
- Create: `src/main/java/io/github/jitshen/brickbreaker/model/Vec2.java`, `Rect.java`, `Collision.java`
- Test: `src/test/java/io/github/jitshen/brickbreaker/model/CollisionTest.java`, `Vec2Test.java`
- Delete: `SmokeTest.java`

**Interfaces — Produces:**
- `record Vec2(double x, double y)`: `plus(Vec2)`, `minus(Vec2)`, `scale(double)`, `dot(Vec2)`,
  `length()`, `normalized()`, `reflect(Vec2 normal)` (= `this − 2(this·n)n`), `rotate(double radians)`,
  `static final Vec2 ZERO`.
- `record Rect(double x, double y, double width, double height)`: `minX() maxX() minY() maxY()`,
  `centerX() centerY()`, `expand(double r)` (grow by r on every side), `intersects(Rect)`.
- `record Hit(double time, Vec2 normal)` (nested in `Collision` or top-level; `time` in [0, 1]).
- `Collision.sweep(Vec2 start, Vec2 delta, double radius, Rect rect) → Optional<Hit>`: circle centre
  moving from `start` by `delta` against `rect` expanded by `radius` (slab ray-vs-AABB). Returns the
  entry time and the axis normal of the entry face (−x, +x, −y or +y). Returns empty if no entry in
  [0, 1], if `delta` is zero, or if `start` is already inside the expanded rect.

- [ ] **Step 1: Failing tests** (`CollisionTest`):
  - `hitsLeftFace`: start (0, 50), delta (100, 0), r 5, rect (50, 40, 20, 20) → time `0.45`
    (±1e-9), normal `(-1, 0)`.
  - `hitsTopFace`: start (60, 0), delta (0, 100), r 5, rect (50, 40, 20, 20) → time `0.35`, normal `(0, -1)`.
  - `missesWhenPassingBeside`: start (0, 0), delta (100, 0), r 5, rect (50, 40, 20, 20) → empty.
  - `noHitBeyondDelta`: start (0, 50), delta (10, 0), same rect → empty.
  - `fastBallDoesNotTunnel`: start (0, 50), delta (10_000, 0), r 5, rect (500, 45, 4, 10) → present,
    normal `(-1, 0)`.
  - `startingInsideIsIgnored`: start (60, 50), delta (5, 0), rect (50, 40, 20, 20) → empty.
  - `Vec2Test.reflectFlipsNormalComponent`: `new Vec2(3, 4).reflect(new Vec2(0, -1))` = `(3, -4)`.
- [ ] **Step 2:** `./gradlew test` → compile failure / FAIL.
- [ ] **Step 3: Implement** the records and `Collision.sweep` (slab method; on ties the x-axis face wins).
- [ ] **Step 4:** `./gradlew test` → PASS.
- [ ] **Step 5: Commit** `feat(model): add vector maths and swept circle-vs-rect collision`.

### Task 3: Paddle, ball, bricks

**Files:**
- Create: `model/GameConfig.java`, `model/Paddle.java`, `model/Ball.java`, `model/BrickType.java`,
  `model/Brick.java`
- Test: `model/PaddleTest.java`, `model/BrickTest.java`

**Interfaces — Consumes:** `Vec2`, `Rect` (Task 2).
**Produces:**
- `final class GameConfig` (constants only): `WIDTH = 800`, `HEIGHT = 600`, `BALL_RADIUS = 8`,
  `BALL_START_SPEED = 360`, `BALL_MAX_SPEED = 620`, `BALL_SPEED_UP = 8` (per paddle hit),
  `MIN_VERTICAL_RATIO = 0.3`, `PADDLE_WIDTH = 100`, `PADDLE_WIDE_WIDTH = 150`, `PADDLE_HEIGHT = 14`,
  `PADDLE_Y = 560`, `PADDLE_SPEED = 600`, `MAX_BOUNCE_ANGLE = Math.toRadians(60)`, `START_LIVES = 3`,
  `MAX_LIVES = 5`, `BRICK_TOP = 60`, `BRICK_HEIGHT = 24`, `BRICK_GAP = 4`, `BRICK_SIDE_MARGIN = 20`.
- `class Paddle`: `Paddle(double centerX)`; `centerX()`, `width()`, `setWidth(double)`, `bounds() → Rect`
  (top at `PADDLE_Y`); `moveBy(double dx)` and `moveTo(double centerX)` clamped to `[width/2, WIDTH − width/2]`;
  `bounceDirection(double hitX) → Vec2` unit vector: offset = clamp((hitX − centerX)/(width/2), −1, 1),
  angle = offset × `MAX_BOUNCE_ANGLE`, result `(sin angle, −cos angle)`.
- `class Ball`: `Ball(Vec2 position, Vec2 velocity)`; mutable `position()/setPosition`,
  `velocity()/setVelocity`, `radius()` (= `BALL_RADIUS`), `speed()`.
- `enum BrickType(char symbol, int hitPoints, int score)`: `ONE('1',1,10)`, `TWO('2',2,20)`,
  `THREE('3',3,40)`, `UNBREAKABLE('#',0,0)`; `isBreakable()`; `static Optional<BrickType> fromSymbol(char)`.
- `class Brick`: `Brick(Rect bounds, BrickType type)`; `bounds()`, `type()`, `hitsLeft()`,
  `isDestroyed()`, `hit() → boolean` (true if this hit destroyed it; unbreakable never changes).

- [ ] **Step 1: Failing tests:**
  - `PaddleTest.centreHitGoesStraightUp`: paddle at 400, `bounceDirection(400)` ≈ `(0, -1)`.
  - `PaddleTest.rightEdgeHitAngles60Degrees`: `bounceDirection(450)` ≈ `(sin 60°, −cos 60°)`; hits beyond
    the edge (e.g. 500) give the same result.
  - `PaddleTest.moveIsClampedToPlayfield`: `moveBy(-10_000)` → `centerX() == 50`; `moveBy(10_000)` → `750`.
  - `BrickTest.twoHitBrickDestroyedOnSecondHit`: first `hit()` false, `hitsLeft()==1`; second true,
    `isDestroyed()`.
  - `BrickTest.unbreakableNeverDestroyed`: 10 hits → all false, not destroyed.
- [ ] **Step 2:** `./gradlew test` → FAIL.
- [ ] **Step 3: Implement** the classes above.
- [ ] **Step 4:** `./gradlew test` → PASS.
- [ ] **Step 5: Commit** `feat(model): add paddle, ball and brick types`.

### Task 4: Levels from text files

**Files:**
- Create: `model/Level.java`, `model/LevelLoader.java`, `model/LevelFormatException.java`,
  `src/main/resources/levels/levels.txt` (one level file name per line, in play order),
  `src/main/resources/levels/01-warm-up.txt`
- Test: `model/LevelLoaderTest.java`

**Interfaces — Consumes:** `Brick`, `BrickType`, `Rect`, `GameConfig`.
**Produces:**
- `record Level(String name, List<String> rows)`: `createBricks() → List<Brick>` — columns =
  row length; brick width = `(WIDTH − 2·BRICK_SIDE_MARGIN − (cols−1)·BRICK_GAP) / cols`; row y =
  `BRICK_TOP + row·(BRICK_HEIGHT + BRICK_GAP)`; `.` cells produce nothing.
- `LevelLoader.parse(String name, List<String> lines) → Level`: ignores blank lines and lines starting
  with `;` (comments); trims trailing whitespace.
- `LevelLoader.loadAll() → List<Level>`: reads `/levels/levels.txt` from the classpath, then each file.
- `LevelFormatException extends RuntimeException`, message contains the level name.

Level file example (`01-warm-up.txt`, 10 columns, 5 rows): rows of `1`, a row of `2`, with `.` gaps.

- [ ] **Step 1: Failing tests:**
  - `parsesGridIntoBricks`: lines `["11.1", "#22."]` → 6 bricks; first brick bounds x = 20, y = 60;
    the `#` brick has type `UNBREAKABLE`.
  - `ignoresCommentsAndBlankLines`: `["; title", "", "11"]` → 2 bricks.
  - `rejectsRaggedRows`: `["111", "11"]` → `LevelFormatException` with message containing the name and "row".
  - `rejectsUnknownSymbol`: `["1x1"]` → exception mentioning `'x'`.
  - `rejectsLevelWithoutBreakableBricks`: `["##", ".."]` → exception.
  - `rejectsEmptyLevel`: `[]` → exception.
  - `bundledLevelsLoad`: `loadAll()` is non-empty and every level's `createBricks()` is non-empty.
- [ ] **Step 2:** `./gradlew test` → FAIL.
- [ ] **Step 3: Implement** loader and the first level file.
- [ ] **Step 4:** `./gradlew test` → PASS.
- [ ] **Step 5: Commit** `feat(model): load levels from text files`.

### Task 5: Game rules (no power-ups yet)

**Files:**
- Create: `model/Game.java`, `model/GameState.java`, `model/InputState.java`, `model/GameEvent.java`
- Test: `model/GameTest.java`, `model/ModelIsUiFreeTest.java`

**Interfaces — Consumes:** Tasks 2–4.
**Produces:**
- `enum GameState { SERVING, PLAYING, PAUSED, LEVEL_CLEARED, GAME_OVER, WON }`
- `record InputState(boolean left, boolean right, OptionalDouble pointerX, boolean launch)` with
  `static final InputState NONE`. `launch` = pressed since last update (edge, set by UI).
- `sealed interface GameEvent` with records: `BallLaunched()`, `PaddleHit()`, `WallHit()`,
  `BrickHit(Brick brick)`, `BrickDestroyed(Brick brick)`, `LifeLost()`, `LevelCleared(int levelIndex)`,
  `GameOver()`, `GameWon()`. (Task 6 adds `PowerUpSpawned`, `PowerUpCollected`.)
- `class Game`: `Game(List<Level> levels, RandomGenerator random)`;
  `List<GameEvent> update(double dt, InputState input)`; `togglePause()` (PLAYING/SERVING ↔ PAUSED,
  remembering the previous state); `restart()` (level 0, `START_LIVES`, score 0, SERVING);
  getters `state()`, `paddle()`, `balls()`, `bricks()`, `lives()`, `score()`, `levelIndex()`, `levelCount()`
  (collections returned unmodifiable).

Rules:
- **SERVING:** paddle moves; one ball sits on the paddle (centre x = paddle centre, y = paddle top − radius).
  `launch` → velocity `bounceDirection(paddle.centerX() + paddle.width() * 0.1) × BALL_START_SPEED`,
  state PLAYING, `BallLaunched`.
- **Paddle input:** `pointerX` present → `moveTo(pointerX)`; else left/right → `moveBy(∓PADDLE_SPEED·dt)`.
- **PLAYING, per ball:** sweep from the current position by `velocity·dt` against walls, paddle and
  live bricks; take the earliest hit, move to it (back off 1e-6 along the normal), react, and repeat
  with the remaining fraction, at most 4 times per step. Walls are rects just outside the left, right
  and top edges (none at the bottom).
  - Wall: reflect; `WallHit`.
  - Brick: reflect; `BrickHit`; if `hit()` destroyed it, add score and `BrickDestroyed`.
  - Paddle: only if `velocity.y > 0` and ball centre y < paddle top; new speed =
    min(speed + `BALL_SPEED_UP`, `BALL_MAX_SPEED`), direction `bounceDirection(hit x)`; `PaddleHit`.
  - After any reflect: if |vy| < `MIN_VERTICAL_RATIO`·speed, raise |vy| to that (keep sign, keep speed).
  - Ball centre y > `HEIGHT + radius` → remove the ball.
- **No balls left:** lives −1, `LifeLost`; lives 0 → GAME_OVER + `GameOver`; else new ball, SERVING.
- **All breakable bricks destroyed:** last level → WON + `GameWon`; else LEVEL_CLEARED + `LevelCleared(i)`.
- **LEVEL_CLEARED + launch:** load next level's bricks, reset paddle and ball, SERVING.
- **PAUSED / GAME_OVER / WON:** `update` returns no events and changes nothing.
- `dt` is clamped to ≤ 0.25 inside `update`.

- [ ] **Step 1: Failing tests** (`GameTest`; build levels with `LevelLoader.parse`; place balls with
  `game.balls().get(0).setPosition/setVelocity` after launching):
  - `servingBallFollowsPaddleUntilLaunch`: update with `right` → ball x == paddle x, state SERVING;
    then `launch` → PLAYING, ball `vy < 0`, events contain `BallLaunched`.
  - `ballBouncesOffSideWall`: ball near right wall moving right → after update `vx < 0`, `WallHit`.
  - `destroyingLastBrickClearsLevel` / `clearingLastLevelWins`: two 1-level vs 2-level setups.
  - `losingAllLivesIsGameOver`: drop the ball out 3 times → `GAME_OVER`, `GameOver` event once.
  - `pausedGameDoesNotMove`: `togglePause()`; update → ball position unchanged.
  - **Review Focus 1** `hugeFrameNeverSkipsBrick`: thin one-row level, ball at max speed aimed at it,
    `update(5.0, NONE)` → brick hit, ball inside the playfield.
  - **Review Focus 2** `seamHitDamagesOneBrick`: ball moving straight up at the seam x between two
    adjacent bricks → exactly one `BrickHit`, ball outside both bricks afterwards.
  - **Review Focus 3** `ballBelowPaddleTopIsNotBounced`: ball beside the paddle, centre below its top,
    moving down; paddle moved into it → no `PaddleHit`, ball keeps falling.
  - `ModelIsUiFreeTest.modelDoesNotImportJavaFx`: walk `src/main/java/io/github/jitshen/brickbreaker/model`
    and assert no file contains `import javafx`.
- [ ] **Step 2:** `./gradlew test` → FAIL.
- [ ] **Step 3: Implement** `Game` per the rules above.
- [ ] **Step 4:** `./gradlew test` → PASS.
- [ ] **Step 5: Commit** `feat(model): add core game rules with continuous collision`.

### Task 6: Playable UI (milestone v0.1)

**Files:**
- Create: `ui/Palette.java`, `ui/GameLoop.java`, `ui/InputHandler.java`, `ui/GameView.java`,
  `ui/GameScreen.java`
- Modify: `BrickBreakerApp.java`

**Interfaces — Consumes:** `Game`, `InputState`, `GameEvent`, `GameState`, `LevelLoader`.
**Produces:**
- `GameLoop(Consumer<Double> step, Runnable render)` wrapping `AnimationTimer`; fixed step `1/120 s`,
  accumulator, frame time clamped to 0.25 s; `start()`, `stop()`.
- `InputHandler`: attaches to a `Scene`; tracks A/D/←/→, mouse x (in playfield coords), Space/click as
  launch edge, Esc/P as pause edge; `InputState poll()` clears edges; `boolean pollPause()`.
- `GameView(Canvas)`: `render(Game)` — scales the 800 × 600 playfield to the canvas with letterboxing;
  draws bricks (shade by `hitsLeft`), paddle, balls, HUD (score, lives, level n/m), and state text:
  "Press Space to launch", "Paused", "Level cleared — press Space", "Game over", "You win!".
- `GameScreen`: `StackPane` with a resizable `Canvas`; owns `Game`, `GameLoop`, `InputHandler`,
  `GameView`; auto-pauses when the stage loses focus; R restarts after GAME_OVER/WON.
- `Palette`: all colours (dark background `#10131a`, bright flat brick colours per type).

- [ ] **Step 1: Implement** the classes; `BrickBreakerApp` shows `GameScreen` (title screen comes in Task 8).
- [ ] **Step 2: Verify** `./gradlew build` passes; `./gradlew run`: paddle moves with keys and mouse,
  ball launches, bounces, breaks bricks, pause works, game over/level cleared appear. Take a screenshot.
- [ ] **Step 3: Commit** `feat(ui): add playable JavaFX game screen`, then `git tag v0.1`.

### Task 7: Power-ups and high score

**Files:**
- Create: `model/PowerUpType.java`, `model/PowerUp.java`,
  `score/HighScoreStore.java`
- Modify: `model/Game.java`, `model/GameEvent.java`, `model/GameConfig.java`, `ui/GameView.java`,
  `ui/GameScreen.java`
- Test: `model/PowerUpTest.java`, `score/HighScoreStoreTest.java`

**Interfaces — Produces:**
- `enum PowerUpType { WIDE_PADDLE, MULTI_BALL, SLOW_BALL, EXTRA_LIFE }`
- `class PowerUp`: `PowerUp(PowerUpType, Vec2 position)`; `type()`, `bounds() → Rect` (24 × 12),
  falls at `POWER_UP_FALL_SPEED = 150`.
- `GameConfig` additions: `POWER_UP_DROP_CHANCE = 0.15`, `WIDE_DURATION = 10`, `SLOW_DURATION = 8`,
  `SLOW_FACTOR = 0.7`, `MULTI_BALL_SPREAD = Math.toRadians(20)`.
- `GameEvent` additions: `PowerUpSpawned(PowerUp)`, `PowerUpCollected(PowerUpType)`.
- `Game.powerUps() → List<PowerUp>`; `Game.activeTimers()` not needed — UI reads `paddle().width()`.
- `HighScoreStore(Path file)`: `int load()` (0 if missing/unreadable/corrupt), `void save(int score)`
  (creates parent dirs; swallows `IOException` and logs via `System.Logger`).
  Default path: `Path.of(System.getProperty("user.home"), ".brick-breaker", "highscore.properties")`.

Rules: on `BrickDestroyed`, if `random.nextDouble() < POWER_UP_DROP_CHANCE` spawn a random type at the
brick centre. Power-ups fall; overlapping the paddle → apply + `PowerUpCollected`; below the screen →
removed. WIDE: paddle width `PADDLE_WIDE_WIDTH` for 10 s (re-collecting resets the timer). MULTI_BALL:
two extra balls copying the first ball's position with velocity rotated ±20°. SLOW: every ball's
speed × 0.7 for 8 s, restored (to current speed / 0.7) on expiry. EXTRA_LIFE: lives + 1, max 5.
All timers and falling power-ups clear on life lost and level change.

- [ ] **Step 1: Failing tests:** use a `RandomGenerator` stub that always returns 0.0 (always drops, first type)
  or a fixed sequence.
  - `destroyedBrickCanDropPowerUp`, `catchingWidePaddleWidensThenExpires` (update past 10 s → width 100),
    `multiBallAddsTwoBalls`, `extraLifeCapsAtFive`, `slowBallRestoresSpeed`,
    `losingLifeClearsPowerUps`.
  - `HighScoreStoreTest` (`@TempDir`): `missingFileIsZero`, `savedScoreRoundTrips`,
    **Review Focus 5** `corruptFileIsZero` (file content `"banana"`), `saveToUnwritablePathDoesNotThrow`
    (path whose parent is an existing regular file).
- [ ] **Step 2:** `./gradlew test` → FAIL.
- [ ] **Step 3: Implement**; UI draws falling power-ups (letter labels W/M/S/+), shows high score in the
  HUD, saves it on GAME_OVER/WON when beaten.
- [ ] **Step 4:** `./gradlew build` → PASS; `./gradlew run` quick manual check.
- [ ] **Step 5: Commit** `feat: add power-ups and persistent high score`.

### Task 8: Screens, effects, remaining levels (milestone v1.0)

**Files:**
- Create: `ui/TitleScreen.java`, `ui/Particles.java`,
  `src/main/resources/levels/02-…txt` to `05-…txt`
- Modify: `BrickBreakerApp.java`, `ui/GameScreen.java`, `ui/GameView.java`, `levels/levels.txt`

**Interfaces — Produces:**
- `TitleScreen(int highScore, Runnable onPlay, Runnable onQuit)`: title, Play / Quit buttons, high score.
- `Particles`: `burst(double x, double y, Color color)` (~16 particles, random velocity, gravity,
  0.6 s life), `update(double dt)`, `render(GraphicsContext)`.
- `BrickBreakerApp`: title → game; Esc on game-over/win screen or "Quit" returns to title.
- `GameView`: brick flash for 0.08 s on `BrickHit`; particle burst on `BrickDestroyed`; end overlay
  shows final score and "New high score!" when beaten.

- [ ] **Step 1: Implement**; write levels 02–05 increasing in difficulty (more 2/3-hit bricks, some `#`).
- [ ] **Step 2: Verify** `./gradlew build` passes (`bundledLevelsLoad` covers the new files);
  `./gradlew run` full play-through of the screens; take a screenshot for the README to
  `docs/screenshot.png`.
- [ ] **Step 3: Commit** `feat(ui): add title screen, particles and levels 2-5`.

### Task 9: Final README

**Files:** Modify `README.md` (keep the Remembrance section verbatim, remove the WIP note).

- [ ] **Step 1:** Write: title + one-line pitch, CI badge
  (`https://github.com/JitShen/brick-breaker/actions/workflows/build.yml/badge.svg`), screenshot,
  features, controls table, requirements (JDK 21) and `./gradlew run` / `./gradlew test`, short
  "How it's built" (model vs ui, fixed-step loop, swept collision, level file format), Remembrance,
  licence MIT.
- [ ] **Step 2: Verify** `./gradlew build` passes; no trademarked names (`grep -ri "breakout\|arkanoid"`
  returns nothing).
- [ ] **Step 3: Commit** `docs: write README`, then `git tag v1.0`.
