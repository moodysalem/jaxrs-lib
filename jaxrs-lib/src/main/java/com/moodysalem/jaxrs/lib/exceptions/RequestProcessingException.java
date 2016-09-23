package com.moodysalem.jaxrs.lib.exceptions;

import com.moodysalem.jaxrs.lib.exceptionmappers.RequestError;
import org.hibernate.exception.ConstraintViolationException;

import javax.persistence.PersistenceException;
import javax.validation.ConstraintViolation;
import javax.ws.rs.core.Response;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

public class RequestProcessingException extends RuntimeException {
    public static RequestProcessingException from(Exception e) {
        if (e == null) {
            return null;
        }

        if (e instanceof PersistenceException) {
            final PersistenceException pe = (PersistenceException) e;
            return new RequestProcessingException(422, pe.getCause().getMessage());
        }

        if (e instanceof ConstraintViolationException) {
            final ConstraintViolationException cve = (ConstraintViolationException) e;
            final StringBuilder sb = new StringBuilder();

            SQLException se = cve.getSQLException();

            while (se != null) {
                if (sb.length() > 0) {
                    sb.append(", ");
                }
                sb.append(se.getMessage());
                se = se.getNextException();
            }

            return new RequestProcessingException(Response.Status.NOT_ACCEPTABLE, sb.toString());
        }

        if (e instanceof javax.validation.ConstraintViolationException) {
            final StringBuilder sb = new StringBuilder();

            final javax.validation.ConstraintViolationException cve = (javax.validation.ConstraintViolationException) e;
            if (cve.getConstraintViolations() != null) {
                for (ConstraintViolation cv : cve.getConstraintViolations()) {
                    if (sb.length() > 0) {
                        sb.append(", ");
                    }
                    final String prop = cv.getPropertyPath() != null ? cv.getPropertyPath().toString() : null;
                    final String error = cv.getMessage() != null ? cv.getMessage() : "unknown error";
                    sb.append(error);
                }
            }

            return new RequestProcessingException(422, "Unprocessable entity");
        }

        return new RequestProcessingException(Response.Status.INTERNAL_SERVER_ERROR, e.getMessage());
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
