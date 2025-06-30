package com.techishthoughts.stocks.config;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import com.techishthoughts.stocks.adapter.out.pekko.StockActorAdapter;

/**
 * Health indicator for monitoring the background initialization status of StockActorAdapter.
 */
@Component
public class StockActorHealthIndicator implements HealthIndicator {

    private final StockActorAdapter stockActorAdapter;

    public StockActorHealthIndicator(StockActorAdapter stockActorAdapter) {
        this.stockActorAdapter = stockActorAdapter;
    }

    @Override
    public Health health() {
        if (!stockActorAdapter.isInitializationStarted()) {
            return Health.down()
                    .withDetail("status", "Initialization not started")
                    .withDetail("progress", stockActorAdapter.getInitializationProgress())
                    .build();
        }

        if (stockActorAdapter.isInitializationCompleted()) {
            return Health.up()
                    .withDetail("status", "Initialization completed")
                    .withDetail("progress", stockActorAdapter.getInitializationProgress())
                    .withDetail("totalSymbols", stockActorAdapter.getTotalSymbolsToProcess())
                    .withDetail("processedSymbols", stockActorAdapter.getSymbolsProcessed())
                    .build();
        }

        // Initialization is in progress
        return Health.up()
                .withDetail("status", "Initialization in progress")
                .withDetail("progress", stockActorAdapter.getInitializationProgress())
                .withDetail("totalSymbols", stockActorAdapter.getTotalSymbolsToProcess())
                .withDetail("processedSymbols", stockActorAdapter.getSymbolsProcessed())
                .build();
    }
}
