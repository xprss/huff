package dev.huff.hexaquot.api;

import dev.huff.hexaquot.auth.*;
import dev.huff.hexaquot.game.HexaskyDailyGameService;
import dev.huff.hexaquot.game.HexaskyDtos.CheckRequest;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;

@Path("/api/hexasky")
public class HexaskyResource {
    @Inject UserService userService; @Inject HexaskyDailyGameService service;
    @GET @Path("/today") public Response today(@CookieParam("huff_session") String session){ResolvedUser user=userService.resolve(session);return user.user()==null?unauthorized():withCookie(Response.ok(service.today(user.user())),user).build();}
    @POST @Path("/today/checks") public Response check(@CookieParam("huff_session") String session,CheckRequest request){ResolvedUser user=userService.resolve(session);return user.user()==null?unauthorized():withCookie(Response.ok(service.check(user.user(),request)),user).build();}
    @GET @Path("/stats") public Response stats(@CookieParam("huff_session") String session){ResolvedUser user=userService.resolve(session);return user.user()==null?unauthorized():withCookie(Response.ok(service.stats(user.user())),user).build();}
    private Response.ResponseBuilder withCookie(Response.ResponseBuilder r,ResolvedUser u){return u.setCookieHeader()==null?r:r.header("Set-Cookie",u.setCookieHeader());}
    private Response unauthorized(){return Response.status(Response.Status.UNAUTHORIZED).entity(new GameResource.ErrorDto("token_required","Token Bearer valido richiesto.")).build();}
}
