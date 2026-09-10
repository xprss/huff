package dev.huff.hexaquot.game;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;

@QuarkusTest
class HexastarResourceTest {
    @Inject HexastarDailyGameProvider provider;
    @Inject DailyGameService clock;

    @Test
    void exposesOnlyTheShapeUntilTheGameEnds() {
        String cookie = given().when().get("/api/me").then().statusCode(200).extract().header("Set-Cookie");
        var solution = provider.solutionFor(clock.todayDate());

        given().header("Cookie", cookie)
            .when().get("/api/hexastar/today")
            .then().statusCode(200)
            .body("rulesVersion", equalTo(HexastarDailyGameProvider.RULES_VERSION))
            .body("maxAttempts", equalTo(6))
            .body("syllableLengths", hasSize(solution.syllables().size()))
            .body("game", nullValue());

        given().header("Cookie", cookie).contentType("application/json")
            .body(Map.of("requestId", "api-invalid", "syllables", new String[] { "xx" }))
            .when().post("/api/hexastar/today/guesses")
            .then().statusCode(400);

        given().header("Cookie", cookie)
            .when().get("/api/hexastar/today")
            .then().statusCode(200).body("game", nullValue());

        given().header("Cookie", cookie).contentType("application/json")
            .body(Map.of("requestId", "api-winning", "syllables", solution.syllables()))
            .when().post("/api/hexastar/today/guesses")
            .then().statusCode(200)
            .body("game.status", equalTo("WON"))
            .body("game.solutionSyllables", equalTo(solution.syllables()));

        given().header("Cookie", cookie)
            .when().get("/api/hexastar/stats")
            .then().statusCode(200)
            .body("won", equalTo(1));
    }
}
