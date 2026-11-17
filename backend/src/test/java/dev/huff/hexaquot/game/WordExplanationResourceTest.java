package dev.huff.hexaquot.game;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.ValidatableResponse;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.function.Supplier;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

@QuarkusTest
@QuarkusTestResource(value = FakeWordExplanationAgent.class, restrictToAnnotatedClass = true)
class WordExplanationResourceTest {
    @Inject DailyGameService clock;
    @Inject HexawordDailyGameProvider hexawordProvider;
    @Inject HexastarDailyGameProvider hexastarProvider;
    @Inject WordExplanationService service;
    @Inject EntityManager entityManager;

    @Test
    void hexawordExplanationIsGeneratedOnceAndSharedByLaterPlayers() {
        String word = hexawordProvider.solutionFor(clock.todayDate());
        forget(word);
        String first = newPlayer();

        given().header("Cookie", first).when().get("/api/hexaword/today/explanation").then().statusCode(409);
        given().header("Cookie", first).contentType("application/json").body(Map.of("mode", "CLASSIC"))
            .when().post("/api/hexaword/today/mode").then().statusCode(200);
        given().header("Cookie", first).when().get("/api/hexaword/today/explanation").then().statusCode(409);
        given().header("Cookie", first).contentType("application/json").body(Map.of("guess", word))
            .when().post("/api/hexaword/today/guesses").then().statusCode(200).body("status", equalTo("WON"));

        awaitReady(() -> given().header("Cookie", first).when().get("/api/hexaword/today/explanation").then().statusCode(200))
            .body("explanation", equalTo(FakeWordExplanationAgent.explanationFor(word)));

        String second = newPlayer();
        given().header("Cookie", second).contentType("application/json").body(Map.of("mode", "CLASSIC"))
            .when().post("/api/hexaword/today/mode").then().statusCode(200);
        given().header("Cookie", second).contentType("application/json").body(Map.of("guess", word))
            .when().post("/api/hexaword/today/guesses").then().statusCode(200);
        given().header("Cookie", second).when().get("/api/hexaword/today/explanation")
            .then().statusCode(200).body("status", equalTo("READY"))
            .body("explanation", equalTo(FakeWordExplanationAgent.explanationFor(word)));

        assertEquals(1, FakeWordExplanationAgent.calls(word));
    }

    @Test
    void hexastarExplanationIsAvailableOnceTheGameEnds() {
        var solution = hexastarProvider.solutionFor(clock.todayDate());
        forget(solution.word());
        String cookie = newPlayer();

        given().header("Cookie", cookie).when().get("/api/hexastar/today/explanation").then().statusCode(409);
        given().header("Cookie", cookie).contentType("application/json")
            .body(Map.of("requestId", "explanation-win", "syllables", solution.syllables()))
            .when().post("/api/hexastar/today/guesses").then().statusCode(200).body("game.status", equalTo("WON"));

        awaitReady(() -> given().header("Cookie", cookie).when().get("/api/hexastar/today/explanation").then().statusCode(200))
            .body("explanation", equalTo(FakeWordExplanationAgent.explanationFor(solution.word())));
        assertEquals(1, FakeWordExplanationAgent.calls(solution.word()));
    }

    @Test
    void failedGenerationIsReportedAsUnavailableAndNotRetriedImmediately() throws InterruptedException {
        String word = FakeWordExplanationAgent.FAILING_WORD;
        forget(word);
        WordExplanationDto result = service.explain(word);
        for (int attempt = 0; attempt < 100 && result.status() == WordExplanationDto.Status.PENDING; attempt++) {
            Thread.sleep(50);
            result = service.explain(word);
        }
        assertEquals(WordExplanationDto.Status.UNAVAILABLE, result.status());
        assertEquals(WordExplanationDto.Status.UNAVAILABLE, service.explain(word).status());
        assertEquals(1, FakeWordExplanationAgent.calls(word));
    }

    private ValidatableResponse awaitReady(Supplier<ValidatableResponse> request) {
        for (int attempt = 0; attempt < 100; attempt++) {
            ValidatableResponse response = request.get();
            String status = response.extract().path("status");
            if ("READY".equals(status)) return response;
            if (!"PENDING".equals(status)) fail("Unexpected explanation status " + status);
            response.body("explanation", nullValue());
            try {
                Thread.sleep(50);
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(error);
            }
        }
        throw new AssertionError("Explanation was not generated in time.");
    }

    private void forget(String word) {
        QuarkusTransaction.requiringNew().run(() -> entityManager
            .createNativeQuery("DELETE FROM word_explanations WHERE word = :word")
            .setParameter("word", word)
            .executeUpdate());
        FakeWordExplanationAgent.CALLS.remove(word);
    }

    private static String newPlayer() {
        return given().when().get("/api/me").then().statusCode(200).extract().header("Set-Cookie");
    }
}
