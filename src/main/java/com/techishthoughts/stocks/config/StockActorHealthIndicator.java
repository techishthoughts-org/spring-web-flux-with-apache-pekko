package com.techishthoughts.stocks.config;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import com.techishthoughts.stocks.adapter.out.pekko.StockActorAdapter;

/**
 * Health indicator for monitoring the background initialization status of StockActorAdapter.
 * This health check provides information about the stock data initialization but does NOT
 * block the application's overall health status since it's a background process.
 */
@Component
public class StockActorHealthIndicator implements HealthIndicator {

    private final StockActorAdapter stockActorAdapter;

    public StockActorHealthIndicator(StockActorAdapter stockActorAdapter) {
        this.stockActorAdapter = stockActorAdapter;
    }

    @Override
    public Health health() {
        // Always return UP - background initialization should not block application health
        Health.Builder healthBuilder = Health.up();

        try {
            // Provide informational details about the background initialization
            if (!stockActorAdapter.isInitializationStarted()) {
                healthBuilder
                        .withDetail("status", "Background initialization not started")
                        .withDetail("description", "Application is healthy, stock data initialization pending")
                        .withDetail("progress", stockActorAdapter.getInitializationProgress());
            } else if (stockActorAdapter.isInitializationCompleted()) {
                healthBuilder
                        .withDetail("status", "Background initialization completed")
                        .withDetail("description", "All stock data has been loaded successfully")
                        .withDetail("progress", stockActorAdapter.getInitializationProgress())
                        .withDetail("totalSymbols", stockActorAdapter.getTotalSymbolsToProcess())
                        .withDetail("processedSymbols", stockActorAdapter.getSymbolsProcessed());
            } else {
                // Initialization is in progress
                healthBuilder
                        .withDetail("status", "Background initialization in progress")
                        .withDetail("description", "Application is healthy, stock data loading in background")
                        .withDetail("progress", stockActorAdapter.getInitializationProgress())
                        .withDetail("totalSymbols", stockActorAdapter.getTotalSymbolsToProcess())
                        .withDetail("processedSymbols", stockActorAdapter.getSymbolsProcessed());
            }
        } catch (Exception e) {
            // Even if there's an error getting initialization status, the application is still healthy
            healthBuilder
                    .withDetail("status", "Error checking initialization status")
                    .withDetail("description", "Application is healthy, but cannot determine stock data initialization status")
                    .withDetail("error", e.getMessage());
        }

        return healthBuilder.build();
    }
}
