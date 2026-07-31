package dev.huff.hexaquot.game;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.huff.hexaquot.auth.AppUser;
import dev.huff.hexaquot.persistence.UserEntity;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.WebApplicationException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class DailyGameServiceTest {
    @Inject
    DailyGameService dailyGameService;

    @Inject
    GameRepository gameRepository;

    @Inject
    ObjectMapper objectMapper;

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
            "@mouse",
            "😀",
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
            "@first-guess-kitten",
            "😀",
            false
        );
        String today = LocalDate.now(ZoneId.of("Europe/Rome")).toString();
        gameRepository.create(user.id(), today, "abbaco", GameMode.MISCHIEVOUS_MOUSE);

        GameDto afterFirstGuess = dailyGameService.guess(user, "abbada");

        assertEquals(1, countTiles(afterFirstGuess.guesses().get(0), TileState.HIDDEN));
        assertTrue(afterFirstGuess.kitten().canUse());
    }

    @Test
    void mischievousMouseRejectsRepeatedGuess() {
        var user = new dev.huff.hexaquot.auth.AppUser(
            "test-repeated-mouse-guess-" + UUID.randomUUID(),
            null,
            "Repeated Mouse Guess",
            "@repeated-mouse-guess",
            "😀",
            false
        );
        String today = LocalDate.now(ZoneId.of("Europe/Rome")).toString();
        gameRepository.create(user.id(), today, "abbaco", GameMode.MISCHIEVOUS_MOUSE);

        dailyGameService.guess(user, "abachi");

        BadRequestException error = assertThrows(
            BadRequestException.class,
            () -> dailyGameService.guess(user, "ABACHI")
        );
        GameDto todayGame = dailyGameService.today(user).game();

        assertEquals("Hai gia' inserito questa parola.", error.getMessage());
        assertEquals(1, todayGame.guesses().size());
    }

    @Test
    @TestTransaction
    void awardsStarAfterThreeConsecutiveCompletedGamesRegardlessOfOutcome() throws Exception {
        AppUser user = createPersistedUser("star-award");
        LocalDate today = LocalDate.now(ZoneId.of("Europe/Rome"));
        createCompletedGame(user, today.minusDays(2), GameStatus.WON);
        createCompletedGame(user, today.minusDays(1), GameStatus.LOST);
        gameRepository.create(user.id(), today.toString(), "abbaco", GameMode.CLASSIC);

        GameDto completed = dailyGameService.guess(user, "abbaco");

        assertTrue(completed.star().justAwarded());
        assertTrue(completed.star().available());
        assertFalse(completed.star().canUse());

        GameDto reloaded = dailyGameService.today(user).game();

        assertFalse(reloaded.star().justAwarded());
        assertTrue(reloaded.star().available());
    }

    @Test
    @TestTransaction
    void skippedPuzzleDatePreventsStarAward() throws Exception {
        AppUser user = createPersistedUser("star-skipped-day");
        LocalDate today = LocalDate.now(ZoneId.of("Europe/Rome"));
        createCompletedGame(user, today.minusDays(3), GameStatus.WON);
        createCompletedGame(user, today.minusDays(1), GameStatus.LOST);
        gameRepository.create(user.id(), today.toString(), "abbaco", GameMode.CLASSIC);

        GameDto completed = dailyGameService.guess(user, "abbaco");

        assertFalse(completed.star().justAwarded());
        assertFalse(completed.star().available());
    }

    @Test
    @TestTransaction
    void doesNotAccumulateStarWhenOneIsAlreadyAvailable() throws Exception {
        AppUser user = createPersistedUser("star-no-stack");
        UserEntity userEntity = UserEntity.findById(user.id());
        userEntity.starAvailable = true;
        LocalDate today = LocalDate.now(ZoneId.of("Europe/Rome"));
        createCompletedGame(user, today.minusDays(2), GameStatus.WON);
        createCompletedGame(user, today.minusDays(1), GameStatus.WON);
        gameRepository.create(user.id(), today.toString(), "abbaco", GameMode.CLASSIC);

        GameDto completed = dailyGameService.guess(user, "abbaco");

        assertFalse(completed.star().justAwarded());
        assertTrue(completed.star().available());
    }

    @Test
    @TestTransaction
    void useStarRevealsFullTilesAndConsumesBonus() {
        AppUser user = createPersistedUser("star-use");
        UserEntity userEntity = UserEntity.findById(user.id());
        userEntity.starAvailable = true;
        String today = LocalDate.now(ZoneId.of("Europe/Rome")).toString();
        gameRepository.create(user.id(), today, "abbaco", GameMode.MISCHIEVOUS_MOUSE);
        GameDto afterFirstGuess = dailyGameService.guess(user, "abachi");

        assertEquals(1, countTiles(afterFirstGuess.guesses().get(0), TileState.HIDDEN));

        StarRevealDto reveal = dailyGameService.useStar(user);

        assertEquals(0, countTiles(reveal.guesses().get(0), TileState.HIDDEN));
        assertFalse(reveal.game().star().available());
        assertFalse(reveal.game().star().canUse());
        assertTrue(reveal.game().star().used());
    }

    @Test
    @TestTransaction
    void useStarRequiresAnAvailableBonus() {
        AppUser user = createPersistedUser("star-missing");
        String today = LocalDate.now(ZoneId.of("Europe/Rome")).toString();
        gameRepository.create(user.id(), today, "abbaco", GameMode.CLASSIC);
        dailyGameService.guess(user, "abachi");

        WebApplicationException error = assertThrows(
            WebApplicationException.class,
            () -> dailyGameService.useStar(user)
        );

        assertEquals(409, error.getResponse().getStatus());
        assertEquals("Completa 3 partite di fila per ottenere una stella.", error.getMessage());
    }

    @Test
    @TestTransaction
    void useStarRequiresAtLeastOneGuess() {
        AppUser user = createPersistedUser("star-before-guess");
        UserEntity userEntity = UserEntity.findById(user.id());
        userEntity.starAvailable = true;
        String today = LocalDate.now(ZoneId.of("Europe/Rome")).toString();
        gameRepository.create(user.id(), today, "abbaco", GameMode.CLASSIC);

        WebApplicationException error = assertThrows(
            WebApplicationException.class,
            () -> dailyGameService.useStar(user)
        );

        assertEquals(409, error.getResponse().getStatus());
        assertEquals("Prova un tentativo prima di usare la stella.", error.getMessage());
    }

    private int countTiles(GuessResult guess, TileState state) {
        return (int) guess.tiles().stream()
            .filter(tile -> tile.state() == state)
            .count();
    }

    private AppUser createPersistedUser(String label) {
        String now = Instant.now().toString();
        UserEntity user = new UserEntity();
        user.id = "test-" + label + "-" + UUID.randomUUID();
        user.displayName = "Star Test";
        user.nickname = "@star-test-" + UUID.randomUUID().toString().substring(0, 8);
        user.profileEmoji = "😀";
        user.createdAt = now;
        user.starAvailable = false;
        user.persist();
        return new AppUser(user.id, null, user.displayName, user.nickname, user.profileEmoji, false);
    }

    private void createCompletedGame(AppUser user, LocalDate date, GameStatus status) throws Exception {
        GameRecord record = gameRepository.create(user.id(), date.toString(), "abbaco", GameMode.CLASSIC);
        String completedAt = Instant.now().toString();
        gameRepository.update(new GameRecord(
            record.id(),
            record.userId(),
            record.puzzleDate(),
            record.mode(),
            record.solution(),
            objectMapper.writeValueAsString(completedGuesses(status)),
            status,
            record.mouseTileIndex(),
            record.mouseRevealed(),
            record.kittenUnlocked(),
            record.kittenUsedAt(),
            record.createdAt(),
            completedAt,
            completedAt
        ));
    }

    private List<GuessResult> completedGuesses(GameStatus status) {
        if (status == GameStatus.WON) {
            return List.of(guess("abbaco", TileState.CORRECT));
        }
        return List.of(
            guess("abachi", TileState.ABSENT),
            guess("abbada", TileState.PRESENT),
            guess("abeteo", TileState.ABSENT),
            guess("adagio", TileState.PRESENT),
            guess("acervo", TileState.ABSENT),
            guess("acerbo", TileState.PRESENT)
        );
    }

    private GuessResult guess(String word, TileState state) {
        return new GuessResult(
            word,
            List.of(
                new TileResult(word.substring(0, 1), state),
                new TileResult(word.substring(1, 2), state),
                new TileResult(word.substring(2, 3), state),
                new TileResult(word.substring(3, 4), state),
                new TileResult(word.substring(4, 5), state),
                new TileResult(word.substring(5, 6), state)
            )
        );
    }
}
