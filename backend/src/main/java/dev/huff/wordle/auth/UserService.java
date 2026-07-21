package dev.huff.wordle.auth;

import dev.huff.wordle.persistence.Database;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotAuthorizedException;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class UserService {
    private static final String SESSION_COOKIE = "huff_session";

    @ConfigProperty(name = "app.auth.enabled")
    boolean authEnabled;

    @ConfigProperty(name = "app.cookie.secure")
    boolean cookieSecure;

    @Inject
    Database database;

    @Inject
    SecurityIdentity securityIdentity;

    public ResolvedUser resolve(String anonymousSessionId) {
        if (authEnabled) {
            if (securityIdentity == null || securityIdentity.isAnonymous()) {
                return new ResolvedUser(null, null, "/api/login");
            }
            String subject = securityIdentity.getPrincipal().getName();
            String email = Optional.ofNullable(securityIdentity.<String>getAttribute("email")).orElse(subject);
            String name = Optional.ofNullable(securityIdentity.<String>getAttribute("name")).orElse(email);
            return new ResolvedUser(upsertGoogleUser(subject, email, name), null, null);
        }

        String sessionId = validSessionId(anonymousSessionId) ? anonymousSessionId : UUID.randomUUID().toString();
        AppUser user = upsertAnonymousUser(sessionId);
        String cookie = sessionId.equals(anonymousSessionId) ? null : sessionCookie(sessionId);
        return new ResolvedUser(user, cookie, null);
    }

    public boolean authEnabled() {
        return authEnabled;
    }

    public String sessionCookieName() {
        return SESSION_COOKIE;
    }

    private boolean validSessionId(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        try {
            UUID.fromString(value);
            return true;
        } catch (IllegalArgumentException error) {
            return false;
        }
    }

    private AppUser upsertAnonymousUser(String sessionId) {
        String userId = "anon:" + sessionId;
        return upsertUser(userId, null, null, "Giocatore", false);
    }

    private AppUser upsertGoogleUser(String subject, String email, String displayName) {
        String existingId = findUserIdByGoogleSubject(subject);
        String userId = existingId == null ? "google:" + subject : existingId;
        return upsertUser(userId, subject, email, displayName, true);
    }

    private String findUserIdByGoogleSubject(String subject) {
        try (Connection connection = database.connection();
             PreparedStatement statement = connection.prepareStatement("SELECT id FROM users WHERE google_subject = ?")) {
            statement.setString(1, subject);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getString("id") : null;
            }
        } catch (Exception error) {
            throw new IllegalStateException("Cannot load Google user", error);
        }
    }

    private AppUser upsertUser(String userId, String googleSubject, String email, String displayName, boolean authenticated) {
        String now = Instant.now().toString();
        try (Connection connection = database.connection();
             PreparedStatement statement = connection.prepareStatement("""
                 INSERT INTO users (id, google_subject, email, display_name, created_at)
                 VALUES (?, ?, ?, ?, ?)
                 ON CONFLICT(id) DO UPDATE SET
                   google_subject = excluded.google_subject,
                   email = excluded.email,
                   display_name = excluded.display_name
                 """)) {
            statement.setString(1, userId);
            statement.setString(2, googleSubject);
            statement.setString(3, email);
            statement.setString(4, displayName);
            statement.setString(5, now);
            statement.executeUpdate();
            return new AppUser(userId, email, displayName, authenticated);
        } catch (Exception error) {
            throw new IllegalStateException("Cannot save user", error);
        }
    }

    private String sessionCookie(String sessionId) {
        String secure = cookieSecure ? "; Secure" : "";
        return SESSION_COOKIE + "=" + sessionId + "; Path=/; Max-Age=31536000; HttpOnly; SameSite=Lax" + secure;
    }

    public void requireAuthenticatedOrCookie(ResolvedUser resolvedUser) {
        if (resolvedUser.user() == null) {
            throw new NotAuthorizedException("Google login required");
        }
    }
}
