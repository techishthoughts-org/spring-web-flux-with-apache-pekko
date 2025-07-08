package com.techishthoughts.stocks.unit.adapter.in.web;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.techishthoughts.stocks.adapter.in.web.StockController;
import com.techishthoughts.stocks.application.StockUseCases;
import com.techishthoughts.stocks.domain.Stock;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class StockControllerTest {

    @Mock
    private StockUseCases stockService;

    private StockController stockController;

    @BeforeEach
    void setUp() {
        stockController = new StockController(stockService);
    }

    @Test
    void shouldReturnStockBySymbol() {
        // Given
        String symbol = "AAPL";
        Stock expectedStock = createTestStock(symbol);

        when(stockService.getStock(symbol)).thenReturn(Mono.just(expectedStock));

        // When
        Mono<Stock> result = stockController.getStock(symbol);

        // Then
        StepVerifier.create(result)
                .expectNext(expectedStock)
                .verifyComplete();

        verify(stockService).getStock(symbol);
    }

    @Test
    void shouldPassSymbolAsIs() {
        // Given
        String symbol = "msft";
        Stock expectedStock = createTestStock(symbol);

        when(stockService.getStock(symbol)).thenReturn(Mono.just(expectedStock));

        // When
        Mono<Stock> result = stockController.getStock(symbol);

        // Then
        StepVerifier.create(result)
                .expectNext(expectedStock)
                .verifyComplete();

        verify(stockService).getStock(symbol);
    }

    @Test
    void shouldHandleStockNotFound() {
        // Given
        String symbol = "NOTFOUND";
        when(stockService.getStock(symbol)).thenReturn(Mono.empty());

        // When
        Mono<Stock> result = stockController.getStock(symbol);

        // Then
        StepVerifier.create(result)
                .verifyComplete();

        verify(stockService).getStock(symbol);
    }

    @Test
    void shouldHandleServiceError() {
        // Given
        String symbol = "ERROR";
        RuntimeException error = new RuntimeException("Service error");
        when(stockService.getStock(symbol)).thenReturn(Mono.error(error));

        // When
        Mono<Stock> result = stockController.getStock(symbol);

        // Then
        StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();

        verify(stockService).getStock(symbol);
    }

    @Test
    void shouldReturnAllStocks() {
        // Given
        Stock stock1 = createTestStock("AAPL");
        Stock stock2 = createTestStock("MSFT");
        Stock stock3 = createTestStock("GOOGL");

        when(stockService.getAllStocks()).thenReturn(Flux.just(stock1, stock2, stock3));

        // When
        Flux<Stock> result = stockController.getAllStocks();

        // Then
        StepVerifier.create(result)
                .expectNext(stock1)
                .expectNext(stock2)
                .expectNext(stock3)
                .verifyComplete();

        verify(stockService).getAllStocks();
    }

    @Test
    void shouldHandleEmptyStocksList() {
        // Given
        when(stockService.getAllStocks()).thenReturn(Flux.empty());

        // When
        Flux<Stock> result = stockController.getAllStocks();

        // Then
        StepVerifier.create(result)
                .verifyComplete();

        verify(stockService).getAllStocks();
    }

    @Test
    void shouldHandleServiceErrorWhenGettingAllStocks() {
        // Given
        RuntimeException error = new RuntimeException("Service error");
        when(stockService.getAllStocks()).thenReturn(Flux.error(error));

        // When
        Flux<Stock> result = stockController.getAllStocks();

        // Then
        StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();

        verify(stockService).getAllStocks();
    }

    private Stock createTestStock(String symbol) {
        return new Stock(
                symbol,
                symbol + " Inc.",
                "NASDAQ",
                "Common Stock",
                "2000-01-01",
                "US",
                "USD",
                "2000-01-01",
                1000000.0,
                "+1-555-0123",
                1000000.0,
                symbol,
                "https://example.com",
                "https://example.com/logo.png",
                "Technology",
                Instant.now()
        );
    }
}
