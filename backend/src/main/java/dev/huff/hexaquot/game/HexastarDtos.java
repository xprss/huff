package dev.huff.hexaquot.game;

import java.util.List;

public final class HexastarDtos {
    private HexastarDtos() {}

    public enum Status { IN_PROGRESS, WON, LOST }

    public record GuessRequest(String requestId, List<String> syllables) {}

    public record LetterResultDto(String letter, TileState state, Integer solutionSyllableIndex) {}

    public record SyllableResultDto(String syllable, TileState state, List<LetterResultDto> letters) {}

    public record AttemptDto(
        int sequence,
        String requestId,
        List<String> syllables,
        List<SyllableResultDto> tiles,
        String occurredAt
    ) {}

    public record GameDto(
        String puzzleDate,
        int rulesVersion,
        Status status,
        int maxAttempts,
        List<Integer> syllableLengths,
        List<AttemptDto> attempts,
        List<String> solutionSyllables,
        String completedAt
    ) {}

    public record TodayDto(
        String puzzleDate,
        int rulesVersion,
        int maxAttempts,
        List<Integer> syllableLengths,
        GameDto game
    ) {}

    public record GuessActionDto(GameDto game, AttemptDto attempt, boolean replayed) {}
}
