package dev.huff.hexaquot.api;

import dev.huff.hexaquot.auth.ResolvedUser;
import dev.huff.hexaquot.auth.UserService;
import dev.huff.hexaquot.game.HexahackDailyGameService;
import dev.huff.hexaquot.game.HexahackDtos.OverrideRequest;
import dev.huff.hexaquot.game.HexahackDtos.ProbeRequest;
import dev.huff.hexaquot.game.HexahackDtos.SubmissionRequest;
import jakarta.inject.Inject;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

@Path("/api/hexahack")
public class HexahackResource {
    @Inject UserService userService;
    @Inject HexahackDailyGameService service;

    @GET @Path("/today")
    public Response today(@CookieParam("huff_session") String sessionId) {
        ResolvedUser user = userService.resolve(sessionId);
        if (user.user() == null) return unauthorized();
        return withCookie(Response.ok(service.today(user.user())), user).build();
    }

    @POST @Path("/today/probes")
    public Response probe(@CookieParam("huff_session") String sessionId, ProbeRequest request) {
        ResolvedUser user = userService.resolve(sessionId);
        if (user.user() == null) return unauthorized();
        return withCookie(Response.ok(service.probe(user.user(), request)), user).build();
    }

    @POST @Path("/today/submissions")
    public Response submit(@CookieParam("huff_session") String sessionId, SubmissionRequest request) {
        ResolvedUser user = userService.resolve(sessionId);
        if (user.user() == null) return unauthorized();
        return withCookie(Response.ok(service.submit(user.user(), request)), user).build();
    }

    @POST @Path("/today/overrides")
    public Response override(@CookieParam("huff_session") String sessionId, OverrideRequest request) {
        ResolvedUser user = userService.resolve(sessionId);
        if (user.user() == null) return unauthorized();
        return withCookie(Response.ok(service.override(user.user(), request)), user).build();
    }

    @GET @Path("/stats")
    public Response stats(@CookieParam("huff_session") String sessionId) {
        ResolvedUser user = userService.resolve(sessionId);
        if (user.user() == null) return unauthorized();
        return withCookie(Response.ok(service.stats(user.user())), user).build();
    }

    private Response.ResponseBuilder withCookie(Response.ResponseBuilder response, ResolvedUser user) {
        return user.setCookieHeader() == null ? response : response.header("Set-Cookie", user.setCookieHeader());
    }

    private Response unauthorized() {
        return Response.status(Response.Status.UNAUTHORIZED)
            .entity(new GameResource.ErrorDto("token_required", "Token Bearer valido richiesto.")).build();
    }
}
