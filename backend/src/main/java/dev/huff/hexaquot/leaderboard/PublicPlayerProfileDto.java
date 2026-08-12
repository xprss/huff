package dev.huff.hexaquot.leaderboard;

import dev.huff.hexaquot.game.StatsDto;

public record PublicPlayerProfileDto(
    String displayName,
    String nickname,
    String profileEmoji,
    String bio,
    StatsDto stats,
    StatsDto overallStats,
    StatsDto hexawordStats,
    StatsDto hexadigitStats,
    MedalCountsDto medals
) {
}
