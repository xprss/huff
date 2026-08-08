package dev.huff.hexaquot.api;

import dev.huff.hexaquot.auth.ResolvedUser;
import dev.huff.hexaquot.auth.UserService;
import dev.huff.hexaquot.auth.AdminPrivileges;
import dev.huff.hexaquot.leaderboard.LeaderboardService;
import dev.huff.hexaquot.leaderboard.MedalCountsDto;
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

    @Inject
    LeaderboardService leaderboardService;

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
            UserDto.from(resolvedUser.user(), leaderboardService.medalCounts(resolvedUser.user().id())),
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
            request == null ? null : request.inputHandPreference(),
            resolvedUser.user().authenticated()
        );
        Response.ResponseBuilder response = Response.ok(
            UserDto.from(updatedUser, leaderboardService.medalCounts(updatedUser.id()))
        );
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
        String inputHandPreference,
        boolean authenticated,
        AdminPrivileges admin,
        MedalCountsDto medals
    ) {
        static UserDto from(dev.huff.hexaquot.auth.AppUser user, MedalCountsDto medals) {
            return new UserDto(
                user.email(),
                user.displayName(),
                user.nickname(),
                user.profileEmoji(),
                user.bio(),
                user.inputHandPreference(),
                user.authenticated(),
                user.admin(),
                medals
            );
        }
    }

    public record ProfileRequest(String displayName, String nickname, String profileEmoji, String bio, String inputHandPreference) {
    }

    public record ErrorDto(String code, String message) {
    }
}
