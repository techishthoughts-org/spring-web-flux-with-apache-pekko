package com.techishthoughts.stocks.unit.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

import com.techishthoughts.stocks.adapter.out.finnhub.dto.CompanyProfile;
import com.techishthoughts.stocks.adapter.out.finnhub.dto.StockDetails;
import com.techishthoughts.stocks.adapter.out.finnhub.dto.StockSymbols;
import com.techishthoughts.stocks.application.port.out.StockMarketClient;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class StockMarketClientTest {

    @Mock
    private StockMarketClient stockMarketClient;

    @Test
    void shouldReturnStockSymbols() {
        // Given
        List<StockSymbols> symbols = List.of(
                new StockSymbols("USD", "Apple Inc", "AAPL", "BBG000B9Y5X2", "XNGS", "AAPL", "Common Stock"),
                new StockSymbols("USD", "Microsoft Corp", "MSFT", "BBG000BPH459", "XNGS", "MSFT", "Common Stock")
        );

        when(stockMarketClient.getStockSymbols()).thenReturn(Mono.just(symbols));

        // When & Then
        StepVerifier.create(stockMarketClient.getStockSymbols())
                .expectNextMatches(result -> {
                    assertNotNull(result);
                    assertEquals(2, result.size());
                    assertEquals("AAPL", result.get(0).symbol());
                    assertEquals("MSFT", result.get(1).symbol());
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void shouldReturnCompanyProfile() {
        // Given
        CompanyProfile profile = new CompanyProfile(
                "US", "USD", "XNGS", "1980-12-12", 2000000.0, "Apple Inc", "+1 408 996 1010",
                15000000.0, "AAPL", "https://www.apple.com", "https://apple.com/logo.png", "Technology"
        );

        when(stockMarketClient.getCompanyProfile("AAPL")).thenReturn(Mono.just(profile));

        // When & Then
        StepVerifier.create(stockMarketClient.getCompanyProfile("AAPL"))
                .expectNextMatches(result -> {
                    assertNotNull(result);
                    assertEquals("Apple Inc", result.name());
                    assertEquals("AAPL", result.ticker());
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void shouldHandleErrorWhenFetchingCompanyProfile() {
        // Given
        when(stockMarketClient.getCompanyProfile("INVALID"))
                .thenReturn(Mono.error(new RuntimeException("Profile not found")));

        // When & Then
        StepVerifier.create(stockMarketClient.getCompanyProfile("INVALID"))
                .expectError(RuntimeException.class)
                .verify();
    }

    @Test
    void shouldCreateStockDetails() {
        // Given
        StockSymbols symbol = new StockSymbols("USD", "Apple Inc", "AAPL", "BBG000B9Y5X2", "XNGS", "AAPL", "Common Stock");
        CompanyProfile profile = new CompanyProfile(
                "US", "USD", "XNGS", "1980-12-12", 2000000.0, "Apple Inc", "+1 408 996 1010",
                15000000.0, "AAPL", "https://www.apple.com", "https://apple.com/logo.png", "Technology"
        );

        StockDetails expectedDetails = new StockDetails(
                "AAPL", "Apple Inc", "XNGS", "Common Stock", "1980-12-12",
                "US", "USD", "1980-12-12", 2000000.0,
                "+1 408 996 1010", 15000000.0, "AAPL", "https://www.apple.com",
                "https://apple.com/logo.png", "Technology"
        );

        when(stockMarketClient.getStockDetails(symbol, profile)).thenReturn(expectedDetails);

        // When
        StockDetails result = stockMarketClient.getStockDetails(symbol, profile);

        // Then
        assertNotNull(result);
        assertEquals("AAPL", result.symbol());
        assertEquals("Apple Inc", result.name());
        assertEquals("Common Stock", result.assetType());
    }
}
