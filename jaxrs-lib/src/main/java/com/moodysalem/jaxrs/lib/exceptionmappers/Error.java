package com.moodysalem.jaxrs.lib.exceptionmappers;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.UUID;

/**
 * Generic error object
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Error {
    private UUID id;
    private String attribute;
    private String message;

    public Error() {
    }

    public Error(String message) {
        this(null, null, message);
    }

    public Error(String attribute, String message) {
        this(null, attribute, message);
    }

    public Error(UUID id, String message) {
        this(id, null, message);
    }

    public Error(UUID id, String attribute, String message) {
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

    public void setId(UUID id) {
        this.id = id;
    }

    public void setAttribute(String attribute) {
        this.attribute = attribute;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
