package com.leaguekit.jaxrs.lib.contextresolvers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;

import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.logging.Level;
import java.util.logging.Logger;

@Provider
public class ObjectMapperContextResolver implements ContextResolver<ObjectMapper> {
    private static final ObjectMapper om;
    private static final Logger LOG = Logger.getLogger(ObjectMapperContextResolver.class.getName());

    static {
        om = new ObjectMapper();
        SimpleModule sm = new SimpleModule();
        sm.addSerializer(LocalDate.class, new JsonSerializer<LocalDate>() {
            @Override
            public void serialize(LocalDate localDate, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
                if (localDate == null) {
                    jsonGenerator.writeNull();
                } else {
                    jsonGenerator.writeString(localDate.toString());
                }
            }
        }).addSerializer(LocalDateTime.class, new JsonSerializer<LocalDateTime>() {
            @Override
            public void serialize(LocalDateTime localDateTime, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
                if (localDateTime == null) {
                    jsonGenerator.writeNull();
                } else {
                    jsonGenerator.writeString(localDateTime.toString());
                }
            }
        }).addDeserializer(LocalDate.class, new JsonDeserializer<LocalDate>() {
            @Override
            public LocalDate deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
                String dateString = jsonParser.getValueAsString();
                if (dateString != null) {
                    try {
                        return LocalDate.parse(dateString);
                    } catch (DateTimeParseException e) {
                        LOG.log(Level.WARNING, String.format("Failed to parse date string as LocalDate: %s", dateString), e);
                    }
                }
                return null;
            }
        }).addDeserializer(LocalDateTime.class, new JsonDeserializer<LocalDateTime>() {
            @Override
            public LocalDateTime deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
                String dateTimeString = jsonParser.getValueAsString();
                if (dateTimeString != null) {
                    try {
                        return LocalDateTime.parse(dateTimeString);
                    } catch (DateTimeParseException e) {
                        LOG.log(Level.WARNING, String.format("Failed to parse datetime string as LocalDateTime: %s", dateTimeString), e);
                    }
                }
                return null;
            }
        });
        om.registerModule(sm);
    }

    @Override
    public ObjectMapper getContext(Class<?> type) {
        return om;
    }
}
