package dev.huff.hexaquot.api;

import dev.huff.hexaquot.auth.ResolvedUser;
import dev.huff.hexaquot.auth.UserService;
import dev.huff.hexaquot.game.HexatrapAdminService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;

import java.util.function.Supplier;

@Path("/api/admin/hexatrap/cases")
public class HexatrapAdminResource {
    @Inject UserService users;
    @Inject HexatrapAdminService service;

    @GET
    public Response list(@CookieParam("huff_session") String session, @QueryParam("from") String from, @QueryParam("to") String to) {
        ResolvedUser user = users.resolve(session);
        return run(user, 200, () -> service.list(user.user(), from, to));
    }

    @GET @Path("/{date}")
    public Response detail(@CookieParam("huff_session") String session, @PathParam("date") String date) {
        ResolvedUser user = users.resolve(session);
        return run(user, 200, () -> service.detail(user.user(), date));
    }

    @POST @Path("/{date}/generate")
    public Response generate(@CookieParam("huff_session") String session, @PathParam("date") String date) {
        ResolvedUser user = users.resolve(session);
        return run(user, 202, () -> service.generate(user.user(), date));
    }

    private Response run(ResolvedUser user, int status, Supplier<Object> action) {
        if (user.user() == null) return Response.status(401)
            .entity(new GameResource.ErrorDto("token_required", "Token Bearer valido richiesto.")).build();
        Response.ResponseBuilder response = Response.status(status).entity(action.get());
        if (user.setCookieHeader() != null) response.header("Set-Cookie", user.setCookieHeader());
        return response.build();
    }
}
