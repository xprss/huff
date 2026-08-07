package dev.huff.hexaquot.leaderboard;

public record LeaderboardsDto(
    LeaderboardPeriodDto allTime,
    LeaderboardPeriodDto yearly,
    LeaderboardPeriodDto monthly,
    LeaderboardPeriodDto weekly
) {
}
