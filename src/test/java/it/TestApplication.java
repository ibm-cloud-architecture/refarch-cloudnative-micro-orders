package it;

import org.junit.Test;

public class TestApplication extends EndpointTest {

    @Test
    public void testDeployment() {
        testEndpoint("/index.html", "Liberty");
    }

}
