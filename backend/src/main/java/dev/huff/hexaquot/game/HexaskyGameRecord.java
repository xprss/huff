package dev.huff.hexaquot.game;

import dev.huff.hexaquot.game.HexaskyDtos.Status;

public record HexaskyGameRecord(
    String id, String userId, String puzzleDate, int rulesVersion, String solutionJson, String proposalJson,
    String eventLogJson, int checksUsed, Status status, String createdAt, String updatedAt, String completedAt
) {}
