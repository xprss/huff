package dev.huff.hexaquot.api;

import dev.huff.hexaquot.auth.ResolvedUser;
import dev.huff.hexaquot.auth.UserService;
import jakarta.inject.Inject;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
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
            UserDto.from(resolvedUser.user()),
            resolvedUser.loginUrl(),
            userService.authEnabled() ? "/api/logout" : null,
            userService.authEnabled()
        ));
        if (resolvedUser.setCookieHeader() != null) {
            response.header("Set-Cookie", resolvedUser.setCookieHeader());
        }
        return response.build();
    }

    @PUT
    @Path("/profile")
    public Response updateProfile(@CookieParam("huff_session") String sessionId, ProfileRequest request) {
        ResolvedUser resolvedUser = userService.resolve(sessionId);
        if (resolvedUser.user() == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                .entity(new ErrorDto("auth_required", "Accesso Google richiesto.", resolvedUser.loginUrl()))
                .build();
        }

        var updatedUser = userService.updateProfile(
            resolvedUser.user().id(),
            request == null ? null : request.displayName(),
            request == null ? null : request.nickname(),
            request == null ? null : request.profileEmoji(),
            resolvedUser.user().authenticated()
        );
        Response.ResponseBuilder response = Response.ok(UserDto.from(updatedUser));
        if (resolvedUser.setCookieHeader() != null) {
            response.header("Set-Cookie", resolvedUser.setCookieHeader());
        }
        return response.build();
    }

    public record MeDto(boolean loggedIn, UserDto user, String loginUrl, String logoutUrl, boolean authEnabled) {
    }

    public record UserDto(String email, String displayName, String nickname, String profileEmoji, boolean authenticated) {
        static UserDto from(dev.huff.hexaquot.auth.AppUser user) {
            return new UserDto(user.email(), user.displayName(), user.nickname(), user.profileEmoji(), user.authenticated());
        }
    }

    public record ProfileRequest(String displayName, String nickname, String profileEmoji) {
    }

    public record ErrorDto(String code, String message, String loginUrl) {
    }
}
