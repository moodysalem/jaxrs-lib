package com.moodysalem.jaxrs.lib.exceptionmappers;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Set;

/**
 * This represents the model of an error response
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ErrorResponse {
    private final int statusCode;
    private final Set<RequestError> requestErrors;

    public ErrorResponse(@JsonProperty("statusCode") int statusCode, @JsonProperty("requestErrors") Set<RequestError> requestErrors) {
        this.statusCode = statusCode;
        this.requestErrors = requestErrors;
    }

    public Integer getNumErrors() {
        return (requestErrors == null) ? 0 : requestErrors.size();
    }

    public Set<RequestError> getRequestErrors() {
        return requestErrors;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
