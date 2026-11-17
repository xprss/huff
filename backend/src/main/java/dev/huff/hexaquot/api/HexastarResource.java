package dev.huff.hexaquot.api;

import dev.huff.hexaquot.auth.ResolvedUser;
import dev.huff.hexaquot.auth.UserService;
import dev.huff.hexaquot.game.HexastarDailyGameService;
import dev.huff.hexaquot.game.HexastarDtos.GuessActionDto;
import dev.huff.hexaquot.game.HexastarDtos.GuessRequest;
import dev.huff.hexaquot.game.WordExplanationService;
import jakarta.inject.Inject;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

@Path("/api/hexastar")
public class HexastarResource {
    @Inject UserService userService;
    @Inject HexastarDailyGameService service;
    @Inject WordExplanationService wordExplanationService;

    @GET
    @Path("/today")
    public Response today(@CookieParam("huff_session") String sessionId) {
        ResolvedUser user = userService.resolve(sessionId);
        return user.user() == null ? unauthorized() : withCookie(Response.ok(service.today(user.user())), user).build();
    }

    @POST
    @Path("/today/guesses")
    public Response guess(@CookieParam("huff_session") String sessionId, GuessRequest request) {
        ResolvedUser user = userService.resolve(sessionId);
        if (user.user() == null) return unauthorized();
        GuessActionDto action = service.guess(user.user(), request);
        wordExplanationService.prefetchHexastar(action.game());
        return withCookie(Response.ok(action), user).build();
    }

    @GET
    @Path("/today/explanation")
    public Response explanation(@CookieParam("huff_session") String sessionId) {
        ResolvedUser user = userService.resolve(sessionId);
        return user.user() == null ? unauthorized() : withCookie(Response.ok(wordExplanationService.hexastar(user.user())), user).build();
    }

    @GET
    @Path("/stats")
    public Response stats(@CookieParam("huff_session") String sessionId) {
        ResolvedUser user = userService.resolve(sessionId);
        return user.user() == null ? unauthorized() : withCookie(Response.ok(service.stats(user.user())), user).build();
    }

    private Response.ResponseBuilder withCookie(Response.ResponseBuilder response, ResolvedUser user) {
        return user.setCookieHeader() == null ? response : response.header("Set-Cookie", user.setCookieHeader());
    }

    private Response unauthorized() {
        return Response.status(Response.Status.UNAUTHORIZED)
            .entity(new GameResource.ErrorDto("token_required", "Token Bearer valido richiesto."))
            .build();
    }
}
