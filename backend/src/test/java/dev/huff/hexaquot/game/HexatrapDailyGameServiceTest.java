package dev.huff.hexaquot.game;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.huff.hexaquot.auth.AdminPrivileges;
import dev.huff.hexaquot.auth.AppUser;
import dev.huff.hexaquot.game.HexatrapDtos.*;
import dev.huff.hexaquot.game.HexatrapDtos.StatsDto;
import dev.huff.hexaquot.game.HexatrapDtos.GameDto;
import dev.huff.hexaquot.game.hexatrap.HexatrapRules;
import dev.huff.hexaquot.leaderboard.LeaderboardRepository;
import dev.huff.hexaquot.leaderboard.LeaderboardService;
import dev.huff.hexaquot.persistence.HexatrapActionEntity;
import dev.huff.hexaquot.persistence.HexatrapGameEntity;
import dev.huff.hexaquot.persistence.UserEntity;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class HexatrapDailyGameServiceTest {
    @Inject HexatrapDailyGameService service;
    @Inject HexatrapCaseStore store;
    @Inject DailyGameService clock;
    @Inject OverallStatsService overall;
    @Inject LeaderboardService leaderboard;
    @Inject ObjectMapper mapper;

    private String today;
    private int difficulty;

    @BeforeEach
    void installTodaysCase() {
        today = clock.todayDate();
        HexatrapTestCases.install(store, today);
        difficulty = store.ready(today).orElseThrow().difficulty();
    }

    @Test @TestTransaction
    void investigatesWithinTheBudgetAndSolvesTheCase() throws Exception {
        AppUser user = user();
        TodayDto start = service.today(user);
        assertTrue(start.available());
        assertNull(start.game());
        assertEquals(2, start.rulesVersion());
        assertEquals("La lanterna spenta di Capo Nero", start.caseFile().title());
        assertEquals(60, start.caseFile().budget());
        assertEquals(Set.of("chi", "diario"), start.caseFile().facts().stream().filter(FactDto::verdict).map(FactDto::id).collect(java.util.stream.Collectors.toSet()));
        StatementDto claim = start.caseFile().statements().stream().filter(s -> s.id().equals("s_livia_ettore")).findFirst().orElseThrow();
        assertEquals("Ettore risponderebbe «sì» a: «È stata Livia a spegnere la lanterna?»", claim.record());
        assertEquals("Non è stata Livia a spegnere la lanterna", claim.readings().ifBLies());

        String publicJson = mapper.writeValueAsString(start);
        for (String hidden : List.of("formula", "solution", "epilogue", "Cenere di carta", "cenere di carta", "affirm", "SAYS", "mozzo, terrorizzato"))
            assertFalse(publicJson.contains(hidden), hidden);

        QuestionRequest stufa = new QuestionRequest("q1", today, 2, "q_ettore_stufa");
        ActionDto first = service.question(user, stufa);
        assertEquals(HexatrapRules.QUESTION_COST, first.game().minutesUsed());
        assertTrue(first.game().asked().get(0).answer());
        assertEquals("Il diario è finito nella stufa", first.game().asked().get(0).readings().ifBLies());
        assertNull(first.game().reveal());
        assertTrue(service.question(user, stufa).replayed());
        assertStatus(409, () -> service.question(user, new QuestionRequest("q1-again", today, 2, "q_ettore_stufa")));
        assertStatus(409, () -> service.question(user, new QuestionRequest("q1", today, 2, "q_ettore_undici")));

        ActionDto clue = service.clue(user, new ClueRequest("c1", today, 2, "c_cenere"));
        assertEquals(HexatrapRules.QUESTION_COST + 12, clue.game().minutesUsed());
        assertTrue(clue.game().revealed().get(0).text().contains("cenere di carta"));
        assertEquals("Il diario è finito nella stufa", clue.game().revealed().get(0).record());

        ActionDto undici = service.question(user, new QuestionRequest("q2", today, 2, "q_ettore_undici"));
        assertFalse(undici.game().asked().get(1).answer());
        assertEquals(3, undici.game().sequence());

        VerdictRequest verdict = new VerdictRequest("v", today, 2, "B", Map.of("chi", "mozzo", "diario", "stufa"));
        ActionDto solved = service.verdict(user, verdict);
        GameDto game = solved.game();
        assertEquals("WON", game.status());
        assertEquals("SOLVED", game.outcome());
        int minutesLeft = 60 - (2 * HexatrapRules.QUESTION_COST + 12);
        assertEquals(HexatrapRules.points(difficulty, minutesLeft), game.points());
        RevealDto reveal = game.reveal();
        assertEquals("B", reveal.liar());
        assertTrue(reveal.liarCorrect() && reveal.factsCorrect());
        assertTrue(reveal.proofFound());
        assertTrue(reveal.proof().stream().anyMatch(e -> e.id().equals("q_ettore_undici")), reveal.proof().toString());
        assertEquals(10, reveal.optimalMinutes());
        assertEquals(List.of("q_ettore_stufa", "q_ettore_undici"), reveal.optimalPath().stream().map(EvidenceDto::id).toList());
        assertTrue(reveal.epilogue().startsWith("Il mozzo"));
        assertTrue(reveal.share().contains("Caso risolto"));
        assertFalse(reveal.share().contains("mozzo"));
        assertTrue(service.verdict(user, verdict).replayed());
        assertStatus(409, () -> service.verdict(user, new VerdictRequest("v2", today, 2, "A", Map.of("chi", "mozzo", "diario", "stufa"))));
        assertStatus(409, () -> service.clue(user, new ClueRequest("late", today, 2, "c_maree")));
        assertEquals(game, service.today(user).game());

        StatsDto stats = service.statsForUserId(user.id());
        assertEquals(1, stats.started());
        assertEquals(1, stats.won());
        assertEquals(1, stats.currentStreak());
        assertEquals(game.points(), stats.bestPoints());
        assertEquals(22.0, stats.averageMinutes());
        assertTrue(overall.statsForUserId(user.id()).won() >= 1);
    }

    @Test @TestTransaction
    void judgesPartialAndWrongVerdicts() {
        AppUser partial = user();
        GameDto half = service.verdict(partial, new VerdictRequest("p", today, 2, "B", Map.of("chi", "mozzo", "diario", "cassa"))).game();
        assertEquals("LOST", half.status());
        assertEquals("PARTIAL", half.outcome());
        assertEquals(0, half.points());
        assertTrue(half.reveal().liarCorrect());
        assertFalse(half.reveal().factsCorrect());
        // No investigation: the statements alone could not prove who lies.
        assertFalse(half.reveal().proofFound());
        assertTrue(half.reveal().share().contains("ricostruzione sbagliata"));
        assertEquals(1, service.statsForUserId(partial.id()).partial());

        AppUser wrong = user();
        GameDto lost = service.verdict(wrong, new VerdictRequest("w", today, 2, "A", Map.of("chi", "ettore", "diario", "pozzo"))).game();
        assertEquals("WRONG", lost.outcome());
        assertEquals(0, service.statsForUserId(wrong.id()).currentStreak());
        assertEquals(1, service.statsForUserId(wrong.id()).lost());
    }

    @Test @TestTransaction
    void refusesActionsBeyondTheBudgetButAlwaysAcceptsTheVerdict() {
        AppUser user = user();
        for (String clue : List.of("c_impronte", "c_cenere", "c_maree", "c_vicino")) service.clue(user, new ClueRequest(clue, today, 2, clue));
        service.question(user, new QuestionRequest("a", today, 2, "q_livia_dieci"));
        service.question(user, new QuestionRequest("b", today, 2, "q_livia_cassa"));
        assertEquals(58, service.today(user).game().minutesUsed());
        assertStatus(409, () -> service.question(user, new QuestionRequest("c", today, 2, "q_ettore_stufa")));
        assertEquals(58, service.today(user).game().minutesUsed());
        assertEquals("WON", service.verdict(user, new VerdictRequest("v", today, 2, "B", Map.of("chi", "mozzo", "diario", "stufa"))).game().status());
    }

    @Test @TestTransaction
    void rejectsInvalidAndStaleRequests() {
        AppUser user = user();
        assertStatus(400, () -> service.question(user, new QuestionRequest("x1", today, 2, "q_missing")));
        assertStatus(400, () -> service.clue(user, new ClueRequest("x2", today, 2, null)));
        assertStatus(400, () -> service.verdict(user, new VerdictRequest("x3", today, 2, "C", Map.of("chi", "mozzo", "diario", "stufa"))));
        assertStatus(400, () -> service.verdict(user, new VerdictRequest("x4", today, 2, "B", Map.of("chi", "mozzo"))));
        assertStatus(400, () -> service.verdict(user, new VerdictRequest("x5", today, 2, "B", Map.of("chi", "mozzo", "diario", "tetto"))));
        assertStatus(400, () -> service.question(user, new QuestionRequest("", today, 2, "q_ettore_stufa")));
        assertStatus(400, () -> service.question(user, new QuestionRequest("x6", "2026-02-30", 2, "q_ettore_stufa")));
        String yesterday = LocalDate.parse(today).minusDays(1).toString();
        assertStatus(409, () -> service.question(user, new QuestionRequest("x7", yesterday, 2, "q_ettore_stufa")));
        assertStatus(409, () -> service.question(user, new QuestionRequest("x8", today, 1, "q_ettore_stufa")));
        assertNull(service.today(user).game());
    }

    @Test
    void previewIsReservedToAdminsUntilPublished() {
        HexatrapAccess access = new HexatrapAccess();
        AppUser player = new AppUser("p", null, "P", "@p", null, null, null, false, null);
        AppUser admin = new AppUser("a", null, "A", "@a", null, null, null, false, new AdminPrivileges(false, false, false, false, false, false));
        access.enabled = false;
        assertFalse(access.available(player));
        assertTrue(access.available(admin));
        assertFalse(access.isPublic());
        access.enabled = true;
        assertTrue(access.available(player));
    }

    @Test @TestTransaction
    void leaderboardSortsByWinsThenPointsAndStreaksFollowTheDays() {
        AppUser a = user(), b = user(), c = user();
        won(a, today, 400);
        won(a, LocalDate.parse(today).minusDays(1).toString(), 400);
        won(b, today, 700);
        won(c, today, 900);
        var entries = leaderboard.leaderboards(LeaderboardRepository.Board.HEXATRAP).allTime().entries().stream()
            .filter(e -> Set.of(a.nickname(), b.nickname(), c.nickname()).contains(e.nickname())).toList();
        assertEquals(List.of(a.nickname(), c.nickname(), b.nickname()), entries.stream().map(e -> e.nickname()).toList());
        assertEquals(2, service.statsForUserId(a.id()).currentStreak());
        assertEquals(400, service.statsForUserId(a.id()).bestPoints());
    }

    @Test
    void concurrentDevicesAreSerializedAndARetriedRequestIsChargedOnce() throws Exception {
        AppUser user = QuarkusTransaction.requiringNew().call(this::user);
        var executor = Executors.newFixedThreadPool(4);
        try {
            var gate = new CountDownLatch(1);
            List<Future<ActionDto>> calls = new ArrayList<>();
            List<String> questions = List.of("q_ettore_undici", "q_livia_dieci", "q_livia_cassa", "q_livia_cassa_diretta");
            for (int i = 0; i < 8; i++) {
                final int n = i;
                calls.add(executor.submit(() -> {
                    gate.await();
                    return service.question(user, n < 4
                        ? new QuestionRequest("shared", today, 2, "q_ettore_stufa")
                        : new QuestionRequest("distinct-" + n, today, 2, questions.get(n - 4)));
                }));
            }
            gate.countDown();
            int replayed = 0;
            for (var call : calls) if (call.get(30, TimeUnit.SECONDS).replayed()) replayed++;
            assertEquals(3, replayed);
            var game = service.today(user).game();
            assertEquals(5, game.asked().size());
            assertEquals(5 * HexatrapRules.QUESTION_COST, game.minutesUsed());
            assertEquals(List.of(1, 2, 3, 4, 5), game.asked().stream().map(AskedDto::sequence).toList());
            assertEquals(5L, QuarkusTransaction.requiringNew().call(() -> HexatrapActionEntity.count("userId", user.id())));
        } finally {
            executor.shutdownNow();
            QuarkusTransaction.requiringNew().run(() -> UserEntity.deleteById(user.id()));
        }
    }

    private void won(AppUser user, String date, long points) {
        if (store.ready(date).isEmpty()) HexatrapTestCases.install(store, date);
        HexatrapGameEntity game = new HexatrapGameEntity();
        game.id = UUID.randomUUID().toString();
        game.userId = user.id();
        game.puzzleDate = date;
        game.rulesVersion = 2;
        game.status = "WON";
        game.outcome = "SOLVED";
        game.points = points;
        game.minutesUsed = 20;
        game.verdictJson = "{}";
        game.createdAt = game.updatedAt = game.completedAt = Instant.now().toString();
        game.persist();
    }

    private void assertStatus(int status, Runnable action) {
        assertEquals(status, assertThrows(WebApplicationException.class, action::run).getResponse().getStatus());
    }

    private AppUser user() {
        UserEntity user = new UserEntity();
        user.id = "hexatrap-test-" + UUID.randomUUID();
        user.nickname = "@trap-" + UUID.randomUUID().toString().substring(0, 8);
        user.displayName = "Trap";
        user.createdAt = Instant.now().toString();
        user.starAvailable = false;
        user.persist();
        return user.toAppUser(false);
    }
}
