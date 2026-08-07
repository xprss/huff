package dev.huff.hexaquot.api;

import dev.huff.hexaquot.auth.UserService;
import jakarta.inject.Inject;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

@Path("/api/logout")
public class LogoutResource {
    @Inject
    UserService userService;

    @POST
    public Response logout(@CookieParam("huff_session") String sessionId) {
        userService.logout(sessionId);
        return Response.noContent()
            .header("Set-Cookie", userService.clearSessionCookie())
            .build();
    }
}
