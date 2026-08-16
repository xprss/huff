package dev.huff.hexaquot.game;

import java.util.List;
import java.util.Map;

public final class HexahackDtos {
    private HexahackDtos() {}

    public enum Status { IN_PROGRESS, COMPLETED }
    public enum Rank { GHOST, SHADOW, BREACH, TRACED }
    public enum ProbeType { PING, BIT_SCAN, LINK_TRACE, CHECKSUM }
    public enum EventKind { PROBE, SUBMISSION, OVERRIDE }
    public enum ProbeComparison { BELOW, EQUAL, ABOVE }
    public enum Parity { EVEN, ODD }

    public record ProbeRequest(
        String requestId,
        ProbeType type,
        Integer position,
        Integer threshold,
        Integer otherPosition
    ) {}

    public record SubmissionRequest(String requestId, String code) {}
    public record OverrideRequest(String requestId, Integer position) {}

    public record ProbeResultDto(
        String requestId,
        ProbeType type,
        int cost,
        int position,
        Integer otherPosition,
        Integer threshold,
        ProbeComparison comparison,
        Parity parity,
        Integer sum,
        String summary
    ) {}

    public record SubmissionResultDto(
        String requestId,
        String code,
        int correctPositions,
        boolean granted
    ) {}

    public record OverrideResultDto(
        String requestId,
        int position,
        String digit,
        int cost
    ) {}

    public record EventDto(
        int sequence,
        EventKind kind,
        String occurredAt,
        ProbeResultDto probe,
        SubmissionResultDto submission,
        OverrideResultDto override
    ) {
        public String requestId() {
            if (probe != null) return probe.requestId();
            if (submission != null) return submission.requestId();
            return override == null ? null : override.requestId();
        }
    }

    public record FreeCluesDto(int totalSum, int distinctDigits) {}

    public record GameDto(
        String puzzleDate,
        int rulesVersion,
        Status status,
        int answerLength,
        List<EventDto> log,
        int totalCost,
        int wrongSubmissions,
        int overrideCount,
        int currentStealth,
        Rank projectedRank,
        Integer finalStealth,
        Rank rank,
        String solution,
        String completedAt
    ) {}

    public record TodayDto(
        String puzzleDate,
        int rulesVersion,
        int answerLength,
        FreeCluesDto freeClues,
        GameDto game
    ) {}

    public record ProbeActionDto(GameDto game, ProbeResultDto result, boolean replayed) {}
    public record SubmissionActionDto(GameDto game, SubmissionResultDto result, boolean replayed) {}
    public record OverrideActionDto(GameDto game, OverrideResultDto result, boolean replayed) {}

    public record CalendarNodeDto(
        String puzzleDate,
        boolean completed,
        Integer stealth,
        Rank rank,
        Integer totalCost,
        Integer wrongSubmissions,
        Boolean overrideUsed
    ) {}

    public record StatsDto(
        int completedAccesses,
        double averageStealth,
        int bestStealth,
        Map<Rank, Integer> rankDistribution,
        int currentStreak,
        int maxStreak,
        int noOverrideStreak,
        int maxNoOverrideStreak,
        List<CalendarNodeDto> last30Nodes
    ) {}
}
