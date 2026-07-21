package dev.huff.wordle.game;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
class GameResourceTest {
    @Test
    void exposesCurrentAnonymousUserWhenAuthIsDisabled() {
        given()
            .when().get("/api/me")
            .then()
            .statusCode(200)
            .header("Set-Cookie", notNullValue())
            .body("loggedIn", equalTo(true))
            .body("authEnabled", equalTo(false))
            .body("user.displayName", equalTo("Giocatore"))
            .body("loginUrl", equalTo(null))
            .body("logoutUrl", equalTo(null));
    }

    @Test
    void createsAnonymousDailyGameAndPersistsSessionCookie() {
        given()
            .when().get("/api/game/today")
            .then()
            .statusCode(200)
            .header("Set-Cookie", notNullValue())
            .body("wordLength", equalTo(6))
            .body("maxAttempts", equalTo(6))
            .body("status", equalTo("IN_PROGRESS"));
    }

    @Test
    void rejectsWordsOutsideTheDictionary() {
        String cookie = given()
            .when().get("/api/game/today")
            .then()
            .statusCode(200)
            .extract().header("Set-Cookie");

        given()
            .header("Cookie", cookie)
            .body("{\"guess\":\"xxxxxx\"}")
            .contentType("application/json")
            .when().post("/api/game/today/guesses")
            .then()
            .statusCode(400)
            .body("code", equalTo("bad_request"));
    }

    @Test
    void exposesGlobalStats() {
        String cookie = given()
            .when().get("/api/game/today")
            .then()
            .statusCode(200)
            .extract().header("Set-Cookie");

        given()
            .header("Cookie", cookie)
            .when().get("/api/stats/global")
            .then()
            .statusCode(200)
            .body("players", greaterThanOrEqualTo(1))
            .body("gamesStarted", greaterThanOrEqualTo(1))
            .body("completed", greaterThanOrEqualTo(0))
            .body("won", greaterThanOrEqualTo(0))
            .body("lost", greaterThanOrEqualTo(0));
    }
}
