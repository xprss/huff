package dev.huff.wordle.game;

public record GameRecord(
    String id,
    String userId,
    String puzzleDate,
    String solution,
    String guessesJson,
    GameStatus status,
    String createdAt,
    String updatedAt,
    String completedAt
) {
}
