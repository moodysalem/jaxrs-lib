package com.moodysalem.jaxrs.lib.exceptionmappers;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.ArrayList;
import java.util.List;

/**
 * Maps web application exceptions to the same format as the request processing exceptions
 */
@Provider
public class WebApplicationExceptionMapper implements ExceptionMapper<WebApplicationException> {
    @Override
    public Response toResponse(WebApplicationException e) {
        List<ErrorObject> errors = new ArrayList<>();
        ErrorObject err = new ErrorObject();
        err.setMessage(e.getMessage());
        errors.add(err);

        return Response.fromResponse(e.getResponse())
            .entity(errors)
            .header(RequestProcessingExceptionMapper.NUMBER_OF_ERRORS_HEADER, errors.size())
            .build();
    }
}
