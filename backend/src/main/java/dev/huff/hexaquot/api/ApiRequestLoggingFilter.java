package dev.huff.hexaquot.api;

import io.quarkus.security.identity.SecurityIdentity;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

/**
 * Emits a concise, correlated audit trail for every JSON API request.
 *
 * Deliberately excludes request bodies, cookies, authorization headers, and
 * query parameter values. Those can contain credentials or player data and do
 * not belong in the long-lived container logs.
 */
@Provider
@Priority(Priorities.AUTHENTICATION - 100)
public class ApiRequestLoggingFilter implements ContainerRequestFilter, ContainerResponseFilter {
    private static final Logger LOG = Logger.getLogger(ApiRequestLoggingFilter.class);
    private static final String REQUEST_ID_PROPERTY = ApiRequestLoggingFilter.class.getName() + ".requestId";
    private static final String START_NANOS_PROPERTY = ApiRequestLoggingFilter.class.getName() + ".startNanos";
    private static final int MAX_LOG_VALUE_LENGTH = 160;

    @Inject
    SecurityIdentity securityIdentity;

    @Override
    public void filter(ContainerRequestContext request) {
        if (!isApiRequest(request)) {
            return;
        }

        String requestId = requestId();
        request.setProperty(REQUEST_ID_PROPERTY, requestId);
        request.setProperty(START_NANOS_PROPERTY, System.nanoTime());

        LOG.infof(
            "API_REQUEST_STARTED requestId=%s method=%s path=%s queryKeys=%s contentType=%s contentLength=%s clientIp=%s origin=%s userAgent=%s hasSessionCookie=%s",
            requestId,
            token(request.getMethod()),
            quoted(request.getUriInfo().getPath()),
            quoted(queryParameterNames(request.getUriInfo())),
            quoted(request.getHeaderString("Content-Type")),
            token(request.getHeaderString("Content-Length")),
            token(clientIp(request)),
            quoted(request.getHeaderString("Origin")),
            quoted(request.getHeaderString("User-Agent")),
            request.getCookies().containsKey("huff_session")
        );
    }

    @Override
    public void filter(ContainerRequestContext request, ContainerResponseContext response) {
        if (!isApiRequest(request)) {
            return;
        }

        String requestId = (String) request.getProperty(REQUEST_ID_PROPERTY);
        if (requestId == null) {
            requestId = requestId();
        }

        long durationMs = durationMs(request.getProperty(START_NANOS_PROPERTY));
        int status = response.getStatus();
        String message = "API_REQUEST_COMPLETED requestId=%s method=%s path=%s status=%d outcome=%s durationMs=%d responseLength=%d authenticated=%s";
        Object[] values = {
            requestId,
            token(request.getMethod()),
            quoted(request.getUriInfo().getPath()),
            status,
            outcome(status),
            durationMs,
            response.getLength(),
            isAuthenticated()
        };

        if (status >= 500) {
            LOG.errorf(message, values);
        } else if (durationMs >= 1_000) {
            LOG.warnf(message, values);
        } else {
            LOG.infof(message, values);
        }
    }

    private boolean isApiRequest(ContainerRequestContext request) {
        String path = request.getUriInfo().getPath();
        String normalizedPath = path.startsWith("/") ? path.substring(1) : path;
        return "api".equals(normalizedPath) || normalizedPath.startsWith("api/");
    }

    private String requestId() {
        return UUID.randomUUID().toString();
    }

    private String queryParameterNames(UriInfo uriInfo) {
        Set<String> names = new TreeSet<>(uriInfo.getQueryParameters(true).keySet());
        return names.isEmpty() ? "-" : String.join(",", names);
    }

    private String clientIp(ContainerRequestContext request) {
        String forwardedFor = request.getHeaderString("X-Forwarded-For");
        if (forwardedFor == null || forwardedFor.isBlank()) {
            return "-";
        }
        int separator = forwardedFor.indexOf(',');
        return separator >= 0 ? forwardedFor.substring(0, separator).trim() : forwardedFor.trim();
    }

    private long durationMs(Object startNanos) {
        if (!(startNanos instanceof Long start)) {
            return -1;
        }
        return (System.nanoTime() - start) / 1_000_000;
    }

    private boolean isAuthenticated() {
        return securityIdentity != null && !securityIdentity.isAnonymous();
    }

    private String token(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        return sanitize(value);
    }

    private String quoted(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        return '"' + sanitize(value).replace("\\", "\\\\").replace("\"", "\\\"") + '"';
    }

    private String sanitize(String value) {
        String sanitized = value.replace('\n', ' ').replace('\r', ' ').replace('\t', ' ');
        return sanitized.length() <= MAX_LOG_VALUE_LENGTH ? sanitized : sanitized.substring(0, MAX_LOG_VALUE_LENGTH) + "…";
    }

    private String outcome(int status) {
        if (status < 400) {
            return "success";
        }
        if (status < 500) {
            return "client_error";
        }
        return "server_error";
    }
}
