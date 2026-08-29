package dev.huff.hexaquot.api;

import dev.huff.hexaquot.auth.*;
import dev.huff.hexaquot.game.*;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;

@Path("/api/admin/hexaflow/puzzles")
public class HexaflowAdminResource {
    @Inject UserService users; @Inject HexaflowPuzzleService service;
    @GET public Response month(@CookieParam("huff_session")String s,@QueryParam("month")String m){var u=users.resolve(s);return run(u,()->service.month(u.user(),m));}
    @GET @Path("/{date}") public Response detail(@CookieParam("huff_session")String s,@PathParam("date")String d){var u=users.resolve(s);return run(u,()->service.detail(u.user(),d));}
    @PUT @Path("/{date}") public Response save(@CookieParam("huff_session")String s,@PathParam("date")String d,HexaflowDtos.PuzzleDraftDto b){var u=users.resolve(s);return run(u,()->service.save(u.user(),d,b));}
    @POST @Path("/{date}/publish") public Response publish(@CookieParam("huff_session")String s,@PathParam("date")String d){var u=users.resolve(s);return run(u,()->service.publish(u.user(),d));}
    @POST @Path("/{date}/draft") public Response draft(@CookieParam("huff_session")String s,@PathParam("date")String d){var u=users.resolve(s);return run(u,()->service.unpublish(u.user(),d));}
    @DELETE @Path("/{date}") public Response delete(@CookieParam("huff_session")String s,@PathParam("date")String d){var u=users.resolve(s);return run(u,()->{service.delete(u.user(),d);return null;});}
    private Response run(ResolvedUser u,java.util.function.Supplier<Object> action){if(u.user()==null)return Response.status(401).entity(new GameResource.ErrorDto("token_required","Token Bearer valido richiesto.")).build();Object body=action.get();var r=body==null?Response.noContent():Response.ok(body);if(u.setCookieHeader()!=null)r.header("Set-Cookie",u.setCookieHeader());return r.build();}
}
