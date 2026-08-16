package dev.huff.hexaquot.api;

import dev.huff.hexaquot.auth.ResolvedUser;
import dev.huff.hexaquot.auth.UserService;
import dev.huff.hexaquot.game.OverallStatsService;
import jakarta.inject.Inject;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

@Path("/api/overall")
public class OverallResource {
    @Inject UserService userService;
    @Inject OverallStatsService service;

    @GET @Path("/stats")
    public Response stats(@CookieParam("huff_session") String sessionId) {
        ResolvedUser user = userService.resolve(sessionId);
        if (user.user() == null) return Response.status(Response.Status.UNAUTHORIZED)
            .entity(new GameResource.ErrorDto("token_required", "Token Bearer valido richiesto.")).build();
        Response.ResponseBuilder response = Response.ok(service.stats(user.user()));
        if (user.setCookieHeader() != null) response.header("Set-Cookie", user.setCookieHeader());
        return response.build();
    }
}
