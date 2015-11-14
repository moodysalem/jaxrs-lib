package com.moodysalem.jaxrs.lib.test;

import com.moodysalem.jaxrs.lib.contextresolvers.ObjectMapperContextResolver;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.message.GZipEncoder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.server.filter.EncodingFilter;
import org.glassfish.jersey.servlet.ServletContainer;
import org.glassfish.jersey.test.DeploymentContext;
import org.glassfish.jersey.test.JerseyTestNg;
import org.glassfish.jersey.test.ServletDeploymentContext;
import org.glassfish.jersey.test.TestProperties;
import org.glassfish.jersey.test.grizzly.GrizzlyWebTestContainerFactory;
import org.glassfish.jersey.test.spi.TestContainerException;
import org.glassfish.jersey.test.spi.TestContainerFactory;

public abstract class BaseTest extends JerseyTestNg.ContainerPerClassTest {

    public abstract ResourceConfig getResourceConfig();

    @Override
    protected TestContainerFactory getTestContainerFactory() throws TestContainerException {
        return new GrizzlyWebTestContainerFactory();
    }

    @Override
    protected DeploymentContext configureDeployment() {
        enable(TestProperties.LOG_TRAFFIC);
        enable(TestProperties.DUMP_ENTITY);
        ResourceConfig toDeploy = getResourceConfig();

        toDeploy.property(ServerProperties.RESPONSE_SET_STATUS_OVER_SEND_ERROR, "true");

        toDeploy.register(JacksonFeature.class);

        EncodingFilter.enableFor(toDeploy, GZipEncoder.class);

        return ServletDeploymentContext.forServlet(new ServletContainer(toDeploy)).build();
    }

    @Override
    protected void configureClient(ClientConfig config) {
        config.register(JacksonFeature.class).register(ObjectMapperContextResolver.class);
    }

}