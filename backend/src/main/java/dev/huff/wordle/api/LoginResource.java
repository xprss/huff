package dev.huff.wordle.api;

import io.quarkus.security.Authenticated;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

import java.net.URI;

@Path("/api/login")
public class LoginResource {
    @GET
    @Authenticated
    public Response login() {
        return Response.seeOther(URI.create("/")).build();
    }
}
