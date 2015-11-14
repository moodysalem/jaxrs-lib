package com.moodysalem.jaxrs.lib.converters;

import javax.ws.rs.ext.ParamConverter;
import javax.ws.rs.ext.ParamConverterProvider;
import javax.ws.rs.ext.Provider;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.logging.Level;
import java.util.logging.Logger;

@Provider
public class LocalDateParamConverterProvider implements ParamConverterProvider {

    @Override
    public <T> ParamConverter<T> getConverter(Class<T> rawType, Type genericType, Annotation[] annotations) {
        if (rawType.equals(LocalDate.class)) {
            return (ParamConverter<T>) new LocalDateParamConverter();
        } else if (rawType.equals(LocalDateTime.class)) {
            return (ParamConverter<T>) new LocalDateTimeParamConverter();
        }
        return null;
    }

    public static class LocalDateParamConverter implements ParamConverter<LocalDate> {
        private static final Logger LOG = Logger.getLogger(LocalDateParamConverter.class.getName());

        @Override
        public LocalDate fromString(String value) {
            if (value != null) {
                try {
                    return LocalDate.parse(value);
                } catch (DateTimeParseException e) {
                    LOG.log(Level.WARNING, "Invalid local date parameter value specified", e);
                }
            }
            return null;
        }

        @Override
        public String toString(LocalDate value) {
            if (value != null) {
                return value.toString();
            }
            return null;
        }
    }

    public static class LocalDateTimeParamConverter implements ParamConverter<LocalDateTime> {
        private static final Logger LOG = Logger.getLogger(LocalDateTimeParamConverter.class.getName());

        @Override
        public LocalDateTime fromString(String value) {
            if (value != null) {
                try {
                    return LocalDateTime.parse(value);
                } catch (DateTimeParseException e) {
                    LOG.log(Level.WARNING, "Invalid local datetime parameter value specified", e);
                }
            }
            return null;
        }

        @Override
        public String toString(LocalDateTime value) {
            return null;
        }
    }

}
