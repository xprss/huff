package dev.huff.hexaquot.game;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;

@QuarkusTest
class GameFeedResourceTest {
    @Inject
    GameRepository gameRepository;

    @Inject
    ObjectMapper objectMapper;

    @Test
    void exposesCompletedGamesAsRssItems() throws Exception {
        createCompletedGame(
            "feed-won-" + UUID.randomUUID(),
            "2099-01-02",
            GameMode.CLASSIC,
            GameStatus.WON,
            "2099-01-02T10:15:30Z",
            List.of(guess("abbaco", TileState.CORRECT))
        );
        createCompletedGame(
            "feed-lost-" + UUID.randomUUID(),
            "2099-01-03",
            GameMode.MISCHIEVOUS_MOUSE,
            GameStatus.LOST,
            "2099-01-03T10:15:30Z",
            List.of(
                guess("abachi", TileState.ABSENT),
                guess("abbada", TileState.PRESENT),
                guess("abbaco", TileState.CORRECT),
                guess("abeteo", TileState.HIDDEN),
                guess("adagio", TileState.ABSENT),
                guess("acervo", TileState.PRESENT)
            )
        );

        given()
            .when().get("/rss.xml")
            .then()
            .statusCode(200)
            .header("Content-Type", startsWith("application/rss+xml"))
            .body(containsString("<rss version=\"2.0\">"))
            .body(containsString("<title>Partita vinta in modalità Classica</title>"))
            .body(containsString("<title>Partita persa in modalità Topolino dispettoso</title>"))
            .body(containsString("Data partita: 2099-01-02"))
            .body(containsString("Fine gioco: 2099-01-03T10:15:30Z"))
            .body(containsString("Tentativi: 6/6"))
            .body(containsString("Tessere: 6 corrette, 12 presenti, 12 assenti, 6 nascoste."))
            .body(containsString("<pubDate>Sat, 3 Jan 2099 10:15:30 GMT</pubDate>"));
    }

    private void createCompletedGame(
        String userId,
        String puzzleDate,
        GameMode mode,
        GameStatus status,
        String completedAt,
        List<GuessResult> guesses
    ) throws Exception {
        GameRecord record = gameRepository.create(userId, puzzleDate, "abbaco", mode);
        gameRepository.update(new GameRecord(
            record.id(),
            record.userId(),
            record.puzzleDate(),
            record.mode(),
            record.solution(),
            objectMapper.writeValueAsString(guesses),
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
