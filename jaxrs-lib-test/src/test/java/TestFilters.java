import com.moodysalem.jaxrs.lib.BaseApplication;
import com.moodysalem.jaxrs.lib.filters.CORSFilter;
import com.moodysalem.jaxrs.lib.filters.HTTPSFilter;
import com.moodysalem.jaxrs.lib.test.BaseTest;
import com.moodysalem.util.RandomStringUtil;
import org.glassfish.jersey.server.ResourceConfig;
import org.testng.annotations.Test;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.testng.AssertJUnit.assertTrue;

public class TestFilters extends BaseTest {

    public static final String X_CUSTOM_HEADER = "X-Custom-Header";
    public static final String X_ANOTHER_HEADER = "X-Another-Header";

    @Test
    public void testHttpsFilter() throws URISyntaxException {
        Response r = target("some/path/string").queryParam("test", "ab").request().header(HTTPSFilter.PROTO_HEADER, "http").get();
        assertTrue(r.getStatus() == Response.Status.FOUND.getStatusCode());
        URI base = target().getUri();
        URI loc = new URI(r.getHeaderString("Location"));
        assertTrue(loc.getScheme().equalsIgnoreCase("HTTPS"));
        assertTrue(loc.getPath().equalsIgnoreCase(base.getPath()));
        assertTrue(loc.getQuery() == null || loc.getQuery().equals(""));
    }

    // test normal operation of CORS filter
    @Test
    public void testCORSFilter() {
        Response r = target("cors").request().header(CORSFilter.ORIGIN_HEADER, "http://fakeurl.com").get();

        // check the exposed headers are all the custom headers returned
        String exposedHeaders = r.getHeaderString(CORSFilter.ACCESS_CONTROL_EXPOSE_HEADERS);
        assertTrue(exposedHeaders != null);
        String[] pcs = exposedHeaders.split(",");
        assertTrue(pcs.length == 2);
        Set<String> eh = new HashSet<>();
        Collections.addAll(eh, pcs);
        assertTrue(eh.contains(X_CUSTOM_HEADER));
        assertTrue(eh.contains(X_ANOTHER_HEADER));


        assertTrue("true".equals(r.getHeaderString(CORSFilter.ACCESS_CONTROL_ALLOW_CREDENTIALS)));
        assertTrue("*".equals(r.getHeaderString(CORSFilter.ACCESS_CONTROL_ALLOW_ORIGIN)));
        assertTrue("2592000".equals(r.getHeaderString(CORSFilter.ACCESS_CONTROL_MAX_AGE)));
        assertTrue(r.getHeaderString(X_CUSTOM_HEADER).length() == 64);
        assertTrue(r.getHeaderString(X_ANOTHER_HEADER).length() == 32);
    }

    // test that the filter can be skipped by setting a request property
    @Test
    public void testTargetedCORSFilterSkip() {
        Response r = target("cors").path("skip").request().header(CORSFilter.ORIGIN_HEADER, "http://fakeurl.com").get();

        // none of that should be available
        assertTrue(null == r.getHeaderString(CORSFilter.ACCESS_CONTROL_ALLOW_ORIGIN));
        assertTrue(null == r.getHeaderString(CORSFilter.ACCESS_CONTROL_ALLOW_CREDENTIALS));
        assertTrue(null == r.getHeaderString(CORSFilter.ACCESS_CONTROL_ALLOW_ORIGIN));
        assertTrue(null == r.getHeaderString(CORSFilter.ACCESS_CONTROL_MAX_AGE));
        assertTrue(null == r.getHeaderString(CORSFilter.ACCESS_CONTROL_EXPOSE_HEADERS));
    }

    @Path("cors")
    public static class CORSResource {
        @Context
        ContainerRequestContext containerRequestContext;

        @QueryParam("nocors")
        Boolean nocors;

        @GET
        public Response cors() {

            return Response.ok()
                .header(X_CUSTOM_HEADER, RandomStringUtil.randomAlphaNumeric(64))
                .header(X_ANOTHER_HEADER, RandomStringUtil.randomAlphaNumeric(32))
                .build();
        }

        @GET
        @Path("skip")
        @CORSFilter.Skip
        public Response testSkip() {
            return cors();
        }
    }

    @Override
    public ResourceConfig getResourceConfig() {
        ResourceConfig rc = new BaseApplication() {
            @Override
            public boolean forceHttps() {
                return true;
            }

            @Override
            public boolean allowCORS() {
                return true;
            }
        };

        rc.register(CORSResource.class);

        return rc;
    }
}
