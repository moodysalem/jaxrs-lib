package com.moodysalem.jaxrs.lib.exceptionmappers;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Set;

/**
 * This represents the structure of an error response from this server
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ErrorResponse {
    private final int statusCode;
    private final Set<Error> errors;

    public ErrorResponse(@JsonProperty("statusCode") int statusCode, @JsonProperty("errors") Set<Error> errors) {
        this.statusCode = statusCode;
        this.errors = errors;
    }

    public Integer getNumErrors() {
        return (errors == null) ? 0 : errors.size();
    }

    public Set<Error> getErrors() {
        return errors;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
