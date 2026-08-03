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
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;

@QuarkusTest
class AdminResourceTest {
    @Inject
    EntityManager entityManager;

    @Test
    void rejectsNonAdminUsers() {
        String sessionId = UUID.randomUUID().toString();

        given()
            .header("Cookie", cookie(sessionId))
            .when().get("/api/admin/players")
            .then()
            .statusCode(403)
            .body("code", equalTo("request_failed"));
    }

    @Test
    void exposesAdminPlayerWorkflow() {
        String adminSessionId = UUID.randomUUID().toString();
        String adminId = UserIds.anonymous(adminSessionId);
        grantAdmin(adminId);

        String targetSessionId = UUID.randomUUID().toString();
        String targetId = UserIds.anonymous(targetSessionId);
        given()
            .header("Cookie", cookie(targetSessionId))
            .when().get("/api/me")
            .then()
            .statusCode(200);
        createCompletedGameAndSubscription(targetId);

        given()
            .header("Cookie", cookie(adminSessionId))
            .when().get("/api/me")
            .then()
            .statusCode(200)
            .body("user.admin.canViewPlayers", equalTo(true))
            .body("user.admin.canManagePlayers", equalTo(true));

        given()
            .header("Cookie", cookie(adminSessionId))
            .when().get("/api/admin/players")
            .then()
            .statusCode(200)
            .body("players.size()", greaterThanOrEqualTo(1))
            .body("pageSize", equalTo(10));

        given()
            .header("Cookie", cookie(adminSessionId))
            .when().get("/api/admin/players/{userId}", targetId)
            .then()
            .statusCode(200)
            .body("player.id", equalTo(targetId))
            .body("games[0].solution", equalTo("abbaco"))
            .body("games[0].guesses[0].word", equalTo("abbaco"));

        given()
            .header("Cookie", cookie(adminSessionId))
            .contentType("application/json")
            .body("""
                {
                  "displayName": "Admin Updated",
                  "nickname": "@admin-updated",
                  "profileEmoji": "😎",
                  "starAvailable": true,
                  "starAwardedAt": "2099-01-01T00:00:00Z",
                  "starUsedAt": null
                }
                """)
            .when().put("/api/admin/players/{userId}", targetId)
            .then()
            .statusCode(200)
            .body("player.displayName", equalTo("Admin Updated"))
            .body("player.nickname", equalTo("@admin-updated"))
            .body("player.profileEmoji", equalTo("😎"))
            .body("player.starAvailable", equalTo(true));

        given()
            .header("Cookie", cookie(adminSessionId))
            .when().delete("/api/admin/players/{userId}", targetId)
            .then()
            .statusCode(200)
            .body("users", equalTo(1))
            .body("games", equalTo(1))
            .body("pushSubscriptions", equalTo(1));

        given()
            .header("Cookie", cookie(adminSessionId))
            .when().get("/api/admin/players/{userId}", targetId)
            .then()
            .statusCode(404);
    }

    @Test
    void listsPlayersWithBackendSortingAndPagination() {
        String adminSessionId = UUID.randomUUID().toString();
        String adminId = UserIds.anonymous(adminSessionId);
        grantAdmin(adminId);

        String lowGamesId = createUser("Admin Sort Low", "@admin-sort-low");
        createCompletedGame(lowGamesId, "2099-01-02", "2099-01-02T12:00:00Z");

        String highGamesId = createUser("Admin Sort High", "@admin-sort-high");
        createCompletedGame(highGamesId, "2099-01-03", "2099-01-03T12:00:00Z");
        createCompletedGame(highGamesId, "2099-01-04", "2099-01-04T12:00:00Z");

        String recentId = createUser("Admin Sort Recent", "@admin-sort-recent");
        createCompletedGame(recentId, "2099-01-05", "2099-01-05T12:00:00Z");

        for (int index = 0; index < 11; index++) {
            createUser(String.format("Admin Page %02d", index), String.format("@admin-page-%02d", index));
        }

        given()
            .header("Cookie", cookie(adminSessionId))
            .queryParam("q", "admin-page")
            .queryParam("page", 0)
            .when().get("/api/admin/players")
            .then()
            .statusCode(200)
            .body("players", hasSize(10))
            .body("page", equalTo(0))
            .body("pageSize", equalTo(10))
            .body("totalPlayers", equalTo(11))
            .body("totalPages", equalTo(2));

        given()
            .header("Cookie", cookie(adminSessionId))
            .queryParam("q", "admin-page")
            .queryParam("page", 1)
            .when().get("/api/admin/players")
            .then()
            .statusCode(200)
            .body("players", hasSize(1))
            .body("page", equalTo(1));

        given()
            .header("Cookie", cookie(adminSessionId))
            .queryParam("q", "admin-sort")
            .queryParam("sort", "games-played")
            .when().get("/api/admin/players")
            .then()
            .statusCode(200)
            .body("sort", equalTo("games-played"))
            .body("players[0].id", equalTo(highGamesId));

        given()
            .header("Cookie", cookie(adminSessionId))
            .queryParam("q", "admin-sort")
            .queryParam("sort", "recent-game")
            .when().get("/api/admin/players")
            .then()
            .statusCode(200)
            .body("sort", equalTo("recent-game"))
            .body("players[0].id", equalTo(recentId));
    }

    private void grantAdmin(String userId) {
        QuarkusTransaction.requiringNew().run(() -> {
            String now = Instant.now().toString();
            entityManager
                .createNativeQuery(
                    "INSERT INTO admin_users "
                        + "(user_id, can_view_players, can_view_player_details, can_manage_players, can_manage_admins, created_at, updated_at) "
                        + "VALUES (?1, true, true, true, true, ?2, ?2)"
                )
                .setParameter(1, userId)
                .setParameter(2, now)
                .executeUpdate();
        });
    }

    private void createCompletedGameAndSubscription(String userId) {
        QuarkusTransaction.requiringNew().run(() -> {
            String now = Instant.now().toString();
            entityManager
                .createNativeQuery(
                    "INSERT INTO games "
                        + "(id, user_id, puzzle_date, mode, solution, guesses_json, status, mouse_revealed, "
                        + "kitten_unlocked, created_at, updated_at, completed_at) "
                        + "VALUES (?1, ?2, '2099-01-01', 'CLASSIC', 'abbaco', ?3, 'WON', true, false, ?4, ?4, ?4)"
                )
                .setParameter(1, UUID.randomUUID().toString())
                .setParameter(2, userId)
                .setParameter(3, """
                    [{"word":"abbaco","tiles":[
                      {"letter":"a","state":"CORRECT"},
                      {"letter":"b","state":"CORRECT"},
                      {"letter":"b","state":"CORRECT"},
                      {"letter":"a","state":"CORRECT"},
                      {"letter":"c","state":"CORRECT"},
                      {"letter":"o","state":"CORRECT"}
                    ]}]
                    """)
                .setParameter(4, now)
                .executeUpdate();

            entityManager
                .createNativeQuery(
                    "INSERT INTO push_subscriptions "
                        + "(id, user_id, endpoint, p256dh, auth, created_at, updated_at) "
                        + "VALUES (?1, ?2, ?3, 'key', 'auth', ?4, ?4)"
                )
                .setParameter(1, UUID.randomUUID().toString())
                .setParameter(2, userId)
                .setParameter(3, "https://example.test/" + UUID.randomUUID())
                .setParameter(4, now)
                .executeUpdate();
        });
    }

    private String createUser(String displayName, String nickname) {
        String userId = UserIds.anonymous("admin-test-" + UUID.randomUUID());
        QuarkusTransaction.requiringNew().run(() -> entityManager
            .createNativeQuery(
                "INSERT INTO users "
                    + "(id, email, display_name, nickname, profile_emoji, created_at, star_available) "
                    + "VALUES (?1, null, ?2, ?3, '🙂', ?4, false)"
            )
            .setParameter(1, userId)
            .setParameter(2, displayName)
            .setParameter(3, nickname)
            .setParameter(4, Instant.now().toString())
            .executeUpdate());
        return userId;
    }

    private void createCompletedGame(String userId, String puzzleDate, String timestamp) {
        QuarkusTransaction.requiringNew().run(() -> entityManager
            .createNativeQuery(
                "INSERT INTO games "
                    + "(id, user_id, puzzle_date, mode, solution, guesses_json, status, mouse_revealed, "
                    + "kitten_unlocked, created_at, updated_at, completed_at) "
                    + "VALUES (?1, ?2, ?3, 'CLASSIC', 'abbaco', ?4, 'WON', true, false, ?5, ?5, ?5)"
            )
            .setParameter(1, UUID.randomUUID().toString())
            .setParameter(2, userId)
            .setParameter(3, puzzleDate)
            .setParameter(4, """
                [{"word":"abbaco","tiles":[
                  {"letter":"a","state":"CORRECT"},
                  {"letter":"b","state":"CORRECT"},
                  {"letter":"b","state":"CORRECT"},
                  {"letter":"a","state":"CORRECT"},
                  {"letter":"c","state":"CORRECT"},
                  {"letter":"o","state":"CORRECT"}
                ]}]
                """)
            .setParameter(5, timestamp)
            .executeUpdate());
    }

    private String cookie(String sessionId) {
        return "huff_session=" + sessionId;
    }
}
