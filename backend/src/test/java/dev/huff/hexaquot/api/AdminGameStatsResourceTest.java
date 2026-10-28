package dev.huff.hexaquot.api;

import dev.huff.hexaquot.auth.UserIds;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

@QuarkusTest
class AdminGameStatsResourceTest {
    @Inject EntityManager entityManager;

    @Test
    void playerSuggestionsRequireAdminAndReturnOnlyPublicLabels() {
        String adminSession = UUID.randomUUID().toString();
        String adminId = UserIds.anonymous(adminSession);
        createUser(adminId, "Admin", "@stats-admin");
        grantAdmin(adminId);

        String playerId = UserIds.anonymous("stats-player-" + UUID.randomUUID());
        createUser(playerId, "Stats Player", "@stats-player");

        given()
            .when().get("/api/admin/game-stats/users")
            .then().statusCode(401);

        given()
            .header("Cookie", cookie(UUID.randomUUID().toString()))
            .when().get("/api/admin/game-stats/users")
            .then().statusCode(403);

        given()
            .header("Cookie", cookie(adminSession))
            .queryParam("q", "stats-play")
            .when().get("/api/admin/game-stats/users")
            .then()
            .statusCode(200)
            .body("users", hasSize(1))
            .body("users[0].nickname", equalTo("@stats-player"))
            .body("users[0].displayName", equalTo("Stats Player"))
            .body("users[0].email", org.hamcrest.Matchers.nullValue())
            .body("users[0].id", org.hamcrest.Matchers.nullValue());

        String prefix = "@suggest-" + UUID.randomUUID().toString().substring(0, 8);
        for (int index = 0; index < 12; index++) {
            createUser(UserIds.anonymous("stats-suggestion-" + index + UUID.randomUUID()), "Suggestion " + index,
                prefix + "-" + index);
        }
        given()
            .header("Cookie", cookie(adminSession))
            .queryParam("q", prefix)
            .when().get("/api/admin/game-stats/users")
            .then().statusCode(200).body("users", hasSize(10));
    }

    @Test
    void chatRequiresAdminAndDoesNotRequirePlayerManagementPrivileges() {
        String adminSession = UUID.randomUUID().toString();
        String adminId = UserIds.anonymous(adminSession);
        createUser(adminId, "Stats Admin", "@stats-admin-two");
        grantAdmin(adminId);

        given()
            .contentType("application/json")
            .body("""
                {"message":"Classifica generale?","threadId":"%s","users":[]}
                """.formatted(UUID.randomUUID()))
            .when().post("/api/admin/game-stats/stream")
            .then().statusCode(401);

        given()
            .header("Cookie", cookie(UUID.randomUUID().toString()))
            .contentType("application/json")
            .body("""
                {"message":"Classifica generale?","threadId":"%s","users":[]}
                """.formatted(UUID.randomUUID()))
            .when().post("/api/admin/game-stats/stream")
            .then().statusCode(403);

        given()
            .header("Cookie", cookie(adminSession))
            .contentType("application/json")
            .body("""
                {"message":"Classifica generale?","threadId":"%s","users":["@missing-user"]}
                """.formatted(UUID.randomUUID()))
            .when().post("/api/admin/game-stats/stream")
            .then().statusCode(400);

        given()
            .header("Cookie", cookie(adminSession))
            .contentType("application/json")
            .body("""
                {"message":"Classifica generale?","threadId":"%s","users":["@a","@b","@c","@d","@e","@f","@g","@h","@i","@j","@k"]}
                """.formatted(UUID.randomUUID()))
            .when().post("/api/admin/game-stats/stream")
            .then().statusCode(400);

        given()
            .header("Cookie", cookie(adminSession))
            .contentType("application/json")
            .body("""
                {"message":"Classifica generale?","threadId":"%s","users":[]}
                """.formatted(UUID.randomUUID()))
            .when().post("/api/admin/game-stats/stream")
            .then()
            .statusCode(503)
            .body("code", equalTo("stats_agent_unavailable"));
    }

    private void createUser(String id, String displayName, String nickname) {
        QuarkusTransaction.requiringNew().run(() -> entityManager.createNativeQuery(
                "INSERT INTO users (id, email, display_name, nickname, profile_emoji, created_at, star_available) "
                    + "VALUES (?1, 'private@example.test', ?2, ?3, '😀', ?4, false) ON CONFLICT (id) DO NOTHING")
            .setParameter(1, id)
            .setParameter(2, displayName)
            .setParameter(3, nickname)
            .setParameter(4, Instant.now().toString())
            .executeUpdate());
    }

    private void grantAdmin(String userId) {
        QuarkusTransaction.requiringNew().run(() -> entityManager.createNativeQuery(
                "INSERT INTO admin_users (user_id, can_view_players, can_view_player_details, can_manage_players, "
                    + "can_manage_admins, can_manage_hexaflow_puzzles, created_at, updated_at) "
                    + "VALUES (?1, false, false, false, false, false, ?2, ?2) ON CONFLICT (user_id) DO NOTHING")
            .setParameter(1, userId)
            .setParameter(2, Instant.now().toString())
            .executeUpdate());
    }

    private String cookie(String sessionId) {
        return "huff_session=" + sessionId;
    }
}
