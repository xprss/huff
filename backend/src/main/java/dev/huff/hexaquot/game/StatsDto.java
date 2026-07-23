package dev.huff.hexaquot.game;

import java.util.Map;

public record StatsDto(
    int played,
    int won,
    int lost,
    int currentStreak,
    int maxStreak,
    Map<Integer, Integer> guessDistribution
) {
}
