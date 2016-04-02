package com.moodysalem.jaxrs.lib.exceptionmappers;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * This represents the structure of an error response from this server
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ErrorResponse {
    private int statusCode;
    private List<Error> errors;

    public Integer getNumErrors() {
        return (errors == null) ? 0 : errors.size();
    }

    public List<Error> getErrors() {
        return errors;
    }

    public void setErrors(List<Error> errors) {
        this.errors = errors;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }
}
