package dev.huff.hexaquot.game;

import dev.huff.hexaquot.auth.AppUser;
import dev.huff.hexaquot.game.HexaskyDtos.*;
import dev.huff.hexaquot.persistence.HexaskyGameEntity;
import dev.huff.hexaquot.persistence.UserEntity;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class HexaskyDailyGameServiceTest {
    @Inject HexaskyDailyGameService service;
    @Inject HexaskyDailyGameProvider provider;
    @Inject DailyGameService clock;

    @Test @TestTransaction
    void firstWrongCheckHighlightsIncorrectCellsSecondRevealsAndRequestIdsReplay() {
        AppUser user = user();
        List<Integer> wrong = differentLatin(provider.solutionFor(clock.todayDate()));
        CheckRequest first = new CheckRequest("first", wrong);
        CheckActionDto firstResult = service.check(user, first);
        assertEquals(Status.IN_PROGRESS, firstResult.game().status());
        assertFalse(firstResult.result().correct());
        assertNull(firstResult.result().solution());
        assertEquals(mismatchedCells(wrong, provider.solutionFor(clock.todayDate())), firstResult.result().incorrectCells());
        assertTrue(service.check(user, first).replayed());
        assertEquals(1, service.today(user).game().checksUsed());

        CheckActionDto last = service.check(user, new CheckRequest("second", wrong));
        assertEquals(Status.LOST, last.game().status());
        assertEquals(provider.solutionFor(clock.todayDate()), last.result().solution());
        assertFalse(last.result().incorrectCells().isEmpty());
        assertEquals(1, HexaskyGameEntity.count("userId", user.id()));
        var stats = service.stats(user);
        assertEquals(1, stats.played()); assertEquals(0, stats.won()); assertEquals(1, stats.lost());
    }

    @Test @TestTransaction
    void correctFirstCheckWinsAndCountsTheOneCheckDistribution() {
        AppUser user = user(); List<Integer> solution = provider.solutionFor(clock.todayDate());
        CheckActionDto result = service.check(user, new CheckRequest("win", solution));
        assertEquals(Status.WON, result.game().status()); assertTrue(result.result().correct()); assertNull(result.result().solution()); assertTrue(result.result().incorrectCells().isEmpty());
        assertEquals(1, service.stats(user).checkDistribution().get(1));
    }

    private List<Integer> differentLatin(List<Integer> solution) {
        List<Integer> first = List.of(1,2,3,4, 2,3,4,1, 3,4,1,2, 4,1,2,3);
        return first.equals(solution) ? List.of(4,3,2,1, 3,2,1,4, 2,1,4,3, 1,4,3,2) : first;
    }
    private List<Integer> mismatchedCells(List<Integer> proposal, List<Integer> solution) { List<Integer> mismatches=new ArrayList<>(); for(int index=0;index<solution.size();index++)if(!proposal.get(index).equals(solution.get(index)))mismatches.add(index); return mismatches; }
    private AppUser user() { UserEntity entity = new UserEntity(); entity.id = "hexasky-test-" + UUID.randomUUID(); entity.displayName="Sky"; entity.nickname="@sky-"+UUID.randomUUID().toString().substring(0,8); entity.profileEmoji="🏙️"; entity.createdAt=Instant.now().toString(); entity.starAvailable=false; entity.persist(); return entity.toAppUser(false); }
}
