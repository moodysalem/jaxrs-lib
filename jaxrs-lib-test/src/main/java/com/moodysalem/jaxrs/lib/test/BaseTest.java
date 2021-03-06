package com.moodysalem.jaxrs.lib.test;

import com.moodysalem.jaxrs.lib.BaseApplication;
import com.moodysalem.jaxrs.lib.contextresolvers.ObjectMapperContextResolver;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
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
        // Find first available port.
        forceSet(TestProperties.CONTAINER_PORT, "0");

        // Log traffic to console
        enable(TestProperties.LOG_TRAFFIC);

        // Dump request and response bodies
        enable(TestProperties.DUMP_ENTITY);

        final ResourceConfig toDeploy = getResourceConfig();

        toDeploy.property(ServerProperties.RESPONSE_SET_STATUS_OVER_SEND_ERROR, "true");

        if (!(toDeploy instanceof BaseApplication)) {
            toDeploy.register(JacksonFeature.class);
        }

        return ServletDeploymentContext.forServlet(new ServletContainer(toDeploy)).build();
    }

    @Override
    protected void configureClient(ClientConfig config) {
        System.setProperty("sun.net.http.allowRestrictedHeaders", "true");
        config.register(JacksonFeature.class).register(ObjectMapperContextResolver.class);
    }

}