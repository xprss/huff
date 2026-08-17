package dev.huff.hexaquot.leaderboard;

import dev.huff.hexaquot.game.GameMode;
import dev.huff.hexaquot.game.GameStatus;
import dev.huff.hexaquot.game.HexahackDtos;
import dev.huff.hexaquot.persistence.GameEntity;
import dev.huff.hexaquot.persistence.HexahackGameEntity;
import dev.huff.hexaquot.persistence.UserEntity;
import dev.huff.hexaquot.persistence.WeeklyMedalEntity;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.DayOfWeek;
import java.time.temporal.TemporalAdjusters;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class LeaderboardServiceTest {
    @Inject
    LeaderboardService leaderboardService;

    @Test
    @TestTransaction
    void exposesWeeklyWinnersAndOnlyPublicProfileFields() {
        String suffix = UUID.randomUUID().toString();
        UserEntity first = createUser("leader-first-" + suffix, "@first-" + suffix.substring(0, 8), "Prima");
        UserEntity second = createUser("leader-second-" + suffix, "@second-" + suffix.substring(0, 8), "Seconda");
        LocalDate weekStart = LocalDate.now(ZoneId.of("Europe/Rome"))
            .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        createWonGame(first, weekStart, "2026-08-07T10:00:00Z");
        createWonGame(first, weekStart.plusDays(1), "2026-08-07T11:00:00Z");
        createCompletedHexahackGame(first, weekStart.plusDays(2), "2026-08-07T12:00:00Z");
        createWonGame(second, weekStart, "2026-08-07T09:00:00Z");

        LeaderboardsDto leaderboards = leaderboardService.leaderboards();
        assertTrue(leaderboards.weekly().entries().stream().anyMatch(entry ->
            entry.nickname().equals(first.nickname) && entry.wins() == 2
        ));

        LeaderboardsDto overallLeaderboards = leaderboardService.leaderboards(LeaderboardRepository.Board.OVERALL);
        assertTrue(overallLeaderboards.weekly().entries().stream().anyMatch(entry ->
            entry.nickname().equals(first.nickname) && entry.wins() == 3
        ));

        PublicPlayerProfileDto profile = leaderboardService.publicProfile(first.nickname);
        assertEquals(first.displayName, profile.displayName());
        assertEquals(first.nickname, profile.nickname());
        assertEquals(2, profile.stats().won());
    }

    @Test
    @TestTransaction
    void countsPersistedMedalsByColor() {
        String suffix = UUID.randomUUID().toString();
        UserEntity user = createUser("medal-user-" + suffix, "@medal-" + suffix.substring(0, 8), "Medagliato");
        createMedal(user.id, MedalType.GOLD, 1);
        createMedal(user.id, MedalType.GOLD, 2);
        createMedal(user.id, MedalType.BRONZE, 3);

        MedalCountsDto counts = leaderboardService.medalCounts(user.id);
        assertEquals(2, counts.gold());
        assertEquals(0, counts.silver());
        assertEquals(1, counts.bronze());
    }

    private UserEntity createUser(String id, String nickname, String displayName) {
        UserEntity user = new UserEntity();
        user.id = id;
        user.createdAt = "2026-01-01T00:00:00Z";
        user.displayName = displayName;
        user.nickname = nickname;
        user.profileEmoji = "😀";
        user.starAvailable = false;
        user.persist();
        return user;
    }

    private void createWonGame(UserEntity user, LocalDate date, String completedAt) {
        GameEntity game = new GameEntity();
        game.id = UUID.randomUUID().toString();
        game.userId = user.id;
        game.puzzleDate = date.toString();
        game.mode = GameMode.CLASSIC;
        game.solution = "abbaco";
        game.guessesJson = "[]";
        game.status = GameStatus.WON;
        game.mouseRevealed = true;
        game.kittenUnlocked = false;
        game.createdAt = completedAt;
        game.updatedAt = completedAt;
        game.completedAt = completedAt;
        game.persist();
    }

    private void createCompletedHexahackGame(UserEntity user, LocalDate date, String completedAt) {
        HexahackGameEntity game = new HexahackGameEntity();
        game.id = UUID.randomUUID().toString();
        game.userId = user.id;
        game.puzzleDate = date.toString();
        game.rulesVersion = 1;
        game.solution = "123456";
        game.eventLogJson = "[]";
        game.totalCost = 0;
        game.wrongSubmissions = 0;
        game.status = HexahackDtos.Status.COMPLETED;
        game.stealth = 100;
        game.rank = HexahackDtos.Rank.GHOST;
        game.createdAt = completedAt;
        game.updatedAt = completedAt;
        game.completedAt = completedAt;
        game.persist();
    }

    private void createMedal(String userId, MedalType medalType, int weeksAgo) {
        WeeklyMedalEntity medal = new WeeklyMedalEntity();
        medal.id = UUID.randomUUID().toString();
        medal.userId = userId;
        medal.weekStart = LocalDate.now(ZoneId.of("Europe/Rome")).minusWeeks(weeksAgo).toString();
        medal.medal = medalType;
        medal.awardedAt = "2026-01-01T00:00:00Z";
        medal.persist();
    }
}
