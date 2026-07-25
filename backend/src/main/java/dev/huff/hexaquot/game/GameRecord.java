package dev.huff.hexaquot.game;

public record GameRecord(
    String id,
    String userId,
    String puzzleDate,
    GameMode mode,
    String solution,
    String guessesJson,
    GameStatus status,
    Integer mouseTileIndex,
    boolean mouseRevealed,
    boolean kittenUnlocked,
    String kittenUsedAt,
    String createdAt,
    String updatedAt,
    String completedAt
) {
}
