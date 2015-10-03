package com.leaguekit.jaxrs.lib;

import com.leaguekit.jaxrs.lib.filters.CORSFilter;
import com.leaguekit.jaxrs.lib.filters.HTTPSFilter;
import com.leaguekit.jaxrs.lib.providers.RequestProcessingExceptionMapper;
import com.leaguekit.jaxrs.lib.providers.WebApplicationExceptionMapper;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.message.GZipEncoder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.filter.EncodingFilter;
import org.glassfish.jersey.server.mvc.freemarker.FreemarkerMvcFeature;

public class BaseApplication extends ResourceConfig {
    public BaseApplication() {
        // register the things that are typically used by a JAX-RS application
        register(JacksonFeature.class);
        register(FreemarkerMvcFeature.class);

        register(CORSFilter.class);
        register(HTTPSFilter.class);

        register(RequestProcessingExceptionMapper.class);
        register(WebApplicationExceptionMapper.class);

        EncodingFilter.enableFor(this, GZipEncoder.class);
    }
}