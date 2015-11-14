import com.moodysalem.jaxrs.lib.BaseApplication;
import com.moodysalem.jaxrs.lib.filters.CORSFilter;
import com.moodysalem.jaxrs.lib.filters.HTTPSFilter;
import com.moodysalem.jaxrs.lib.test.BaseTest;
import com.moodysalem.util.RandomStringUtil;
import org.glassfish.jersey.server.ResourceConfig;
import org.testng.annotations.Test;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.net.URISyntaxException;

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

    @Test
    public void testCORSFilter() {
        System.setProperty("sun.net.http.allowRestrictedHeaders", "true");
        Response r = target("cors").request().header(CORSFilter.ORIGIN_HEADER, "http://fakeurl.com").get();
//        assertTrue("*".equals(r.getHeaderString(CORSFilter.ACCESS_CONTROL_ALLOW_ORIGIN)));
        assertTrue(r.getHeaderString(X_CUSTOM_HEADER).length() == 64);
        assertTrue(r.getHeaderString(X_ANOTHER_HEADER).length() == 32);
    }

    @Path("cors")
    public static class CORSResource {
        @GET
        public Response cors() {
            return Response.ok()
                .header(X_CUSTOM_HEADER, RandomStringUtil.randomAlphaNumeric(64))
                .header(X_ANOTHER_HEADER, RandomStringUtil.randomAlphaNumeric(32))
                .build();
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
