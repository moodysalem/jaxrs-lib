package com.moodysalem.jaxrs.lib.contextresolvers.jackson.modules;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.TextNode;

import java.io.IOException;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.logging.Level;
import java.util.logging.Logger;

public class JodaTimeSerializerModule extends SimpleModule {

    private static final Logger LOG = Logger.getLogger(JodaTimeSerializerModule.class.getName());

    public JodaTimeSerializerModule() {
        super("JodaTime Serializer Module");
        addDeserializer(LocalDate.class, new JsonDeserializer<LocalDate>() {
            @Override
            public LocalDate deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
                ObjectCodec oc = jsonParser.getCodec();
                TextNode node = oc.readTree(jsonParser);
                String dateString = node.textValue();
                if (dateString == null || dateString.trim().isEmpty()) {
                    return null;
                }
                try {
                    return LocalDate.parse(dateString);
                } catch (DateTimeException ignored) {
                    LOG.log(Level.SEVERE, "Failed to parse a LocalDate from an object: " + dateString);
                }
                return null;
            }
        });

        addSerializer(LocalDate.class, new JsonSerializer<LocalDate>() {
            @Override
            public void serialize(LocalDate localDate, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
                jsonGenerator.writeString((localDate != null) ? localDate.toString() : null);
            }
        });

        addDeserializer(LocalDateTime.class, new JsonDeserializer<LocalDateTime>() {
            @Override
            public LocalDateTime deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
                ObjectCodec oc = jsonParser.getCodec();
                TextNode node = oc.readTree(jsonParser);
                String dateString = node.textValue();
                if (dateString == null || dateString.trim().isEmpty()) {
                    return null;
                }
                try {
                    return LocalDateTime.parse(dateString);
                } catch (DateTimeException ignored) {
                    LOG.log(Level.SEVERE, "Failed to parse a LocalDateTime from an object: " + dateString);
                }
                return null;
            }
        });

        addSerializer(LocalDateTime.class, new JsonSerializer<LocalDateTime>() {
            @Override
            public void serialize(LocalDateTime localDateTime, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
                jsonGenerator.writeString((localDateTime != null) ? localDateTime.toString() : null);
            }
        });


        addDeserializer(LocalTime.class, new JsonDeserializer<LocalTime>() {
            @Override
            public LocalTime deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
                ObjectCodec oc = jsonParser.getCodec();
                TextNode node = oc.readTree(jsonParser);
                String timeString = node.textValue();
                if (timeString == null || timeString.trim().isEmpty()) {
                    return null;
                }
                try {
                    return LocalTime.parse(timeString);
                } catch (DateTimeException ignored) {
                    LOG.log(Level.SEVERE, "Failed to parse a LocalTime from an object: " + timeString);
                }
                return null;
            }
        });

        addSerializer(LocalTime.class, new JsonSerializer<LocalTime>() {
            @Override
            public void serialize(LocalTime localTime, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
                jsonGenerator.writeString((localTime != null) ? localTime.toString() : null);
            }
        });
    }
}
