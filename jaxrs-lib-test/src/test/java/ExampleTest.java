import com.fasterxml.jackson.databind.JsonNode;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class ExampleTest extends ExampleBaseTest {

    @Test
    public void runExampleTest() {
        JsonNode jn = target("test").request().get(JsonNode.class);

        assertEquals(jn.get("responseText").asText(), "Some Text");
    }

}
