package com.techishthoughts.stocks.infrastructure.logging;

import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import com.techishthoughts.stocks.config.TracingConfiguration;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import reactor.core.publisher.Mono;

/**
 * WebFilter that populates MDC with LoggingContext and Micrometer Tracing information.
 * This bridges the gap between our custom LoggingContext, Micrometer Tracing, and Logback's MDC.
 * Follows OpenTelemetry standards for trace context propagation.
 */
@Component
public class LoggingMDCInterceptor implements WebFilter, Ordered {

    private final Tracer tracer;

    public LoggingMDCInterceptor(Tracer tracer) {
        this.tracer = tracer;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }

    @Override
    @NonNull
    public Mono<Void> filter(@NonNull ServerWebExchange exchange, @NonNull WebFilterChain chain) {
        // Populate MDC before processing
        RequestContext requestContext = LoggingContext.getContext();
        if (requestContext != null) {
            populateMDC(requestContext);
        }
        populateTracingMDC();

        return chain.filter(exchange)
                .doFinally(signalType -> {
                    // Update MDC with final request information
                    RequestContext finalContext = LoggingContext.getContext();
                    if (finalContext != null) {
                        updateMDCWithFinalInfo(finalContext);
                    }
                    // Clean up MDC
                    MDC.clear();
                });
    }

    /**
     * Populate MDC with initial request context information
     */
    private void populateMDC(RequestContext context) {
        if (context.getRequestId() != null) {
            MDC.put("requestId", context.getRequestId());
        }
        if (context.getMethod() != null) {
            MDC.put("method", context.getMethod());
        }
        if (context.getUri() != null) {
            MDC.put("uri", context.getUri());
        }

        // Add metadata to MDC
        if (context.getMetadata() != null) {
            context.getMetadata().forEach((key, value) -> {
                if (value != null) {
                    MDC.put(key, value.toString());
                }
            });
        }

        // Add correlation ID from TracingConfiguration
        String correlationId = TracingConfiguration.getCorrelationId();
        if (correlationId != null) {
            MDC.put("correlationId", correlationId);
        }
    }

    /**
     * Populate MDC with tracing information from Micrometer Tracing
     */
    private void populateTracingMDC() {
        if (tracer != null) {
            Span currentSpan = tracer.currentSpan();
            if (currentSpan != null) {
                TraceContext traceContext = currentSpan.context();
                if (traceContext != null) {
                    MDC.put("traceId", traceContext.traceId());
                    MDC.put("spanId", traceContext.spanId());

                    // Add parent span ID if available
                    String parentId = traceContext.parentId();
                    if (parentId != null) {
                        MDC.put("parentId", parentId);
                    }
                }
            }
        }
    }

    /**
     * Update MDC with final request information
     */
    private void updateMDCWithFinalInfo(RequestContext context) {
        if (context.getStatusCode() != null) {
            MDC.put("statusCode", context.getStatusCode().toString());
        }
        if (context.getDurationMs() > 0) {
            MDC.put("durationMs", String.valueOf(context.getDurationMs()));
        }
        if (context.getRouteStepCount() > 0) {
            MDC.put("routeStepCount", String.valueOf(context.getRouteStepCount()));
        }
        if (context.getError() != null) {
            MDC.put("hasError", "true");
            MDC.put("errorMessage", context.getError());
        }
    }

    /**
     * Static method to update MDC during request processing
     */
    public static void updateMDC() {
        RequestContext context = LoggingContext.getContext();
        if (context != null) {
            // Update dynamic values
            if (context.getMetadata() != null) {
                context.getMetadata().forEach((key, value) -> {
                    if (value != null) {
                        MDC.put(key, value.toString());
                    }
                });
            }
        }
    }
}
