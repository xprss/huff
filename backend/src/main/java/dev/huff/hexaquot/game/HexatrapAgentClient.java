package dev.huff.hexaquot.game;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** HTTP client of the private Mastra routes that author and audit Hexatrap cases. */
@ApplicationScoped
public class HexatrapAgentClient {
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    @ConfigProperty(name = "app.game-stats.agent.url", defaultValue = "http://127.0.0.1:4111")
    String agentUrl;

    @ConfigProperty(name = "app.game-stats.agent.token", defaultValue = "unset")
    String agentToken;

    @ConfigProperty(name = "app.hexatrap.generation.timeout-seconds", defaultValue = "240")
    int timeoutSeconds;

    @Inject ObjectMapper mapper;

    public static final class AgentException extends Exception {
        public AgentException(String message) { super(message); }
        public AgentException(String message, Throwable cause) { super(message, cause); }
    }

    public record Brief(String date, int difficulty, String format, int budget, int questionCost, List<Integer> clueCosts,
                        List<String> icons, Avoid avoid) {}
    public record Avoid(List<String> titles, List<String> names, List<String> formats) {}
    public record Draft(JsonNode draft, String model) {}

    public boolean configured() {
        return agentToken != null && !agentToken.isBlank() && !"unset".equals(agentToken);
    }

    public Draft draft(Brief brief, JsonNode previousDraft, List<String> feedback) throws AgentException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("brief", brief);
        body.put("previousDraft", previousDraft);
        body.put("feedback", feedback);
        JsonNode response = post("/internal/hexatrap/draft", body);
        JsonNode draft = response.path("draft");
        if (!draft.isObject()) throw new AgentException("risposta senza bozza");
        return new Draft(draft, response.path("model").asText(null));
    }

    /** Returns the auditor's formula for each item id. */
    public Map<String, String> audit(Map<String, Object> request) throws AgentException {
        JsonNode response = post("/internal/hexatrap/audit", request);
        Map<String, String> formulas = new LinkedHashMap<>();
        for (JsonNode item : response.path("items")) {
            if (item.path("id").isTextual() && item.path("formula").isTextual()) formulas.put(item.get("id").asText(), item.get("formula").asText());
        }
        if (formulas.isEmpty()) throw new AgentException("revisione vuota");
        return formulas;
    }

    private JsonNode post(String path, Object body) throws AgentException {
        if (!configured()) throw new AgentException("agente non configurato");
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(agentUrl.replaceAll("/+$", "") + path))
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .header("Authorization", "Bearer " + agentToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body), StandardCharsets.UTF_8))
                .build();
            HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != 200) throw new AgentException("l'agente ha risposto HTTP " + response.statusCode());
            return mapper.readTree(response.body());
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new AgentException("richiesta interrotta", error);
        } catch (IOException | IllegalArgumentException error) {
            throw new AgentException("richiesta non riuscita: " + error.getMessage(), error);
        }
    }
}
