package dev.huff.hexaquot.game;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.huff.hexaquot.auth.AppUser;
import dev.huff.hexaquot.persistence.WordExplanationEntity;
import io.quarkus.narayana.jta.QuarkusTransaction;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Explains the daily Hexaword and Hexastar solutions. The explanation is written
 * once by the word explanation agent, when the first game of the day ends, and is
 * then served from the database to every other player.
 */
@ApplicationScoped
public class WordExplanationService {
    private static final Logger LOG = Logger.getLogger(WordExplanationService.class);
    private static final Duration RETRY_AFTER_FAILURE = Duration.ofMinutes(1);
    private static final int MAX_EXPLANATION_LENGTH = 1200;
    private static final HttpClient HTTP = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .build();

    @ConfigProperty(name = "app.game-stats.agent.url", defaultValue = "http://127.0.0.1:4111")
    String agentUrl;

    @ConfigProperty(name = "app.game-stats.agent.token", defaultValue = "unset")
    String agentToken;

    @Inject ObjectMapper objectMapper;
    @Inject EntityManager entityManager;
    @Inject DailyGameService dailyGameService;
    @Inject HexastarDailyGameService hexastarService;

    private final Set<String> inFlight = ConcurrentHashMap.newKeySet();
    private final Map<String, Instant> failedAt = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newFixedThreadPool(2, new DaemonThreads());

    public WordExplanationDto hexaword(AppUser user) {
        GameDto game = dailyGameService.today(user).game();
        if (game == null || game.status() == GameStatus.IN_PROGRESS) throw notFinished();
        return explain(game.solution());
    }

    public WordExplanationDto hexastar(AppUser user) {
        HexastarDtos.GameDto game = hexastarService.today(user).game();
        if (game == null || game.status() == HexastarDtos.Status.IN_PROGRESS) throw notFinished();
        return explain(String.join("", game.solutionSyllables()));
    }

    /** Starts writing the explanation as soon as a game ends, so it is ready when the client asks. */
    public void prefetch(String rawWord) {
        String word = normalize(rawWord);
        if (word != null && isConfigured() && cached(word) == null) start(word);
    }

    public void prefetchHexaword(GameDto game) {
        if (game != null && game.status() != GameStatus.IN_PROGRESS) prefetch(game.solution());
    }

    public void prefetchHexastar(HexastarDtos.GameDto game) {
        if (game != null && game.status() != HexastarDtos.Status.IN_PROGRESS && game.solutionSyllables() != null) {
            prefetch(String.join("", game.solutionSyllables()));
        }
    }

    WordExplanationDto explain(String rawWord) {
        String word = normalize(rawWord);
        if (word == null) return WordExplanationDto.unavailable();
        String explanation = cached(word);
        if (explanation != null) return WordExplanationDto.ready(explanation);
        if (!isConfigured()) return WordExplanationDto.unavailable();
        return start(word) ? WordExplanationDto.pending() : WordExplanationDto.unavailable();
    }

    private String cached(String word) {
        return QuarkusTransaction.joiningExisting().call(() -> {
            WordExplanationEntity entity = entityManager.find(WordExplanationEntity.class, word);
            return entity == null ? null : entity.explanation;
        });
    }

    /** Returns true while generation is running, false while a recent failure is cooling down. */
    private boolean start(String word) {
        if (inFlight.contains(word)) return true;
        Instant failure = failedAt.get(word);
        if (failure != null && failure.plus(RETRY_AFTER_FAILURE).isAfter(Instant.now())) return false;
        if (!inFlight.add(word)) return true;
        try {
            executor.execute(() -> {
                try {
                    generate(word);
                } finally {
                    inFlight.remove(word);
                }
            });
        } catch (RejectedExecutionException error) {
            inFlight.remove(word);
            return false;
        }
        return true;
    }

    private void generate(String word) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(agentUrl.replaceAll("/+$", "") + "/internal/words/explanation"))
                .timeout(Duration.ofSeconds(60))
                .header("Authorization", "Bearer " + agentToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(new AgentRequest(word)), StandardCharsets.UTF_8))
                .build();
            HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != 200) {
                fail(word, "agent returned HTTP " + response.statusCode(), null);
                return;
            }
            AgentResponse generated = objectMapper.readValue(response.body(), AgentResponse.class);
            String explanation = generated == null || generated.explanation() == null ? "" : generated.explanation().trim();
            if (!word.equals(generated == null ? null : generated.word()) || explanation.isEmpty()) {
                fail(word, "invalid response from agent", null);
                return;
            }
            if (explanation.length() > MAX_EXPLANATION_LENGTH) explanation = explanation.substring(0, MAX_EXPLANATION_LENGTH).trim();
            store(word, explanation);
            failedAt.remove(word);
            LOG.infof("Word explanation generated for %s.", word);
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            fail(word, "interrupted", error);
        } catch (Exception error) {
            fail(word, "request failed", error);
        }
    }

    private void store(String word, String explanation) {
        // Another instance may have written the same word meanwhile: keep the first one.
        QuarkusTransaction.requiringNew().run(() -> entityManager.createNativeQuery(
                "INSERT INTO word_explanations (word, explanation, generated_at) VALUES (:word, :explanation, :generatedAt) "
                    + "ON CONFLICT (word) DO NOTHING")
            .setParameter("word", word)
            .setParameter("explanation", explanation)
            .setParameter("generatedAt", Instant.now().toString())
            .executeUpdate());
    }

    private void fail(String word, String reason, Exception error) {
        failedAt.put(word, Instant.now());
        if (error == null) LOG.errorf("Word explanation for %s was not generated: %s.", word, reason);
        else LOG.errorf(error, "Word explanation for %s was not generated: %s.", word, reason);
    }

    private boolean isConfigured() {
        return !agentToken.isBlank() && !"unset".equals(agentToken);
    }

    private static String normalize(String rawWord) {
        if (rawWord == null) return null;
        String word = rawWord.trim().toLowerCase(Locale.ROOT);
        return word.matches("[a-z]{6}") ? word : null;
    }

    private static WebApplicationException notFinished() {
        return new WebApplicationException("La spiegazione è disponibile a partita conclusa.", Response.Status.CONFLICT);
    }

    @PreDestroy
    void shutdown() {
        executor.shutdownNow();
    }

    public record AgentRequest(String word) {}
    public record AgentResponse(String word, String explanation) {}

    private static final class DaemonThreads implements java.util.concurrent.ThreadFactory {
        private final AtomicInteger counter = new AtomicInteger();

        @Override
        public Thread newThread(Runnable task) {
            Thread thread = new Thread(task, "word-explanation-" + counter.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        }
    }
}
