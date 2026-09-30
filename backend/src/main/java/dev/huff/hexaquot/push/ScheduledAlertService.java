package dev.huff.hexaquot.push;

import dev.huff.hexaquot.auth.AppUser;
import dev.huff.hexaquot.persistence.PushCampaignDeliveryEntity;
import dev.huff.hexaquot.persistence.PushSubscriptionEntity;
import dev.huff.hexaquot.persistence.ScheduledAlertEntity;
import dev.huff.hexaquot.persistence.SeenAlertEntity;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class ScheduledAlertService {
    private static final String PUSH = "PUSH";
    private static final String MODAL = "MODAL";
    private static final String CAMPAIGN_PREFIX = "ALERT_";

    @Inject PushNotificationService pushNotificationService;

    public record AlertDto(String id, String type, String title, String body, String url,
                           String scheduledAt, String createdAt, String dispatchedAt) {
        static AlertDto from(ScheduledAlertEntity row) {
            return new AlertDto(row.id, row.type, row.title, row.body, row.url,
                row.scheduledAt, row.createdAt, row.dispatchedAt);
        }
    }

    public record AlertRequest(String type, String title, String body, String url, String scheduledAt) {}

    public List<AlertDto> list(AppUser admin) {
        requireAdmin(admin);
        return ScheduledAlertEntity.<ScheduledAlertEntity>list("order by scheduledAt desc")
            .stream().map(AlertDto::from).toList();
    }

    public List<AlertDto> pendingModals(String userId) {
        String now = Instant.now().toString();
        return ScheduledAlertEntity.getEntityManager().createQuery("""
            SELECT alert FROM ScheduledAlertEntity alert, UserEntity recipient
            WHERE recipient.id = :userId AND recipient.createdAt <= alert.scheduledAt
              AND alert.type = :type AND alert.scheduledAt <= :now
              AND NOT EXISTS (SELECT seen.alertId FROM SeenAlertEntity seen
                WHERE seen.userId = :userId AND seen.alertId = alert.id)
            ORDER BY alert.scheduledAt
            """, ScheduledAlertEntity.class)
            .setParameter("type", MODAL).setParameter("now", now).setParameter("userId", userId)
            .getResultList().stream().map(AlertDto::from).toList();
    }

    @Transactional
    public AlertDto create(AppUser admin, AlertRequest request) {
        requireAdmin(admin);
        validate(request);
        ScheduledAlertEntity row = new ScheduledAlertEntity();
        row.id = UUID.randomUUID().toString();
        copy(row, request);
        row.createdAt = Instant.now().toString();
        row.persist();
        return AlertDto.from(row);
    }

    @Transactional
    public AlertDto update(AppUser admin, String id, AlertRequest request) {
        requireAdmin(admin);
        ScheduledAlertEntity row = editable(id);
        validate(request);
        copy(row, request);
        return AlertDto.from(row);
    }

    @Transactional
    public void delete(AppUser admin, String id) {
        requireAdmin(admin);
        editable(id).delete();
    }

    @Transactional
    public void markSeen(String userId, String id) {
        ScheduledAlertEntity row = ScheduledAlertEntity.findById(id);
        dev.huff.hexaquot.persistence.UserEntity recipient = dev.huff.hexaquot.persistence.UserEntity.findById(userId);
        if (row == null || !MODAL.equals(row.type) || row.scheduledAt.compareTo(Instant.now().toString()) > 0
            || recipient == null || recipient.createdAt.compareTo(row.scheduledAt) > 0) {
            throw new NotFoundException("Alert non trovato.");
        }
        if (SeenAlertEntity.findById(new SeenAlertEntity.Key(userId, id)) != null) return;
        SeenAlertEntity seen = new SeenAlertEntity();
        seen.userId = userId;
        seen.alertId = id;
        seen.seenAt = Instant.now().toString();
        seen.persist();
    }

    @Scheduled(every = "1m", delayed = "5s")
    @Transactional
    void dispatchDuePushes() {
        String now = Instant.now().toString();
        for (ScheduledAlertEntity alert : ScheduledAlertEntity.<ScheduledAlertEntity>list(
            "type = ?1 and scheduledAt <= ?2 and dispatchedAt is null order by scheduledAt", PUSH, now)) {
            String campaign = CAMPAIGN_PREFIX + alert.id;
            for (PushSubscriptionEntity subscription : PushSubscriptionEntity.<PushSubscriptionEntity>listAll()) {
                PushCampaignDeliveryEntity delivery = new PushCampaignDeliveryEntity();
                delivery.id = UUID.randomUUID().toString();
                delivery.subscriptionId = subscription.id;
                delivery.campaign = campaign;
                delivery.createdAt = now;
                delivery.attempts = 0;
                delivery.persist();
            }
            alert.dispatchedAt = now;
        }
        for (ScheduledAlertEntity alert : ScheduledAlertEntity.<ScheduledAlertEntity>list(
            "type = ?1 and dispatchedAt is not null and scheduledAt > ?2 and scheduledAt <= ?3",
            PUSH, Instant.now().minusSeconds(7 * 24 * 60 * 60).toString(), now)) {
            pushNotificationService.dispatchScheduledAlert(alert);
        }
    }

    private ScheduledAlertEntity editable(String id) {
        ScheduledAlertEntity row = ScheduledAlertEntity.findById(id);
        if (row == null) throw new NotFoundException("Alert non trovato.");
        if (row.scheduledAt.compareTo(Instant.now().toString()) <= 0 || row.dispatchedAt != null) {
            throw new BadRequestException("Un messaggio passato non può essere modificato.");
        }
        return row;
    }

    private void copy(ScheduledAlertEntity row, AlertRequest request) {
        row.type = request.type();
        row.title = request.title().trim();
        row.body = request.body().trim();
        row.url = request.url() == null || request.url().isBlank() ? null : request.url().trim();
        row.scheduledAt = Instant.parse(request.scheduledAt()).toString();
    }

    private void validate(AlertRequest request) {
        if (request == null || (!PUSH.equals(request.type()) && !MODAL.equals(request.type()))
            || request.title() == null || request.title().isBlank() || request.title().trim().length() > 120
            || request.body() == null || request.body().isBlank() || request.body().trim().length() > 1000
            || request.url() != null && (!request.url().isBlank() && (!request.url().startsWith("/#/") || request.url().length() > 255))) {
            throw new BadRequestException("Contenuto alert non valido.");
        }
        try {
            if (!Instant.parse(request.scheduledAt()).isAfter(Instant.now())) {
                throw new BadRequestException("Seleziona una data futura.");
            }
        } catch (NullPointerException | DateTimeParseException error) {
            throw new BadRequestException("Data alert non valida.");
        }
    }

    private void requireAdmin(AppUser user) {
        if (user == null || user.admin() == null) throw new ForbiddenException("Privilegio admin richiesto.");
    }
}
