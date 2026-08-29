package dev.huff.hexaquot.api;

import dev.huff.hexaquot.leaderboard.LeaderboardService;
import dev.huff.hexaquot.leaderboard.LeaderboardRepository;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;

@Path("/api")
public class LeaderboardResource {
    @Inject
    LeaderboardService leaderboardService;

    @GET
    @Path("/leaderboards")
    public Object leaderboards(@QueryParam("game") String game) {
        return leaderboardService.leaderboards(boardFor(game));
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

    @GET @Path("/hexaflow/leaderboards") public Object hexaflowLeaderboards() {
        return leaderboardService.leaderboards(LeaderboardRepository.Board.HEXAFLOW);
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

    private LeaderboardRepository.Board boardFor(String game) {
        if (game == null || game.isBlank() || game.equalsIgnoreCase("hexaword")) {
            return LeaderboardRepository.Board.HEXAWORD;
        }
        return switch (game.toLowerCase(java.util.Locale.ROOT)) {
            case "overall" -> LeaderboardRepository.Board.OVERALL;
            case "hexahack" -> LeaderboardRepository.Board.HEXAHACK;
            case "hexasky" -> LeaderboardRepository.Board.HEXASKY;
            case "hexaflow" -> LeaderboardRepository.Board.HEXAFLOW;
            default -> throw new jakarta.ws.rs.BadRequestException("Gioco della leaderboard non valido.");
        };
    }
}
