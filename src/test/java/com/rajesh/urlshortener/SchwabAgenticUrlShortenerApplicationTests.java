package com.rajesh.urlshortener;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
class SchwabAgenticUrlShortenerApplicationTests {

    @Value("${management.endpoints.web.exposure.include}")
    private String exposedActuatorEndpoints;

    @Test
    void contextLoads() {
    }

    @Test
    void exposesOnlyHealthActuatorEndpoint() {
        assertEquals("health", exposedActuatorEndpoints);
    }

}
