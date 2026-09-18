package dev.huff.hexaquot.game;

import dev.huff.hexaquot.game.HexaecoDtos.SubmitRequest;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import java.util.Map;
import java.util.concurrent.*;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class HexaecoResourceTest {
    @Inject HexaecoDailyGameProvider provider;
    @Inject DailyGameService clock;

    @Test
    void endpointsValidateRestoreAndDeduplicateConcurrentCompletions() throws Exception {
        var identity = given().get("/api/me").then().statusCode(200).extract();
        String cookie = identity.header("Set-Cookie");
        String nickname = identity.path("user.nickname");
        assertNotNull(identity.path("user.id"));
        String date = clock.todayDate();
        given().header("Cookie", cookie).get("/api/hexaeco/today").then().statusCode(200)
            .body("puzzleDate", equalTo(date)).body("rulesVersion", equalTo(1))
            .body("board.walls", not(empty())).body("game", nullValue()).body("solution", nullValue());
        given().header("Cookie", cookie).contentType("application/json")
            .body(Map.of("requestId", "bad", "puzzleDate", date, "rulesVersion", 1, "moves", new String[]{"FLY"}))
            .post("/api/hexaeco/today/submissions").then().statusCode(400);

        var request = new SubmitRequest("concurrent", date, 1, HexaecoRulesTest.shortestSolution(provider.boardFor(date)));
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        Callable<Boolean> submit = () -> {
            start.await(10, TimeUnit.SECONDS);
            return given().header("Cookie", cookie).contentType("application/json").body(request)
                .post("/api/hexaeco/today/submissions").then().statusCode(200)
                .body("game.status", equalTo("WON")).extract().path("replayed");
        };
        try {
            Future<Boolean> first = executor.submit(submit);
            Future<Boolean> second = executor.submit(submit);
            start.countDown();
            assertNotEquals(first.get(30, TimeUnit.SECONDS), second.get(30, TimeUnit.SECONDS));
        } finally { executor.shutdownNow(); }

        given().header("Cookie", cookie).get("/api/hexaeco/today").then().statusCode(200)
            .body("game.moves", hasSize(request.moves().size()));
        given().header("Cookie", cookie).get("/api/hexaeco/stats").then().statusCode(200).body("completed", equalTo(1));
        given().header("Cookie", cookie).get("/api/overall/stats").then().statusCode(200).body("won", equalTo(1));
        given().header("Cookie", cookie).get("/api/leaderboards?game=hexaeco").then().statusCode(200);
        given().header("Cookie", cookie).get("/api/hexaeco/leaderboards").then().statusCode(200);
        given().header("Cookie", cookie).get("/api/player/" + nickname).then().statusCode(200)
            .body("hexaecoStats.completed", equalTo(1));
    }
}
