package dev.huff.hexaquot.game;

import dev.huff.hexaquot.game.HexahackDtos.Rank;
import dev.huff.hexaquot.game.HexahackDtos.Status;

public record HexahackGameRecord(
    String id,
    String userId,
    String puzzleDate,
    int rulesVersion,
    String solution,
    String eventLogJson,
    int totalCost,
    int wrongSubmissions,
    Status status,
    Integer stealth,
    Rank rank,
    String createdAt,
    String updatedAt,
    String completedAt
) {}
