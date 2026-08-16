package dev.huff.hexaquot.push;

import dev.huff.hexaquot.persistence.UserAnnouncementEntity;
import dev.huff.hexaquot.persistence.UserEntity;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
class AnnouncementServiceTest {
    @Inject AnnouncementService service;

    @Test @TestTransaction
    void exposesAndIdempotentlyMarksTheLaunchAnnouncementSeen() {
        String userId = "announcement-test-" + UUID.randomUUID();
        UserEntity user = new UserEntity();
        user.id = userId;
        user.displayName = "Annuncio";
        user.nickname = "@annuncio-" + UUID.randomUUID().toString().substring(0, 8);
        user.profileEmoji = "😀";
        user.createdAt = Instant.now().toString();
        user.persist();
        UserAnnouncementEntity row = new UserAnnouncementEntity();
        row.id = UUID.randomUUID().toString();
        row.userId = userId;
        row.campaign = AnnouncementService.HEXAHACK_LAUNCH;
        row.createdAt = Instant.now().toString();
        row.persist();

        assertEquals(1, service.pending(userId).size());
        service.markSeen(userId, AnnouncementService.HEXAHACK_LAUNCH);
        service.markSeen(userId, AnnouncementService.HEXAHACK_LAUNCH);
        assertEquals(0, service.pending(userId).size());
    }
}
