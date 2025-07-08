package com.techishthoughts.stocks.adapter.in.web;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.techishthoughts.stocks.application.StockUseCases;
import com.techishthoughts.stocks.domain.Stock;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
public class StockController {

    private final StockUseCases stockUseCases;

    public StockController(StockUseCases stockUseCases) {
        this.stockUseCases = stockUseCases;
    }

    @GetMapping("/stocks/{symbol}")
    public Mono<Stock> getStock(@PathVariable String symbol) {
        return stockUseCases.getStock(symbol);
    }

    @GetMapping("/stocks")
    public Flux<Stock> getAllStocks() {
        return stockUseCases.getAllStocks();
    }

    /**
     * Simple health check endpoint that bypasses Spring Boot health aggregation
     */
    @GetMapping("/health-simple")
    public ResponseEntity<Map<String, String>> healthSimple() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "timestamp", String.valueOf(System.currentTimeMillis()),
            "message", "Application is running"
        ));
    }
}
