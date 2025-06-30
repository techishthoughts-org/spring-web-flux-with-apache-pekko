package com.techishthoughts.stocks.infrastructure.logging;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Logging context using InheritableThreadLocal to maintain request lifecycle information
 * across thread boundaries, including actor system interactions.
 */
public class LoggingContext {

    private static final Logger log = LoggerFactory.getLogger(LoggingContext.class);
    private static final InheritableThreadLocal<RequestContext> CONTEXT = new InheritableThreadLocal<>();
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    /**
     * Initialize logging context for a new request
     */
    public static void initialize(String requestId, String method, String uri) {
        RequestContext context = RequestContext.builder()
                .requestId(requestId != null ? requestId : UUID.randomUUID().toString())
                .method(method)
                .uri(uri)
                .startTime(LocalDateTime.now())
                .routeSteps(new ArrayList<>())
                .metadata(new ConcurrentHashMap<>())
                .build();

        CONTEXT.set(context);
        log.info("Initialized logging context: requestId={}, method={}, uri={}",
                context.getRequestId(), method, uri);
    }

    /**
     * Get current request context
     */
    public static RequestContext getContext() {
        return CONTEXT.get();
    }

    /**
     * Get current request ID
     */
    public static String getRequestId() {
        RequestContext context = CONTEXT.get();
        return context != null ? context.getRequestId() : "unknown";
    }

    /**
     * Add a route step to track HTTP calls
     */
    public static void addRouteStep(RouteStep routeStep) {
        RequestContext context = CONTEXT.get();
        if (context != null) {
            context.getRouteSteps().add(routeStep);
            log.debug("Added route step: requestId={}, step={}",
                    context.getRequestId(), routeStep.getStepName());
        }
    }

    /**
     * Add metadata to the current context
     */
    public static void addMetadata(String key, Object value) {
        RequestContext context = CONTEXT.get();
        if (context != null) {
            context.getMetadata().put(key, value);
            // Update MDC immediately for structured logging
            LoggingMDCInterceptor.updateMDC();
        }
    }

    /**
     * Get metadata from current context
     */
    public static Object getMetadata(String key) {
        RequestContext context = CONTEXT.get();
        return context != null ? context.getMetadata().get(key) : null;
    }

    /**
     * Mark request as completed and log summary
     */
    public static void complete(int statusCode, String responseBody) {
        RequestContext context = CONTEXT.get();
        if (context != null) {
            context.setEndTime(LocalDateTime.now());
            context.setStatusCode(statusCode);
            context.setResponseBody(responseBody);

            logRequestSummary(context);
        }
    }

    /**
     * Mark request as failed and log error
     */
    public static void fail(Throwable throwable) {
        RequestContext context = CONTEXT.get();
        if (context != null) {
            context.setEndTime(LocalDateTime.now());
            context.setError(throwable.getMessage());
            context.setStatusCode(500);

            logRequestSummary(context);
            log.error("Request failed: requestId={}, error={}",
                    context.getRequestId(), throwable.getMessage(), throwable);
        }
    }

    /**
     * Clear the current context
     */
    public static void clear() {
        RequestContext context = CONTEXT.get();
        if (context != null) {
            log.debug("Clearing logging context: requestId={}", context.getRequestId());
        }
        CONTEXT.remove();
    }

    /**
     * Create a structured log entry with current context
     */
    public static void logWithContext(String message, Object... params) {
        RequestContext context = CONTEXT.get();
        if (context != null) {
            log.info("[{}] {}", context.getRequestId(), String.format(message, params));
        } else {
            log.info(message, params);
        }
    }

    /**
     * Log request summary with all route steps
     */
    private static void logRequestSummary(RequestContext context) {
        try {
            String contextJson = OBJECT_MAPPER.writeValueAsString(context);
            log.info("Request completed: {}", contextJson);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize request context: requestId={}, error={}",
                    context.getRequestId(), e.getMessage());
            log.info("Request completed: requestId={}, method={}, uri={}, statusCode={}, routeSteps={}",
                    context.getRequestId(), context.getMethod(), context.getUri(),
                    context.getStatusCode(), context.getRouteSteps().size());
        }
    }

    /**
     * Get current context as JSON string for debugging
     */
    public static String getContextAsJson() {
        RequestContext context = CONTEXT.get();
        if (context == null) {
            return "{}";
        }

        try {
            return OBJECT_MAPPER.writeValueAsString(context);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }
}
