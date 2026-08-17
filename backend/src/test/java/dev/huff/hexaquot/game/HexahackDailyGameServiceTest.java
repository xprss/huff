package dev.huff.hexaquot.game;

import dev.huff.hexaquot.auth.AppUser;
import dev.huff.hexaquot.game.HexahackDtos.ProbeRequest;
import dev.huff.hexaquot.game.HexahackDtos.ProbeType;
import dev.huff.hexaquot.game.HexahackDtos.Rank;
import dev.huff.hexaquot.game.HexahackDtos.Status;
import dev.huff.hexaquot.game.HexahackDtos.SubmissionRequest;
import dev.huff.hexaquot.persistence.HexahackGameEntity;
import dev.huff.hexaquot.persistence.UserEntity;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

@QuarkusTest
class HexahackDailyGameServiceTest {
    @Inject HexahackDailyGameService service;
    @Inject HexahackDailyGameProvider provider;
    @Inject DailyGameService clock;

    @Test @TestTransaction
    void actionsAreIdempotentAndPersistAcrossReads() {
        AppUser user = user();
        assertNull(service.today(user).game());

        ProbeRequest probe = new ProbeRequest("same-probe", ProbeType.BIT_SCAN, 1, null, null);
        assertFalse(service.probe(user, probe).replayed());
        var probeReplay = service.probe(user, probe);
        assertEquals(true, probeReplay.replayed());
        assertEquals(1, probeReplay.game().totalCost());
        assertEquals(1, probeReplay.game().log().size());

        SubmissionRequest wrong = new SubmissionRequest("same-wrong", impossibleWrongCode());
        service.submit(user, wrong);
        var wrongReplay = service.submit(user, wrong);
        assertEquals(true, wrongReplay.replayed());
        assertEquals(1, wrongReplay.game().wrongSubmissions());
        assertNull(wrongReplay.game().solution());

        String solution = provider.solutionFor(clock.todayDate());
        var completed = service.submit(user, new SubmissionRequest("win", solution));
        assertEquals(Status.COMPLETED, completed.game().status());
        assertEquals(Rank.SHADOW, completed.game().rank());
        assertEquals(solution, completed.game().solution());
        assertEquals(93, completed.game().finalStealth());
        assertEquals(3, service.today(user).game().log().size());
        assertEquals(1, HexahackGameEntity.count("userId", user.id()));
        var stats = service.stats(user);
        assertEquals(1, stats.completedAccesses());
        assertEquals(93.0, stats.averageStealth());
        assertEquals(93, stats.bestStealth());
        assertEquals(1, stats.rankDistribution().get(Rank.SHADOW));
        assertEquals(1, stats.currentStreak());
        assertEquals(30, stats.last30Nodes().size());
    }

    @Test
    void calculatesAllStealthAndRankBoundaries() {
        assertEquals(100, HexahackDailyGameService.stealth(0, 0));
        assertEquals(70, HexahackDailyGameService.stealth(15, 0));
        assertEquals(-5, HexahackDailyGameService.stealth(40, 5));
        assertEquals(Rank.GHOST, HexahackDailyGameService.rank(70, 0));
        assertEquals(Rank.SHADOW, HexahackDailyGameService.rank(69, 0));
        assertEquals(Rank.SHADOW, HexahackDailyGameService.rank(55, 1));
        assertEquals(Rank.BREACH, HexahackDailyGameService.rank(35, 2));
        assertEquals(Rank.TRACED, HexahackDailyGameService.rank(34, 0));
    }

    @Test @TestTransaction
    void keepsNegativeStealthAsTheBestScoreWhenThereAreNoOtherGames() {
        AppUser user = user();
        for (int index = 0; index < 51; index++) {
            service.probe(user, new ProbeRequest("negative-" + index, ProbeType.BIT_SCAN, 1, null, null));
        }
        service.submit(user, new SubmissionRequest("negative-win", provider.solutionFor(clock.todayDate())));

        var stats = service.stats(user);
        assertEquals(-2.0, stats.averageStealth());
        assertEquals(-2, stats.bestStealth());
    }

    private AppUser user() {
        UserEntity entity = new UserEntity();
        entity.id = "hexahack-test-" + UUID.randomUUID();
        entity.displayName = "Hack";
        entity.nickname = "@hack-" + UUID.randomUUID().toString().substring(0, 8);
        entity.profileEmoji = "🕶️";
        entity.createdAt = Instant.now().toString();
        entity.starAvailable = false;
        entity.persist();
        return entity.toAppUser(false);
    }

    private String impossibleWrongCode() {
        String solution = provider.solutionFor(clock.todayDate());
        char replacement = solution.charAt(0) == '0' ? '1' : '0';
        return replacement + solution.substring(1);
    }
}
