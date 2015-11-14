package com.moodysalem.jaxrs.lib.providers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
@Produces(MediaType.APPLICATION_JSON)
public class WebApplicationExceptionMapper implements ExceptionMapper<WebApplicationException> {

    private static final ObjectMapper om = new ObjectMapper();

    private Response mapException(WebApplicationException e) {
        ArrayNode errors = om.createArrayNode();

        ObjectNode errObj = om.createObjectNode();
        errObj.put(RequestProcessingExceptionMapper.MESSAGE_KEY, e.getMessage());
        errors.add(errObj);

        return Response.fromResponse(e.getResponse())
            .entity(errors)
            .header(RequestProcessingExceptionMapper.NUMBER_OF_ERRORS_HEADER, errors.size())
            .build();
    }

    @Override
    public Response toResponse(WebApplicationException e) {
        return mapException(e);
    }
}
