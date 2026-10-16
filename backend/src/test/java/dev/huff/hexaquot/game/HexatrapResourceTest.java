package dev.huff.hexaquot.game;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class HexatrapResourceTest {
    @Test
    void endpointsExposeOnlyPublicStateAndRejectNonIntegerNumbers() {
        String date = given().get("/api/hexatrap/today").then().statusCode(200)
            .body("rulesVersion", equalTo(1)).body("dossier.litCount", anyOf(equalTo(2),equalTo(4)))
            .body("secretNumber", nullValue()).body("configuration",nullValue())
            .extract().path("puzzleDate");
        for (String number : new String[]{"1.5", "\"42\"", "9865", "null"}) {
            given().contentType("application/json").header("X-Huff-Request","1")
                .body("{\"requestId\":\"invalid-number-"+number.replace('"','_')+"\",\"puzzleDate\":\""+date+"\",\"rulesVersion\":1,\"number\":"+number+"}")
                .post("/api/hexatrap/today/submissions").then().statusCode(400);
        }
        for (String question : new String[]{
            "\"rulesVersion\":1.5,\"type\":\"SEAL\",\"advisor\":0,\"seal\":0",
            "\"rulesVersion\":1,\"type\":\"SEAL\",\"advisor\":0.5,\"seal\":0",
            "\"rulesVersion\":1,\"type\":\"SEAL\",\"advisor\":0,\"seal\":0.5"}) {
            given().contentType("application/json").header("X-Huff-Request","1")
                .body("{\"requestId\":\"invalid-question\",\"puzzleDate\":\""+date+"\","+question+"}")
                .post("/api/hexatrap/today/questions").then().statusCode(400);
        }
        given().get("/api/hexatrap/today/events?limit=101").then().statusCode(400);
        given().get("/api/hexatrap/today/events?before=0").then().statusCode(400);
        given().get("/api/hexatrap/leaderboards").then().statusCode(200).body("weekly.entries",notNullValue());
        given().get("/api/leaderboards?game=hexatrap").then().statusCode(200);
    }
}
