package dev.huff.hexaquot.game;

import dev.huff.hexaquot.auth.AppUser;
import dev.huff.hexaquot.game.HexaecoDtos.*;
import dev.huff.hexaquot.leaderboard.LeaderboardRepository;
import dev.huff.hexaquot.persistence.HexaecoGameEntity;
import dev.huff.hexaquot.persistence.UserEntity;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.WebApplicationException;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class HexaecoDailyGameServiceTest {
    @Inject HexaecoDailyGameService service;
    @Inject OverallStatsService overall;
    @Inject LeaderboardRepository leaderboard;

    @Test @TestTransaction
    void completionPersistsAndRetriesCountOnlyOnceInAllStatsAndBoards() {
        AppUser user = user();
        var today = service.today(user);
        assertNull(today.game());
        var moves = HexaecoRulesTest.shortestSolution(today.board());
        var request = new SubmitRequest("win", today.puzzleDate(), today.rulesVersion(), moves);
        var action = service.submit(user, request);
        assertFalse(action.replayed());
        assertEquals("WON", action.game().status());
        assertEquals(moves, service.today(user).game().moves());
        assertTrue(service.submit(user, request).replayed());
        assertTrue(service.submit(user, new SubmitRequest("other-device", today.puzzleDate(), 1, moves)).replayed());
        assertEquals(1, HexaecoGameEntity.count("userId", user.id()));
        assertEquals(1, service.statsForUserId(user.id()).completed());
        assertEquals(1, service.statsForUserId(user.id()).currentStreak());
        assertEquals(1, overall.statsForUserId(user.id()).won());
        for (var board : List.of(LeaderboardRepository.Board.HEXAECO, LeaderboardRepository.Board.OVERALL)) {
            assertEquals(1, leaderboard.winnerScores(board, today.puzzleDate(), LocalDate.parse(today.puzzleDate()).plusDays(1).toString())
                .stream().filter(score -> score.user().id.equals(user.id())).findFirst().orElseThrow().wins());
        }
        assertNull(service.today(user()).game());
    }

    @Test @TestTransaction
    void invalidAndStaleSubmissionsNeverCreateGames() {
        AppUser user = user();
        var today = service.today(user);
        assertThrows(BadRequestException.class, () -> service.submit(user, null));
        assertThrows(BadRequestException.class, () -> service.submit(user, new SubmitRequest("bad", today.puzzleDate(), 1, List.of(Command.WAIT))));
        assertThrows(BadRequestException.class, () -> service.submit(user, new SubmitRequest("bad", today.puzzleDate(), 1, Arrays.asList((Command) null))));
        assertThrows(BadRequestException.class, () -> service.submit(user, new SubmitRequest("bad", "2026-99-99", 1, List.of(Command.WAIT))));
        assertThrows(BadRequestException.class, () -> service.submit(user, new SubmitRequest("bad", today.puzzleDate(), 1, Collections.nCopies(HexaecoRules.MAX_MOVES + 1, Command.WAIT))));
        var stale = assertThrows(WebApplicationException.class, () -> service.submit(user,
            new SubmitRequest("stale", LocalDate.parse(today.puzzleDate()).minusDays(1).toString(), 1, List.of(Command.WAIT))));
        assertEquals(409, stale.getResponse().getStatus());
        var version = assertThrows(WebApplicationException.class, () -> service.submit(user,
            new SubmitRequest("version", today.puzzleDate(), 2, List.of(Command.WAIT))));
        assertEquals(409, version.getResponse().getStatus());
        assertEquals(0, HexaecoGameEntity.count("userId", user.id()));
    }

    @Test @TestTransaction
    void expiredStreakIsZeroWhileRecordIsRetained() {
        AppUser user = user();
        var today = service.today(user);
        service.submit(user, new SubmitRequest("win", today.puzzleDate(), 1, HexaecoRulesTest.shortestSolution(today.board())));
        HexaecoGameEntity game = HexaecoGameEntity.<HexaecoGameEntity>find("userId", user.id()).firstResult();
        game.puzzleDate = LocalDate.parse(today.puzzleDate()).minusDays(3).toString();
        assertEquals(0, service.statsForUserId(user.id()).currentStreak());
        assertEquals(1, service.statsForUserId(user.id()).maxStreak());
    }

    private AppUser user() {
        UserEntity entity = new UserEntity();
        entity.id = "hexaeco-test-" + UUID.randomUUID();
        entity.displayName = "Eco";
        entity.nickname = "@eco-" + UUID.randomUUID().toString().substring(0, 8);
        entity.profileEmoji = "✨";
        entity.createdAt = Instant.now().toString();
        entity.starAvailable = false;
        entity.persist();
        return entity.toAppUser(false);
    }
}
