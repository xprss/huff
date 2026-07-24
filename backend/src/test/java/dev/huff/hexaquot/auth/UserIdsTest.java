package dev.huff.hexaquot.auth;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserIdsTest {
    @Test
    void appendsSha256SuffixAndRemovesWhitespace() {
        String userId = UserIds.google("subject with spaces");

        assertFalse(userId.contains(" "));
        assertTrue(userId.matches("google:subjectwithspaces:[0-9a-f]{64}"));
        assertTrue(UserIds.isCanonical(userId));
    }

    @Test
    void canonicalKeepsExistingShaSuffix() {
        String userId = UserIds.anonymous("cdb9d5bd-b0f5-4cdd-a533-bd09b03310e4");

        assertEquals(userId, UserIds.canonical(userId));
        assertTrue(UserIds.canonical(userId).matches("anon:[^\\s]+:[0-9a-f]{64}"));
    }

    @Test
    void shaSuffixKeepsSanitizedCollisionsDistinct() {
        String compact = UserIds.google("abc");
        String spaced = UserIds.google("a b c");

        assertNotEquals(compact, spaced);
        assertTrue(compact.matches("google:abc:[0-9a-f]{64}"));
        assertTrue(spaced.matches("google:abc:[0-9a-f]{64}"));
    }
}
