package dev.huff.hexaquot.game;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/** Public contract of Hexatrap v2: formulas, the solution and unrevealed clues never leave the server. */
public final class HexatrapDtos {
    private HexatrapDtos() {}

    /** Scope strict input handling to this game's contract; never silently truncate a number. */
    public static class StrictInteger extends JsonDeserializer<Integer> {
        @Override public Integer deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            if (parser.currentToken() != JsonToken.VALUE_NUMBER_INT)
                return context.reportInputMismatch(Integer.class, "È richiesto un numero intero.");
            return parser.getIntValue();
        }
    }

    public record QuestionRequest(String requestId, String puzzleDate,
                                  @JsonDeserialize(using = StrictInteger.class) int rulesVersion, String questionId) {}
    public record ClueRequest(String requestId, String puzzleDate,
                              @JsonDeserialize(using = StrictInteger.class) int rulesVersion, String clueId) {}
    public record VerdictRequest(String requestId, String puzzleDate,
                                 @JsonDeserialize(using = StrictInteger.class) int rulesVersion,
                                 String liar, Map<String, String> facts) {}

    public record CharacterDto(String id, String name, String role, String icon) {}
    public record OptionDto(String id, String label) {}
    public record FactDto(String id, String label, List<OptionDto> options, boolean verdict) {}
    /** What an item implies about the facts if A lies, and if B lies (the hypothesis lens). */
    public record ReadingsDto(String ifALies, String ifBLies) {}
    public record CertaintyDto(String id, String text, String record) {}
    public record StatementDto(String id, String speaker, String text, String record, ReadingsDto readings) {}
    public record QuestionDto(String id, String to, String text, String record) {}
    public record ClueCardDto(String id, String title, int cost) {}
    public record CaseDto(String title, String format, int difficulty, String intro, int budget, int questionCost,
                          List<CharacterDto> characters, List<FactDto> facts, List<CertaintyDto> certainties,
                          List<StatementDto> statements, List<QuestionDto> questions, List<ClueCardDto> clues) {}

    public record AskedDto(int sequence, String questionId, String to, boolean answer, ReadingsDto readings) {}
    public record RevealedDto(int sequence, String clueId, String title, String text, String record) {}
    public record VerdictDto(String liar, Map<String, String> facts) {}
    public record EvidenceDto(String kind, String id, String label) {}
    public record RevealDto(String liar, Map<String, String> facts, String epilogue, boolean liarCorrect, boolean factsCorrect,
                            boolean proofFound, List<EvidenceDto> proof, List<EvidenceDto> optimalPath, int optimalMinutes,
                            int expertMinutes, String share) {}
    public record GameDto(String puzzleDate, int rulesVersion, String status, String outcome, int minutesUsed, int minutesLeft,
                          int sequence, List<AskedDto> asked, List<RevealedDto> revealed, VerdictDto verdict, long points,
                          RevealDto reveal, String completedAt) {}
    public record TodayDto(String puzzleDate, int rulesVersion, boolean available, CaseDto caseFile, GameDto game) {}
    public record ActionDto(GameDto game, boolean replayed) {}

    /** Averages and best score refer to solved cases; average minutes to every finished case. */
    public record StatsDto(int started, int won, int lost, int partial, int currentStreak, int maxStreak,
                           double averagePoints, Long bestPoints, double averageMinutes) {}

    public record AttemptDto(String runId, int attempt, String phase, boolean ok, JsonNode issues, JsonNode metrics,
                             long durationMs, String createdAt) {}
    public record CaseSummaryDto(String puzzleDate, String status, int difficulty, String format, String title, int attempts,
                                 String model, String generatedAt, String lastError, long games, JsonNode metrics,
                                 JsonNode warnings, boolean regenerable) {}
    public record CaseDetailDto(CaseSummaryDto summary, JsonNode caseFile, Map<String, String> records, List<AttemptDto> attempts) {}
    public record GenerationQueuedDto(String puzzleDate, boolean queued) {}
}
