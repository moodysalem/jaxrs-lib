package com.moodysalem.jaxrs.lib.contextresolvers;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moodysalem.jaxrs.lib.contextresolvers.jackson.modules.JodaTimeSerializerModule;

import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;

/**
 * Provides an object mapper with additional serialization support
 */
@Provider
public class ObjectMapperContextResolver implements ContextResolver<ObjectMapper> {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    static {
        OBJECT_MAPPER.registerModule(new JodaTimeSerializerModule());
        OBJECT_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Override
    public ObjectMapper getContext(Class<?> type) {
        return OBJECT_MAPPER;
    }
}
