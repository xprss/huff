package dev.huff.hexaquot.api;

import dev.huff.hexaquot.auth.*;
import dev.huff.hexaquot.game.HexasquareDailyGameService;
import dev.huff.hexaquot.game.HexasquareDtos.SimulationRequestDto;
import dev.huff.hexaquot.leaderboard.LeaderboardRepository;
import dev.huff.hexaquot.leaderboard.LeaderboardService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;

@Path("/api/hexasquare")
public class HexasquareResource {
    @Inject UserService userService;
    @Inject HexasquareDailyGameService service;
    @Inject LeaderboardService leaderboardService;

    @GET @Path("/today") public Response today(@CookieParam("huff_session")String session) {
        ResolvedUser user=userService.resolve(session); return user.user()==null?unauthorized():withCookie(Response.ok(service.today(user.user())),user).build();
    }
    @POST @Path("/today/simulations") public Response simulate(@CookieParam("huff_session")String session,SimulationRequestDto request) {
        ResolvedUser user=userService.resolve(session); return user.user()==null?unauthorized():withCookie(Response.ok(service.simulate(user.user(),request)),user).build();
    }
    @GET @Path("/stats") public Response stats(@CookieParam("huff_session")String session) {
        ResolvedUser user=userService.resolve(session); return user.user()==null?unauthorized():withCookie(Response.ok(service.stats(user.user())),user).build();
    }
    @GET @Path("/leaderboards") public Object leaderboards() { return leaderboardService.leaderboards(LeaderboardRepository.Board.HEXASQUARE); }
    private Response.ResponseBuilder withCookie(Response.ResponseBuilder response,ResolvedUser user){return user.setCookieHeader()==null?response:response.header("Set-Cookie",user.setCookieHeader());}
    private Response unauthorized(){return Response.status(Response.Status.UNAUTHORIZED).entity(new GameResource.ErrorDto("token_required","Token Bearer valido richiesto.")).build();}
}
