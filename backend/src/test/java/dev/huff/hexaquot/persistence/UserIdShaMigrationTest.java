package dev.huff.hexaquot.persistence;

import dev.huff.hexaquot.auth.UserIds;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class UserIdShaMigrationTest {
    @Inject
    EntityManager entityManager;

    @Inject
    UserIdShaMigration migration;

    @Test
    @TestTransaction
    void migratesExistingUsersAndTheirGamesToShaSuffixedIds() {
        String oldUserId = "google:legacy subject " + UUID.randomUUID();
        String newUserId = UserIds.canonical(oldUserId);
        String gameId = UUID.randomUUID().toString();
        String googleSubject = "legacy-subject-" + UUID.randomUUID();
        String now = Instant.now().toString();

        entityManager
            .createNativeQuery(
                "INSERT INTO users (id, google_subject, email, display_name, created_at) VALUES (?1, ?2, ?3, ?4, ?5)"
            )
            .setParameter(1, oldUserId)
            .setParameter(2, googleSubject)
            .setParameter(3, "legacy@example.com")
            .setParameter(4, "Legacy")
            .setParameter(5, now)
            .executeUpdate();
        entityManager
            .createNativeQuery(
                "INSERT INTO games (id, user_id, puzzle_date, solution, guesses_json, status, created_at, updated_at) "
                    + "VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8)"
            )
            .setParameter(1, gameId)
            .setParameter(2, oldUserId)
            .setParameter(3, "2099-01-01")
            .setParameter(4, "parola")
            .setParameter(5, "[]")
            .setParameter(6, "IN_PROGRESS")
            .setParameter(7, now)
            .setParameter(8, now)
            .executeUpdate();

        assertEquals(1, migration.migrate());

        assertFalse(newUserId.contains(" "));
        assertTrue(UserIds.isCanonical(newUserId));
        assertEquals(0, countUsers(oldUserId));
        assertEquals(1, countUsers(newUserId));
        assertEquals(1, countGamesForUser(newUserId));
        assertEquals(0, countGamesForUser(oldUserId));
    }

    private long countUsers(String userId) {
        return ((Number) entityManager
            .createNativeQuery("SELECT COUNT(*) FROM users WHERE id = ?1")
            .setParameter(1, userId)
            .getSingleResult()).longValue();
    }

    private long countGamesForUser(String userId) {
        return ((Number) entityManager
            .createNativeQuery("SELECT COUNT(*) FROM games WHERE user_id = ?1")
            .setParameter(1, userId)
            .getSingleResult()).longValue();
    }
}
