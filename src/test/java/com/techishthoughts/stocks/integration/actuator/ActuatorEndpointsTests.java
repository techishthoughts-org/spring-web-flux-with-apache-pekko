package com.techishthoughts.stocks.integration.actuator;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "finnhub.api.key=test-key",
        "logging.level.com.techishthoughts.stocks=WARN"
})
class ActuatorEndpointsTests {

    private static final Logger log = LoggerFactory.getLogger(ActuatorEndpointsTests.class);

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void healthEndpointShouldBeAccessible() {
        webTestClient.get()
                .uri("/actuator/health")
                .exchange()
                .expectStatus().isOk();
        log.debug("Health endpoint test passed");
    }

    @Test
    void infoEndpointShouldBeAccessible() {
        webTestClient.get()
                .uri("/actuator/info")
                .exchange()
                .expectStatus().isOk();
        log.debug("Info endpoint test passed");
    }

    @Test
    void metricsEndpointShouldBeAccessible() {
        webTestClient.get()
                .uri("/actuator/metrics")
                .exchange()
                .expectStatus().isOk();
        log.debug("Metrics endpoint test passed");
    }
}
