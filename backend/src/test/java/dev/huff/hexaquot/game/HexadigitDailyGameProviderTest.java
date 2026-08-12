package dev.huff.hexaquot.game;

import jakarta.ws.rs.BadRequestException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HexadigitDailyGameProviderTest {
    private final HexadigitDailyGameProvider provider = new HexadigitDailyGameProvider();

    @Test
    void acceptsExactlySixDigitsIncludingLeadingZeroes() {
        provider.validate("001122");
        assertThrows(BadRequestException.class, () -> provider.validate("1122"));
        assertThrows(BadRequestException.class, () -> provider.validate("12a456"));
    }

    @Test
    void scoresDuplicatesInTwoPassesAndAddsPerPositionComparison() {
        HexadigitGuessResult result = provider.score("111111", "100000");
        assertEquals(TileState.CORRECT, result.tiles().get(0).state());
        assertEquals(DigitComparison.EQUAL, result.tiles().get(0).comparison());
        for (int index = 1; index < 6; index++) {
            assertEquals(TileState.ABSENT, result.tiles().get(index).state());
            assertEquals(DigitComparison.LOWER, result.tiles().get(index).comparison());
        }

        HexadigitGuessResult higher = provider.score("000000", "999999");
        higher.tiles().forEach(tile -> assertEquals(DigitComparison.HIGHER, tile.comparison()));
    }
}
