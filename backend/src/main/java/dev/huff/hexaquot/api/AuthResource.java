package dev.huff.hexaquot.api;

import dev.huff.hexaquot.auth.ResolvedUser;
import dev.huff.hexaquot.auth.UserService;
import jakarta.inject.Inject;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

@Path("/api/me")
public class AuthResource {
    @Inject
    UserService userService;

    @GET
    public Response me(@CookieParam("huff_session") String sessionId) {
        ResolvedUser resolvedUser = userService.resolve(sessionId);
        if (resolvedUser.user() == null) {
            return Response.ok(new MeDto(false, null, resolvedUser.loginUrl(), null, userService.authEnabled()))
                .build();
        }

        Response.ResponseBuilder response = Response.ok(new MeDto(
            true,
            new UserDto(resolvedUser.user().email(), resolvedUser.user().displayName(), resolvedUser.user().authenticated()),
            resolvedUser.loginUrl(),
            userService.authEnabled() ? "/api/logout" : null,
            userService.authEnabled()
        ));
        if (resolvedUser.setCookieHeader() != null) {
            response.header("Set-Cookie", resolvedUser.setCookieHeader());
        }
        return response.build();
    }

    public record MeDto(boolean loggedIn, UserDto user, String loginUrl, String logoutUrl, boolean authEnabled) {
    }

    public record UserDto(String email, String displayName, boolean authenticated) {
    }
}
