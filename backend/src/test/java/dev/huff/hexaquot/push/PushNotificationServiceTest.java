package dev.huff.hexaquot.push;

import dev.huff.hexaquot.game.GameMode;
import dev.huff.hexaquot.game.GameRecord;
import dev.huff.hexaquot.game.GameRepository;
import dev.huff.hexaquot.game.GameStatus;
import dev.huff.hexaquot.persistence.PushSubscriptionEntity;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class PushNotificationServiceTest {
    @Inject
    PushNotificationService pushNotificationService;

    @Inject
    GameRepository gameRepository;

    @Test
    @TestTransaction
    void findsSubscriptionsForUsersThatHaveNotSubmittedTodaysGuess() {
        String puzzleDate = "2099-02-03";
        PushSubscriptionEntity noGame = subscription("reminder-no-game-" + UUID.randomUUID(), puzzleDate, false);
        PushSubscriptionEntity emptyGame = subscription("reminder-empty-game-" + UUID.randomUUID(), puzzleDate, false);
        PushSubscriptionEntity playedGame = subscription("reminder-played-game-" + UUID.randomUUID(), puzzleDate, false);
        subscription("reminder-already-sent-" + UUID.randomUUID(), puzzleDate, true);

        gameRepository.create(emptyGame.userId, puzzleDate, "abbaco", GameMode.CLASSIC);
        GameRecord record = gameRepository.create(playedGame.userId, puzzleDate, "abbaco", GameMode.CLASSIC);
        gameRepository.update(new GameRecord(
            record.id(),
            record.userId(),
            record.puzzleDate(),
            record.mode(),
            record.solution(),
            """
                [{"word":"abbaco","tiles":[]}]
                """,
            GameStatus.WON,
            record.mouseTileIndex(),
            record.mouseRevealed(),
            record.kittenUnlocked(),
            record.kittenUsedAt(),
            record.createdAt(),
            Instant.now().toString(),
            Instant.now().toString()
        ));

        List<String> subscriptionIds = pushNotificationService.findSubscriptionsForDailyReminder(puzzleDate)
            .stream()
            .map(subscription -> subscription.id)
            .toList();

        assertEquals(2, subscriptionIds.size());
        assertTrue(subscriptionIds.contains(noGame.id));
        assertTrue(subscriptionIds.contains(emptyGame.id));
    }

    private PushSubscriptionEntity subscription(String userId, String puzzleDate, boolean alreadyReminded) {
        String now = Instant.now().toString();
        PushSubscriptionEntity entity = new PushSubscriptionEntity();
        entity.id = UUID.randomUUID().toString();
        entity.userId = userId;
        entity.endpoint = "https://example.test/push/" + entity.id;
        entity.p256dh = "p256dh";
        entity.auth = "auth";
        entity.lastRemindedPuzzleDate = alreadyReminded ? puzzleDate : null;
        entity.createdAt = now;
        entity.updatedAt = now;
        entity.persist();
        return entity;
    }
}
