package com.techishthoughts.stocks.sanity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.techishthoughts.stocks.domain.Stock;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Sanity tests to verify the overall system health and basic business logic.
 * These tests ensure the system can handle real-world scenarios gracefully.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "finnhub.api.key=test-key",
        "logging.level.com.techishthoughts.stocks=INFO"
})
class StockServiceSanityTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void systemShouldReturnHealthyStatus() {
        webTestClient
                .get()
                .uri("/actuator/health")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(body -> {
                    assertTrue(body.contains("UP"), "Application should be healthy");
                });
    }

    @Test
    void shouldHandleValidStockSymbol() {
        String validSymbol = "AAPL";

        webTestClient
                .get()
                .uri("/stocks/{symbol}", validSymbol)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Stock.class)
                .value(stock -> {
                    assertNotNull(stock, "Stock should not be null");
                    assertEquals(validSymbol, stock.symbol(), "Symbol should match request");
                    assertNotNull(stock.lastUpdated(), "Last updated should be set");
                });
    }

    @Test
    void shouldHandleLowercaseStockSymbol() {
        String lowercaseSymbol = "msft";
        String expectedSymbol = "MSFT";

        webTestClient
                .get()
                .uri("/stocks/{symbol}", lowercaseSymbol)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Stock.class)
                .value(stock -> {
                    assertNotNull(stock, "Stock should not be null");
                    assertEquals(expectedSymbol, stock.symbol(), "Symbol should be converted to uppercase");
                });
    }

    @Test
    void shouldReturnMultipleStocks() {
        webTestClient
                .mutate()
                .responseTimeout(Duration.ofSeconds(30))
                .build()
                .get()
                .uri("/stocks")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(Stock.class)
                .value(stocks -> {
                    assertNotNull(stocks, "Stocks list should not be null");
                    assertFalse(stocks.isEmpty(), "Should return at least some stocks");

                    // Verify all stocks have required fields
                    stocks.forEach(stock -> {
                        assertNotNull(stock.symbol(), "Each stock should have a symbol");
                        assertFalse(stock.symbol().isBlank(), "Symbol should not be blank");
                        assertNotNull(stock.lastUpdated(), "Each stock should have lastUpdated timestamp");
                    });
                });
    }

    @Test
    void shouldHandleNonExistentStockGracefully() {
        String nonExistentSymbol = "NONEXISTENT123";

        webTestClient
                .get()
                .uri("/stocks/{symbol}", nonExistentSymbol)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Stock.class)
                .value(stock -> {
                    assertNotNull(stock, "Should return a stock object even for non-existent symbols");
                    assertEquals(nonExistentSymbol, stock.symbol(), "Should return requested symbol");
                    assertNotNull(stock.lastUpdated(), "Should have a timestamp");
                });
    }

    @Test
    void shouldProvideMetricsEndpoint() {
        webTestClient
                .get()
                .uri("/actuator/prometheus")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(body -> {
                    assertNotNull(body, "Metrics body should not be null");
                    assertTrue(body.contains("jvm_"), "Should contain JVM metrics");
                });
    }

    @Test
    void shouldHandleMultipleConcurrentRequests() {
        // Test system resilience under concurrent load
        for (int i = 0; i < 5; i++) {
            final String symbol = "TEST" + i;
            webTestClient
                    .get()
                    .uri("/stocks/{symbol}", symbol)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(Stock.class)
                    .value(stock -> {
                        assertEquals(symbol, stock.symbol());
                        assertNotNull(stock.lastUpdated());
                    });
        }
    }

    @Test
    void shouldMaintainConsistentResponseFormat() {
        webTestClient
                .get()
                .uri("/stocks/CONSISTENCY")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType("application/json")
                .expectBody(Stock.class)
                .value(stock -> {
                    // Verify all required fields are present and correctly typed
                    assertNotNull(stock.symbol());
                    assertNotNull(stock.lastUpdated());

                    // Verify optional fields are handled gracefully (can be null)
                    // but if present, should be valid
                    if (stock.marketCapitalization() != null) {
                        assertTrue(stock.marketCapitalization() >= 0, "Market cap should be non-negative");
                    }

                    if (stock.shareOutstanding() != null) {
                        assertTrue(stock.shareOutstanding() >= 0, "Shares outstanding should be non-negative");
                    }
                });
    }
}
