package dev.huff.hexaquot.leaderboard;

import dev.huff.hexaquot.game.DailyGameService;
import dev.huff.hexaquot.persistence.UserEntity;
import dev.huff.hexaquot.persistence.WeeklyMedalAwardStateEntity;
import dev.huff.hexaquot.persistence.WeeklyMedalEntity;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class LeaderboardService {
    @ConfigProperty(name = "app.game.timezone")
    String timezone;

    @Inject
    LeaderboardRepository leaderboardRepository;

    @Inject
    DailyGameService dailyGameService;

    @Transactional
    void onStart(@Observes StartupEvent event) {
        initializeAwarding();
        awardPendingWeeks();
    }

    @Transactional
    public void initializeAwarding() {
        if (WeeklyMedalAwardStateEntity.findById(WeeklyMedalAwardStateEntity.SINGLETON_ID) != null) {
            return;
        }
        LocalDate currentWeekStart = currentWeekStart();
        WeeklyMedalAwardStateEntity state = new WeeklyMedalAwardStateEntity();
        state.id = WeeklyMedalAwardStateEntity.SINGLETON_ID;
        state.firstEligibleWeekStart = currentWeekStart.toString();
        state.lastProcessedWeekStart = currentWeekStart.minusWeeks(1).toString();
        state.persist();
    }

    @Scheduled(cron = "{app.leaderboard.weekly-awards.cron}", timeZone = "{app.game.timezone}")
    @Transactional
    public void awardPendingWeeks() {
        WeeklyMedalAwardStateEntity state = WeeklyMedalAwardStateEntity.findById(WeeklyMedalAwardStateEntity.SINGLETON_ID);
        if (state == null) {
            initializeAwarding();
            state = WeeklyMedalAwardStateEntity.findById(WeeklyMedalAwardStateEntity.SINGLETON_ID);
        }
        LocalDate firstEligible = LocalDate.parse(state.firstEligibleWeekStart);
        LocalDate latestClosed = currentWeekStart().minusWeeks(1);
        LocalDate nextWeek = LocalDate.parse(state.lastProcessedWeekStart).plusWeeks(1);
        if (nextWeek.isBefore(firstEligible)) {
            nextWeek = firstEligible;
        }
        while (!nextWeek.isAfter(latestClosed)) {
            awardWeek(nextWeek);
            state.lastProcessedWeekStart = nextWeek.toString();
            nextWeek = nextWeek.plusWeeks(1);
        }
    }

    public LeaderboardsDto leaderboards() {
        LocalDate today = LocalDate.now(zoneId());
        LocalDate yearStart = today.withDayOfYear(1);
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        return new LeaderboardsDto(
            period(null, null, false),
            period(yearStart, yearStart.plusYears(1), false),
            period(monthStart, monthStart.plusMonths(1), false),
            period(weekStart, weekStart.plusWeeks(1), true)
        );
    }

    public PublicPlayerProfileDto publicProfile(String nickname) {
        String normalizedNickname = nickname == null ? "" : nickname.trim().toLowerCase(Locale.ROOT);
        UserEntity user = UserEntity.<UserEntity>find("nickname", normalizedNickname).firstResult();
        if (user == null) {
            throw new NotFoundException("Giocatore non trovato.");
        }
        return new PublicPlayerProfileDto(
            user.displayName,
            user.nickname,
            user.profileEmoji,
            user.bio,
            dailyGameService.statsForUserId(user.id),
            medalCounts(user.id)
        );
    }

    public MedalCountsDto medalCounts(String userId) {
        Map<MedalType, Integer> counts = new EnumMap<>(MedalType.class);
        WeeklyMedalEntity.<WeeklyMedalEntity>list("userId", userId).forEach(medal ->
            counts.merge(medal.medal, 1, Integer::sum)
        );
        return new MedalCountsDto(
            counts.getOrDefault(MedalType.GOLD, 0),
            counts.getOrDefault(MedalType.SILVER, 0),
            counts.getOrDefault(MedalType.BRONZE, 0)
        );
    }

    private LeaderboardPeriodDto period(LocalDate startDate, LocalDate endDate, boolean weekly) {
        Comparator<LeaderboardRepository.PlayerScore> comparator = Comparator
            .comparingInt(LeaderboardRepository.PlayerScore::wins)
            .reversed();
        if (weekly) {
            comparator = comparator
                .thenComparing(LeaderboardRepository.PlayerScore::lastWinAt, Comparator.nullsLast(String::compareTo))
                .thenComparing(score -> score.user().id);
        } else {
            comparator = comparator.thenComparing(score -> score.user().nickname);
        }
        List<LeaderboardEntryDto> entries = leaderboardRepository
            .winnerScores(startDate == null ? null : startDate.toString(), endDate == null ? null : endDate.toString())
            .stream()
            .sorted(comparator)
            .map(score -> new LeaderboardEntryDto(0, score.user().displayName, score.user().nickname, score.user().profileEmoji, score.wins()))
            .toList();
        List<LeaderboardEntryDto> ranked = java.util.stream.IntStream.range(0, entries.size())
            .mapToObj(index -> {
                LeaderboardEntryDto entry = entries.get(index);
                return new LeaderboardEntryDto(index + 1, entry.displayName(), entry.nickname(), entry.profileEmoji(), entry.wins());
            })
            .toList();
        return new LeaderboardPeriodDto(
            startDate == null ? null : startDate.toString(),
            endDate == null ? null : endDate.minusDays(1).toString(),
            ranked
        );
    }

    private void awardWeek(LocalDate weekStart) {
        List<LeaderboardRepository.PlayerScore> podium = leaderboardRepository
            .winnerScores(weekStart.toString(), weekStart.plusWeeks(1).toString())
            .stream()
            .sorted(Comparator.comparingInt(LeaderboardRepository.PlayerScore::wins).reversed()
                .thenComparing(LeaderboardRepository.PlayerScore::lastWinAt, Comparator.nullsLast(String::compareTo))
                .thenComparing(score -> score.user().id))
            .limit(3)
            .toList();
        MedalType[] medals = {MedalType.GOLD, MedalType.SILVER, MedalType.BRONZE};
        for (int index = 0; index < podium.size(); index++) {
            LeaderboardRepository.PlayerScore score = podium.get(index);
            if (WeeklyMedalEntity.count("userId = ?1 and weekStart = ?2", score.user().id, weekStart.toString()) > 0) {
                continue;
            }
            WeeklyMedalEntity medal = new WeeklyMedalEntity();
            medal.id = UUID.randomUUID().toString();
            medal.userId = score.user().id;
            medal.weekStart = weekStart.toString();
            medal.medal = medals[index];
            medal.awardedAt = Instant.now().toString();
            medal.persist();
        }
    }

    private LocalDate currentWeekStart() {
        return LocalDate.now(zoneId()).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    private ZoneId zoneId() {
        return ZoneId.of(timezone);
    }
}
