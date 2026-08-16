package dev.huff.hexaquot.game;

import java.util.List;
import java.util.Map;

public final class HexaskyDtos {
    private HexaskyDtos() {}

    public enum Status { IN_PROGRESS, WON, LOST }
    public enum EventKind { CHECK }

    public record VisibilityDto(List<Integer> top, List<Integer> right, List<Integer> bottom, List<Integer> left) {}
    public record CheckRequest(String requestId, List<Integer> solution) {}
    public record CheckResultDto(String requestId, boolean correct, int checksUsed, Status status, List<Integer> solution) {}
    public record EventDto(int sequence, EventKind kind, String occurredAt, CheckResultDto check) {
        public String requestId() { return check == null ? null : check.requestId(); }
    }
    public record GameDto(String puzzleDate, int rulesVersion, Status status, int checksUsed, List<Integer> proposal,
                          List<EventDto> log, List<Integer> solution, String completedAt) {}
    public record TodayDto(String puzzleDate, int rulesVersion, VisibilityDto visibility, GameDto game) {}
    public record CheckActionDto(GameDto game, CheckResultDto result, boolean replayed) {}
    public record StatsDto(int played, int won, int lost, int currentStreak, int maxStreak,
                           Map<Integer, Integer> checkDistribution) {}
}
