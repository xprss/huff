package dev.huff.hexaquot.api;

import dev.huff.hexaquot.auth.ResolvedUser;
import dev.huff.hexaquot.auth.UserService;
import dev.huff.hexaquot.game.HexaecoDailyGameService;
import dev.huff.hexaquot.game.HexaecoDtos.SubmitRequest;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;

@Path("/api/hexaeco")
public class HexaecoResource {
    @Inject UserService userService;
    @Inject HexaecoDailyGameService service;
    @Inject dev.huff.hexaquot.leaderboard.LeaderboardService leaderboard;

    @GET @Path("/today")
    public Response today(@CookieParam("huff_session") String session) {
        ResolvedUser user = userService.resolve(session);
        return user.user() == null ? unauthorized() : withCookie(Response.ok(service.today(user.user())), user).build();
    }

    @POST @Path("/today/submissions")
    public Response submit(@CookieParam("huff_session") String session, SubmitRequest request) {
        ResolvedUser user = userService.resolve(session);
        return user.user() == null ? unauthorized() : withCookie(Response.ok(service.submit(user.user(), request)), user).build();
    }

    @GET @Path("/stats")
    public Response stats(@CookieParam("huff_session") String session) {
        ResolvedUser user = userService.resolve(session);
        return user.user() == null ? unauthorized() : withCookie(Response.ok(service.statsForUserId(user.user().id())), user).build();
    }

    @GET @Path("/leaderboards")
    public Response leaderboards(@CookieParam("huff_session") String session) {
        ResolvedUser user = userService.resolve(session);
        return user.user() == null ? unauthorized() : withCookie(Response.ok(
            leaderboard.leaderboards(dev.huff.hexaquot.leaderboard.LeaderboardRepository.Board.HEXAECO)), user).build();
    }

    private Response.ResponseBuilder withCookie(Response.ResponseBuilder response, ResolvedUser user) {
        return user.setCookieHeader() == null ? response : response.header("Set-Cookie", user.setCookieHeader());
    }

    private Response unauthorized() {
        return Response.status(Response.Status.UNAUTHORIZED)
            .entity(new GameResource.ErrorDto("token_required", "Token Bearer valido richiesto.")).build();
    }
}
