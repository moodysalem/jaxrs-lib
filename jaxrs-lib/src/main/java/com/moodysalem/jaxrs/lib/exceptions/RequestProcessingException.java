package com.moodysalem.jaxrs.lib.exceptions;

import com.moodysalem.jaxrs.lib.exceptionmappers.Error;

import javax.ws.rs.core.Response;
import java.util.HashSet;
import java.util.Set;

public class RequestProcessingException extends RuntimeException {

    private final Set<Error> errors = new HashSet<>();
    private final int statusCode;

    public Set<Error> getErrors() {
        return errors;
    }

    public int getStatusCode() {
        return statusCode;
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
        this.statusCode = statusCode;
        if (errors != null) {
            for (Error e : errors) {
                addError(e);
            }
        }
    }

    public RequestProcessingException(Response.Status status, Error... errors) {
        this.statusCode = status != null ? status.getStatusCode() : 400;
        if (errors != null) {
            for (Error e : errors) {
                addError(e);
            }
        }
    }

    public RequestProcessingException(Response.Status status, String... errors) {
        this.statusCode = status != null ? status.getStatusCode() : 400;
        if (errors != null) {
            for (String e : errors) {
                addError(e);
            }
        }
    }

    public RequestProcessingException(int statusCode, String... errors) {
        this.statusCode = statusCode;
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
