package dev.huff.hexaquot.api;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
class LeaderboardResourceTest {
    @ParameterizedTest
    @ValueSource(strings = { "overall", "hexaword", "hexahack", "hexasky" })
    void servesEveryLeaderboardGameFromTheSharedEndpoint(String game) {
        String cookie = given().when().get("/api/me").then().statusCode(200).extract().header("Set-Cookie");

        given()
            .header("Cookie", cookie)
            .queryParam("game", game)
            .when().get("/api/leaderboards")
            .then()
            .statusCode(200)
            .body("allTime", notNullValue())
            .body("yearly", notNullValue())
            .body("monthly", notNullValue())
            .body("weekly", notNullValue());
    }
}
