package com.moodysalem.jaxrs.lib.converters;

import javax.ws.rs.ext.ParamConverter;
import javax.ws.rs.ext.ParamConverterProvider;
import javax.ws.rs.ext.Provider;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Allows Jersey to handle JodaTime types as parameters
 */
@SuppressWarnings("unchecked")
@Provider
public class JodaTimeParamConverterProvider implements ParamConverterProvider {
    private static final Logger LOG = Logger.getLogger(JodaTimeParamConverterProvider.class.getName());
    private static final LocalDateParamConverter localDateParamConverter = new LocalDateParamConverter();
    private static final LocalDateTimeParamConverter localDateTimeParamConverter = new LocalDateTimeParamConverter();
    private static final LocalTimeParamConverter localTimeParamConverter = new LocalTimeParamConverter();
    private static final InstantParamConverter instantParamConverter = new InstantParamConverter();

    @Override
    public <T> ParamConverter<T> getConverter(Class<T> rawType, Type genericType, Annotation[] annotations) {
        if (rawType.equals(LocalDate.class)) {
            return (ParamConverter<T>) localDateParamConverter;
        } else if (rawType.equals(LocalDateTime.class)) {
            return (ParamConverter<T>) localDateTimeParamConverter;
        } else if (rawType.equals(LocalTime.class)) {
            return (ParamConverter<T>) localTimeParamConverter;
        } else if (rawType.equals(Instant.class)) {
            return (ParamConverter<T>) instantParamConverter;
        }
        return null;
    }

    private static class LocalDateParamConverter implements ParamConverter<LocalDate> {
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

    private static class LocalTimeParamConverter implements ParamConverter<LocalTime> {
        @Override
        public LocalTime fromString(String value) {
            if (value != null) {
                try {
                    return LocalTime.parse(value);
                } catch (DateTimeParseException e) {
                    LOG.log(Level.WARNING, "Invalid local time parameter value specified", e);
                }
            }
            return null;
        }

        @Override
        public String toString(LocalTime value) {
            if (value != null) {
                return value.toString();
            }
            return null;
        }
    }

    private static class LocalDateTimeParamConverter implements ParamConverter<LocalDateTime> {
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
            if (value != null) {
                return value.toString();
            }
            return null;
        }
    }

    private static class InstantParamConverter implements ParamConverter<Instant> {
        @Override
        public Instant fromString(String value) {
            if (value != null) {
                try {
                    return Instant.parse(value);
                } catch (DateTimeParseException e) {
                    LOG.log(Level.WARNING, "Invalid instant parameter value specified", e);
                }
            }
            return null;
        }

        @Override
        public String toString(Instant instant) {
            if (instant != null) {
                return instant.toString();
            }
            return null;
        }
    }

}
