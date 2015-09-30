package com.leaguekit.jaxrs.lib;

import com.leaguekit.jaxrs.lib.exceptions.RequestProcessingException;
import com.leaguekit.jaxrs.lib.filters.CORSFilter;
import com.leaguekit.jaxrs.lib.filters.HTTPSFilter;
import com.leaguekit.jaxrs.lib.providers.RequestProcessingExceptionMapper;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.message.GZipEncoder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.filter.EncodingFilter;

public class BaseApplication extends ResourceConfig {
    public BaseApplication() {
        // register the things that are typically used by a JAX-RS application
        register(JacksonFeature.class);
        register(CORSFilter.class);
        register(HTTPSFilter.class);
        register(RequestProcessingExceptionMapper.class);

        EncodingFilter.enableFor(this, GZipEncoder.class);
    }
}