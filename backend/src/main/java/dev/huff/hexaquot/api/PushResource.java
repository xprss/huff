package dev.huff.hexaquot.api;

import dev.huff.hexaquot.auth.ResolvedUser;
import dev.huff.hexaquot.auth.UserService;
import dev.huff.hexaquot.push.PushNotificationService;
import dev.huff.hexaquot.push.PushSubscriptionRequest;
import jakarta.inject.Inject;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

@Path("/api/push")
public class PushResource {
    @Inject
    UserService userService;

    @Inject
    PushNotificationService pushNotificationService;

    @GET
    @Path("/settings")
    public Response settings() {
        return Response.ok(pushNotificationService.settings()).build();
    }

    @POST
    @Path("/subscriptions")
    public Response subscribe(@CookieParam("huff_session") String sessionId, PushSubscriptionRequest request) {
        ResolvedUser resolvedUser = userService.resolve(sessionId);
        if (resolvedUser.user() == null) {
            return unauthorized();
        }
        pushNotificationService.subscribe(resolvedUser.user().id(), request);
        Response.ResponseBuilder response = Response.noContent();
        if (resolvedUser.setCookieHeader() != null) {
            response.header("Set-Cookie", resolvedUser.setCookieHeader());
        }
        return response.build();
    }

    @DELETE
    @Path("/subscriptions")
    public Response unsubscribe(@CookieParam("huff_session") String sessionId, PushSubscriptionRequest request) {
        ResolvedUser resolvedUser = userService.resolve(sessionId);
        if (resolvedUser.user() == null) {
            return unauthorized();
        }
        pushNotificationService.unsubscribe(resolvedUser.user().id(), request);
        Response.ResponseBuilder response = Response.noContent();
        if (resolvedUser.setCookieHeader() != null) {
            response.header("Set-Cookie", resolvedUser.setCookieHeader());
        }
        return response.build();
    }

    private Response unauthorized() {
        return Response.status(Response.Status.UNAUTHORIZED)
            .entity(new GameResource.ErrorDto("token_required", "Token Bearer valido richiesto."))
            .build();
    }
}
