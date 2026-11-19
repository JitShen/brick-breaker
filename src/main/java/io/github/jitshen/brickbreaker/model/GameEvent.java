package io.github.jitshen.brickbreaker.model;

/**
 * Something that happened during an update. The UI uses these for feedback (effects, flashes)
 * without the game rules knowing anything about the UI.
 */
public sealed interface GameEvent {

    record BallLaunched() implements GameEvent {}

    record PaddleHit() implements GameEvent {}

    record WallHit() implements GameEvent {}

    record BrickHit(Brick brick) implements GameEvent {}

    record BrickDestroyed(Brick brick) implements GameEvent {}

    record PowerUpSpawned(PowerUp powerUp) implements GameEvent {}

    record PowerUpCollected(PowerUpType type) implements GameEvent {}

    record LifeLost() implements GameEvent {}

    record LevelCleared(int levelIndex) implements GameEvent {}

    record GameOver() implements GameEvent {}

    record GameWon() implements GameEvent {}
}
