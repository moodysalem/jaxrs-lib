import com.fasterxml.jackson.databind.JsonNode;
import org.testng.annotations.Test;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class ExampleTest extends ExampleBaseTest {

    @Test
    public void runExampleTest() {
        JsonNode jn = target("test").queryParam("localDate", "2015-05-10")
            .queryParam("localDateTime", "2015-05-10T09:30:00")
            .request().get(JsonNode.class);

        assertEquals(jn.get("responseText").asText(), "Some Text");
    }

    public static class RequestObject {
        private LocalDate ld;
        private LocalDateTime ldt;

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

    @Test
    public void runExamplePost() {
        RequestObject ro = new RequestObject();
        ro.setLd(LocalDate.now());
        ro.setLdt(LocalDateTime.now());


        Response r = target("test").request().post(Entity.entity(ro, MediaType.APPLICATION_JSON));

        assertTrue(r.getStatus() == 200);
    }

}
