package dev.huff.hexaquot.game;

import dev.huff.hexaquot.auth.AppUser;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.ArrayList;

@ApplicationScoped
public class OverallStatsService {
    @Inject DailyGameService hexaword;
    @Inject HexadigitDailyGameService hexadigit;

    public StatsDto stats(AppUser user) { return statsForUserId(user.id()); }

    public StatsDto statsForUserId(String userId) {
        var completed = new ArrayList<StatsCalculator.CompletedGame>();
        completed.addAll(hexaword.completedForUser(userId));
        completed.addAll(hexadigit.completedForUser(userId));
        return StatsCalculator.calculateOverall(completed);
    }

    public PlayerStatsDto allForUser(String userId) {
        return new PlayerStatsDto(statsForUserId(userId), hexaword.statsForUserId(userId), hexadigit.statsForUserId(userId));
    }

    public record PlayerStatsDto(StatsDto overall, StatsDto hexaword, StatsDto hexadigit) {}
}
