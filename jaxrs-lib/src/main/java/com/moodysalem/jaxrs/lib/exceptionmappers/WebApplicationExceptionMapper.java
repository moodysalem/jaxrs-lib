package com.moodysalem.jaxrs.lib.exceptionmappers;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Maps web application exceptions to error responses
 */
@Provider
public class WebApplicationExceptionMapper implements ExceptionMapper<WebApplicationException> {
    @Override
    public Response toResponse(WebApplicationException e) {
        final Set<RequestError> requestErrors = new HashSet<>(Collections.singletonList(new RequestError(e.getMessage())));
        final ErrorResponse response = new ErrorResponse(e.getResponse().getStatus(), requestErrors);

        return Response.fromResponse(e.getResponse())
                .entity(response)
                .header(RequestProcessingExceptionMapper.NUMBER_OF_ERRORS_HEADER, requestErrors.size())
                .build();
    }
}
