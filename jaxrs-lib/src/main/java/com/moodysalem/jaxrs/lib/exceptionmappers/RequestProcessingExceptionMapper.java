package com.moodysalem.jaxrs.lib.exceptionmappers;

import com.moodysalem.jaxrs.lib.exceptions.RequestProcessingException;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.ArrayList;
import java.util.List;

/**
 * Maps a request processing exception
 */
@Provider
public class RequestProcessingExceptionMapper implements ExceptionMapper<RequestProcessingException> {
    public static final String MESSAGE_KEY = "message";
    public static final String NUMBER_OF_ERRORS_HEADER = "X-Number-Of-Errors";

    @Override
    public Response toResponse(RequestProcessingException e) {
        List<ErrorObject> errorObjects = new ArrayList<>();
        if (e.getErrors() != null) {
            for (String err : e.getErrors()) {
                if (err != null) {
                    ErrorObject errObj = new ErrorObject();
                    errObj.setMessage(err);
                    errorObjects.add(errObj);
                }
            }
        }

        return Response.status(e.getStatusCode())
            .entity(errorObjects)
            .header(NUMBER_OF_ERRORS_HEADER, errorObjects.size())
            .build();
    }
}
