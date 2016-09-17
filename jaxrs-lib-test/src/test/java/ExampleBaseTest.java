import com.moodysalem.jaxrs.lib.BaseApplication;
import com.moodysalem.jaxrs.lib.test.BaseTest;
import org.glassfish.jersey.server.ResourceConfig;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class ExampleBaseTest extends BaseTest {
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("test")
    public static class ExampleResource {
        class ResponseObject {
            private String responseText;
            private LocalDate ld;
            private LocalDateTime ldt;

            public String getResponseText() {
                return responseText;
            }

            public void setResponseText(String responseText) {
                this.responseText = responseText;
            }

            public LocalDate getLd() {
                return ld;
            }

            public void setLd(LocalDate ld) {
                this.ld = ld;
            }

            public LocalDateTime getLdt() {
                return ldt;
            }

            public void setLdt(LocalDateTime ldt) {
                this.ldt = ldt;
            }
        }

        @QueryParam("localDate")
        LocalDate ld;

        @QueryParam("localDateTime")
        LocalDateTime ldt;

        @GET
        public Response getJson() {
            ResponseObject ro = new ResponseObject();
            ro.setResponseText("Some Text");
            ro.setLd(ld);
            ro.setLdt(ldt);
            return Response.ok(ro).build();
        }

        @POST
        public Response post(ExampleTest.RequestObject ro) {
            return Response.ok(ro).build();
        }
    }

    @Override
    public ResourceConfig getResourceConfig() {
        ResourceConfig testConfig = new BaseApplication() {
            @Override
            public boolean forceLoadBalancerHTTPS() {
                return false;
            }

            @Override
            public boolean allowCORS() {
                return true;
            }
        };
        testConfig.register(ExampleResource.class);
        return testConfig;
    }
}
