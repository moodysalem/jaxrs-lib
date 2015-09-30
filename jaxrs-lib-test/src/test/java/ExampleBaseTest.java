import com.leaguekit.jaxrs.lib.BaseApplication;
import com.leaguekit.jaxrs.lib.test.BaseTest;
import org.glassfish.jersey.server.ResourceConfig;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

public class ExampleBaseTest extends BaseTest {
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("test")
    public static class ExampleResource {
        class ResponseObject {
            private String responseText;

            public String getResponseText() {
                return responseText;
            }

            public void setResponseText(String responseText) {
                this.responseText = responseText;
            }
        }

        @GET
        public Response getJson() {
            ResponseObject ro = new ResponseObject();
            ro.setResponseText("Some Text");
            return Response.ok(ro).build();
        }
    }

    @Override
    public ResourceConfig getResourceConfig() {
        ResourceConfig testConfig = new BaseApplication();
        testConfig.register(ExampleResource.class);
        return testConfig;
    }
}
