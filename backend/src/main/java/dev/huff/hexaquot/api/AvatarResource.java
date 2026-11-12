package dev.huff.hexaquot.api;

import dev.huff.hexaquot.auth.AvatarService;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;

/**
 * Serves profile photos. Under /api/, so ApiSecurityFilter already requires a
 * session (same-origin <img> requests send the cookie); the URL carries a
 * content version (?v=), so the filter lets these responses be cached.
 */
@Path("/api/avatars")
public class AvatarResource {
    public static final String CACHE_CONTROL = "private, max-age=31536000, immutable";

    @Inject
    AvatarService avatarService;

    @GET
    @Path("/{userId}")
    public Response avatar(@PathParam("userId") String userId) {
        AvatarService.Avatar avatar = avatarService.read(userId);
        return Response.ok(avatar.bytes(), avatar.format().contentType())
            .header("Cache-Control", CACHE_CONTROL)
            .build();
    }
}
