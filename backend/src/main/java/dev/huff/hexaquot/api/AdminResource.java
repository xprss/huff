package dev.huff.hexaquot.api;

import dev.huff.hexaquot.admin.AdminService;
import dev.huff.hexaquot.admin.AdminService.AdminPlayerUpdateRequest;
import dev.huff.hexaquot.auth.ResolvedUser;
import dev.huff.hexaquot.auth.UserService;
import jakarta.inject.Inject;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;

@Path("/api/admin")
public class AdminResource {
    @Inject
    UserService userService;

    @Inject
    AdminService adminService;

    @GET
    @Path("/players")
    public Response players(
        @CookieParam("huff_session") String sessionId,
        @QueryParam("q") String query,
        @QueryParam("sort") String sort,
        @QueryParam("page") Integer page
    ) {
        ResolvedUser resolvedUser = userService.resolve(sessionId);
        if (resolvedUser.user() == null) {
            return unauthorized();
        }
        Response.ResponseBuilder response = Response.ok(adminService.listPlayers(resolvedUser.user(), query, sort, page));
        setCookie(response, resolvedUser);
        return response.build();
    }

    @GET
    @Path("/players/{userId}")
    public Response player(@CookieParam("huff_session") String sessionId, @PathParam("userId") String userId) {
        ResolvedUser resolvedUser = userService.resolve(sessionId);
        if (resolvedUser.user() == null) {
            return unauthorized();
        }
        Response.ResponseBuilder response = Response.ok(adminService.playerDetail(resolvedUser.user(), userId));
        setCookie(response, resolvedUser);
        return response.build();
    }

    @PUT
    @Path("/players/{userId}")
    public Response updatePlayer(
        @CookieParam("huff_session") String sessionId,
        @PathParam("userId") String userId,
        AdminPlayerUpdateRequest request
    ) {
        ResolvedUser resolvedUser = userService.resolve(sessionId);
        if (resolvedUser.user() == null) {
            return unauthorized();
        }
        Response.ResponseBuilder response = Response.ok(adminService.updatePlayer(resolvedUser.user(), userId, request));
        setCookie(response, resolvedUser);
        return response.build();
    }

    @DELETE
    @Path("/players/{userId}")
    public Response deletePlayer(@CookieParam("huff_session") String sessionId, @PathParam("userId") String userId) {
        ResolvedUser resolvedUser = userService.resolve(sessionId);
        if (resolvedUser.user() == null) {
            return unauthorized();
        }
        Response.ResponseBuilder response = Response.ok(adminService.deletePlayer(resolvedUser.user(), userId));
        setCookie(response, resolvedUser);
        return response.build();
    }

    private Response unauthorized() {
        return Response.status(Response.Status.UNAUTHORIZED)
            .entity(new GameResource.ErrorDto("token_required", "Token Bearer valido richiesto."))
            .build();
    }

    private void setCookie(Response.ResponseBuilder response, ResolvedUser resolvedUser) {
        if (resolvedUser.setCookieHeader() != null) {
            response.header("Set-Cookie", resolvedUser.setCookieHeader());
        }
    }
}
