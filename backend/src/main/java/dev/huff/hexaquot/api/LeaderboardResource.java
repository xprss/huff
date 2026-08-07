package dev.huff.hexaquot.api;

import dev.huff.hexaquot.leaderboard.LeaderboardService;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

@Path("/api")
public class LeaderboardResource {
    @Inject
    LeaderboardService leaderboardService;

    @GET
    @Path("/leaderboards")
    public Object leaderboards() {
        return leaderboardService.leaderboards();
    }

    @GET
    @Path("/player/{nickname}")
    public Object publicProfile(@PathParam("nickname") String nickname) {
        return leaderboardService.publicProfile(nickname);
    }
}
