package dev.huff.wordle.game;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
class DailyGameServiceTest {
    @Inject
    DailyGameService dailyGameService;

    @Test
    void scoresDuplicateLettersWithWordleRules() {
        GuessResult result = dailyGameService.score("albero", "ancora");

        assertEquals(TileState.CORRECT, result.tiles().get(0).state());
        assertEquals(TileState.ABSENT, result.tiles().get(1).state());
        assertEquals(TileState.ABSENT, result.tiles().get(2).state());
        assertEquals(TileState.ABSENT, result.tiles().get(3).state());
        assertEquals(TileState.CORRECT, result.tiles().get(4).state());
        assertEquals(TileState.PRESENT, result.tiles().get(5).state());
    }

    @Test
    void scoresPresentLettersOnlyWhileCopiesRemain() {
        GuessResult result = dailyGameService.score("barche", "boccia");

        assertEquals(TileState.CORRECT, result.tiles().get(0).state());
        assertEquals(TileState.PRESENT, result.tiles().get(1).state());
        assertEquals(TileState.ABSENT, result.tiles().get(2).state());
        assertEquals(TileState.CORRECT, result.tiles().get(3).state());
        assertEquals(TileState.ABSENT, result.tiles().get(4).state());
        assertEquals(TileState.ABSENT, result.tiles().get(5).state());
    }
}
