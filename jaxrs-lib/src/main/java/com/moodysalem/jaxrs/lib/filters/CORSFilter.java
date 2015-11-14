package com.moodysalem.jaxrs.lib.filters;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Adds CORS headers to allow any origin
 */
@Provider
@Priority(Priorities.HEADER_DECORATOR)
public class CORSFilter implements ContainerResponseFilter {

    public static final int ACCESS_CONTROL_CACHE_SECONDS = 2592000;
    public static final String ACCESS_CONTROL_EXPOSE_HEADERS = "Access-Control-Expose-Headers";
    public static final String ACCESS_CONTROL_MAX_AGE = "Access-Control-Max-Age";
    public static final String ACCESS_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin";
    public static final String ACCESS_CONTROL_ALLOW_CREDENTIALS = "Access-Control-Allow-Credentials";
    public static final String ACCESS_CONTROL_ALLOW_METHODS = "Access-Control-Allow-Methods";
    public static final String ACCESS_CONTROL_ALLOW_HEADERS = "Access-Control-Allow-Headers";
    public static final String ACCESS_CONTROL_REQUEST_HEADERS = "Access-Control-Request-Headers";
    public static final String ALL_METHODS = "GET,POST,DELETE,PUT,OPTIONS";
    public static final String ORIGIN_HEADER = "Origin";

    @Override
    public void filter(ContainerRequestContext containerRequestContext, ContainerResponseContext containerResponseContext)
        throws IOException {
        MultivaluedMap<String, Object> headers = containerResponseContext.getHeaders();

        // only if origin header is present do we slap on these origin headers
        if (containerRequestContext.getHeaderString(ORIGIN_HEADER) != null) {
            // these are always ok
            headers.putSingle(ACCESS_CONTROL_ALLOW_ORIGIN, "*");
            headers.putSingle(ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
            headers.putSingle(ACCESS_CONTROL_ALLOW_METHODS, ALL_METHODS);

            // allow all the request headers
            String requestHeadersAllowed = containerRequestContext.getHeaderString(ACCESS_CONTROL_REQUEST_HEADERS);
            if (requestHeadersAllowed != null) {
                headers.putSingle(ACCESS_CONTROL_ALLOW_HEADERS, requestHeadersAllowed);
            }

            Set<String> customHeaders = containerResponseContext.getHeaders().keySet().stream()
                .filter((s) -> s != null && s.toUpperCase().startsWith("X-")).collect(Collectors.toSet());
            if (customHeaders.size() > 0) {
                headers.putSingle(ACCESS_CONTROL_EXPOSE_HEADERS, customHeaders.stream().collect(Collectors.joining(",")));
            }

            // allow browser to cache this forever
            headers.putSingle(ACCESS_CONTROL_MAX_AGE, ACCESS_CONTROL_CACHE_SECONDS);
        }

    }

}
