package com.moodysalem.jaxrs.lib;

import com.moodysalem.jaxrs.lib.contextresolvers.ObjectMapperContextResolver;
import com.moodysalem.jaxrs.lib.converters.LocalDateParamConverterProvider;
import com.moodysalem.jaxrs.lib.filters.CORSFilter;
import com.moodysalem.jaxrs.lib.filters.HTTPSFilter;
import com.moodysalem.jaxrs.lib.providers.RequestProcessingExceptionMapper;
import com.moodysalem.jaxrs.lib.providers.WebApplicationExceptionMapper;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.message.GZipEncoder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.filter.EncodingFilter;
import org.glassfish.jersey.server.mvc.freemarker.FreemarkerMvcFeature;

public abstract class BaseApplication extends ResourceConfig {
    public BaseApplication() {
        // register the things that are typically used by a JAX-RS application

        // allow accepting localdate and localdatetime as query parameters, etc.
        register(LocalDateParamConverterProvider.class);

        // allow parsing and writing localdate and localdatetime with jackson
        register(ObjectMapperContextResolver.class);

        // json parsing
        register(JacksonFeature.class);

        // template engine
        property(FreemarkerMvcFeature.CACHE_TEMPLATES, true);
        register(FreemarkerMvcFeature.class);

        // send CORS headers
        if (allowCORS()) {
            register(CORSFilter.class);
        }

        // force HTTPS behind ELB
        if (forceHttps()) {
            register(HTTPSFilter.class);
        }

        // custom exception handler
        register(RequestProcessingExceptionMapper.class);

        // map webapplicationexceptions to the appropriate json structure
        register(WebApplicationExceptionMapper.class);

        EncodingFilter.enableFor(this, GZipEncoder.class);
    }

    public abstract boolean forceHttps();

    public abstract boolean allowCORS();
}