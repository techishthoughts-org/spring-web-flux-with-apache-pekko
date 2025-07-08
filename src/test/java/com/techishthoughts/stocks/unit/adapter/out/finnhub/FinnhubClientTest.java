package com.techishthoughts.stocks.unit.adapter.out.finnhub;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.reactive.function.client.WebClient;

import com.techishthoughts.stocks.adapter.out.finnhub.FinnhubClient;
import com.techishthoughts.stocks.adapter.out.finnhub.dto.StockSymbols;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class FinnhubClientTest {

    @Mock
    private WebClient.Builder webClientBuilder;

    @Mock
    private WebClient webClient;

    @Mock
    @SuppressWarnings("rawtypes")
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    @SuppressWarnings("rawtypes")
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    private FinnhubClient finnhubClient;

    @BeforeEach
    void setUp() {
        when(webClientBuilder.baseUrl(anyString())).thenReturn(webClientBuilder);
        when(webClientBuilder.build()).thenReturn(webClient);

        finnhubClient = new FinnhubClient(webClientBuilder, "test-api-key");
    }

    @Test
    void shouldFetchStockSymbolsSuccessfully() {
        // Given
        List<StockSymbols> mockStockSymbols = List.of(
                new StockSymbols("USD", "Apple Inc", "AAPL", "BBG000B9Y5X2", "XNGS", "AAPL", "Common Stock"),
                new StockSymbols("USD", "Microsoft Corp", "MSFT", "BBG000BPH459", "XNGS", "MSFT", "Common Stock"),
                new StockSymbols("USD", "Alphabet Inc", "GOOGL", "BBG009S39JX6", "XNGS", "GOOGL", "Common Stock")
        );

        // Mock the WebClient chain
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(java.util.function.Function.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.header(anyString(), anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
                .thenReturn(Mono.just(mockStockSymbols));

        // When & Then
        StepVerifier.create(finnhubClient.getStockSymbols())
                .expectNextMatches(listings -> {
                    assertNotNull(listings);
                    assertEquals(3, listings.size());

                    assertEquals("AAPL", listings.get(0).symbol());
                    assertEquals("Common Stock", listings.get(0).type());

                    assertEquals("MSFT", listings.get(1).symbol());
                    assertEquals("Common Stock", listings.get(1).type());

                    assertEquals("GOOGL", listings.get(2).symbol());
                    assertEquals("Common Stock", listings.get(2).type());

                    return true;
                })
                .verifyComplete();
    }

    @Test
    void shouldReturnEmptyListWhenNoStockSymbolsFound() {
        // Given
        List<StockSymbols> emptyList = List.of();

        // Mock the WebClient chain
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(java.util.function.Function.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.header(anyString(), anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
                .thenReturn(Mono.just(emptyList));

        // When & Then
        StepVerifier.create(finnhubClient.getStockSymbols())
                .expectNextMatches(listings -> {
                    assertNotNull(listings);
                    assertEquals(0, listings.size());
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void shouldHandleErrorWhenFetchingStockSymbols() {
        // Given
        RuntimeException error = new RuntimeException("API Error");

        // Mock the WebClient chain
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(java.util.function.Function.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.header(anyString(), anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
                .thenReturn(Mono.error(error));

        // When & Then
        StepVerifier.create(finnhubClient.getStockSymbols())
                .expectError(RuntimeException.class)
                .verify();
    }
}
