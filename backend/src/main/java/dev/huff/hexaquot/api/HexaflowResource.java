package dev.huff.hexaquot.api;

import dev.huff.hexaquot.auth.*;
import dev.huff.hexaquot.game.*;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;

@Path("/api/hexaflow")
public class HexaflowResource {
    @Inject UserService users; @Inject HexaflowDailyGameService service;
    @GET @Path("/today") public Response today(@CookieParam("huff_session")String s){var u=users.resolve(s);return respond(u,u.user()==null?null:service.today(u.user()));}
    @POST @Path("/today/paths") public Response path(@CookieParam("huff_session")String s,HexaflowDtos.PathRequest r){var u=users.resolve(s);return respond(u,u.user()==null?null:service.submitPath(u.user(),r));}
    @GET @Path("/stats") public Response stats(@CookieParam("huff_session")String s){var u=users.resolve(s);return respond(u,u.user()==null?null:service.stats(u.user()));}
    private Response respond(ResolvedUser u,Object body){if(u.user()==null)return Response.status(401).entity(new GameResource.ErrorDto("token_required","Token Bearer valido richiesto.")).build();var r=Response.ok(body);if(u.setCookieHeader()!=null)r.header("Set-Cookie",u.setCookieHeader());return r.build();}
}
