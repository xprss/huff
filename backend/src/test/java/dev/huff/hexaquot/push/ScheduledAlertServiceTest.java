package dev.huff.hexaquot.push;

import dev.huff.hexaquot.auth.AdminPrivileges;
import dev.huff.hexaquot.auth.AppUser;
import dev.huff.hexaquot.persistence.ScheduledAlertEntity;
import dev.huff.hexaquot.persistence.PushCampaignDeliveryEntity;
import dev.huff.hexaquot.persistence.PushSubscriptionEntity;
import dev.huff.hexaquot.persistence.UserEntity;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class ScheduledAlertServiceTest {
    @Inject ScheduledAlertService service;

    @Test @TestTransaction
    void schedulesEditsAndArchivesAModalPerUser() {
        String userId = "alert-user-" + UUID.randomUUID();
        UserEntity user = new UserEntity();
        user.id = userId;
        user.displayName = "Alert User";
        user.nickname = "@alert-" + UUID.randomUUID().toString().substring(0, 8);
        user.profileEmoji = "😀";
        user.createdAt = Instant.now().minusSeconds(3600).toString();
        user.persist();

        AppUser admin = new AppUser("admin", null, "Admin", "@admin", "😀", null, false,
            new AdminPrivileges(true, true, true, true, true));
        String future = Instant.now().plusSeconds(3600).toString();
        var request = new ScheduledAlertService.AlertRequest("MODAL", "Titolo", "Messaggio", "/#/games", future);
        var created = service.create(admin, request);
        assertEquals(0, service.pendingModals(userId).size());
        assertEquals("Titolo", service.list(admin).stream().filter(row -> row.id().equals(created.id())).findFirst().orElseThrow().title());

        service.update(admin, created.id(), new ScheduledAlertService.AlertRequest("MODAL", "Nuovo titolo", "Testo", null, future));
        ScheduledAlertEntity row = ScheduledAlertEntity.findById(created.id());
        row.scheduledAt = Instant.now().minusSeconds(1).toString();
        assertEquals("Nuovo titolo", service.pendingModals(userId).get(0).title());
        service.markSeen(userId, created.id());
        service.markSeen(userId, created.id());
        assertTrue(service.pendingModals(userId).isEmpty());
        assertThrows(BadRequestException.class, () -> service.delete(admin, created.id()));
    }

    @Test
    void requiresAdminToCreate() {
        assertThrows(ForbiddenException.class, () -> service.create(null,
            new ScheduledAlertService.AlertRequest("MODAL", "Titolo", "Messaggio", null,
                Instant.now().plusSeconds(3600).toString())));
    }

    @Test @TestTransaction
    void rejectsPastAndExternalLinks() {
        AppUser admin = new AppUser("admin", null, "Admin", "@admin", "😀", null, false,
            new AdminPrivileges(true, true, true, true, true));
        assertThrows(BadRequestException.class, () -> service.create(admin,
            new ScheduledAlertService.AlertRequest("PUSH", "Test", "Body", null, Instant.now().minusSeconds(1).toString())));
        assertThrows(BadRequestException.class, () -> service.create(admin,
            new ScheduledAlertService.AlertRequest("PUSH", "Test", "Body", "https://example.com", Instant.now().plusSeconds(3600).toString())));
    }

    @Test @TestTransaction
    void queuesEachPushSubscriptionOnceWhenDue() {
        String userId = "push-alert-user-" + UUID.randomUUID();
        UserEntity user = new UserEntity();
        user.id = userId;
        user.displayName = "Push Alert User";
        user.nickname = "@push-alert-" + UUID.randomUUID().toString().substring(0, 8);
        user.profileEmoji = "😀";
        user.createdAt = Instant.now().minusSeconds(3600).toString();
        user.persist();
        PushSubscriptionEntity subscription = new PushSubscriptionEntity();
        subscription.id = UUID.randomUUID().toString();
        subscription.userId = userId;
        subscription.endpoint = "https://example.test/push/" + subscription.id;
        subscription.p256dh = "p256dh";
        subscription.auth = "auth";
        subscription.createdAt = Instant.now().toString();
        subscription.updatedAt = subscription.createdAt;
        subscription.persist();

        AppUser admin = new AppUser("admin", null, "Admin", "@admin", "😀", null, false,
            new AdminPrivileges(true, true, true, true, true));
        var created = service.create(admin, new ScheduledAlertService.AlertRequest("PUSH", "Titolo", "Messaggio", null,
            Instant.now().plusSeconds(3600).toString()));
        ScheduledAlertEntity row = ScheduledAlertEntity.findById(created.id());
        row.scheduledAt = Instant.now().minusSeconds(1).toString();
        service.dispatchDuePushes();
        service.dispatchDuePushes();
        assertNotNull(row.dispatchedAt);
        assertEquals(1, PushCampaignDeliveryEntity.count("campaign = ?1 and subscriptionId = ?2", "ALERT_" + row.id, subscription.id));
    }
}
