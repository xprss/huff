package dev.huff.hexaquot.persistence;

import dev.huff.hexaquot.auth.UserIds;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class UserIdShaMigration {
    @Inject
    EntityManager entityManager;

    @Transactional
    void onStart(@Observes StartupEvent event) {
        migrate();
    }

    @Transactional
    public int migrate() {
        var userIds = entityManager
            .createNativeQuery("SELECT id FROM users")
            .getResultList();

        int migrated = 0;
        for (Object rawUserId : userIds) {
            String userId = String.valueOf(rawUserId);
            String canonicalUserId = UserIds.canonical(userId);
            if (userId.equals(canonicalUserId)) {
                continue;
            }

            assertTargetIsAvailable(userId, canonicalUserId);
            entityManager
                .createNativeQuery("UPDATE users SET id = ?1 WHERE id = ?2")
                .setParameter(1, canonicalUserId)
                .setParameter(2, userId)
                .executeUpdate();
            entityManager
                .createNativeQuery("UPDATE games SET user_id = ?1 WHERE user_id = ?2")
                .setParameter(1, canonicalUserId)
                .setParameter(2, userId)
                .executeUpdate();
            migrated++;
        }
        return migrated;
    }

    private void assertTargetIsAvailable(String userId, String canonicalUserId) {
        Number existingUsers = (Number) entityManager
            .createNativeQuery("SELECT COUNT(*) FROM users WHERE id = ?1")
            .setParameter(1, canonicalUserId)
            .getSingleResult();
        if (existingUsers.longValue() > 0) {
            throw new IllegalStateException("Cannot migrate user id " + userId + ": target already exists");
        }
    }
}
