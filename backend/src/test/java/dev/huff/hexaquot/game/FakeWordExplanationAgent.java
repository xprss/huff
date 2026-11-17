package dev.huff.hexaquot.game;

import com.sun.net.httpserver.HttpServer;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Stands in for the Mastra word explanation route; "zzzzzz" always fails. */
public class FakeWordExplanationAgent implements QuarkusTestResourceLifecycleManager {
    static final String TOKEN = "word-explanation-test-token";
    static final String FAILING_WORD = "zzzzzz";
    static final Map<String, AtomicInteger> CALLS = new ConcurrentHashMap<>();
    private static final Pattern WORD = Pattern.compile("\"word\"\\s*:\\s*\"([a-z]+)\"");

    private HttpServer server;

    static int calls(String word) {
        AtomicInteger count = CALLS.get(word);
        return count == null ? 0 : count.get();
    }

    static String explanationFor(String word) {
        return "Spiegazione di " + word + ".";
    }

    @Override
    public Map<String, String> start() {
        try {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        } catch (IOException error) {
            throw new IllegalStateException(error);
        }
        server.createContext("/internal/words/explanation", exchange -> {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            Matcher matcher = WORD.matcher(body);
            String word = matcher.find() ? matcher.group(1) : "";
            CALLS.computeIfAbsent(word, ignored -> new AtomicInteger()).incrementAndGet();
            boolean authorized = ("Bearer " + TOKEN).equals(exchange.getRequestHeaders().getFirst("Authorization"));
            int status = !authorized ? 401 : FAILING_WORD.equals(word) ? 503 : 200;
            byte[] response = (status == 200
                ? "{\"word\":\"" + word + "\",\"explanation\":\"" + explanationFor(word) + "\"}"
                : "{\"message\":\"failure\"}").getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, response.length);
            try (OutputStream output = exchange.getResponseBody()) {
                output.write(response);
            }
        });
        server.start();
        return Map.of(
            "app.game-stats.agent.url", "http://127.0.0.1:" + server.getAddress().getPort(),
            "app.game-stats.agent.token", TOKEN
        );
    }

    @Override
    public void stop() {
        if (server != null) server.stop(0);
    }
}
