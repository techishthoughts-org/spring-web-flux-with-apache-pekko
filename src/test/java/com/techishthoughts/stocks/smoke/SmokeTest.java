package com.techishthoughts.stocks.smoke;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Smoke tests to verify basic application functionality.
 * These tests check if the application can start and basic endpoints are accessible.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "finnhub.api.key=test-key",
        "logging.level.com.techishthoughts.stocks=WARN"
})
class SmokeTest {

    private static final Logger log = LoggerFactory.getLogger(SmokeTest.class);

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void applicationContextLoads() {
        // This test verifies that the Spring context loads successfully
        // If this test passes, it means all beans are wired correctly
    }

    @Test
    void healthEndpointIsAccessible() {
        webTestClient
                .get()
                .uri("/actuator/health")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(body -> {
                    assert body.contains("UP") || body.contains("\"status\":\"UP\"");
                });
    }

    @Test
    void stocksEndpointIsAccessible() {
        webTestClient
                .mutate()
                .responseTimeout(Duration.ofSeconds(10))
                .build()
                .get()
                .uri("/stocks")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void singleStockEndpointIsAccessible() {
        webTestClient
                .get()
                .uri("/stocks/TEST")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void actuatorEndpointsAreExposed() {
        webTestClient
                .get()
                .uri("/actuator")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(body -> {
                    log.debug("Actuator response: {}", body);
                    // Just check that we get a response
                });
    }

    @Test
    void debugActuatorEndpoints() {
        webTestClient
                .get()
                .uri("/actuator")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(body -> {
                    log.debug("=== ACTUATOR ENDPOINTS DEBUG ===");
                    log.debug(body);
                    log.debug("=== END DEBUG ===");
                });
    }


    @Test
    void infoEndpointIsAccessible() {
        webTestClient
                .get()
                .uri("/actuator/info")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void metricsEndpointIsAccessible() {
        webTestClient
                .get()
                .uri("/actuator/metrics")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(body -> {
                    log.debug("=== METRICS ENDPOINT DEBUG ===");
                    log.debug(body);
                    log.debug("=== END METRICS DEBUG ===");
                });
    }
}
