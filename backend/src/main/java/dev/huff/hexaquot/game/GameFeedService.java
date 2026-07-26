package dev.huff.hexaquot.game;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@ApplicationScoped
public class GameFeedService {
    @ConfigProperty(name = "app.public.url")
    Optional<String> publicUrl;

    @Inject
    GameRepository gameRepository;

    @Inject
    ObjectMapper objectMapper;

    public String rss() {
        List<GameRecord> records = gameRepository.findCompletedForFeed();
        String newestDate = records.stream()
            .findFirst()
            .map(this::feedInstant)
            .map(this::rssDate)
            .orElse(null);

        StringBuilder rss = new StringBuilder();
        rss.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        rss.append("<rss version=\"2.0\">\n");
        rss.append("  <channel>\n");
        appendElement(rss, "title", "Huff - partite concluse", 4);
        appendElement(rss, "link", siteLink(), 4);
        appendElement(rss, "description", "Vittorie e sconfitte completate su Huff.", 4);
        appendElement(rss, "language", "it-IT", 4);
        if (newestDate != null) {
            appendElement(rss, "lastBuildDate", newestDate, 4);
        }

        for (GameRecord record : records) {
            appendItem(rss, record);
        }

        rss.append("  </channel>\n");
        rss.append("</rss>\n");
        return rss.toString();
    }

    private void appendItem(StringBuilder rss, GameRecord record) {
        List<GuessResult> guesses = readGuesses(record);
        GameFeedStats stats = stats(guesses);
        String outcome = record.status() == GameStatus.WON ? "vinta" : "persa";
        String completedAt = feedInstant(record).toString();
        String description = String.format(
            "Esito: %s. Modalità: %s. Data partita: %s. Fine gioco: %s. Tentativi: %d/%d. Tessere: %d corrette, %d presenti, %d assenti, %d nascoste.",
            record.status() == GameStatus.WON ? "Vinta" : "Persa",
            record.mode().label(),
            record.puzzleDate(),
            completedAt,
            guesses.size(),
            DailyGameService.MAX_ATTEMPTS,
            stats.correct(),
            stats.present(),
            stats.absent(),
            stats.hidden()
        );

        rss.append("    <item>\n");
        appendElement(rss, "title", "Partita " + outcome + " in modalità " + record.mode().label(), 6);
        appendElement(rss, "link", siteLink(), 6);
        appendElement(rss, "guid", "huff-game-" + record.id(), 6, Map.of("isPermaLink", "false"));
        appendElement(rss, "pubDate", rssDate(feedInstant(record)), 6);
        appendElement(rss, "description", description, 6);
        rss.append("    </item>\n");
    }

    private GameFeedStats stats(List<GuessResult> guesses) {
        Map<TileState, Long> counts = guesses.stream()
            .flatMap(guess -> guess.tiles().stream())
            .collect(Collectors.groupingBy(TileResult::state, Collectors.counting()));
        return new GameFeedStats(
            count(counts, TileState.CORRECT),
            count(counts, TileState.PRESENT),
            count(counts, TileState.ABSENT),
            count(counts, TileState.HIDDEN)
        );
    }

    private int count(Map<TileState, Long> counts, TileState state) {
        return Math.toIntExact(counts.getOrDefault(state, 0L));
    }

    private List<GuessResult> readGuesses(GameRecord record) {
        try {
            return objectMapper.readValue(record.guessesJson(), new TypeReference<>() {
            });
        } catch (Exception error) {
            throw new IllegalStateException("Cannot parse guesses for feed", error);
        }
    }

    private Instant feedInstant(GameRecord record) {
        String timestamp = record.completedAt() == null ? record.updatedAt() : record.completedAt();
        return Instant.parse(timestamp);
    }

    private String rssDate(Instant instant) {
        return DateTimeFormatter.RFC_1123_DATE_TIME.format(instant.atZone(ZoneOffset.UTC));
    }

    private String siteLink() {
        return publicUrl.filter(url -> !url.isBlank()).orElse("/");
    }

    private void appendElement(StringBuilder rss, String name, String value, int spaces) {
        appendElement(rss, name, value, spaces, Map.of());
    }

    private void appendElement(StringBuilder rss, String name, String value, int spaces, Map<String, String> attributes) {
        rss.append(" ".repeat(spaces)).append("<").append(name);
        attributes.forEach((key, attributeValue) ->
            rss.append(" ").append(key).append("=\"").append(escape(attributeValue)).append("\"")
        );
        rss.append(">")
            .append(escape(value))
            .append("</")
            .append(name)
            .append(">\n");
    }

    private String escape(String value) {
        return value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;");
    }

    private record GameFeedStats(int correct, int present, int absent, int hidden) {
    }
}
