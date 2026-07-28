package dev.huff.hexaquot.push;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.huff.hexaquot.game.DailyGameService;
import dev.huff.hexaquot.persistence.PushSubscriptionEntity;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
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

    private void sendNewGameNotification(PushSubscriptionEntity subscription, String puzzleDate) {
        try {
            String payload = objectMapper.writeValueAsString(new NotificationPayload(
                "new-game",
                "Nuova partita disponibile",
                "La sfida di oggi ti aspetta su HexaQuot.",
                "/",
                "/icons/huff-icon.svg",
                "new-game-" + puzzleDate
            ));
            Notification notification = new Notification(
                subscription.endpoint,
                subscription.p256dh,
                subscription.auth,
                payload.getBytes(StandardCharsets.UTF_8),
                NEW_GAME_PUSH_TTL_SECONDS
            );
            HttpResponse response = service().send(notification);
            int status = response.getStatusLine().getStatusCode();
            if (status >= 200 && status < 300) {
                subscription.lastNotifiedPuzzleDate = puzzleDate;
                subscription.updatedAt = Instant.now().toString();
            } else if (status == 404 || status == 410) {
                subscription.delete();
            } else {
                LOG.warnf("Push notification failed for subscription %s with status %d", subscription.id, status);
            }
        } catch (Exception error) {
            LOG.warnf(error, "Push notification failed for subscription %s", subscription.id);
        }
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
