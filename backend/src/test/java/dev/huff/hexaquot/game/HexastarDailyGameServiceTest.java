package dev.huff.hexaquot.game;

import dev.huff.hexaquot.auth.AppUser;
import dev.huff.hexaquot.game.HexastarDailyGameProvider.WordEntry;
import dev.huff.hexaquot.game.HexastarDtos.GuessRequest;
import dev.huff.hexaquot.game.HexastarDtos.Status;
import dev.huff.hexaquot.persistence.HexastarGameEntity;
import dev.huff.hexaquot.persistence.UserEntity;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class HexastarDailyGameServiceTest {
    @Inject HexastarDailyGameService service;
    @Inject HexastarDailyGameProvider provider;
    @Inject DailyGameService clock;

    @Test
    @TestTransaction
    void invalidGuessesDoNotCreateOrConsumeAGame() {
        AppUser user = createUser();
        var today = service.today(user);
        assertEquals(HexastarDailyGameProvider.MAX_ATTEMPTS, today.maxAttempts());
        assertNull(today.game());

        assertThrows(BadRequestException.class, () -> service.guess(user,
            new GuessRequest("invalid-guess-1", List.of("xx"))));
        assertEquals(0, HexastarGameEntity.count("userId", user.id()));
        assertNull(service.today(user).game());
    }

    @Test
    @TestTransaction
    void storesValidAttemptsRejectsDuplicatesAndReplaysRequestIds() {
        AppUser user = createUser();
        WordEntry solution = provider.solutionFor(clock.todayDate());
        WordEntry wrong = wrongEntries(solution, 1).get(0);

        var first = service.guess(user, new GuessRequest("request-first", wrong.syllables()));
        assertFalse(first.replayed());
        assertEquals(1, first.game().attempts().size());
        assertNull(first.game().solutionSyllables());

        var replay = service.guess(user, new GuessRequest("request-first", wrong.syllables()));
        assertTrue(replay.replayed());
        assertEquals(first.attempt(), replay.attempt());
        assertEquals(1, replay.game().attempts().size());

        assertThrows(BadRequestException.class, () -> service.guess(user,
            new GuessRequest("request-duplicate", wrong.syllables())));
        assertEquals(1, service.today(user).game().attempts().size());
    }

    @Test
    @TestTransaction
    void winsAndRevealsThePersistedCanonicalSyllables() {
        AppUser user = createUser();
        WordEntry solution = provider.solutionFor(clock.todayDate());
        var action = service.guess(user, new GuessRequest("request-winning", solution.syllables()));

        assertEquals(Status.WON, action.game().status());
        assertEquals(solution.syllables(), action.game().solutionSyllables());
        assertEquals(1, service.stats(user).won());
        assertEquals(1, service.stats(user).guessDistribution().get(1));
    }

    @Test
    @TestTransaction
    void losesOnTheSixthDistinctValidAttemptAndThenRevealsTheSolution() {
        AppUser user = createUser();
        WordEntry solution = provider.solutionFor(clock.todayDate());
        List<WordEntry> wrong = wrongEntries(solution, HexastarDailyGameProvider.MAX_ATTEMPTS);

        for (int index = 0; index < wrong.size(); index++) {
            var action = service.guess(user, new GuessRequest("request-loss-" + index, wrong.get(index).syllables()));
            if (index < HexastarDailyGameProvider.MAX_ATTEMPTS - 1) {
                assertEquals(Status.IN_PROGRESS, action.game().status());
                assertNull(action.game().solutionSyllables());
            } else {
                assertEquals(Status.LOST, action.game().status());
                assertEquals(solution.syllables(), action.game().solutionSyllables());
            }
        }
        assertEquals(1, service.stats(user).lost());
    }

    private List<WordEntry> wrongEntries(WordEntry solution, int count) {
        List<WordEntry> entries = provider.pool().stream()
            .filter(entry -> !entry.word().equals(solution.word()))
            .filter(entry -> entry.lengths().equals(solution.lengths()))
            .limit(count)
            .toList();
        assertEquals(count, entries.size());
        return entries;
    }

    private AppUser createUser() {
        String suffix = UUID.randomUUID().toString();
        UserEntity entity = new UserEntity();
        entity.id = "hexastar-" + suffix;
        entity.displayName = "Hexastar";
        entity.nickname = "@hexastar-" + suffix.substring(0, 8);
        entity.profileEmoji = "✨";
        entity.createdAt = Instant.now().toString();
        entity.starAvailable = false;
        entity.persist();
        return entity.toAppUser(false);
    }
}
