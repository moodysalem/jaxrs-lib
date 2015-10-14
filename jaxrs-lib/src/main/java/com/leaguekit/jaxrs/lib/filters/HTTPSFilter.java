package com.leaguekit.jaxrs.lib.filters;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import java.io.IOException;

@Provider
@PreMatching
public class HTTPSFilter implements ContainerRequestFilter {
    public static final String PROTO_HEADER = "X-Forwarded-Proto";

    @Override
    public void filter(ContainerRequestContext containerRequestContext) throws IOException {
        String proto = containerRequestContext.getHeaderString(PROTO_HEADER);

        if (proto != null && !proto.equalsIgnoreCase("HTTPS")) {
            // forward the client to the https version of the site
            containerRequestContext.abortWith(
                Response.status(Response.Status.FOUND)
                    .header("Location",
                        containerRequestContext.getUriInfo().getRequestUriBuilder()
                            // remove all the information they shouldn't have communicated over http
                            .replacePath("")
                            .replaceQuery("")
                            .build()
                            .toURL()
                            .toString()
                    ).build()
            );
        }
    }
}
