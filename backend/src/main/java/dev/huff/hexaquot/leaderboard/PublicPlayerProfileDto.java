package dev.huff.hexaquot.leaderboard;

import dev.huff.hexaquot.game.StatsDto;
import dev.huff.hexaquot.game.HexahackDtos;
import dev.huff.hexaquot.game.HexaskyDtos;
import dev.huff.hexaquot.game.HexasquareDtos;

public record PublicPlayerProfileDto(
    String displayName,
    String nickname,
    String profileEmoji,
    String bio,
    StatsDto stats,
    StatsDto overallStats,
    StatsDto hexawordStats,
    HexahackDtos.StatsDto hexahackStats,
    HexaskyDtos.StatsDto hexaskyStats,
    HexasquareDtos.StatsDto hexasquareStats,
    MedalCountsDto medals
) {
}
