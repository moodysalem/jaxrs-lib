package com.leaguekit.jaxrs.lib.providers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import javax.ws.rs.ClientErrorException;
import javax.ws.rs.RedirectionException;
import javax.ws.rs.ServerErrorException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class ClientErrorServerErrorExceptionMapper implements ExceptionMapper<WebApplicationException> {

    private static final ObjectMapper om = new ObjectMapper();

    private Response mapException(WebApplicationException e) {
        ArrayNode errors = om.createArrayNode();

        ObjectNode errObj = om.createObjectNode();
        errObj.put(RequestProcessingExceptionMapper.MESSAGE_KEY, e.getMessage());
        errors.add(errObj);

        return Response.status(e.getResponse().getStatus())
            .entity(errors)
            .header(RequestProcessingExceptionMapper.NUMBER_OF_ERRORS_HEADER, errors.size())
            .build();
    }

    @Override
    public Response toResponse(WebApplicationException e) {
        if (e instanceof RedirectionException) {
            return e.getResponse();
        }
        return mapException(e);
    }
}
