package dev.huff.hexaquot.game;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;

@QuarkusTest
class HexahackResourceTest {
    @Test
    void exposesOnlyCanonicalEndpointsAndNeverLeaksUnpurchasedAnswers() {
        String cookie = given().when().get("/api/me").then().statusCode(200).extract().header("Set-Cookie");

        given().header("Cookie", cookie)
            .when().get("/api/hexahack/today")
            .then().statusCode(200)
            .body("answerLength", equalTo(6))
            .body("freeClues.totalSum", org.hamcrest.Matchers.greaterThanOrEqualTo(0))
            .body("game", nullValue());

        String probe = """
            {"requestId":"probe-api","type":"PING","position":1,"threshold":5}
            """;
        given().header("Cookie", cookie).contentType("application/json").body(probe)
            .when().post("/api/hexahack/today/probes")
            .then().statusCode(200)
            .body("game.solution", nullValue())
            .body("game.totalCost", equalTo(1))
            .body("game.log", hasSize(1))
            .body("result.type", equalTo("PING"));

        given().header("Cookie", cookie).contentType("application/json").body(probe)
            .when().post("/api/hexahack/today/probes")
            .then().statusCode(200)
            .body("replayed", equalTo(true))
            .body("game.totalCost", equalTo(1))
            .body("game.log", hasSize(1));

        String wrong = """
            {"requestId":"wrong-api","code":"000000"}
            """;
        given().header("Cookie", cookie).contentType("application/json").body(wrong)
            .when().post("/api/hexahack/today/submissions")
            .then().statusCode(200)
            .body("game.solution", nullValue())
            .body("result.correctPositions", org.hamcrest.Matchers.both(
                org.hamcrest.Matchers.greaterThanOrEqualTo(0)).and(org.hamcrest.Matchers.lessThanOrEqualTo(6)));

        given().header("Cookie", cookie).contentType("application/json").body(wrong)
            .when().post("/api/hexahack/today/submissions")
            .then().statusCode(200)
            .body("replayed", equalTo(true))
            .body("game.wrongSubmissions", equalTo(1));

        given().header("Cookie", cookie)
            .when().get("/api/hexahack/today")
            .then().statusCode(200)
            .body("game.log", hasSize(2))
            .body("game.solution", nullValue());

        given().header("Cookie", cookie)
            .when().get("/api/hexahack/stats")
            .then().statusCode(200)
            .body("completedAccesses", equalTo(0))
            .body("last30Nodes", hasSize(30));

    }

    @Test
    void serializesConcurrentRetriesWithoutChargingTwice() {
        String cookie = given().when().get("/api/me").then().statusCode(200).extract().header("Set-Cookie");
        String body = """
            {"requestId":"concurrent-probe","type":"BIT_SCAN","position":3}
            """;
        var first = CompletableFuture.supplyAsync(() -> postProbe(cookie, body));
        var second = CompletableFuture.supplyAsync(() -> postProbe(cookie, body));
        assertStatus(first.join());
        assertStatus(second.join());

        given().header("Cookie", cookie)
            .when().get("/api/hexahack/today")
            .then().statusCode(200)
            .body("game.totalCost", equalTo(1))
            .body("game.log", hasSize(1));
    }

    private Response postProbe(String cookie, String body) {
        return given().header("Cookie", cookie).contentType("application/json").body(body)
            .when().post("/api/hexahack/today/probes");
    }

    private void assertStatus(Response response) {
        response.then().statusCode(200);
    }
}
