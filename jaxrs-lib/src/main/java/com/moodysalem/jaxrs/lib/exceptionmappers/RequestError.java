package com.moodysalem.jaxrs.lib.exceptionmappers;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;
import java.util.UUID;

/**
 * A value type that stores information about a request error
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RequestError {
    private final UUID id;
    private final String attribute, message;

    public RequestError(final String message) {
        this(null, null, message);
    }

    public RequestError(@JsonProperty("id") UUID id,
                        @JsonProperty("attribute") String attribute,
                        @JsonProperty("message") String message) {
        this.id = id;
        this.attribute = attribute;
        this.message = message;
    }

    public UUID getId() {
        return id;
    }

    public String getAttribute() {
        return attribute;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RequestError requestError = (RequestError) o;
        return Objects.equals(getId(), requestError.getId()) &&
                Objects.equals(getAttribute(), requestError.getAttribute()) &&
                Objects.equals(getMessage(), requestError.getMessage());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getAttribute(), getMessage());
    }
}
