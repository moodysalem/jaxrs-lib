package com.moodysalem.jaxrs.lib.exceptionmappers;

import com.moodysalem.jaxrs.lib.exceptions.RequestProcessingException;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.ArrayList;
import java.util.List;

/**
 * Maps a request exception to an error response
 */
@Provider
public class RequestProcessingExceptionMapper implements ExceptionMapper<RequestProcessingException> {
    public static final String NUMBER_OF_ERRORS_HEADER = "X-Number-Of-Errors";

    @Override
    public Response toResponse(RequestProcessingException e) {
        ErrorResponse res = new ErrorResponse();

        res.setErrors(e.getErrors());
        res.setStatusCode(e.getStatusCode());

        return Response.status(e.getStatusCode())
                .entity(res)
                .header(NUMBER_OF_ERRORS_HEADER, res.getNumErrors())
                .build();
    }
}
