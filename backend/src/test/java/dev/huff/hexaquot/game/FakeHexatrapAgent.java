package dev.huff.hexaquot.game;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Stands in for the Mastra Hexatrap routes. Drafts are served from a queue (503 when empty);
 * the audit echoes the last draft's formulas, optionally garbling one item to simulate a text
 * that does not say what its formula says.
 */
public class FakeHexatrapAgent implements QuarkusTestResourceLifecycleManager {
    static final String TOKEN = "hexatrap-test-token";
    static final ObjectMapper MAPPER = new ObjectMapper();
    static final Deque<JsonNode> DRAFTS = new ConcurrentLinkedDeque<>();
    static final List<JsonNode> DRAFT_REQUESTS = new CopyOnWriteArrayList<>();
    static final List<JsonNode> AUDIT_REQUESTS = new CopyOnWriteArrayList<>();
    /** Item ids whose audit translation is wrong, consumed one per audit call. */
    static final Deque<String> GARBLED = new ConcurrentLinkedDeque<>();
    static volatile JsonNode lastDraft;

    private HttpServer server;

    static void reset() {
        DRAFTS.clear();
        DRAFT_REQUESTS.clear();
        AUDIT_REQUESTS.clear();
        GARBLED.clear();
        lastDraft = null;
    }

    @Override
    public Map<String, String> start() {
        try {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        } catch (IOException error) {
            throw new IllegalStateException(error);
        }
        server.createContext("/internal/hexatrap/draft", exchange -> {
            JsonNode body = read(exchange);
            if (body == null) return;
            DRAFT_REQUESTS.add(body);
            JsonNode draft = DRAFTS.poll();
            if (draft == null) {
                respond(exchange, 503, "{\"message\":\"failure\"}");
                return;
            }
            lastDraft = draft;
            ObjectNode response = MAPPER.createObjectNode();
            response.set("draft", draft);
            response.put("model", "openai/gpt-6-luna");
            respond(exchange, 200, MAPPER.writeValueAsString(response));
        });
        server.createContext("/internal/hexatrap/audit", exchange -> {
            JsonNode body = read(exchange);
            if (body == null) return;
            AUDIT_REQUESTS.add(body);
            String garbled = GARBLED.poll();
            ObjectNode response = MAPPER.createObjectNode();
            ArrayNode items = response.putArray("items");
            for (String list : List.of("certainties", "statements", "questions", "clues")) {
                for (JsonNode item : lastDraft.path(list)) {
                    String id = item.path("id").asText();
                    items.addObject().put("id", id).put("formula", id.equals(garbled) ? "NOT (" + item.path("formula").asText() + ")" : item.path("formula").asText());
                }
            }
            respond(exchange, 200, MAPPER.writeValueAsString(response));
        });
        server.start();
        return Map.of(
            "app.game-stats.agent.url", "http://127.0.0.1:" + server.getAddress().getPort(),
            "app.game-stats.agent.token", TOKEN,
            "app.hexatrap.generation.max-attempts", "3"
        );
    }

    private static JsonNode read(HttpExchange exchange) throws IOException {
        if (!("Bearer " + TOKEN).equals(exchange.getRequestHeaders().getFirst("Authorization"))) {
            respond(exchange, 401, "{}");
            return null;
        }
        return MAPPER.readTree(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
    }

    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }

    @Override
    public void stop() {
        if (server != null) server.stop(0);
    }

    static List<String> feedbackOf(JsonNode request) {
        List<String> feedback = new ArrayList<>();
        request.path("feedback").forEach(node -> feedback.add(node.asText()));
        return feedback;
    }
}
