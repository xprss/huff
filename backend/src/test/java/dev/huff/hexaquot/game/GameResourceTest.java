package dev.huff.hexaquot.game;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;

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
            .body("user.nickname", startsWith("@giocatore-"))
            .body("user.profileEmoji", equalTo("😀"))
            .body("user.bio", equalTo(null))
            .body("authEnabled", equalTo(false));
    }

    @Test
    void updatesCurrentUserProfileAndKeepsCustomDisplayNameOnResolve() {
        String cookie = given()
            .when().get("/api/me")
            .then()
            .statusCode(200)
            .extract().header("Set-Cookie");

        given()
            .header("Cookie", cookie)
            .body("""
                {
                  "displayName": "Nome Profilo",
                  "nickname": "Profilo.Test",
                  "profileEmoji": "😎",
                  "bio": "Bio profilo breve"
                }
                """)
            .contentType("application/json")
            .when().put("/api/me/profile")
            .then()
            .statusCode(200)
            .body("displayName", equalTo("Nome Profilo"))
            .body("nickname", equalTo("@profilo.test"))
            .body("profileEmoji", equalTo("😎"))
            .body("bio", equalTo("Bio profilo breve"));

        given()
            .header("Cookie", cookie)
            .when().get("/api/me")
            .then()
            .statusCode(200)
            .body("user.displayName", equalTo("Nome Profilo"))
            .body("user.nickname", equalTo("@profilo.test"))
            .body("user.profileEmoji", equalTo("😎"))
            .body("user.bio", equalTo("Bio profilo breve"));
    }

    @Test
    void rejectsDuplicateNickname() {
        String firstCookie = given()
            .when().get("/api/me")
            .then()
            .statusCode(200)
            .extract().header("Set-Cookie");
        String secondCookie = given()
            .when().get("/api/me")
            .then()
            .statusCode(200)
            .extract().header("Set-Cookie");

        given()
            .header("Cookie", firstCookie)
            .body("{\"displayName\":\"Primo\",\"nickname\":\"@duplicato\",\"profileEmoji\":\"😀\"}")
            .contentType("application/json")
            .when().put("/api/me/profile")
            .then()
            .statusCode(200);

        given()
            .header("Cookie", secondCookie)
            .body("{\"displayName\":\"Secondo\",\"nickname\":\"duplicato\",\"profileEmoji\":\"😄\"}")
            .contentType("application/json")
            .when().put("/api/me/profile")
            .then()
            .statusCode(409)
            .body("code", equalTo("request_failed"));
    }

    @Test
    void rejectsInvalidProfileUpdates() {
        String cookie = given()
            .when().get("/api/me")
            .then()
            .statusCode(200)
            .extract().header("Set-Cookie");

        given()
            .header("Cookie", cookie)
            .body("{\"displayName\":\"   \",\"nickname\":\"@valido\",\"profileEmoji\":\"😀\"}")
            .contentType("application/json")
            .when().put("/api/me/profile")
            .then()
            .statusCode(400)
            .body("code", equalTo("bad_request"));

        given()
            .header("Cookie", cookie)
            .body("{\"displayName\":\"Nome\",\"nickname\":\"@non valido\",\"profileEmoji\":\"😀\"}")
            .contentType("application/json")
            .when().put("/api/me/profile")
            .then()
            .statusCode(400)
            .body("code", equalTo("bad_request"));

        given()
            .header("Cookie", cookie)
            .body("{\"displayName\":\"Nome\",\"nickname\":\"@abcdefghijklmnopqrstuvwxyz1234\",\"profileEmoji\":\"😀\"}")
            .contentType("application/json")
            .when().put("/api/me/profile")
            .then()
            .statusCode(400)
            .body("code", equalTo("bad_request"));

        given()
            .header("Cookie", cookie)
            .body("{\"displayName\":\"Nome\",\"nickname\":\"@valido\",\"profileEmoji\":\"🔥\"}")
            .contentType("application/json")
            .when().put("/api/me/profile")
            .then()
            .statusCode(400)
            .body("code", equalTo("bad_request"));

        given()
            .header("Cookie", cookie)
            .body("{\"displayName\":\"Nome\",\"nickname\":\"@valido\",\"profileEmoji\":\"😀\",\"bio\":\"" + "a".repeat(201) + "\"}")
            .contentType("application/json")
            .when().put("/api/me/profile")
            .then()
            .statusCode(400)
            .body("code", equalTo("bad_request"));

    }

    @Test
    void exposesDailyModeChoiceAndPersistsSessionCookie() {
        given()
            .when().get("/api/game/today")
            .then()
            .statusCode(200)
            .header("Set-Cookie", notNullValue())
            .body("game", equalTo(null))
            .body("modes[0].mode", equalTo("CLASSIC"))
            .body("modes[1].mode", equalTo("MISCHIEVOUS_MOUSE"));
    }

    @Test
    void createsAnonymousDailyGameAfterModeSelection() {
        String cookie = given()
            .when().get("/api/game/today")
            .then()
            .statusCode(200)
            .extract().header("Set-Cookie");

        given()
            .header("Cookie", cookie)
            .body("{\"mode\":\"CLASSIC\"}")
            .contentType("application/json")
            .when().post("/api/game/today/mode")
            .then()
            .statusCode(200)
            .body("answerLength", equalTo(6))
            .body("maxAttempts", equalTo(6))
            .body("mode", equalTo("CLASSIC"))
            .body("status", equalTo("IN_PROGRESS"));
    }

    @Test
    void rejectsWordsOutsideTheDictionary() {
        String cookie = given()
            .body("{\"mode\":\"CLASSIC\"}")
            .contentType("application/json")
            .when().post("/api/game/today/mode")
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
    void rejectsGuessWithAnEmptyCell() {
        String cookie = given()
            .body("{\"mode\":\"CLASSIC\"}")
            .contentType("application/json")
            .when().post("/api/game/today/mode")
            .then()
            .statusCode(200)
            .extract().header("Set-Cookie");

        given()
            .header("Cookie", cookie)
            .body("{\"guess\":\"ab  de\"}")
            .contentType("application/json")
            .when().post("/api/game/today/guesses")
            .then()
            .statusCode(400)
            .body("code", equalTo("bad_request"))
            .body("message", equalTo("Completa tutte e 6 le caselle."));
    }

    @Test
    void exposesGlobalStats() {
        String cookie = given()
            .body("{\"mode\":\"CLASSIC\"}")
            .contentType("application/json")
            .when().post("/api/game/today/mode")
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

    @Test
    void exposesDisabledPushSettingsWhenVapidKeysAreMissing() {
        given()
            .when().get("/api/push/settings")
            .then()
            .statusCode(200)
            .body("supported", equalTo(false))
            .body("publicKey", equalTo(null));
    }

    @Test
    void rejectsPushSubscriptionWhenVapidKeysAreMissing() {
        given()
            .body("""
                {
                  "endpoint": "https://example.test/push",
                  "keys": {
                    "p256dh": "key",
                    "auth": "auth"
                  }
                }
                """)
            .contentType("application/json")
            .when().post("/api/push/subscriptions")
            .then()
            .statusCode(400)
            .body("code", equalTo("bad_request"));
    }
}
