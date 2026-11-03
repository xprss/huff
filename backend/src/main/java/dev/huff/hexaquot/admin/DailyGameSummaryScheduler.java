package dev.huff.hexaquot.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.huff.hexaquot.persistence.DailyGameSummaryEntity;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

@ApplicationScoped
public class DailyGameSummaryScheduler {
    private static final Logger LOG = Logger.getLogger(DailyGameSummaryScheduler.class);
    private static final HttpClient HTTP = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .build();

    @ConfigProperty(name = "app.game.timezone", defaultValue = "Europe/Rome")
    String timezone;

    @ConfigProperty(name = "app.game-stats.agent.url", defaultValue = "http://127.0.0.1:4111")
    String agentUrl;

    @ConfigProperty(name = "app.game-stats.agent.token", defaultValue = "unset")
    String agentToken;

    @Inject ObjectMapper objectMapper;

    @Scheduled(cron = "{app.game-stats.daily-summary.cron}", timeZone = "{app.game.timezone}")
    @Transactional
    void generatePreviousDaySummary() {
        generateForDate(previousDay(), false);
    }

    @Transactional
    public DailyGameSummaryEntity generatePreviousDaySummaryNow() {
        return generateForDate(previousDay(), true);
    }

    private DailyGameSummaryEntity generateForDate(LocalDate date, boolean replaceExisting) {
        DailyGameSummaryEntity existing = DailyGameSummaryEntity.findById(date.toString());
        if (existing != null && !replaceExisting) return existing;
        if (agentToken.isBlank() || "unset".equals(agentToken)) {
            LOG.warn("Daily game summary skipped: statistics agent token is not configured.");
            return null;
        }

        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(agentUrl.replaceAll("/+$", "") + "/internal/game-stats/daily-summary"))
                .timeout(Duration.ofSeconds(120))
                .header("Authorization", "Bearer " + agentToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(new SummaryRequest(date.toString())), StandardCharsets.UTF_8))
                .build();
            HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != 200) {
                LOG.errorf("Daily game summary for %s was not generated: statistics agent returned HTTP %d.", date, response.statusCode());
                return null;
            }

            AgentSummary generated = objectMapper.readValue(response.body(), AgentSummary.class);
            if (generated == null || !date.toString().equals(generated.date()) || generated.summary() == null || generated.summary().isBlank()) {
                LOG.errorf("Daily game summary for %s was not generated: invalid response from statistics agent.", date);
                return null;
            }

            DailyGameSummaryEntity summary = existing == null ? new DailyGameSummaryEntity() : existing;
            summary.summaryDate = date.toString();
            summary.summary = generated.summary().trim();
            summary.generatedAt = Instant.now().toString();
            if (existing == null) summary.persist();
            LOG.infof("Daily game summary %s for %s.", existing == null ? "generated" : "regenerated", date);
            return summary;
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            LOG.errorf(error, "Daily game summary generation interrupted for %s.", date);
        } catch (IOException | IllegalArgumentException error) {
            LOG.errorf(error, "Daily game summary generation failed for %s.", date);
        }
        return null;
    }

    private LocalDate previousDay() {
        return LocalDate.now(ZoneId.of(timezone)).minusDays(1);
    }

    public record SummaryRequest(String date) {}
    public record AgentSummary(String date, String summary) {}
}
