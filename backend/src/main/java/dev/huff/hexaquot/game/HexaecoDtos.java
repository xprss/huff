package dev.huff.hexaquot.game;

import java.util.List;

public final class HexaecoDtos {
    private HexaecoDtos() {}

    public enum Command { UP, RIGHT, DOWN, LEFT, WAIT }
    public record BoardDto(List<Integer> walls, int lightStart, int echoStart, int lightGoal, int echoGoal) {}
    public record GameDto(String puzzleDate, int rulesVersion, String status, List<Command> moves, String completedAt) {}
    public record TodayDto(String puzzleDate, int rulesVersion, BoardDto board, GameDto game) {}
    public record SubmitRequest(String requestId, String puzzleDate, int rulesVersion, List<Command> moves) {}
    public record SubmitActionDto(GameDto game, boolean replayed) {}
    public record StatsDto(int completed, int currentStreak, int maxStreak) {}
}
