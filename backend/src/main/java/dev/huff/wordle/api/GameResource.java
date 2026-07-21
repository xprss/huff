package dev.huff.wordle.api;

import dev.huff.wordle.auth.ResolvedUser;
import dev.huff.wordle.auth.UserService;
import dev.huff.wordle.game.DailyGameService;
import dev.huff.wordle.game.GuessRequest;
import jakarta.inject.Inject;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

@Path("/api")
public class GameResource {
    @Inject
    UserService userService;

    @Inject
    DailyGameService dailyGameService;

    @GET
    @Path("/game/today")
    public Response today(@CookieParam("huff_session") String sessionId) {
        ResolvedUser resolvedUser = userService.resolve(sessionId);
        if (resolvedUser.user() == null) {
            return unauthorized(resolvedUser.loginUrl());
        }
        Response.ResponseBuilder response = Response.ok(dailyGameService.today(resolvedUser.user()));
        if (resolvedUser.setCookieHeader() != null) {
            response.header("Set-Cookie", resolvedUser.setCookieHeader());
        }
        return response.build();
    }

    @POST
    @Path("/game/today/guesses")
    public Response guess(@CookieParam("huff_session") String sessionId, GuessRequest request) {
        ResolvedUser resolvedUser = userService.resolve(sessionId);
        if (resolvedUser.user() == null) {
            return unauthorized(resolvedUser.loginUrl());
        }
        Response.ResponseBuilder response = Response.ok(dailyGameService.guess(resolvedUser.user(), request.guess()));
        if (resolvedUser.setCookieHeader() != null) {
            response.header("Set-Cookie", resolvedUser.setCookieHeader());
        }
        return response.build();
    }

    @GET
    @Path("/stats")
    public Response stats(@CookieParam("huff_session") String sessionId) {
        ResolvedUser resolvedUser = userService.resolve(sessionId);
        if (resolvedUser.user() == null) {
            return unauthorized(resolvedUser.loginUrl());
        }
        Response.ResponseBuilder response = Response.ok(dailyGameService.stats(resolvedUser.user()));
        if (resolvedUser.setCookieHeader() != null) {
            response.header("Set-Cookie", resolvedUser.setCookieHeader());
        }
        return response.build();
    }

    @GET
    @Path("/stats/global")
    public Response globalStats(@CookieParam("huff_session") String sessionId) {
        ResolvedUser resolvedUser = userService.resolve(sessionId);
        if (resolvedUser.user() == null) {
            return unauthorized(resolvedUser.loginUrl());
        }
        Response.ResponseBuilder response = Response.ok(dailyGameService.globalStats());
        if (resolvedUser.setCookieHeader() != null) {
            response.header("Set-Cookie", resolvedUser.setCookieHeader());
        }
        return response.build();
    }

    private Response unauthorized(String loginUrl) {
        return Response.status(Response.Status.UNAUTHORIZED)
            .entity(new ErrorDto("auth_required", "Accesso Google richiesto.", loginUrl))
            .build();
    }

    public record ErrorDto(String code, String message, String loginUrl) {
    }
}
