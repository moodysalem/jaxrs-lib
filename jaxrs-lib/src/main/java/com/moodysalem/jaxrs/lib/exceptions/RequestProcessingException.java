package com.moodysalem.jaxrs.lib.exceptions;

import com.moodysalem.jaxrs.lib.exceptionmappers.Error;
import jersey.repackaged.com.google.common.collect.Lists;

import javax.ws.rs.core.Response;
import java.util.*;
import java.util.stream.Collectors;

public class RequestProcessingException extends RuntimeException {

    private final List<Error> errors = new ArrayList<>();
    private int statusCode;

    public List<Error> getErrors() {
        return errors;
    }

    public int getStatusCode() {
        return statusCode;
    }

    private void setStatusCode(Response.Status status) {
        setStatusCode((status != null) ? status.getStatusCode() : 400);
    }

    private void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    private void addError(String error) {
        if (error != null) {
            errors.add(new Error(error));
        }
    }

    private void addError(Error error) {
        if (error != null) {
            errors.add(error);
        }
    }

    public RequestProcessingException(int statusCode, Error... errors) {
        setStatusCode(statusCode);
        if (errors != null) {
            for (Error e : errors) {
                addError(e);
            }
        }
    }

    public RequestProcessingException(Response.Status status, Error... errors) {
        setStatusCode(statusCode);
        if (errors != null) {
            for (Error e : errors) {
                addError(e);
            }
        }
    }

    public RequestProcessingException(Response.Status status, String... errors) {
        setStatusCode(status);
        if (errors != null) {
            for (String e : errors) {
                addError(e);
            }
        }
    }

    public RequestProcessingException(int statusCode, String... errors) {
        setStatusCode(statusCode);
        if (errors != null) {
            for (String e : errors) {
                addError(e);
            }
        }
    }

    public RequestProcessingException(Error... errors) {
        this(400, errors);
    }

    public RequestProcessingException(String... errors) {
        this(400, errors);
    }
}
