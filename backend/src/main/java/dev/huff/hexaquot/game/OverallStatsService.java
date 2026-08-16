package dev.huff.hexaquot.game;

import dev.huff.hexaquot.auth.AppUser;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class OverallStatsService {
    @Inject DailyGameService hexaword;

    public StatsDto stats(AppUser user) { return statsForUserId(user.id()); }

    public StatsDto statsForUserId(String userId) {
        return StatsCalculator.calculateOverall(hexaword.completedForUser(userId));
    }

    public PlayerStatsDto allForUser(String userId) {
        return new PlayerStatsDto(statsForUserId(userId), hexaword.statsForUserId(userId));
    }

    public record PlayerStatsDto(StatsDto overall, StatsDto hexaword) {}
}
