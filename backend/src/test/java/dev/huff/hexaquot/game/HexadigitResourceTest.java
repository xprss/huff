package dev.huff.hexaquot.game;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

@QuarkusTest
class HexadigitResourceTest {
    @Test
    void exposesCanonicalTodayGuessAndStatsEndpoints() {
        String cookie = given().when().get("/api/me").then().statusCode(200).extract().header("Set-Cookie");

        given().header("Cookie", cookie)
            .when().get("/api/hexadigit/today")
            .then().statusCode(200).body("game", nullValue());

        given().header("Cookie", cookie)
            .body("{\"guess\":\"000000\"}").contentType("application/json")
            .when().post("/api/hexadigit/today/guesses")
            .then().statusCode(200)
            .body("answerLength", equalTo(6))
            .body("maxAttempts", equalTo(6))
            .body("guesses[0].guess", equalTo("000000"));

        given().header("Cookie", cookie)
            .when().get("/api/hexadigit/stats")
            .then().statusCode(200).body("played", equalTo(0));

        given().header("Cookie", cookie)
            .when().get("/api/overall/stats")
            .then().statusCode(200).body("played", equalTo(0));

        given().header("Cookie", cookie)
            .when().get("/api/hexadigit/leaderboards")
            .then().statusCode(200)
            .body("allTime.entries.size()", equalTo(0));
    }
}
