package dev.huff.wordle.game;

import java.util.Map;

public record GlobalStatsDto(
    int players,
    int gamesStarted,
    int completed,
    int won,
    int lost,
    Map<Integer, Integer> guessDistribution
) {
}
