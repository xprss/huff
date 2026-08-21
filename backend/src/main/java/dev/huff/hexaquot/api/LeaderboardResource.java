package dev.huff.hexaquot.api;

import dev.huff.hexaquot.leaderboard.LeaderboardService;
import dev.huff.hexaquot.leaderboard.LeaderboardRepository;
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
    @Path("/hexaword/leaderboards")
    public Object hexawordLeaderboards() {
        return leaderboardService.leaderboards(LeaderboardRepository.Board.HEXAWORD);
    }

    @GET
    @Path("/hexahack/leaderboards")
    public Object hexahackLeaderboards() {
        return leaderboardService.leaderboards(LeaderboardRepository.Board.HEXAHACK);
    }

    @GET
    @Path("/hexasky/leaderboards")
    public Object hexaskyLeaderboards() {
        return leaderboardService.leaderboards(LeaderboardRepository.Board.HEXASKY);
    }

    @GET
    @Path("/leaderboards/overall")
    public Object overallLeaderboards() {
        return leaderboardService.leaderboards(LeaderboardRepository.Board.OVERALL);
    }

    @GET
    @Path("/player/{nickname}")
    public Object publicProfile(@PathParam("nickname") String nickname) {
        return leaderboardService.publicProfile(nickname);
    }
}
