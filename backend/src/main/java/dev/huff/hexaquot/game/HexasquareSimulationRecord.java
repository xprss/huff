package dev.huff.hexaquot.game;

public record HexasquareSimulationRecord(
    String id, String gameId, String requestId, String placementsJson, String outcomeJson,
    boolean successful, String createdAt
) {}
