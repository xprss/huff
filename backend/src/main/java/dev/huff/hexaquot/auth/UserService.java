package dev.huff.hexaquot.auth;

import dev.huff.hexaquot.persistence.UserEntity;
import dev.huff.hexaquot.persistence.AdminUserEntity;
import dev.huff.hexaquot.persistence.AuthSessionEntity;
import io.quarkus.oidc.UserInfo;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

@ApplicationScoped
public class UserService {
    private static final String SESSION_COOKIE = "huff_session";
    private static final long SESSION_MAX_AGE_SECONDS = 30L * 24 * 60 * 60;
    public static final String DEFAULT_PROFILE_EMOJI = "😀";
    public static final String DEFAULT_INPUT_HAND_PREFERENCE = "RIGHT";
    public static final Set<String> ALLOWED_PROFILE_EMOJIS = Set.of(
        "😀", "😄", "😎", "🤓", "🥳", "😇", "🤠", "😴", "😤", "😍", "🙃", "😌"
    );
    private static final Pattern NICKNAME_PATTERN = Pattern.compile("@[a-z0-9._-]+");
    private static final int MAX_NICKNAME_LENGTH = 30;
    private static final int MAX_BIO_LENGTH = 200;

    @ConfigProperty(name = "app.auth.enabled")
    boolean authEnabled;

    @ConfigProperty(name = "app.cookie.secure")
    boolean cookieSecure;

    @Inject
    SecurityIdentity securityIdentity;

    @Inject
    Instance<UserInfo> userInfo;

    @Transactional
    public ResolvedUser resolve(String sessionId) {
        if (authEnabled) {
            if (securityIdentity != null && !securityIdentity.isAnonymous()) {
                AppUser user = authenticatedGoogleUser();
                AuthSessionEntity session = activeSession(sessionId);
                if (session != null && session.userId.equals(user.id())) {
                    return new ResolvedUser(user, null);
                }
                String newSessionId = UUID.randomUUID().toString();
                persistSession(newSessionId, user.id());
                return new ResolvedUser(user, sessionCookie(newSessionId));
            }
            AuthSessionEntity session = activeSession(sessionId);
            if (session == null) {
                return new ResolvedUser(null, null);
            }
            UserEntity user = UserEntity.findById(session.userId);
            return user == null
                ? new ResolvedUser(null, null)
                : new ResolvedUser(user.toAppUser(true, adminPrivileges(user.id)), null);
        }

        String anonymousSessionId = sessionId;
        sessionId = validSessionId(anonymousSessionId) ? anonymousSessionId : UUID.randomUUID().toString();
        AppUser user = upsertAnonymousUser(sessionId);
        String cookie = sessionId.equals(anonymousSessionId) ? null : sessionCookie(sessionId);
        return new ResolvedUser(user, cookie);
    }

    @Transactional
    public boolean hasAuthenticatedSession(String sessionId) {
        if (!authEnabled) {
            return false;
        }
        AuthSessionEntity session = activeSession(sessionId);
        if (session == null) {
            return false;
        }
        if (UserEntity.findById(session.userId) != null) {
            return true;
        }
        session.delete();
        return false;
    }

    @Transactional
    public void logout(String sessionId) {
        if (authEnabled && validSessionId(sessionId)) {
            AuthSessionEntity.deleteById(sessionId);
        }
    }

    public boolean authEnabled() {
        return authEnabled;
    }

    public String sessionCookieName() {
        return SESSION_COOKIE;
    }

    public String clearSessionCookie() {
        String secure = cookieSecure ? "; Secure" : "";
        return SESSION_COOKIE + "=; Path=/; Max-Age=0; HttpOnly; SameSite=Lax" + secure;
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

    private AppUser authenticatedGoogleUser() {
        UserInfo verifiedUserInfo = userInfo.isResolvable() ? userInfo.get() : null;
        // In service mode UserInfo can be injected even when its payload is
        // empty. The SecurityIdentity is built only after Quarkus has
        // validated the Bearer token, and its principal is the stable OIDC
        // subject. Never derive a user id from an optional UserInfo claim.
        String subject = requireSubject(securityIdentity.getPrincipal().getName());
        String email = firstNonBlank(
            verifiedUserInfo == null ? null : verifiedUserInfo.getEmail(),
            securityIdentity.getAttribute("email"),
            subject
        );
        String name = firstNonBlank(
            verifiedUserInfo == null ? null : verifiedUserInfo.getName(),
            securityIdentity.getAttribute("name"),
            email
        );
        return upsertGoogleUser(subject, email, name);
    }

    private AuthSessionEntity activeSession(String sessionId) {
        if (!validSessionId(sessionId)) {
            return null;
        }
        AuthSessionEntity session = AuthSessionEntity.findById(sessionId);
        if (session == null) {
            return null;
        }
        if (Instant.now().isBefore(Instant.parse(session.expiresAt))) {
            return session;
        }
        session.delete();
        return null;
    }

    private void persistSession(String sessionId, String userId) {
        AuthSessionEntity session = new AuthSessionEntity();
        session.id = sessionId;
        session.userId = userId;
        session.expiresAt = Instant.now().plusSeconds(SESSION_MAX_AGE_SECONDS).toString();
        session.persist();
    }

    private String requireSubject(String subject) {
        if (isBlank(subject) || "null".equalsIgnoreCase(subject.trim())) {
            throw new NotAuthorizedException("Token Google senza subject valido.");
        }
        return subject.trim();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (!isBlank(value)) {
                return value;
            }
        }
        return null;
    }

    private AppUser upsertAnonymousUser(String sessionId) {
        return upsertUser(UserIds.anonymous(sessionId), null, null, "Giocatore", false);
    }

    private AppUser upsertGoogleUser(String subject, String email, String displayName) {
        String existingId = findUserIdByGoogleSubject(subject);
        String userId = existingId == null ? UserIds.google(subject) : existingId;
        return upsertUser(userId, subject, email, displayName, true);
    }

    private String findUserIdByGoogleSubject(String subject) {
        return UserEntity.<UserEntity>find("googleSubject", subject)
            .firstResultOptional()
            .map(user -> user.id)
            .orElse(null);
    }

    private AppUser upsertUser(String userId, String googleSubject, String email, String displayName, boolean authenticated) {
        if (!UserIds.isCanonical(userId)) {
            throw new IllegalArgumentException("User id must include a SHA-256 suffix and no whitespace");
        }
        String now = Instant.now().toString();
        UserEntity user = UserEntity.findById(userId);
        if (user == null) {
            user = new UserEntity();
            user.id = userId;
            user.createdAt = now;
            user.starAvailable = false;
            user.googleSubject = googleSubject;
            user.email = email;
            user.displayName = defaultDisplayName(displayName);
            user.nickname = defaultNickname(userId, user.displayName);
            user.profileEmoji = DEFAULT_PROFILE_EMOJI;
            user.inputHandPreference = DEFAULT_INPUT_HAND_PREFERENCE;
            user.persist();
            return user.toAppUser(authenticated, adminPrivileges(userId));
        }
        user.googleSubject = googleSubject;
        user.email = email;
        if (isBlank(user.displayName)) {
            user.displayName = defaultDisplayName(displayName);
        }
        if (isBlank(user.nickname)) {
            user.nickname = defaultNickname(userId, user.displayName);
        }
        if (!ALLOWED_PROFILE_EMOJIS.contains(user.profileEmoji)) {
            user.profileEmoji = DEFAULT_PROFILE_EMOJI;
        }
        if (!isValidInputHandPreference(user.inputHandPreference)) {
            user.inputHandPreference = DEFAULT_INPUT_HAND_PREFERENCE;
        }
        return user.toAppUser(authenticated, adminPrivileges(userId));
    }

    @Transactional
    public AppUser updateProfile(
        String userId,
        String displayName,
        String nickname,
        String profileEmoji,
        String bio,
        String inputHandPreference,
        boolean authenticated
    ) {
        UserEntity user = UserEntity.findById(userId);
        if (user == null) {
            throw new NotAuthorizedException("Utente non trovato.");
        }

        String normalizedDisplayName = normalizeDisplayName(displayName);
        String normalizedNickname = normalizeNickname(nickname);
        String normalizedProfileEmoji = normalizeProfileEmoji(profileEmoji);
        String normalizedBio = normalizeBio(bio);
        String normalizedInputHandPreference = inputHandPreference == null
            ? (isValidInputHandPreference(user.inputHandPreference) ? user.inputHandPreference : DEFAULT_INPUT_HAND_PREFERENCE)
            : normalizeInputHandPreference(inputHandPreference);
        UserEntity nicknameOwner = UserEntity.<UserEntity>find("nickname", normalizedNickname).firstResult();
        if (nicknameOwner != null && !nicknameOwner.id.equals(userId)) {
            throw new WebApplicationException("Nickname già in uso.", Response.Status.CONFLICT);
        }

        user.displayName = normalizedDisplayName;
        user.nickname = normalizedNickname;
        user.profileEmoji = normalizedProfileEmoji;
        user.bio = normalizedBio;
        user.inputHandPreference = normalizedInputHandPreference;
        return user.toAppUser(authenticated, adminPrivileges(userId));
    }

    private AdminPrivileges adminPrivileges(String userId) {
        AdminUserEntity admin = AdminUserEntity.findById(userId);
        return admin == null ? null : admin.toPrivileges();
    }

    private String defaultDisplayName(String displayName) {
        if (isBlank(displayName)) {
            return "Giocatore";
        }
        return displayName.trim();
    }

    private String defaultNickname(String userId, String displayName) {
        String slug = slugify(defaultDisplayName(displayName));
        String suffix = deterministicSuffix(userId);
        int maxSlugLength = MAX_NICKNAME_LENGTH - 1 - 1 - suffix.length();
        String trimmedSlug = slug.length() > maxSlugLength ? slug.substring(0, maxSlugLength) : slug;
        return "@" + trimmedSlug + "-" + suffix;
    }

    private String normalizeDisplayName(String displayName) {
        if (isBlank(displayName)) {
            throw new BadRequestException("Il nome non può essere vuoto.");
        }
        return displayName.trim();
    }

    public static String normalizeNickname(String nickname) {
        if (isBlank(nickname)) {
            throw new BadRequestException("Il nickname non può essere vuoto.");
        }
        String normalized = nickname.trim().toLowerCase(Locale.ROOT);
        if (!normalized.startsWith("@")) {
            normalized = "@" + normalized;
        }
        if (normalized.length() > MAX_NICKNAME_LENGTH) {
            throw new BadRequestException("Il nickname può avere al massimo 30 caratteri.");
        }
        if (!NICKNAME_PATTERN.matcher(normalized).matches()) {
            throw new BadRequestException("Il nickname deve avere formato @[a-z0-9._-]+.");
        }
        return normalized;
    }

    private String normalizeProfileEmoji(String profileEmoji) {
        if (!ALLOWED_PROFILE_EMOJIS.contains(profileEmoji)) {
            throw new BadRequestException("Emoji profilo non ammessa.");
        }
        return profileEmoji;
    }

    private String normalizeBio(String bio) {
        if (isBlank(bio)) {
            return null;
        }
        String normalized = bio.trim();
        if (normalized.length() > MAX_BIO_LENGTH) {
            throw new BadRequestException("La bio può avere al massimo 200 caratteri.");
        }
        return normalized;
    }

    private String normalizeInputHandPreference(String inputHandPreference) {
        if (!isValidInputHandPreference(inputHandPreference)) {
            throw new BadRequestException("Preferenza mano non ammessa.");
        }
        return inputHandPreference;
    }

    private boolean isValidInputHandPreference(String inputHandPreference) {
        return "LEFT".equals(inputHandPreference) || "RIGHT".equals(inputHandPreference);
    }

    private String slugify(String value) {
        String slug = value
            .toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9._-]+", "-")
            .replaceAll("^[._-]+|[._-]+$", "");
        return slug.isBlank() ? "giocatore" : slug;
    }

    private String deterministicSuffix(String userId) {
        int separator = userId.lastIndexOf(':');
        if (separator >= 0 && userId.length() >= separator + 9) {
            return userId.substring(separator + 1, separator + 9);
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(userId.getBytes(StandardCharsets.UTF_8));
            StringBuilder value = new StringBuilder();
            for (int index = 0; index < 4; index++) {
                value.append(String.format("%02x", bytes[index]));
            }
            return value.toString();
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 not available", error);
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String sessionCookie(String sessionId) {
        String secure = cookieSecure ? "; Secure" : "";
        return SESSION_COOKIE + "=" + sessionId + "; Path=/; Max-Age=" + SESSION_MAX_AGE_SECONDS + "; HttpOnly; SameSite=Lax" + secure;
    }

    public void requireAuthenticatedOrCookie(ResolvedUser resolvedUser) {
        if (resolvedUser.user() == null) {
            throw new NotAuthorizedException("Google login required");
        }
    }
}
