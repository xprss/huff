package dev.huff.hexaquot.game;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StatsCalculatorTest {
    @Test
    void overallCountsBothWinsButOnlyOneWinningDayInTheStreak() {
        StatsDto stats = StatsCalculator.calculateOverall(List.of(
            new StatsCalculator.CompletedGame("2026-08-08", GameStatus.WON, 2),
            new StatsCalculator.CompletedGame("2026-08-08", GameStatus.WON, 4),
            new StatsCalculator.CompletedGame("2026-08-09", GameStatus.WON, 1),
            new StatsCalculator.CompletedGame("2026-08-10", GameStatus.LOST, 6)
        ));
        assertEquals(4, stats.played());
        assertEquals(3, stats.won());
        assertEquals(2, stats.maxStreak());
        assertEquals(0, stats.currentStreak());
        assertEquals(1, stats.guessDistribution().get(2));
        assertEquals(1, stats.guessDistribution().get(4));
    }
}
