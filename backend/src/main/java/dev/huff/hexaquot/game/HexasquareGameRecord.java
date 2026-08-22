package dev.huff.hexaquot.game;

public record HexasquareGameRecord(
    String id, String userId, String puzzleDate, int rulesVersion, String puzzleJson, String placementsJson,
    String canonicalPathsJson, HexasquareDtos.Status status, int simulationsCount, Integer usedCells,
    Integer remainingCells, String createdAt, String updatedAt, String completedAt
) {}
