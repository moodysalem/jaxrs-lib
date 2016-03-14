package com.moodysalem.jaxrs.lib.contextresolvers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moodysalem.jaxrs.lib.contextresolvers.jackson.modules.JodaTimeSerializerModule;

import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;

@Provider
public class ObjectMapperContextResolver implements ContextResolver<ObjectMapper> {
    private static final ObjectMapper om;

    static {
        om = new ObjectMapper();
        om.registerModule(new JodaTimeSerializerModule());
    }

    @Override
    public ObjectMapper getContext(Class<?> type) {
        return om;
    }
}
