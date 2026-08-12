package dev.huff.hexaquot.api;

import dev.huff.hexaquot.auth.ResolvedUser;
import dev.huff.hexaquot.auth.UserService;
import dev.huff.hexaquot.game.DailyGameService;
import dev.huff.hexaquot.game.GuessRequest;
import dev.huff.hexaquot.game.ModeRequest;
import dev.huff.hexaquot.game.StarRevealDto;
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
            return unauthorized();
        }
        Response.ResponseBuilder response = Response.ok(dailyGameService.today(resolvedUser.user()));
        if (resolvedUser.setCookieHeader() != null) {
            response.header("Set-Cookie", resolvedUser.setCookieHeader());
        }
        return response.build();
    }

    @GET
    @Path("/hexaword/today")
    public Response hexawordToday(@CookieParam("huff_session") String sessionId) {
        return today(sessionId);
    }

    @POST
    @Path("/game/today/mode")
    public Response selectMode(@CookieParam("huff_session") String sessionId, ModeRequest request) {
        ResolvedUser resolvedUser = userService.resolve(sessionId);
        if (resolvedUser.user() == null) {
            return unauthorized();
        }
        Response.ResponseBuilder response = Response.ok(
            dailyGameService.selectMode(resolvedUser.user(), request == null ? null : request.mode())
        );
        if (resolvedUser.setCookieHeader() != null) {
            response.header("Set-Cookie", resolvedUser.setCookieHeader());
        }
        return response.build();
    }

    @POST
    @Path("/hexaword/today/mode")
    public Response selectHexawordMode(@CookieParam("huff_session") String sessionId, ModeRequest request) {
        return selectMode(sessionId, request);
    }

    @POST
    @Path("/game/today/guesses")
    public Response guess(@CookieParam("huff_session") String sessionId, GuessRequest request) {
        ResolvedUser resolvedUser = userService.resolve(sessionId);
        if (resolvedUser.user() == null) {
            return unauthorized();
        }
        Response.ResponseBuilder response = Response.ok(dailyGameService.guess(resolvedUser.user(), request == null ? null : request.guess()));
        if (resolvedUser.setCookieHeader() != null) {
            response.header("Set-Cookie", resolvedUser.setCookieHeader());
        }
        return response.build();
    }

    @POST
    @Path("/hexaword/today/guesses")
    public Response hexawordGuess(@CookieParam("huff_session") String sessionId, GuessRequest request) {
        return guess(sessionId, request);
    }

    @POST
    @Path("/game/today/kitten")
    public Response useKitten(@CookieParam("huff_session") String sessionId) {
        ResolvedUser resolvedUser = userService.resolve(sessionId);
        if (resolvedUser.user() == null) {
            return unauthorized();
        }
        Response.ResponseBuilder response = Response.ok(dailyGameService.useKitten(resolvedUser.user()));
        if (resolvedUser.setCookieHeader() != null) {
            response.header("Set-Cookie", resolvedUser.setCookieHeader());
        }
        return response.build();
    }

    @POST
    @Path("/hexaword/today/kitten")
    public Response useHexawordKitten(@CookieParam("huff_session") String sessionId) { return useKitten(sessionId); }

    @POST
    @Path("/game/today/star")
    public Response useStar(@CookieParam("huff_session") String sessionId) {
        ResolvedUser resolvedUser = userService.resolve(sessionId);
        if (resolvedUser.user() == null) {
            return unauthorized();
        }
        StarRevealDto reveal = dailyGameService.useStar(resolvedUser.user());
        Response.ResponseBuilder response = Response.ok(reveal);
        if (resolvedUser.setCookieHeader() != null) {
            response.header("Set-Cookie", resolvedUser.setCookieHeader());
        }
        return response.build();
    }

    @POST
    @Path("/hexaword/today/star")
    public Response useHexawordStar(@CookieParam("huff_session") String sessionId) { return useStar(sessionId); }

    @GET
    @Path("/stats")
    public Response stats(@CookieParam("huff_session") String sessionId) {
        ResolvedUser resolvedUser = userService.resolve(sessionId);
        if (resolvedUser.user() == null) {
            return unauthorized();
        }
        Response.ResponseBuilder response = Response.ok(dailyGameService.stats(resolvedUser.user()));
        if (resolvedUser.setCookieHeader() != null) {
            response.header("Set-Cookie", resolvedUser.setCookieHeader());
        }
        return response.build();
    }

    @GET
    @Path("/hexaword/stats")
    public Response hexawordStats(@CookieParam("huff_session") String sessionId) { return stats(sessionId); }

    @GET
    @Path("/stats/global")
    public Response globalStats() {
        return Response.ok(dailyGameService.globalStats()).build();
    }

    private Response unauthorized() {
        return Response.status(Response.Status.UNAUTHORIZED)
            .entity(new ErrorDto("token_required", "Token Bearer valido richiesto."))
            .build();
    }

    public record ErrorDto(String code, String message) {
    }
}
