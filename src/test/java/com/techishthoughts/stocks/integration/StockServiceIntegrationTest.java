package com.techishthoughts.stocks.integration;

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
 * Integration tests that verify the complete flow from HTTP request to response
 * using the actual Spring context and components.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "finnhub.api.key=test-key",
        "logging.level.com.techishthoughts.stocks=DEBUG"
})
class StockServiceIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void shouldGetSingleStockSuccessfully() {
        // Given
        String symbol = "AAPL";

        // When & Then
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

    @Test
    void shouldGetSingleStockWithLowercaseSymbol() {
        // Given
        String lowercaseSymbol = "msft";
        String expectedSymbol = "MSFT";

        // When & Then
        webTestClient
                .get()
                .uri("/stocks/{symbol}", lowercaseSymbol)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Stock.class)
                .value(stock -> {
                    assertEquals(expectedSymbol, stock.symbol());
                    assertNotNull(stock.lastUpdated());
                });
    }

    @Test
    void shouldGetAllStocksSuccessfully() {
        // When & Then
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
                    // We expect some stocks to be loaded
                    assertFalse(stocks.isEmpty());
                    // Verify all stocks have required fields
                    stocks.forEach(stock -> {
                        assertNotNull(stock.symbol());
                        assertFalse(stock.symbol().isBlank());
                        assertNotNull(stock.lastUpdated());
                    });
                });
    }

    @Test
    void shouldHandleNonExistentStock() {
        // Given
        String nonExistentSymbol = "NONEXISTENT123";

        // When & Then
        webTestClient
                .get()
                .uri("/stocks/{symbol}", nonExistentSymbol)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Stock.class)
                .value(stock -> {
                    // Should return a stock with the requested symbol and current timestamp
                    assertEquals(nonExistentSymbol, stock.symbol());
                    assertNotNull(stock.lastUpdated());
                });
    }

    @Test
    void shouldHandleHealthCheck() {
        webTestClient
                .get()
                .uri("/actuator/health")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(body -> assertTrue(body.contains("UP")));
    }

    @Test
    void shouldReturnJsonContentType() {
        webTestClient
                .get()
                .uri("/stocks/AAPL")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType("application/json");
    }

    @Test
    void shouldHandleInvalidSymbolCharacters() {
        // Given - symbol with invalid characters
        String invalidSymbol = "INVALID@SYMBOL";

        // When & Then - should still work but normalize the symbol
        webTestClient
                .get()
                .uri("/stocks/{symbol}", invalidSymbol)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Stock.class)
                .value(stock -> {
                    assertEquals(invalidSymbol, stock.symbol());
                    assertNotNull(stock.lastUpdated());
                });
    }
}
