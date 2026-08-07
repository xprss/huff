package dev.huff.hexaquot.api;

import dev.huff.hexaquot.auth.ResolvedUser;
import dev.huff.hexaquot.auth.UserService;
import dev.huff.hexaquot.auth.AdminPrivileges;
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
            return Response.status(Response.Status.UNAUTHORIZED)
                .entity(new ErrorDto("token_required", "Token Bearer valido richiesto."))
                .build();
        }

        Response.ResponseBuilder response = Response.ok(new MeDto(
            true,
            UserDto.from(resolvedUser.user()),
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
                .entity(new ErrorDto("token_required", "Token Bearer valido richiesto."))
                .build();
        }

        var updatedUser = userService.updateProfile(
            resolvedUser.user().id(),
            request == null ? null : request.displayName(),
            request == null ? null : request.nickname(),
            request == null ? null : request.profileEmoji(),
            request == null ? null : request.bio(),
            resolvedUser.user().authenticated()
        );
        Response.ResponseBuilder response = Response.ok(UserDto.from(updatedUser));
        if (resolvedUser.setCookieHeader() != null) {
            response.header("Set-Cookie", resolvedUser.setCookieHeader());
        }
        return response.build();
    }

    public record MeDto(boolean loggedIn, UserDto user, boolean authEnabled) {
    }

    public record UserDto(
        String email,
        String displayName,
        String nickname,
        String profileEmoji,
        String bio,
        boolean authenticated,
        AdminPrivileges admin
    ) {
        static UserDto from(dev.huff.hexaquot.auth.AppUser user) {
            return new UserDto(
                user.email(),
                user.displayName(),
                user.nickname(),
                user.profileEmoji(),
                user.bio(),
                user.authenticated(),
                user.admin()
            );
        }
    }

    public record ProfileRequest(String displayName, String nickname, String profileEmoji, String bio) {
    }

    public record ErrorDto(String code, String message) {
    }
}
