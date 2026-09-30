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

@QuarkusTest
class ScheduledAlertResourceTest {
    @Inject EntityManager entityManager;

    @Test
    void adminCanScheduleEditAndDeleteWhilePlayersCannot() {
        String session = UUID.randomUUID().toString();
        given().header("Cookie", cookie(session)).when().get("/api/me").then().statusCode(200);
        given().header("Cookie", cookie(session)).when().get("/api/admin/alerts").then().statusCode(403);

        QuarkusTransaction.requiringNew().run(() -> entityManager.createNativeQuery("""
            INSERT INTO admin_users (user_id, can_view_players, can_view_player_details,
              can_manage_players, can_manage_admins, created_at, updated_at)
            VALUES (?1, true, true, true, true, ?2, ?2)
            """).setParameter(1, UserIds.anonymous(session)).setParameter(2, Instant.now().toString()).executeUpdate());

        String future = Instant.now().plusSeconds(3600).toString();
        String id = given().header("Cookie", cookie(session)).contentType("application/json")
            .body(new Request("MODAL", "Titolo", "Messaggio", "/#/games", future))
            .when().post("/api/admin/alerts").then().statusCode(200)
            .body("title", equalTo("Titolo")).extract().path("id");
        given().header("Cookie", cookie(session)).contentType("application/json")
            .body(new Request("MODAL", "Aggiornato", "Testo", null, future))
            .when().put("/api/admin/alerts/{id}", id).then().statusCode(200)
            .body("title", equalTo("Aggiornato"));
        given().header("Cookie", cookie(session)).when().delete("/api/admin/alerts/{id}", id).then().statusCode(204);
    }

    private String cookie(String session) { return "huff_session=" + session; }
    record Request(String type, String title, String body, String url, String scheduledAt) {}
}
