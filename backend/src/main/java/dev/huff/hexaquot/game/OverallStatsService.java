package dev.huff.hexaquot.game;

import dev.huff.hexaquot.auth.AppUser;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class OverallStatsService {
    @Inject DailyGameService hexaword;
    @Inject HexahackDailyGameService hexahack;
    @Inject HexaskyDailyGameService hexasky;
    @Inject HexasquareDailyGameService hexasquare;

    public StatsDto stats(AppUser user) { return statsForUserId(user.id()); }

    public StatsDto statsForUserId(String userId) {
        java.util.List<StatsCalculator.CompletedGame> completed = new java.util.ArrayList<>(hexaword.completedForUser(userId));
        completed.addAll(hexasky.completedForUser(userId));
        completed.addAll(hexasquare.completedForUser(userId));
        return StatsCalculator.calculateOverall(completed);
    }

    public PlayerStatsDto allForUser(String userId) {
        return new PlayerStatsDto(statsForUserId(userId), hexaword.statsForUserId(userId), hexahack.statsForUserId(userId), hexasky.statsForUserId(userId), hexasquare.statsForUserId(userId));
    }

    public record PlayerStatsDto(StatsDto overall, StatsDto hexaword, HexahackDtos.StatsDto hexahack, HexaskyDtos.StatsDto hexasky,
                                 HexasquareDtos.StatsDto hexasquare) {}
}
