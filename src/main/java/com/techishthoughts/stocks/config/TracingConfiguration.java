package com.techishthoughts.stocks.config;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

import io.micrometer.context.ContextRegistry;
import io.micrometer.observation.ObservationRegistry;

/**
 * Configuration for distributed tracing with OpenTelemetry and context propagation.
 * Based on: https://spring.io/blog/2023/03/30/context-propagation-with-project-reactor-3-unified-bridging-between-reactive
 */
@Configuration
public class TracingConfiguration {

    private static final ThreadLocal<String> CORRELATION_ID = new ThreadLocal<>();

    /**
     * Initialize context propagation after application is ready.
     * This registers ThreadLocal accessors for automatic context propagation between
     * reactive and imperative code as described in the Spring blog post.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void configureContextPropagation() {
        // Register correlation ID for context propagation
        ContextRegistry.getInstance()
                .registerThreadLocalAccessor(
                        "CORRELATION_ID",
                        CORRELATION_ID::get,
                        CORRELATION_ID::set,
                        CORRELATION_ID::remove
                );
    }

    /**
     * Get the current correlation ID from ThreadLocal
     */
    public static String getCorrelationId() {
        return CORRELATION_ID.get();
    }

    /**
     * Set the correlation ID in ThreadLocal
     */
    public static void setCorrelationId(String correlationId) {
        CORRELATION_ID.set(correlationId);
    }

    /**
     * Remove the correlation ID from ThreadLocal
     */
    public static void removeCorrelationId() {
        CORRELATION_ID.remove();
    }

    /**
     * Bean configuration for observation registry to ensure it's available
     */
    @Bean
    public ObservationRegistry observationRegistry() {
        return ObservationRegistry.create();
    }
}
