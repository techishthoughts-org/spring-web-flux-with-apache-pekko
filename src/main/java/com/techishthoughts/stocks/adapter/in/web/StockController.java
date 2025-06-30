package com.techishthoughts.stocks.adapter.in.web;

import com.techishthoughts.stocks.application.StockUseCases;
import com.techishthoughts.stocks.config.TracingConfiguration;
import com.techishthoughts.stocks.domain.Stock;
import com.techishthoughts.stocks.infrastructure.logging.LoggingContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * REST API endpoints for stock operations.
 * Implements OpenTelemetry context propagation best practices.
 */
@RestController
@RequestMapping("/stocks")
public class StockController {

    private final StockUseCases service;

    public StockController(StockUseCases service) {
        this.service = service;
    }

    @GetMapping
    public Flux<Stock> all() {
        LoggingContext.logWithContext("Fetching all stocks");
        LoggingContext.addMetadata("operation", "getAllStocks");

        return service.getAllStocks()
                .doOnNext(stock -> {
                    LoggingContext.addMetadata("processedStock", stock.symbol());
                    LoggingContext.logWithContext("Processed stock: %s", stock.symbol());
                })
                .doOnComplete(() -> LoggingContext.logWithContext("Completed fetching all stocks"))
                .contextCapture(); // Capture current ThreadLocal values into Reactor Context
    }

    @GetMapping("/{symbol}")
    public Mono<Stock> one(@PathVariable String symbol) {
        String upperSymbol = symbol.toUpperCase();

        // Set correlation ID if available
        String correlationId = TracingConfiguration.getCorrelationId();
        if (correlationId != null) {
            LoggingContext.addMetadata("correlationId", correlationId);
        }

        LoggingContext.logWithContext("Fetching single stock for symbol: %s", upperSymbol);
        LoggingContext.addMetadata("operation", "getStock");
        LoggingContext.addMetadata("requestedSymbol", symbol);
        LoggingContext.addMetadata("normalizedSymbol", upperSymbol);

        return service.getStock(upperSymbol)
                .doOnNext(stock -> {
                    LoggingContext.addMetadata("stockFound", true);
                    LoggingContext.addMetadata("stockName", stock.name());
                    LoggingContext.logWithContext("Successfully retrieved stock: %s (%s)", upperSymbol, stock.name());
                })
                .doOnError(error -> {
                    LoggingContext.addMetadata("stockFound", false);
                    LoggingContext.addMetadata("errorType", error.getClass().getSimpleName());
                    LoggingContext.logWithContext("Failed to retrieve stock: %s, error: %s", upperSymbol, error.getMessage());
                })
                .contextCapture(); // Capture current ThreadLocal values into Reactor Context
    }
}
