package com.moodysalem.jaxrs.lib.exceptionmappers;

import com.moodysalem.jaxrs.lib.exceptions.RequestProcessingException;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * Maps a request exception to an error response
 */
@Provider
public class RequestProcessingExceptionMapper implements ExceptionMapper<RequestProcessingException> {
    public static final String NUMBER_OF_ERRORS_HEADER = "X-Number-Of-Errors";

    @Override
    public Response toResponse(RequestProcessingException e) {
        final ErrorResponse res = new ErrorResponse(e.getStatusCode(), e.getErrors());

        return Response.status(e.getStatusCode())
                .entity(res)
                .header(NUMBER_OF_ERRORS_HEADER, res.getNumErrors())
                .build();
    }
}
