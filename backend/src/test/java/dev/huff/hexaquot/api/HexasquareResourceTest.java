package dev.huff.hexaquot.api;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class HexasquareResourceTest {
    @Test void exposesTheDailyPuzzleAndDedicatedStatsAndLeaderboard() {
        given().when().get("/api/hexasquare/today").then().statusCode(200)
            .body("size",equalTo(24)).body("quadrants",hasSize(4)).body("characters.size()",allOf(greaterThanOrEqualTo(3),lessThanOrEqualTo(6)))
            .body("inventory.STRAIGHT",notNullValue());
        given().when().get("/api/hexasquare/stats").then().statusCode(200).body("gamesStarted",greaterThanOrEqualTo(0));
        given().when().get("/api/hexasquare/leaderboards").then().statusCode(200).body("weekly.entries",notNullValue());
        given().queryParam("game","hexasquare").when().get("/api/leaderboards").then().statusCode(200);
    }

    @Test void rejectsInvalidPlacementsBeforeTheSolver() {
        given().contentType("application/json").body("""
            {"requestId":"invalid-coordinate","placements":[{"row":24,"column":0,"type":"STRAIGHT","rotation":0}]}
            """).when().post("/api/hexasquare/today/simulations").then().statusCode(400);
    }
}
