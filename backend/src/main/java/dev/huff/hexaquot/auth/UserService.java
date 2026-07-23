package dev.huff.hexaquot.auth;

import dev.huff.hexaquot.persistence.UserEntity;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotAuthorizedException;
import org.eclipse.microprofile.config.inject.ConfigProperty;

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
    SecurityIdentity securityIdentity;

    @Transactional
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
        return UserEntity.<UserEntity>find("googleSubject", subject)
            .firstResultOptional()
            .map(user -> user.id)
            .orElse(null);
    }

    private AppUser upsertUser(String userId, String googleSubject, String email, String displayName, boolean authenticated) {
        String now = Instant.now().toString();
        UserEntity user = UserEntity.findById(userId);
        if (user == null) {
            user = new UserEntity();
            user.id = userId;
            user.createdAt = now;
            user.persist();
        }
        user.googleSubject = googleSubject;
        user.email = email;
        user.displayName = displayName;
        return user.toAppUser(authenticated);
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
