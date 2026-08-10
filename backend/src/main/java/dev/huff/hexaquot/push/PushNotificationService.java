package dev.huff.hexaquot.push;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.huff.hexaquot.game.DailyGameService;
import dev.huff.hexaquot.persistence.PushSubscriptionEntity;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import nl.martijndwars.webpush.Encoding;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import org.apache.http.HttpResponse;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.Security;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class PushNotificationService {
    private static final Logger LOG = Logger.getLogger(PushNotificationService.class);
    private static final int NEW_GAME_PUSH_TTL_SECONDS = 24 * 60 * 60;
    private static final int DAILY_REMINDER_PUSH_TTL_SECONDS = 60 * 60;

    @ConfigProperty(name = "app.push.vapid.public-key")
    Optional<String> vapidPublicKey;

    @ConfigProperty(name = "app.push.vapid.private-key")
    Optional<String> vapidPrivateKey;

    @ConfigProperty(name = "app.push.vapid.subject")
    String vapidSubject;

    @Inject
    DailyGameService dailyGameService;

    @Inject
    ObjectMapper objectMapper;

    volatile PushService pushService;

    public PushSettingsDto settings() {
        boolean supported = configured();
        return new PushSettingsDto(supported, supported ? vapidPublicKey.orElse(null) : null);
    }

    @Transactional
    public void subscribe(String userId, PushSubscriptionRequest request) {
        validate(request);
        String now = Instant.now().toString();
        PushSubscriptionEntity entity = PushSubscriptionEntity.find("endpoint", request.endpoint()).firstResult();
        if (entity == null) {
            entity = new PushSubscriptionEntity();
            entity.id = UUID.randomUUID().toString();
            entity.endpoint = request.endpoint();
            entity.createdAt = now;
        }
        entity.userId = userId;
        entity.p256dh = request.keys().p256dh();
        entity.auth = request.keys().auth();
        entity.updatedAt = now;
        entity.persist();
    }

    @Transactional
    public void unsubscribe(String userId, PushSubscriptionRequest request) {
        if (request == null || request.endpoint() == null || request.endpoint().isBlank()) {
            return;
        }
        PushSubscriptionEntity.delete("userId = ?1 and endpoint = ?2", userId, request.endpoint());
    }

    @Scheduled(cron = "{app.push.new-game.cron}", timeZone = "{app.game.timezone}")
    void notifyNewGame() {
        if (!configured()) {
            LOG.debug("Skipping new game push notifications because VAPID is not configured");
            return;
        }
        notifyNewGame(dailyGameService.todayDate());
    }

    @Scheduled(cron = "{app.push.daily-reminder.cron}", timeZone = "{app.game.timezone}")
    void remindUnplayedDailyGame() {
        if (!configured()) {
            LOG.debug("Skipping daily reminder push notifications because VAPID is not configured");
            return;
        }
        remindUnplayedDailyGame(dailyGameService.todayDate());
    }

    @Transactional
    public void notifyNewGame(String puzzleDate) {
        if (!configured()) {
            return;
        }

        List<PushSubscriptionEntity> subscriptions = PushSubscriptionEntity.<PushSubscriptionEntity>list(
            "lastNotifiedPuzzleDate is null or lastNotifiedPuzzleDate <> ?1",
            puzzleDate
        );
        for (PushSubscriptionEntity subscription : subscriptions) {
            sendNewGameNotification(subscription, puzzleDate);
        }
    }

    @Transactional
    public void remindUnplayedDailyGame(String puzzleDate) {
        if (!configured()) {
            return;
        }

        List<PushSubscriptionEntity> subscriptions = findSubscriptionsForDailyReminder(puzzleDate);

        for (PushSubscriptionEntity subscription : subscriptions) {
            sendDailyReminderNotification(subscription, puzzleDate);
        }
    }

    List<PushSubscriptionEntity> findSubscriptionsForDailyReminder(String puzzleDate) {
        return PushSubscriptionEntity.getEntityManager()
            .createQuery("""
                SELECT subscription
                FROM PushSubscriptionEntity subscription
                WHERE (subscription.lastRemindedPuzzleDate IS NULL OR subscription.lastRemindedPuzzleDate <> :puzzleDate)
                  AND NOT EXISTS (
                    SELECT game.id
                    FROM GameEntity game
                    WHERE game.userId = subscription.userId
                      AND game.puzzleDate = :puzzleDate
                      AND game.guessesJson <> '[]'
                  )
                """, PushSubscriptionEntity.class)
            .setParameter("puzzleDate", puzzleDate)
            .getResultList();
    }

    private void sendNewGameNotification(PushSubscriptionEntity subscription, String puzzleDate) {
        NotificationPayload payload = new NotificationPayload(
            "new-game",
            "Nuova partita disponibile",
            "La sfida di oggi ti aspetta su HexaQuot.",
            "/",
            "/icons/huff-icon.svg",
            "new-game-" + puzzleDate
        );
        if (sendNotification(subscription, payload, NEW_GAME_PUSH_TTL_SECONDS)) {
            subscription.lastNotifiedPuzzleDate = puzzleDate;
            subscription.updatedAt = Instant.now().toString();
        }
    }

    private void sendDailyReminderNotification(PushSubscriptionEntity subscription, String puzzleDate) {
        NotificationPayload payload = new NotificationPayload(
            "daily-reminder",
            "HexaQuot ti aspetta",
            "Hai ancora tempo per giocare la partita di oggi.",
            "/",
            "/icons/huff-icon.svg",
            "daily-reminder-" + puzzleDate
        );
        if (sendNotification(subscription, payload, DAILY_REMINDER_PUSH_TTL_SECONDS)) {
            subscription.lastRemindedPuzzleDate = puzzleDate;
            subscription.updatedAt = Instant.now().toString();
        }
    }

    private boolean sendNotification(PushSubscriptionEntity subscription, NotificationPayload payload, int ttlSeconds) {
        try {
            String payloadJson = objectMapper.writeValueAsString(payload);
            PushService pushService = service();
            Notification notification = new Notification(
                subscription.endpoint,
                subscription.p256dh,
                subscription.auth,
                payloadJson.getBytes(StandardCharsets.UTF_8),
                ttlSeconds
            );
            HttpResponse response = pushService.send(notification, Encoding.AES128GCM);
            int status = response.getStatusLine().getStatusCode();
            if (status >= 200 && status < 300) {
                return true;
            }
            if (status == 404 || status == 410) {
                subscription.delete();
            } else {
                LOG.warnf("Push notification failed for subscription %s with status %d", subscription.id, status);
            }
        } catch (Exception error) {
            LOG.warnf(error, "Push notification failed for subscription %s", subscription.id);
        }
        return false;
    }

    private PushService service() throws GeneralSecurityException {
        PushService current = pushService;
        if (current != null) {
            return current;
        }
        synchronized (this) {
            if (pushService == null) {
                if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
                    Security.addProvider(new BouncyCastleProvider());
                }
                pushService = new PushService(vapidPublicKey.orElseThrow(), vapidPrivateKey.orElseThrow(), vapidSubject);
            }
            return pushService;
        }
    }

    private boolean configured() {
        return vapidPublicKey.filter(this::hasText).isPresent() &&
            vapidPrivateKey.filter(this::hasText).isPresent() &&
            hasText(vapidSubject);
    }

    private void validate(PushSubscriptionRequest request) {
        if (!configured()) {
            throw new BadRequestException("Notifiche push non configurate.");
        }
        if (
            request == null ||
            !hasText(request.endpoint()) ||
            request.keys() == null ||
            !hasText(request.keys().p256dh()) ||
            !hasText(request.keys().auth())
        ) {
            throw new BadRequestException("Subscription push non valida.");
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    record NotificationPayload(String type, String title, String body, String url, String icon, String tag) {
        NotificationPayload {
            Objects.requireNonNull(type, "type");
            Objects.requireNonNull(title, "title");
            Objects.requireNonNull(body, "body");
            Objects.requireNonNull(url, "url");
            Objects.requireNonNull(icon, "icon");
            Objects.requireNonNull(tag, "tag");
        }
    }
}
