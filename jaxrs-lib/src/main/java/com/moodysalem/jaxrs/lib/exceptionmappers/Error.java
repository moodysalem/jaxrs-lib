package com.moodysalem.jaxrs.lib.exceptionmappers;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;
import java.util.UUID;

/**
 * Generic error object
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Error {
    private final UUID id;
    private final String attribute;
    private final String message;

    public Error(String message) {
        this(null, null, message);
    }

    public Error(@JsonProperty("id") UUID id,
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
        Error error = (Error) o;
        return Objects.equals(getId(), error.getId()) &&
                Objects.equals(getAttribute(), error.getAttribute()) &&
                Objects.equals(getMessage(), error.getMessage());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getAttribute(), getMessage());
    }
}
