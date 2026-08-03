package dev.huff.hexaquot.api;

import io.quarkus.security.identity.SecurityIdentity;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.Set;

/**
 * Central, fail-closed protection for the JSON API.
 *
 * Resource-level checks remain in place as defense in depth. This filter makes
 * authentication the default for every current and future /api endpoint, with
 * only the explicitly listed read-only bootstrap endpoints left public.
 */
@Provider
@Priority(Priorities.AUTHORIZATION)
public class ApiSecurityFilter implements ContainerRequestFilter, ContainerResponseFilter {
    public static final String REQUEST_HEADER = "X-Huff-Request";
    public static final String REQUEST_HEADER_VALUE = "1";

    private static final Set<String> OIDC_ENDPOINTS = Set.of("api/login", "api/logout");
    private static final Set<String> PUBLIC_GET_ENDPOINTS = Set.of("api/stats/global", "api/push/settings");
    private static final Set<String> SAFE_METHODS = Set.of(HttpMethod.GET, HttpMethod.HEAD, HttpMethod.OPTIONS);

    @ConfigProperty(name = "app.auth.enabled")
    boolean authEnabled;

    @Inject
    SecurityIdentity securityIdentity;

    @Override
    public void filter(ContainerRequestContext request) {
        String path = normalizedPath(request);
        if (!path.startsWith("api/") || OIDC_ENDPOINTS.contains(path)) {
            return;
        }

        boolean publicRead = HttpMethod.GET.equals(request.getMethod()) && PUBLIC_GET_ENDPOINTS.contains(path);
        if (!authEnabled || publicRead) {
            return;
        }

        if (securityIdentity == null || securityIdentity.isAnonymous()) {
            request.abortWith(error(
                Response.Status.UNAUTHORIZED,
                "auth_required",
                "Accesso Google richiesto.",
                "/api/login"
            ));
            return;
        }

        if (!SAFE_METHODS.contains(request.getMethod())
            && !REQUEST_HEADER_VALUE.equals(request.getHeaderString(REQUEST_HEADER))) {
            request.abortWith(error(
                Response.Status.FORBIDDEN,
                "csrf_rejected",
                "Richiesta non attendibile.",
                null
            ));
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

    private Response error(Response.Status status, String code, String message, String loginUrl) {
        return Response.status(status)
            .type(MediaType.APPLICATION_JSON_TYPE)
            .entity(new SecurityErrorDto(code, message, loginUrl))
            .build();
    }

    public record SecurityErrorDto(String code, String message, String loginUrl) {
    }
}
