package dev.huff.hexaquot.api;

import dev.huff.hexaquot.auth.ResolvedUser;
import dev.huff.hexaquot.auth.UserService;
import dev.huff.hexaquot.push.ScheduledAlertService;
import dev.huff.hexaquot.push.ScheduledAlertService.AlertRequest;
import jakarta.inject.Inject;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;

@Path("/api/admin/alerts")
public class ScheduledAlertResource {
    @Inject UserService userService;
    @Inject ScheduledAlertService alerts;

    @GET
    public Response list(@CookieParam("huff_session") String sessionId) {
        var user = userService.resolve(sessionId);
        if (user.user() == null) return unauthorized();
        return reply(user, alerts.list(user.user()));
    }

    @POST
    public Response create(@CookieParam("huff_session") String sessionId, AlertRequest request) {
        var user = userService.resolve(sessionId);
        if (user.user() == null) return unauthorized();
        return reply(user, alerts.create(user.user(), request));
    }

    @PUT @Path("/{id}")
    public Response update(@CookieParam("huff_session") String sessionId, @PathParam("id") String id, AlertRequest request) {
        var user = userService.resolve(sessionId);
        if (user.user() == null) return unauthorized();
        return reply(user, alerts.update(user.user(), id, request));
    }

    @DELETE @Path("/{id}")
    public Response delete(@CookieParam("huff_session") String sessionId, @PathParam("id") String id) {
        var user = userService.resolve(sessionId);
        if (user.user() == null) return unauthorized();
        alerts.delete(user.user(), id);
        return reply(user, null);
    }

    private Response reply(ResolvedUser user, Object body) {
        Response.ResponseBuilder response = body == null ? Response.noContent() : Response.ok(body);
        if (user.setCookieHeader() != null) response.header("Set-Cookie", user.setCookieHeader());
        return response.build();
    }

    private Response unauthorized() {
        return Response.status(Response.Status.UNAUTHORIZED)
            .entity(new GameResource.ErrorDto("token_required", "Token Bearer valido richiesto.")).build();
    }
}
