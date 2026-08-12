package dev.huff.hexaquot.api;

import dev.huff.hexaquot.auth.ResolvedUser;
import dev.huff.hexaquot.auth.UserService;
import dev.huff.hexaquot.game.GuessRequest;
import dev.huff.hexaquot.game.HexadigitDailyGameService;
import dev.huff.hexaquot.leaderboard.LeaderboardRepository;
import dev.huff.hexaquot.leaderboard.LeaderboardService;
import jakarta.inject.Inject;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

@Path("/api/hexadigit")
public class HexadigitResource {
    @Inject UserService userService;
    @Inject HexadigitDailyGameService service;
    @Inject LeaderboardService leaderboardService;

    @GET @Path("/today")
    public Response today(@CookieParam("huff_session") String sessionId) {
        ResolvedUser user = userService.resolve(sessionId);
        if (user.user() == null) return unauthorized();
        return withCookie(Response.ok(service.today(user.user())), user).build();
    }

    @POST @Path("/today/guesses")
    public Response guess(@CookieParam("huff_session") String sessionId, GuessRequest request) {
        ResolvedUser user = userService.resolve(sessionId);
        if (user.user() == null) return unauthorized();
        return withCookie(Response.ok(service.guess(user.user(), request == null ? null : request.guess())), user).build();
    }

    @GET @Path("/stats")
    public Response stats(@CookieParam("huff_session") String sessionId) {
        ResolvedUser user = userService.resolve(sessionId);
        if (user.user() == null) return unauthorized();
        return withCookie(Response.ok(service.stats(user.user())), user).build();
    }

    @GET @Path("/leaderboards")
    public Object leaderboards() {
        return leaderboardService.leaderboards(LeaderboardRepository.Board.HEXADIGIT);
    }

    private Response.ResponseBuilder withCookie(Response.ResponseBuilder response, ResolvedUser user) {
        return user.setCookieHeader() == null ? response : response.header("Set-Cookie", user.setCookieHeader());
    }

    private Response unauthorized() {
        return Response.status(Response.Status.UNAUTHORIZED)
            .entity(new GameResource.ErrorDto("token_required", "Token Bearer valido richiesto.")).build();
    }
}
