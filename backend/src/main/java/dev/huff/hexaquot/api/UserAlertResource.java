package dev.huff.hexaquot.api;

import dev.huff.hexaquot.auth.UserService;
import dev.huff.hexaquot.push.ScheduledAlertService;
import jakarta.inject.Inject;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;

@Path("/api/me/alerts")
public class UserAlertResource {
    @Inject UserService userService;
    @Inject ScheduledAlertService alerts;

    @GET
    public Response pending(@CookieParam("huff_session") String sessionId) {
        var user = userService.resolve(sessionId);
        if (user.user() == null) return unauthorized();
        Response.ResponseBuilder response = Response.ok(alerts.pendingModals(user.user().id()));
        if (user.setCookieHeader() != null) response.header("Set-Cookie", user.setCookieHeader());
        return response.build();
    }

    @POST @Path("/{id}/seen")
    public Response seen(@CookieParam("huff_session") String sessionId, @PathParam("id") String id) {
        var user = userService.resolve(sessionId);
        if (user.user() == null) return unauthorized();
        alerts.markSeen(user.user().id(), id);
        Response.ResponseBuilder response = Response.noContent();
        if (user.setCookieHeader() != null) response.header("Set-Cookie", user.setCookieHeader());
        return response.build();
    }

    private Response unauthorized() {
        return Response.status(Response.Status.UNAUTHORIZED)
            .entity(new GameResource.ErrorDto("token_required", "Token Bearer valido richiesto.")).build();
    }
}
