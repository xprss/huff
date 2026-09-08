package dev.huff.hexaquot.game;

import dev.huff.hexaquot.game.HexastarDtos.Status;

public record HexastarGameRecord(
    String id,
    String userId,
    String puzzleDate,
    int rulesVersion,
    String solution,
    String solutionSyllablesJson,
    String attemptsJson,
    Status status,
    String createdAt,
    String updatedAt,
    String completedAt
) {}
