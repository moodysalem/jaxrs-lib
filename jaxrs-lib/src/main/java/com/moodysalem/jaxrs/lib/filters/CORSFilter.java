package com.moodysalem.jaxrs.lib.filters;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.*;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Adds CORS headers to all the corresponding resources
 * <p>
 * Apply the Skip annotation to a method or class to disable this filter for a specific method or class
 */
public class CORSFilter implements DynamicFeature {

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

    /**
     * Apply the filter to the methods and classes that don't have the Skip annotation
     *
     * @param resourceInfo info about the resource method
     * @param context      registration context
     */
    @Override
    public void configure(ResourceInfo resourceInfo, FeatureContext context) {
        if (!resourceInfo.getResourceClass().isAnnotationPresent(Skip.class) &&
                !resourceInfo.getResourceMethod().isAnnotationPresent(Skip.class)) {
            context.register(Filter.class);
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface Skip {
    }

    /**
     * Adds CORS headers to allow any origin
     */
    @Provider
    @Priority(Priorities.HEADER_DECORATOR)
    public static class Filter implements ContainerResponseFilter {

        @Override
        public void filter(ContainerRequestContext req, ContainerResponseContext resp)
                throws IOException {
            final MultivaluedMap<String, Object> headers = resp.getHeaders();

            final String origin = req.getHeaderString(ORIGIN_HEADER);
            // only if origin header is present do we slap on these origin headers
            if (origin != null) {
                // these are always ok
                headers.putSingle(ACCESS_CONTROL_ALLOW_ORIGIN, origin);
                headers.putSingle(ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
                headers.putSingle(ACCESS_CONTROL_ALLOW_METHODS, ALL_METHODS);

                // allow all the request headers
                final String requestHeadersAllowed = req.getHeaderString(ACCESS_CONTROL_REQUEST_HEADERS);
                if (requestHeadersAllowed != null) {
                    headers.putSingle(ACCESS_CONTROL_ALLOW_HEADERS, requestHeadersAllowed);
                }

                final Set<String> customHeaders = resp.getHeaders().keySet().stream()
                        .filter((s) -> s != null && s.toUpperCase().startsWith("X-")).collect(Collectors.toSet());
                if (customHeaders.size() > 0) {
                    headers.putSingle(ACCESS_CONTROL_EXPOSE_HEADERS, customHeaders.stream().collect(Collectors.joining(",")));
                }

                // allow browser to cache this forever
                headers.putSingle(ACCESS_CONTROL_MAX_AGE, ACCESS_CONTROL_CACHE_SECONDS);
            }

        }

    }

}

