package dev.huff.hexaquot.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import io.quarkus.test.junit.QuarkusTest;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

@QuarkusTest
class AvatarResourceTest {
    private static final byte[] JPEG = jpeg(64);

    @Test
    void uploadsServesAndDeletesTheProfilePhoto() {
        String cookie = sessionCookie();

        String avatarUrl = given()
            .header("Cookie", cookie)
            .contentType("image/jpeg")
            .body(JPEG)
            .when().put("/api/me/avatar")
            .then()
            .statusCode(200)
            .body("avatarUrl", startsWith("/api/avatars/"))
            .extract().path("avatarUrl");

        given()
            .header("Cookie", cookie)
            .when().get("/api/me")
            .then()
            .statusCode(200)
            .body("user.avatarUrl", equalTo(avatarUrl));

        byte[] served = given()
            .header("Cookie", cookie)
            .when().get(avatarUrl)
            .then()
            .statusCode(200)
            .contentType("image/jpeg")
            .header("Cache-Control", equalTo(AvatarResource.CACHE_CONTROL))
            .extract().asByteArray();
        assertArrayEquals(JPEG, served);

        given()
            .header("Cookie", cookie)
            .when().delete("/api/me/avatar")
            .then()
            .statusCode(200)
            .body("avatarUrl", nullValue());

        given()
            .header("Cookie", cookie)
            .when().get(avatarUrl)
            .then()
            .statusCode(404)
            .header("Cache-Control", equalTo("no-store"));
    }

    @Test
    void rejectsPhotosLargerThanTwoMegabytes() {
        given()
            .header("Cookie", sessionCookie())
            .contentType("image/jpeg")
            .body(jpeg(2 * 1024 * 1024 + 1))
            .when().put("/api/me/avatar")
            .then()
            .statusCode(413);
    }

    @Test
    void rejectsFilesThatAreNotImages() {
        given()
            .header("Cookie", sessionCookie())
            .contentType("image/png")
            .body("not really a png".getBytes())
            .when().put("/api/me/avatar")
            .then()
            .statusCode(415);
    }

    @Test
    void doesNotServePathsOutsideTheAvatarDirectory() {
        given()
            .header("Cookie", sessionCookie())
            .when().get("/api/avatars/..%2Fapplication.properties")
            .then()
            .statusCode(404);
    }

    private static String sessionCookie() {
        return given()
            .when().get("/api/me")
            .then()
            .statusCode(200)
            .extract().header("Set-Cookie");
    }

    /** JPEG magic bytes followed by filler: enough for format detection. */
    private static byte[] jpeg(int size) {
        byte[] bytes = new byte[size];
        Arrays.fill(bytes, (byte) 0x42);
        bytes[0] = (byte) 0xFF;
        bytes[1] = (byte) 0xD8;
        bytes[2] = (byte) 0xFF;
        return bytes;
    }
}
