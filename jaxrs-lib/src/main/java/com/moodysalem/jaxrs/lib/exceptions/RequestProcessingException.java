package com.moodysalem.jaxrs.lib.exceptions;

import com.moodysalem.jaxrs.lib.exceptionmappers.RequestError;

import javax.validation.ConstraintViolation;
import javax.ws.rs.core.Response;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class RequestProcessingException extends RuntimeException {
    private static Throwable root(Throwable throwable) {
        while (throwable != null && throwable.getCause() != null) {
            throwable = throwable.getCause();
        }
        return throwable;
    }

    public static RequestProcessingException from(Throwable e) {
        if (e == null) {
            return null;
        }

        e = root(e);

        if (e instanceof javax.validation.ConstraintViolationException) {
            final javax.validation.ConstraintViolationException cve = (javax.validation.ConstraintViolationException) e;
            final List<RequestError> errors = new LinkedList<>();
            if (cve.getConstraintViolations() != null) {
                for (ConstraintViolation cv : cve.getConstraintViolations()) {
                    final String prop = cv.getPropertyPath() != null ? cv.getPropertyPath().toString() : null;
                    final String error = cv.getMessage() != null ? cv.getMessage() : "unknown error";
                    errors.add(new RequestError(null, prop, error));
                }
            }

            return new RequestProcessingException(422, errors.stream().toArray(RequestError[]::new));
        }

        return new RequestProcessingException(Response.Status.CONFLICT, e.getMessage());
    }


    private final Set<RequestError> requestErrors = new HashSet<>();
    private final int statusCode;

    public Set<RequestError> getRequestErrors() {
        return requestErrors;
    }

    public int getStatusCode() {
        return statusCode;
    }

    private void addError(String error) {
        if (error != null) {
            requestErrors.add(new RequestError(error));
        }
    }

    private void addError(RequestError requestError) {
        if (requestError != null) {
            requestErrors.add(requestError);
        }
    }

    public RequestProcessingException(int statusCode, RequestError... requestErrors) {
        this.statusCode = statusCode;
        if (requestErrors != null) {
            for (RequestError e : requestErrors) {
                addError(e);
            }
        }
    }

    public RequestProcessingException(Response.Status status, RequestError... requestErrors) {
        this.statusCode = status != null ? status.getStatusCode() : 400;
        if (requestErrors != null) {
            for (RequestError e : requestErrors) {
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

    public RequestProcessingException(RequestError... requestErrors) {
        this(400, requestErrors);
    }

    public RequestProcessingException(String... errors) {
        this(400, errors);
    }
}
