package dev.huff.hexaquot.game;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.huff.hexaquot.auth.AppUser;
import dev.huff.hexaquot.game.HexatrapDtos.*;
import dev.huff.hexaquot.leaderboard.LeaderboardRepository;
import dev.huff.hexaquot.leaderboard.LeaderboardService;
import dev.huff.hexaquot.persistence.HexatrapGameEntity;
import dev.huff.hexaquot.persistence.HexatrapEventEntity;
import dev.huff.hexaquot.persistence.UserEntity;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.narayana.jta.QuarkusTransaction;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.ws.rs.WebApplicationException;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class HexatrapDailyGameServiceTest {
    @Inject HexatrapDailyGameService service;
    @Inject HexatrapDailyGameProvider provider;
    @Inject DailyGameService clock;
    @Inject OverallStatsService overall;
    @Inject LeaderboardService leaderboard;
    @Inject ObjectMapper mapper;
    @Inject EntityManager entityManager;

    @Test @TestTransaction
    void questionsRetriesNegativeScoresPaginationWinAndAllStats() throws Exception {
        var user = user();
        var today = service.today(user);
        var date = today.puzzleDate();
        var puzzle = provider.puzzleFor(date);
        assertNull(today.game());
        assertEquals(0, service.statsForUserId(user.id()).started());
        var firstRequest = question("first", date, puzzle.loyalAdvisor(), 0);
        var first = service.question(user, firstRequest);
        assertNull(first.game().reveal());
        String publicJson = mapper.writeValueAsString(first);
        for (String hidden : List.of("configuration", "loyalAdvisor", "secretNumber", "falseNumber", "hypotheses")) assertFalse(publicJson.contains(hidden));
        assertTrue(service.question(user, firstRequest).replayed());
        assertEquals(first.game(), service.question(user, firstRequest).game());
        for (int seal = 1; seal < 6; seal++) service.question(user, question("seal-"+seal, date, puzzle.loyalAdvisor(), seal));
        assertTrue(service.today(user).game().numberUnlocked());
        assertFalse(service.today(user).game().numberRequested());
        for (int repeat = 0; repeat < 20; repeat++) service.question(user, question("repeat-"+repeat, date, puzzle.loyalAdvisor(), 0));
        assertEquals(-1600, service.today(user).game().points());
        assertEquals(26, service.today(user).game().questions());
        // Replaying an old request returns its original response, without reverting the saved state.
        assertEquals(first.game(), service.question(user, firstRequest).game());
        assertEquals(26, service.today(user).game().questions());
        var liarNumber = new QuestionRequest("liar-number", date, 1, QuestionType.NUMBER, 1-puzzle.loyalAdvisor(), null);
        var lie = service.question(user, liarNumber);
        assertEquals(puzzle.falseNumber(), lie.event().number());
        assertNotEquals(puzzle.secretNumber(), lie.event().number());
        assertEquals(lie.event().number(), service.question(user, new QuestionRequest("liar-again", date, 1, QuestionType.NUMBER, 1-puzzle.loyalAdvisor(), null)).event().number());
        var wrong = new SubmissionRequest("wrong", date, 1, puzzle.falseNumber());
        var incorrect = service.submit(user, wrong);
        assertFalse(incorrect.event().correct());
        assertEquals(1, incorrect.game().wrongSubmissions());
        assertEquals(-2000, incorrect.game().points());
        assertTrue(service.submit(user, wrong).replayed());
        var number = service.question(user, new QuestionRequest("loyal-number", date, 1, QuestionType.NUMBER, puzzle.loyalAdvisor(), null));
        assertEquals(puzzle.secretNumber(), number.event().number());
        assertEquals(29, number.game().questions());
        var winRequest = new SubmissionRequest("win", date, 1, puzzle.secretNumber());
        var win = service.submit(user, winRequest);
        assertEquals("WON", win.game().status());
        assertEquals(-2100, win.game().points());
        assertEquals(puzzle.secretNumber(), win.game().reveal().number());
        assertEquals(puzzle.configuration(), win.game().reveal().configuration());
        assertEquals(puzzle.loyalAdvisor(), win.game().reveal().loyalAdvisor());
        assertTrue(win.game().reveal().explanation().contains("risposta #1"));
        assertTrue(service.submit(user, winRequest).replayed());
        assertEquals(win.game(), service.today(user).game()); // Resume on another device.
        assertNull(service.today(user()).game());
        Set<Long> sequences = new HashSet<>(); Long before = null;
        do {
            var page = service.events(user, date, before, 7);
            for (var event : page.events()) assertTrue(sequences.add(event.sequence()));
            before = page.nextBefore();
        } while (before != null);
        assertEquals(31, sequences.size());
        assertEquals(31, HexatrapEventEntity.count("userId", user.id()));
        var stats = service.statsForUserId(user.id());
        assertEquals(1, stats.started()); assertEquals(1, stats.won()); assertEquals(1, stats.currentStreak());
        assertEquals(-2100, stats.bestPoints()); assertEquals(-2100, stats.averagePoints()); assertEquals(29, stats.averageQuestions());
        assertEquals(1, overall.statsForUserId(user.id()).won());
        assertEquals(1, leaderboard.publicProfile(user.nickname()).hexatrapStats().won());
        for (var board : List.of(LeaderboardRepository.Board.HEXATRAP, LeaderboardRepository.Board.OVERALL)) {
            var entry = leaderboard.leaderboards(board).allTime().entries().stream().filter(e -> e.nickname().equals(user.nickname())).findFirst().orElseThrow();
            assertEquals(1, entry.wins()); assertEquals(board == LeaderboardRepository.Board.HEXATRAP ? -2100L : null, entry.points());
        }
    }

    @Test @TestTransaction
    void shortcutsMalformedActionsAndChangedRequestContentsAreRejectedWithoutCharges() {
        var user = user(); var date = service.today(user).puzzleDate();
        assertStatus(409, () -> service.submit(user, new SubmissionRequest("early", date, 1, 1)));
        assertStatus(409, () -> service.question(user, new QuestionRequest("early-number", date, 1, QuestionType.NUMBER, 0, null)));
        assertStatus(400, () -> service.question(user, question("invalid", date, 2, 0)));
        assertStatus(400, () -> service.question(user, question("invalid", date, 0, 6)));
        assertStatus(400, () -> service.question(user, question("invalid", "2026-99-01", 0, 0)));
        assertStatus(409, () -> service.question(user, new QuestionRequest("version", date, 2, QuestionType.SEAL, 0, 0)));
        assertStatus(409, () -> service.question(user, question("stale", LocalDate.parse(date).minusDays(1).toString(), 0, 0)));
        assertNull(service.today(user).game());
        var first = service.question(user, question("used", date, 0, 0));
        assertStatus(409, () -> service.question(user, question("used", date, 1, 0)));
        assertStatus(409, () -> service.submit(user, new SubmissionRequest("used", date, 1, 7)));
        for (int seal=1; seal<6; seal++) service.question(user, question("seal"+seal,date,0,seal));
        assertTrue(service.today(user).game().numberUnlocked());
        assertStatus(409, () -> service.submit(user, new SubmissionRequest("still-early", date, 1, provider.puzzleFor(date).secretNumber())));
        service.question(user, new QuestionRequest("number",date,1,QuestionType.NUMBER,0,null));
        assertStatus(400, () -> service.submit(user, new SubmissionRequest("invalid-number",date,1,0)));
        assertStatus(400, () -> service.submit(user, new SubmissionRequest("invalid-number",date,1,9865)));
        assertEquals(0, service.today(user).game().wrongSubmissions());
        assertEquals(7, service.today(user).game().questions());
        assertEquals(1, first.game().questions());
    }

    @Test @TestTransaction
    void midnightAllowsOnlyRecoveryAndPersistedPuzzleSurvivesProviderChanges() {
        var user = user(); var today = service.today(user).puzzleDate();
        String yesterday = LocalDate.parse(today).minusDays(1).toString();
        var past = new HexatrapDailyGameService();
        past.clock = new DailyGameService() { @Override public String todayDate() { return yesterday; } };
        past.provider=provider; past.mapper=mapper; past.entityManager=entityManager;
        var request = question("yesterday",yesterday,0,0);
        var original = past.question(user, request);
        assertEquals(original.game(), service.question(user, request).game());
        assertStatus(409, () -> service.question(user, question("new-yesterday",yesterday,0,1)));
        assertNull(service.today(user).game());
        service.question(user, question("today",today,0,0));
        var p = provider.puzzleFor(today);
        var resumed = new HexatrapDailyGameService();
        resumed.clock = clock; resumed.mapper=mapper; resumed.entityManager=entityManager;
        resumed.provider = new HexatrapDailyGameProvider() {
            @Override public Puzzle puzzleFor(String date) { throw new AssertionError("Saved games must use the stored puzzle"); }
        };
        var next = resumed.question(user,question("stored-config",today,p.loyalAdvisor(),1));
        assertEquals(HexatrapRules.lit(p.configuration(),1),next.event().answer());
    }

    @Test
    void concurrentRequestsFromTwoDevicesSerializeAndSameIdIsChargedOnce() throws Exception {
        var user = QuarkusTransaction.requiringNew().call(this::user);
        var executor = Executors.newFixedThreadPool(4);
        try {
            String date = service.today(user).puzzleDate();
            var gate = new CountDownLatch(1);
            var reader = executor.submit(() -> {
                gate.await();
                for (int n=0;n<8;n++) {
                    var snapshot=service.today(user).game();
                    if (snapshot != null) {
                        assertEquals(snapshot.questions(),snapshot.history().events().get(0).sequence());
                        assertEquals(HexatrapRules.points(snapshot.questions(),0),snapshot.points());
                    }
                }
                return true;
            });
            List<Future<ActionDto>> calls = new ArrayList<>();
            for (int i=0;i<8;i++) {
                final int n=i;
                calls.add(executor.submit(() -> { gate.await(); return service.question(user,question(n<4?"shared":"distinct-"+n,date,0,n<4?0:n%6)); }));
            }
            gate.countDown();
            for (var call : calls) assertNotNull(call.get(30,TimeUnit.SECONDS));
            assertTrue(reader.get(30,TimeUnit.SECONDS));
            assertEquals(5,service.today(user).game().questions());
            assertEquals(5,HexatrapEventEntity.count("userId",user.id()));
            assertEquals(1,HexatrapGameEntity.count("userId",user.id()));
        } finally {
            executor.shutdownNow();
            QuarkusTransaction.requiringNew().run(() -> UserEntity.deleteById(user.id()));
        }
    }

    @Test @TestTransaction
    void leaderboardSortsByWinsThenTotalPointsBeforeExistingTies() {
        var a=user(); var b=user(); var c=user();
        String today=service.today(a).puzzleDate();
        won(a,today,-500); won(a,LocalDate.parse(today).minusDays(1).toString(),-500);
        won(b,today,400); won(c,today,600);
        var entries=leaderboard.leaderboards(LeaderboardRepository.Board.HEXATRAP).allTime().entries().stream()
            .filter(e -> Set.of(a.nickname(),b.nickname(),c.nickname()).contains(e.nickname())).toList();
        assertEquals(List.of(a.nickname(),c.nickname(),b.nickname()),entries.stream().map(e -> e.nickname()).toList());
        assertEquals(2,service.statsForUserId(a.id()).currentStreak());
        assertEquals(-500,service.statsForUserId(a.id()).bestPoints());
        HexatrapGameEntity.<HexatrapGameEntity>list("userId",a.id()).forEach(g ->
            g.puzzleDate=LocalDate.parse(g.puzzleDate).minusDays(5).toString());
        assertEquals(0,service.statsForUserId(a.id()).currentStreak());
        assertEquals(2,service.statsForUserId(a.id()).maxStreak());
    }
    private void won(AppUser user,String date,long points) {
        var g=new HexatrapGameEntity();g.id=UUID.randomUUID().toString();g.userId=user.id();g.puzzleDate=date;g.rulesVersion=1;
        var p=provider.puzzleFor(date);g.puzzleJson=serialize(p);g.hypothesesJson=serialize(List.of(p.configuration()*2+p.loyalAdvisor()));
        g.status="WON";g.points=points;g.questions=(1000-points)/100;g.createdAt=g.updatedAt=g.completedAt=Instant.now().toString();g.persist();
    }
    private String serialize(Object value) { try {return mapper.writeValueAsString(value);}catch(Exception e){throw new RuntimeException(e);} }
    private QuestionRequest question(String id,String date,int advisor,int seal) { return new QuestionRequest(id,date,1,QuestionType.SEAL,advisor,seal); }
    private void assertStatus(int status,Runnable action) { assertEquals(status,assertThrows(WebApplicationException.class,action::run).getResponse().getStatus()); }
    private AppUser user() {
        var u=new UserEntity();u.id="hexatrap-test-"+UUID.randomUUID();u.nickname="@trap-"+UUID.randomUUID().toString().substring(0,8);
        u.displayName="Trap";u.profileEmoji="✨";u.createdAt=Instant.now().toString();u.starAvailable=false;u.persist();return u.toAppUser(false);
    }
}
