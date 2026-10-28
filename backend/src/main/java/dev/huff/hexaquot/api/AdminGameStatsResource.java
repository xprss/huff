package dev.huff.hexaquot.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.huff.hexaquot.auth.ResolvedUser;
import dev.huff.hexaquot.auth.UserService;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

@Path("/api/admin/game-stats")
public class AdminGameStatsResource {
    private static final int MAX_USERS = 10;
    private static final int MAX_MESSAGE_LENGTH = 4000;
    private static final Set<String> GAMES = Set.of("hexaword", "hexahack", "hexasky", "hexaflow", "hexastar", "hexaeco", "hexatrap");
    private static final Pattern NICKNAME = Pattern.compile("@[a-z0-9._-]{1,30}");
    private static final HttpClient HTTP = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(3))
        .build();

    @Inject UserService userService;
    @Inject EntityManager entityManager;
    @Inject ObjectMapper objectMapper;

    @ConfigProperty(name = "app.game-stats.agent.url", defaultValue = "http://127.0.0.1:4111")
    String agentUrl;

    @ConfigProperty(name = "app.game-stats.agent.token", defaultValue = "unset")
    String agentToken;

    @GET
    @Path("/users")
    @Produces("application/json")
    public Response users(@CookieParam("huff_session") String sessionId, @QueryParam("q") String query) {
        ResolvedUser resolved = requireAdmin(sessionId);
        String value = query == null ? "" : query.trim();
        if (value.length() > 80) throw new BadRequestException("Testo di ricerca troppo lungo.");

        String pattern = "%" + escapeLike(value.toLowerCase(Locale.ROOT)) + "%";
        TypedQuery<Object[]> statement = entityManager.createQuery(
            "SELECT u.nickname, u.displayName FROM UserEntity u "
                + "WHERE :empty = true OR LOWER(u.nickname) LIKE :pattern ESCAPE '\\' "
                + "OR LOWER(COALESCE(u.displayName, '')) LIKE :pattern ESCAPE '\\' "
                + "ORDER BY LOWER(u.nickname), LOWER(COALESCE(u.displayName, ''))",
            Object[].class
        );
        List<PlayerSuggestion> players = statement
            .setParameter("empty", value.isEmpty())
            .setParameter("pattern", pattern)
            .setMaxResults(MAX_USERS)
            .getResultList()
            .stream()
            .map(row -> new PlayerSuggestion((String) row[0], (String) row[1]))
            .toList();

        Response.ResponseBuilder response = Response.ok(new PlayerSuggestions(players));
        setCookie(response, resolved);
        return response.build();
    }

    @POST
    @Path("/stream")
    @Produces("text/event-stream")
    public Response stream(@CookieParam("huff_session") String sessionId, ChatRequest request) {
        ResolvedUser resolved = requireAdmin(sessionId);
        ChatRequest normalized = validate(request);
        if (agentToken.isBlank() || "unset".equals(agentToken)) return unavailable(resolved, "Servizio statistiche non configurato.");

        String resourceId = sha256(resolved.user().id());
        InternalChatRequest internalRequest = new InternalChatRequest(
            normalized.message(), normalized.threadId(), resourceId, normalized.users(), normalized.games(), normalized.from(), normalized.to()
        );

        final HttpResponse<InputStream> agentResponse;
        try {
            HttpRequest outbound = HttpRequest.newBuilder(URI.create(agentUrl.replaceAll("/+$", "") + "/internal/game-stats/stream"))
                .header("Authorization", "Bearer " + agentToken)
                .header("Content-Type", "application/json")
                .header("Accept", "text/event-stream")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(internalRequest), StandardCharsets.UTF_8))
                .build();
            agentResponse = HTTP.send(outbound, HttpResponse.BodyHandlers.ofInputStream());
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            return unavailable(resolved, "Richiesta interrotta.");
        } catch (IOException | IllegalArgumentException error) {
            return unavailable(resolved, "Agente statistiche non disponibile.");
        }

        if (agentResponse.statusCode() != 200) {
            closeQuietly(agentResponse.body());
            return unavailable(resolved, "Agente statistiche non disponibile.");
        }

        StreamingOutput body = output -> {
            try (InputStream input = agentResponse.body()) {
                byte[] buffer = new byte[4096];
                int count;
                while ((count = input.read(buffer)) != -1) {
                    output.write(buffer, 0, count);
                    output.flush();
                }
            }
        };
        Response.ResponseBuilder response = Response.ok(body)
            .type("text/event-stream; charset=utf-8")
            .header("Cache-Control", "no-cache, no-transform")
            .header("X-Accel-Buffering", "no");
        setCookie(response, resolved);
        return response.build();
    }

    @GET
    @Path("/threads")
    @Produces("application/json")
    public Response threads(@CookieParam("huff_session") String sessionId) {
        ResolvedUser resolved = requireAdmin(sessionId);
        return proxyJson(resolved, "/internal/game-stats/threads?resourceId=" + sha256(resolved.user().id()), "GET");
    }

    @GET
    @Path("/threads/{threadId}")
    @Produces("application/json")
    public Response thread(@CookieParam("huff_session") String sessionId, @PathParam("threadId") String threadId) {
        ResolvedUser resolved = requireAdmin(sessionId);
        String normalizedId = validateThreadId(threadId);
        return proxyJson(resolved, "/internal/game-stats/threads/" + normalizedId + "?resourceId=" + sha256(resolved.user().id()), "GET");
    }

    @DELETE
    @Path("/threads/{threadId}")
    @Produces("application/json")
    public Response deleteThread(@CookieParam("huff_session") String sessionId, @PathParam("threadId") String threadId) {
        ResolvedUser resolved = requireAdmin(sessionId);
        String normalizedId = validateThreadId(threadId);
        return proxyJson(resolved, "/internal/game-stats/threads/" + normalizedId + "?resourceId=" + sha256(resolved.user().id()), "DELETE");
    }

    private Response proxyJson(ResolvedUser resolved, String path, String method) {
        if (agentToken.isBlank() || "unset".equals(agentToken)) return unavailable(resolved, "Servizio statistiche non configurato.");
        try {
            HttpRequest outbound = HttpRequest.newBuilder(URI.create(agentUrl.replaceAll("/+$", "") + path))
                .header("Authorization", "Bearer " + agentToken)
                .header("Accept", "application/json")
                .method(method, HttpRequest.BodyPublishers.noBody())
                .build();
            HttpResponse<String> agentResponse = HTTP.send(outbound, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            Response.ResponseBuilder response;
            if (agentResponse.statusCode() == 200) {
                response = Response.ok(agentResponse.body()).type("application/json; charset=utf-8");
            } else if (agentResponse.statusCode() == 404) {
                response = Response.status(Response.Status.NOT_FOUND).type("application/json")
                    .entity(new ErrorDto("conversation_not_found", "Conversazione non trovata."));
            } else {
                return unavailable(resolved, "Agente statistiche non disponibile.");
            }
            setCookie(response, resolved);
            return response.build();
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            return unavailable(resolved, "Richiesta interrotta.");
        } catch (IOException | IllegalArgumentException error) {
            return unavailable(resolved, "Agente statistiche non disponibile.");
        }
    }

    private ResolvedUser requireAdmin(String sessionId) {
        ResolvedUser resolved = userService.resolve(sessionId);
        if (resolved.user() == null || (sessionId == null && !resolved.user().authenticated())) {
            throw new WebApplicationException(Response.status(Response.Status.UNAUTHORIZED)
                .entity(new ErrorDto("token_required", "Token Bearer valido richiesto."))
                .build());
        }
        if (resolved.user().admin() == null) throw new ForbiddenException("Privilegio admin richiesto.");
        return resolved;
    }

    private ChatRequest validate(ChatRequest request) {
        if (request == null || request.message() == null || request.message().isBlank()
            || request.message().length() > MAX_MESSAGE_LENGTH) {
            throw new BadRequestException("Messaggio vuoto o troppo lungo.");
        }
        String threadId = validateThreadId(request.threadId());

        List<String> users = request.users() == null ? List.of() : request.users();
        if (users.size() > MAX_USERS) throw new BadRequestException("Puoi selezionare al massimo 10 utenti.");
        Set<String> distinct = new HashSet<>();
        for (String nickname : users) {
            if (nickname == null || !NICKNAME.matcher(nickname).matches() || !distinct.add(nickname)) {
                throw new BadRequestException("Elenco utenti non valido.");
            }
        }
        if (!users.isEmpty()) {
            Long matches = entityManager.createQuery(
                    "SELECT COUNT(u) FROM UserEntity u WHERE u.nickname IN :nicknames", Long.class)
                .setParameter("nicknames", users)
                .getSingleResult();
            if (matches != users.size()) throw new BadRequestException("Uno o più utenti selezionati non esistono.");
        }
        List<String> games = request.games() == null ? List.of() : request.games();
        if (games.size() > GAMES.size() || games.stream().anyMatch(game -> !GAMES.contains(game)) || new HashSet<>(games).size() != games.size()) {
            throw new BadRequestException("Elenco giochi non valido.");
        }
        String from = normalizeDate(request.from());
        String to = normalizeDate(request.to());
        validateDateRange(from, to);
        return new ChatRequest(request.message().trim(), threadId, List.copyOf(users), List.copyOf(games), from, to);
    }

    private String normalizeDate(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private void validateDateRange(String from, String to) {
        try {
            LocalDate start = from == null || from.isBlank() ? null : LocalDate.parse(from);
            LocalDate end = to == null || to.isBlank() ? null : LocalDate.parse(to);
            if (start != null && end != null && end.isBefore(start)) {
                throw new BadRequestException("Intervallo di date non valido.");
            }
        } catch (DateTimeParseException error) {
            throw new BadRequestException("Intervallo di date non valido.");
        }
    }

    private String validateThreadId(String value) {
        try {
            return UUID.fromString(value).toString();
        } catch (RuntimeException error) {
            throw new BadRequestException("Identificativo conversazione non valido.");
        }
    }

    private Response unavailable(ResolvedUser resolved, String message) {
        Response.ResponseBuilder response = Response.status(Response.Status.SERVICE_UNAVAILABLE)
            .type("application/json")
            .entity(new ErrorDto("stats_agent_unavailable", message));
        setCookie(response, resolved);
        return response.build();
    }

    private String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(digest.length * 2);
            for (byte item : digest) result.append(String.format("%02x", item));
            return result.toString();
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException(error);
        }
    }

    private void closeQuietly(InputStream input) {
        try { input.close(); } catch (IOException ignored) { }
    }

    private void setCookie(Response.ResponseBuilder response, ResolvedUser resolved) {
        if (resolved.setCookieHeader() != null) response.header("Set-Cookie", resolved.setCookieHeader());
    }

    public record ChatRequest(String message, String threadId, List<String> users, List<String> games, String from, String to) { }
    public record InternalChatRequest(String message, String threadId, String resourceId, List<String> users, List<String> games, String from, String to) { }
    public record PlayerSuggestion(String nickname, String displayName) { }
    public record PlayerSuggestions(List<PlayerSuggestion> users) { }
    public record ErrorDto(String code, String message) { }
}
