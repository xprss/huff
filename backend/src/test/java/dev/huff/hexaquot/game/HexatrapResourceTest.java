package dev.huff.hexaquot.game;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class HexatrapResourceTest {
    @Inject HexatrapCaseStore store;
    @Inject DailyGameService clock;

    @BeforeEach
    void installTodaysCase() {
        HexatrapTestCases.install(store, clock.todayDate());
    }

    @Test
    void endpointsExposeOnlyPublicStateAndRejectMalformedRequests() {
        String body = given().get("/api/hexatrap/today").then().statusCode(200)
            .body("rulesVersion", equalTo(2)).body("available", equalTo(true))
            .body("caseFile.title", equalTo("La lanterna spenta di Capo Nero"))
            .body("caseFile.clues[0].title", notNullValue()).body("caseFile.clues[0].text", nullValue())
            .extract().asString();
        org.junit.jupiter.api.Assertions.assertFalse(body.contains("formula"));
        org.junit.jupiter.api.Assertions.assertFalse(body.contains("solution"));
        String date = clock.todayDate();
        for (String version : new String[] {"1.5", "\"2\"", "null"}) {
            given().contentType("application/json").header("X-Huff-Request", "1")
                .body("{\"requestId\":\"bad-version\",\"puzzleDate\":\"" + date + "\",\"rulesVersion\":" + version + ",\"questionId\":\"q_ettore_stufa\"}")
                .post("/api/hexatrap/today/questions").then().statusCode(400);
        }
        given().contentType("application/json").header("X-Huff-Request", "1")
            .body("{\"requestId\":\"bad-clue\",\"puzzleDate\":\"" + date + "\",\"rulesVersion\":2,\"clueId\":\"c_missing\"}")
            .post("/api/hexatrap/today/clues").then().statusCode(400);
        given().get("/api/hexatrap/today/events").then().statusCode(404);
        given().get("/api/hexatrap/stats").then().statusCode(200).body("started", greaterThanOrEqualTo(0));
        given().get("/api/hexatrap/leaderboards").then().statusCode(200).body("weekly.entries", notNullValue());
        given().get("/api/leaderboards?game=hexatrap").then().statusCode(200);
        given().get("/api/me").then().statusCode(200).body("hexatrapAvailable", equalTo(true));
    }
}
