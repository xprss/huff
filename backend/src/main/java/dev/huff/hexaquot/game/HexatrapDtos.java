package dev.huff.hexaquot.game;

import java.util.List;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import java.io.IOException;

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
    public enum QuestionType { SEAL, NUMBER }
    public enum Relation { SAME, IMPLIES, AT_LEAST_ONE }
    public record Dossier(int litCount, Relation relation, int firstSeal, int secondSeal) {}
    public record Puzzle(Dossier dossier, int configuration, int loyalAdvisor, int secretNumber, int falseNumber) {}
    public record QuestionRequest(String requestId, String puzzleDate, @JsonDeserialize(using=StrictInteger.class) int rulesVersion,
                                  QuestionType type, @JsonDeserialize(using=StrictInteger.class) Integer advisor, @JsonDeserialize(using=StrictInteger.class) Integer seal) {}
    public record SubmissionRequest(String requestId, String puzzleDate, @JsonDeserialize(using=StrictInteger.class) int rulesVersion, @JsonDeserialize(using=StrictInteger.class) Integer number) {}
    public record EventDto(long sequence, String type, Integer advisor, Integer seal, Boolean answer,
                           Integer number, Boolean correct, String createdAt) {}
    public record EventsDto(List<EventDto> events, Long nextBefore) {}
    public record Reveal(int number, int loyalAdvisor, int configuration, String explanation) {}
    public record GameDto(String puzzleDate, int rulesVersion, String status, long questions, long wrongSubmissions,
                          long points, boolean numberUnlocked, boolean numberRequested, EventsDto history, Reveal reveal) {}
    public record TodayDto(String puzzleDate, int rulesVersion, Dossier dossier, GameDto game) {}
    public record ActionDto(GameDto game, EventDto event, boolean replayed) {}
    /** Averages and best score refer to wins only. */
    public record StatsDto(int started, int won, int currentStreak, int maxStreak,
                           double averagePoints, Long bestPoints, double averageQuestions) {}
}
