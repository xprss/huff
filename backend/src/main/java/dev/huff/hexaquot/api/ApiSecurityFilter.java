package dev.huff.hexaquot.api;

import io.quarkus.security.identity.SecurityIdentity;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Central, fail-closed protection for the JSON API.
 *
 * Resource-level checks remain in place as defense in depth. This filter makes
 * authentication the default for every current and future /api endpoint.
 * Every endpoint is private, including read-only endpoints. In production the
 * only accepted credential is a Bearer token validated by Quarkus OIDC.
 */
@Provider
@Priority(Priorities.AUTHORIZATION)
public class ApiSecurityFilter implements ContainerRequestFilter, ContainerResponseFilter {
    @ConfigProperty(name = "app.auth.enabled")
    boolean authEnabled;

    @Inject
    SecurityIdentity securityIdentity;

    @Override
    public void filter(ContainerRequestContext request) {
        String path = normalizedPath(request);
        if (!path.startsWith("api/")) {
            return;
        }

        if (!authEnabled) {
            return;
        }

        if (securityIdentity == null || securityIdentity.isAnonymous()) {
            request.abortWith(error(
                Response.Status.UNAUTHORIZED,
                "token_required",
                "Token Bearer valido richiesto."
            ));
            return;
        }

    }

    @Override
    public void filter(ContainerRequestContext request, ContainerResponseContext response) {
        if (!normalizedPath(request).startsWith("api/")) {
            return;
        }
        response.getHeaders().putSingle("Cache-Control", "no-store");
        response.getHeaders().putSingle("Pragma", "no-cache");
        response.getHeaders().putSingle("X-Content-Type-Options", "nosniff");
    }

    private String normalizedPath(ContainerRequestContext request) {
        String path = request.getUriInfo().getPath();
        return path.startsWith("/") ? path.substring(1) : path;
    }

    private Response error(Response.Status status, String code, String message) {
        Response.ResponseBuilder response = Response.status(status)
            .type(MediaType.APPLICATION_JSON_TYPE)
            .entity(new SecurityErrorDto(code, message));
        if (status == Response.Status.UNAUTHORIZED) {
            response.header("WWW-Authenticate", "Bearer");
            clearLegacySessionCookies(response);
        }
        return response.build();
    }

    private void clearLegacySessionCookies(Response.ResponseBuilder response) {
        response.header("Set-Cookie", "huff_session=; Path=/; Max-Age=0; HttpOnly; SameSite=Lax");
        response.header("Set-Cookie", "q_session=; Path=/; Max-Age=0; HttpOnly; SameSite=Lax");
    }

    public record SecurityErrorDto(String code, String message) {
    }
}
