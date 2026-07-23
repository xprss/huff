package dev.huff.hexaquot.api;

import io.quarkus.oidc.OidcSession;
import io.quarkus.security.Authenticated;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

import java.net.URI;

@Path("/api/logout")
public class LogoutResource {
    @Inject
    Instance<OidcSession> oidcSession;

    @GET
    @Authenticated
    public Response logout() {
        if (oidcSession.isResolvable()) {
            oidcSession.get().logout().await().indefinitely();
        }
        return Response.seeOther(URI.create("/"))
            .header("Set-Cookie", "huff_session=; Path=/; Max-Age=0; HttpOnly; SameSite=Lax")
            .build();
    }
}
