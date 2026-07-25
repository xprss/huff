package dev.huff.hexaquot.game;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class DailyGameServiceTest {
    @Inject
    DailyGameService dailyGameService;

    @Inject
    GameRepository gameRepository;

    @Test
    void scoresDuplicateLettersWithPuzzleRules() {
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

    @Test
    void mischievousMouseHidesExactlyOneTileOnlyInFirstGuess() {
        var user = new dev.huff.hexaquot.auth.AppUser(
            "test-mouse-" + UUID.randomUUID(),
            null,
            "Mouse",
            false
        );
        String today = LocalDate.now(ZoneId.of("Europe/Rome")).toString();
        gameRepository.create(user.id(), today, "abbaco", GameMode.MISCHIEVOUS_MOUSE);

        GameDto afterFirstGuess = dailyGameService.guess(user, "abachi");

        assertEquals(1, countTiles(afterFirstGuess.guesses().get(0), TileState.HIDDEN));
        assertEquals(false, afterFirstGuess.kitten().canUse());

        GameDto afterSecondGuess = dailyGameService.guess(user, "abbada");

        assertEquals(1, countTiles(afterSecondGuess.guesses().get(0), TileState.HIDDEN));
        assertEquals(0, countTiles(afterSecondGuess.guesses().get(1), TileState.HIDDEN));
        assertTrue(afterSecondGuess.kitten().canUse());

        GameDto afterKitten = dailyGameService.useKitten(user);

        assertEquals(0, countTiles(afterKitten.guesses().get(0), TileState.HIDDEN));
        assertTrue(afterKitten.kitten().used());
    }

    @Test
    void mischievousMouseUnlocksKittenOnFirstGuessWithAtLeastThreeCorrectTiles() {
        var user = new dev.huff.hexaquot.auth.AppUser(
            "test-first-guess-kitten-" + UUID.randomUUID(),
            null,
            "First Guess Kitten",
            false
        );
        String today = LocalDate.now(ZoneId.of("Europe/Rome")).toString();
        gameRepository.create(user.id(), today, "abbaco", GameMode.MISCHIEVOUS_MOUSE);

        GameDto afterFirstGuess = dailyGameService.guess(user, "abbada");

        assertEquals(1, countTiles(afterFirstGuess.guesses().get(0), TileState.HIDDEN));
        assertTrue(afterFirstGuess.kitten().canUse());
    }

    private int countTiles(GuessResult guess, TileState state) {
        return (int) guess.tiles().stream()
            .filter(tile -> tile.state() == state)
            .count();
    }
}
