package com.moodysalem.jaxrs.lib.exceptionmappers;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.ArrayList;
import java.util.List;

/**
 * Maps web application exceptions to error responses
 */
@Provider
public class WebApplicationExceptionMapper implements ExceptionMapper<WebApplicationException> {
    @Override
    public Response toResponse(WebApplicationException e) {
        ErrorResponse response = new ErrorResponse();
        List<Error> errors = new ArrayList<>();
        errors.add(new Error(e.getMessage()));
        response.setErrors(errors);
        response.setStatusCode(e.getResponse().getStatus());

        return Response.fromResponse(e.getResponse())
                .entity(response)
                .header(RequestProcessingExceptionMapper.NUMBER_OF_ERRORS_HEADER, errors.size())
                .build();
    }
}
