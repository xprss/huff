package dev.huff.hexaquot.game;

public record HexadigitGameRecord(
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
