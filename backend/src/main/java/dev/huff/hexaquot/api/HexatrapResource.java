package dev.huff.hexaquot.api;

import dev.huff.hexaquot.auth.ResolvedUser;
import dev.huff.hexaquot.auth.UserService;
import dev.huff.hexaquot.game.HexatrapDailyGameService;
import dev.huff.hexaquot.game.HexatrapDtos.ClueRequest;
import dev.huff.hexaquot.game.HexatrapDtos.QuestionRequest;
import dev.huff.hexaquot.game.HexatrapDtos.VerdictRequest;
import dev.huff.hexaquot.leaderboard.LeaderboardRepository;
import dev.huff.hexaquot.leaderboard.LeaderboardService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;

import java.util.function.Function;

@Path("/api/hexatrap")
public class HexatrapResource {
    @Inject UserService userService;
    @Inject HexatrapDailyGameService service;
    @Inject LeaderboardService leaderboard;

    @GET @Path("/today")
    public Response today(@CookieParam("huff_session") String session) {
        return run(session, user -> service.today(user.user()));
    }

    @POST @Path("/today/questions")
    public Response question(@CookieParam("huff_session") String session, QuestionRequest request) {
        return run(session, user -> service.question(user.user(), request));
    }

    @POST @Path("/today/clues")
    public Response clue(@CookieParam("huff_session") String session, ClueRequest request) {
        return run(session, user -> service.clue(user.user(), request));
    }

    @POST @Path("/today/verdict")
    public Response verdict(@CookieParam("huff_session") String session, VerdictRequest request) {
        return run(session, user -> service.verdict(user.user(), request));
    }

    @GET @Path("/stats")
    public Response stats(@CookieParam("huff_session") String session) {
        return run(session, user -> service.statsForUserId(user.user().id()));
    }

    @GET @Path("/leaderboards")
    public Response leaderboards(@CookieParam("huff_session") String session) {
        return run(session, user -> leaderboard.leaderboards(LeaderboardRepository.Board.HEXATRAP));
    }

    private Response run(String session, Function<ResolvedUser, Object> action) {
        ResolvedUser user = userService.resolve(session);
        if (user.user() == null) return Response.status(Response.Status.UNAUTHORIZED)
            .entity(new GameResource.ErrorDto("token_required", "Token Bearer valido richiesto.")).build();
        Response.ResponseBuilder response = Response.ok(action.apply(user));
        if (user.setCookieHeader() != null) response.header("Set-Cookie", user.setCookieHeader());
        return response.build();
    }
}
