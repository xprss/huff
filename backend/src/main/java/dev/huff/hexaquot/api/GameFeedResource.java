package dev.huff.hexaquot.api;

import dev.huff.hexaquot.game.GameFeedService;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

@Path("/rss.xml")
public class GameFeedResource {
    @Inject
    GameFeedService gameFeedService;

    @GET
    public Response rss() {
        return Response.ok(gameFeedService.rss())
            .type("application/rss+xml; charset=UTF-8")
            .build();
    }
}
