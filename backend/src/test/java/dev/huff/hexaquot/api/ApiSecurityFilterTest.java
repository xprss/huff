package dev.huff.hexaquot.api;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.Method;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

@QuarkusTest
@TestProfile(AuthEnabledTestProfile.class)
class ApiSecurityFilterTest {
    @ParameterizedTest(name = "{0} {1} requires a Bearer-authenticated identity")
    @MethodSource("privateEndpoints")
    void rejectsEveryApiEndpointWithoutBearerIdentity(Method method, String path) {
        given()
            .contentType("application/json")
            .body("{}")
            .when().request(method, path)
            .then()
            .statusCode(401)
            .header("Cache-Control", equalTo("no-store"))
            .header("X-Content-Type-Options", equalTo("nosniff"))
            .header("WWW-Authenticate", equalTo("Bearer"))
            .body("code", equalTo("token_required"));
    }

    @Test
    void rejectsAStaleAnonymousCookieInsteadOfRestoringAnAnonymousUser() {
        given()
            .header("Cookie", "huff_session=4c8d7d25-2680-4d48-b7c3-3245274ff20b")
            .when().get("/api/me")
            .then()
            .statusCode(401)
            .header("Set-Cookie", containsString("huff_session=;"))
            .body("code", equalTo("token_required"));
    }

    @Test
    void rejectsAggregateAndBootstrapReadsWithoutAnIdentity() {
        given()
            .when().get("/api/stats/global")
            .then()
            .statusCode(401)
            .body("code", equalTo("token_required"));

        given()
            .when().get("/api/push/settings")
            .then()
            .statusCode(401)
            .body("code", equalTo("token_required"));
    }

    @Test
    @TestSecurity(user = "verified-google-subject")
    void allowsVerifiedIdentityToReadPrivateEndpoints() {
        given()
            .when().get("/api/me")
            .then()
            .statusCode(200)
            .body("loggedIn", equalTo(true))
            .body("user.authenticated", equalTo(true));
    }

    @Test
    @TestSecurity(user = "verified-google-subject")
    void allowsBearerAuthenticatedMutationWithoutCsrfHeader() {
        given()
            .contentType("application/json")
            .body("{\"mode\":\"CLASSIC\"}")
            .when().post("/api/game/today/mode")
            .then()
            .statusCode(200)
            .body("mode", equalTo("CLASSIC"));
    }

    @Test
    @TestSecurity(user = "verified-google-subject")
    void acceptsBearerAuthorizationHeader() {
        given()
            .header("Authorization", "Bearer verified-access-token")
            .contentType("application/json")
            .body("{\"mode\":\"CLASSIC\"}")
            .when().post("/api/game/today/mode")
            .then()
            .statusCode(200)
            .body("mode", equalTo("CLASSIC"));
    }

    private static Stream<Arguments> privateEndpoints() {
        return Stream.of(
            Arguments.of(Method.GET, "/api/me"),
            Arguments.of(Method.PUT, "/api/me/profile"),
            Arguments.of(Method.GET, "/api/game/today"),
            Arguments.of(Method.POST, "/api/game/today/mode"),
            Arguments.of(Method.POST, "/api/game/today/guesses"),
            Arguments.of(Method.POST, "/api/game/today/kitten"),
            Arguments.of(Method.POST, "/api/game/today/star"),
            Arguments.of(Method.GET, "/api/stats"),
            Arguments.of(Method.POST, "/api/push/subscriptions"),
            Arguments.of(Method.DELETE, "/api/push/subscriptions"),
            Arguments.of(Method.GET, "/api/admin/players"),
            Arguments.of(Method.GET, "/api/admin/players/missing"),
            Arguments.of(Method.PUT, "/api/admin/players/missing"),
            Arguments.of(Method.DELETE, "/api/admin/players/missing")
        );
    }
}
