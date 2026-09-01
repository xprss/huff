package dev.huff.hexaquot.game;

import java.util.List;

public final class HexaflowDtos {
    private HexaflowDtos() {}
    public enum PuzzleStatus { DRAFT, PUBLISHED }
    public enum AnswerType { THEME, FLOW }
    public enum GameStatus { IN_PROGRESS, COMPLETED }
    public enum PathOutcome { THEME, FLOW, EXTRA, DUPLICATE }
    public enum EventKind { PATH }

    public record AnswerDto(String id, String label, AnswerType type, List<Integer> path) {}
    public record PuzzleDraftDto(String puzzleDate, String themeClue, List<String> grid, List<AnswerDto> answers) {}
    public record BoardGenerationRequest(List<String> themeWords, String flowWord) {}
    public record GeneratedBoardDto(List<String> grid, List<AnswerDto> answers) {}
    public record ValidationErrorDto(String code, String field, String message, Integer answerIndex, Integer cellIndex) {}
    public record PuzzleAdminDto(String id, String puzzleDate, PuzzleStatus status, String themeClue,
                                 List<String> grid, List<AnswerDto> answers, List<ValidationErrorDto> validationErrors,
                                 String createdBy, String updatedBy, String publishedBy,
                                 String createdAt, String updatedAt, String publishedAt, boolean immutable) {}
    public record PuzzleSummaryDto(String puzzleDate, PuzzleStatus status, String themeClue, int answerCount,
                                   int coveredCells, boolean valid, boolean immutable) {}
    public record PuzzleMonthDto(String month, List<PuzzleSummaryDto> puzzles) {}

    public record FoundAnswerDto(String id, String label, AnswerType type, List<Integer> cells) {}
    public record GameDto(String puzzleDate, GameStatus status, List<FoundAnswerDto> foundAnswers,
                          int extraCount, String completedAt) {}
    public record TodayDto(String puzzleDate, boolean available, String themeClue, List<String> grid,
                           int totalAnswers, GameDto game) {}
    public record PathRequest(String requestId, List<Integer> cells) {}
    public record PathResultDto(String requestId, PathOutcome outcome, FoundAnswerDto answer,
                                String sequence, int extraCount) {}
    public record EventDto(int sequence, EventKind kind, String requestId, String occurredAt,
                           PathResultDto path) {}
    public record PathActionDto(GameDto game, PathResultDto result, boolean replayed) {}
    public record StatsDto(int started, int completed, int currentStreak, int maxStreak) {}
}
