package dev.huff.hexaquot.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class UserIds {
    private static final Pattern SHA_256_SUFFIX = Pattern.compile(":([0-9a-f]{64})$");

    private UserIds() {
    }

    public static String anonymous(String sessionId) {
        return canonical("anon:" + sessionId);
    }

    public static String google(String subject) {
        return canonical("google:" + subject);
    }

    public static String canonical(String rawId) {
        if (rawId == null || rawId.isBlank()) {
            throw new IllegalArgumentException("User id cannot be blank");
        }

        String trimmed = rawId.trim();
        Matcher matcher = SHA_256_SUFFIX.matcher(trimmed);
        if (matcher.find()) {
            String stem = withoutWhitespace(trimmed.substring(0, matcher.start()));
            return stem + ":" + matcher.group(1);
        }

        return withoutWhitespace(trimmed) + ":" + sha256Hex(trimmed);
    }

    public static boolean isCanonical(String userId) {
        if (userId == null || containsWhitespace(userId)) {
            return false;
        }
        return SHA_256_SUFFIX.matcher(userId).find();
    }

    private static String withoutWhitespace(String value) {
        StringBuilder sanitized = new StringBuilder(value.length());
        value.codePoints()
            .filter(codePoint -> !Character.isWhitespace(codePoint))
            .forEach(sanitized::appendCodePoint);
        if (sanitized.isEmpty()) {
            throw new IllegalArgumentException("User id cannot contain only whitespace");
        }
        return sanitized.toString();
    }

    private static boolean containsWhitespace(String value) {
        return value.codePoints().anyMatch(Character::isWhitespace);
    }

    private static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte valueByte : hash) {
                hex.append(String.format("%02x", valueByte));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 is not available", error);
        }
    }
}
