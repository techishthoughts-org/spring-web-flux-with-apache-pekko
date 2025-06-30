package com.techishthoughts.stocks.unit.application;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.techishthoughts.stocks.adapter.out.pekko.StockActorAdapter;
import com.techishthoughts.stocks.application.StockUseCases;
import com.techishthoughts.stocks.domain.Stock;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class StockUseCasesTest {

    @Mock
    private StockActorAdapter stockActorAdapter;

    private StockUseCases stockUseCases;

    @BeforeEach
    void setUp() {
        stockUseCases = new StockUseCases(stockActorAdapter);
    }

    @Test
    void shouldGetStockBySymbol() {
        // Given
        String symbol = "AAPL";
        Stock expectedStock = createTestStock(symbol);
        when(stockActorAdapter.askStockActor(symbol)).thenReturn(Mono.just(expectedStock));

        // When
        Mono<Stock> result = stockUseCases.getStock(symbol);

        // Then
        StepVerifier.create(result)
                .expectNext(expectedStock)
                .verifyComplete();

        verify(stockActorAdapter).askStockActor(symbol);
    }

    @Test
    void shouldHandleStockNotFound() {
        // Given
        String symbol = "NOTFOUND";
        when(stockActorAdapter.askStockActor(symbol)).thenReturn(Mono.empty());

        // When
        Mono<Stock> result = stockUseCases.getStock(symbol);

        // Then
        StepVerifier.create(result)
                .verifyComplete();

        verify(stockActorAdapter).askStockActor(symbol);
    }

    @Test
    void shouldHandleErrorWhenGettingStock() {
        // Given
        String symbol = "ERROR";
        RuntimeException error = new RuntimeException("Database error");
        when(stockActorAdapter.askStockActor(symbol)).thenReturn(Mono.error(error));

        // When
        Mono<Stock> result = stockUseCases.getStock(symbol);

        // Then
        StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();

        verify(stockActorAdapter).askStockActor(symbol);
    }

    @Test
    void shouldGetAllStocks() {
        // Given
        Stock stock1 = createTestStock("AAPL");
        Stock stock2 = createTestStock("MSFT");
        Stock stock3 = createTestStock("GOOGL");

        when(stockActorAdapter.askAllStockActors())
                .thenReturn(Flux.just(stock1, stock2, stock3));

        // When
        Flux<Stock> result = stockUseCases.getAllStocks();

        // Then
        StepVerifier.create(result)
                .expectNext(stock1)
                .expectNext(stock2)
                .expectNext(stock3)
                .verifyComplete();

        verify(stockActorAdapter).askAllStockActors();
    }

    @Test
    void shouldHandleEmptyStocksList() {
        // Given
        when(stockActorAdapter.askAllStockActors()).thenReturn(Flux.empty());

        // When
        Flux<Stock> result = stockUseCases.getAllStocks();

        // Then
        StepVerifier.create(result)
                .verifyComplete();

        verify(stockActorAdapter).askAllStockActors();
    }

    @Test
    void shouldHandleErrorWhenGettingAllStocks() {
        // Given
        RuntimeException error = new RuntimeException("Actor system error");
        when(stockActorAdapter.askAllStockActors()).thenReturn(Flux.error(error));

        // When
        Flux<Stock> result = stockUseCases.getAllStocks();

        // Then
        StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();

        verify(stockActorAdapter).askAllStockActors();
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
