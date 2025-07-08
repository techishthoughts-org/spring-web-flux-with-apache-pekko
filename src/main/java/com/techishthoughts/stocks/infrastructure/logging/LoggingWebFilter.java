package com.techishthoughts.stocks.infrastructure.logging;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Enhanced WebFlux filter to capture comprehensive HTTP request/response information
 * for detailed logging including headers, bodies, and route steps
 */
@Component
@Order(1)
public class LoggingWebFilter implements WebFilter {

    private final ObjectMapper objectMapper;

    public LoggingWebFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    @NonNull
    public Mono<Void> filter(@NonNull ServerWebExchange exchange, @NonNull WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();

        // Initialize logging context
        String requestId = extractRequestId(request);
        LoggingContext.initialize(requestId, request.getMethod().name(), request.getURI().getPath());

        // Capture request headers
        Map<String, String> requestHeaders = new HashMap<>();
        request.getHeaders().forEach((headerName, headerValues) -> {
            if (!headerValues.isEmpty()) {
                requestHeaders.put(headerName, String.join(", ", headerValues));
            }
        });

        // Set headers in context
        RequestContext context = LoggingContext.getContext();
        if (context != null) {
            context.setHeaders(requestHeaders);
            context.setContentType(request.getHeaders().getFirst("Content-Type"));
            context.setUserAgent(request.getHeaders().getFirst("User-Agent"));
            context.setAuthorization(sanitizeAuthHeader(request.getHeaders().getFirst("Authorization")));
        }

        // Create enhanced request and response decorators to capture bodies
        ServerHttpRequestDecorator requestDecorator = new ServerHttpRequestDecorator(request) {
            @Override
            @NonNull
            public Flux<DataBuffer> getBody() {
                return super.getBody().doOnNext(dataBuffer -> {
                    // Capture request body
                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);
                    dataBuffer.readPosition(0); // Reset read position
                    String requestBody = new String(bytes, StandardCharsets.UTF_8);

                    RequestContext ctx = LoggingContext.getContext();
                    if (ctx != null) {
                        ctx.setRequestBody(requestBody);
                    }
                });
            }
        };

        ServerHttpResponseDecorator responseDecorator = new ServerHttpResponseDecorator(response) {
            @Override
            @NonNull
            public Mono<Void> writeWith(@NonNull org.reactivestreams.Publisher<? extends DataBuffer> body) {
                if (body instanceof Flux) {
                    @SuppressWarnings("unchecked")
                    Flux<DataBuffer> flux = (Flux<DataBuffer>) body;
                    return super.writeWith(flux.doOnNext(dataBuffer -> {
                        // Capture response body
                        byte[] bytes = new byte[dataBuffer.readableByteCount()];
                        dataBuffer.read(bytes);
                        dataBuffer.readPosition(0); // Reset read position
                        String responseBody = new String(bytes, StandardCharsets.UTF_8);

                        RequestContext ctx = LoggingContext.getContext();
                        if (ctx != null) {
                            ctx.setResponseBody(responseBody);
                        }
                    }));
                }
                return super.writeWith(body);
            }
        };

        // Create modified exchange with decorators
        ServerWebExchange decoratedExchange = exchange.mutate()
                .request(requestDecorator)
                .response(responseDecorator)
                .build();

        // Add initial route step for incoming request
        RouteStep inboundStep = RouteStep.httpIn(
                "incoming-request",
                request.getMethod().name(),
                request.getURI().toString()
        );
        inboundStep.setHeaders(requestHeaders);

        LoggingContext.addRouteStep(inboundStep);

        return chain.filter(decoratedExchange)
                .doOnSuccess(aVoid -> {
                    // Complete the inbound step
                    int statusCode = (Objects.nonNull(response.getStatusCode()) && Objects.nonNull(response.getStatusCode().value())) ? response.getStatusCode().value() : 200;

                    RequestContext ctx = LoggingContext.getContext();
                    if (ctx != null) {
                        // Add the main route step with complete information
                        ctx.addRouteStep(
                                request.getMethod().name(),
                                request.getURI().getPath(),
                                statusCode,
                                ctx.getRequestBody(),
                                ctx.getResponseBody()
                        );
                    }

                    inboundStep.complete(statusCode, ctx != null ? ctx.getResponseBody() : null);
                    LoggingContext.complete(statusCode, ctx != null ? ctx.getResponseBody() : null);

                    // Log the final comprehensive format
                    logDetailedRequest(ctx);
                })
                .doOnError(throwable -> {
                    // Handle error
                    inboundStep.fail(throwable.getMessage());
                    LoggingContext.fail(throwable);

                    RequestContext ctx = LoggingContext.getContext();
                    logDetailedRequest(ctx);
                })
                .doFinally(signalType -> {
                    // Always clear context to prevent memory leaks
                    LoggingContext.clear();
                });
    }

    /**
     * Extract request ID from headers or generate new one
     */
    private String extractRequestId(ServerHttpRequest request) {
        String requestId = request.getHeaders().getFirst("X-Request-ID");
        if (requestId == null || requestId.isEmpty()) {
            requestId = request.getHeaders().getFirst("X-Correlation-ID");
        }
        if (requestId == null || requestId.isEmpty()) {
            requestId = UUID.randomUUID().toString();
        }
        return requestId;
    }

    /**
     * Sanitize authorization header for logging (mask sensitive data)
     */
    private String sanitizeAuthHeader(String authHeader) {
        if (authHeader == null || authHeader.isEmpty()) {
            return null;
        }
        if (authHeader.toLowerCase().startsWith("bearer ")) {
            return "Bearer ***masked***";
        }
        if (authHeader.toLowerCase().startsWith("basic ")) {
            return "Basic ***masked***";
        }
        return "***masked***";
    }

    /**
     * Log the detailed request in the expected JSON format
     */
    private void logDetailedRequest(RequestContext context) {
        if (context != null) {
            try {
                String jsonLog = objectMapper.writeValueAsString(context.toLogFormat());
                // Log exactly as requested - just the JSON format
                LoggingContext.logWithContext("HTTP Request Details: %s", jsonLog);
            } catch (JsonProcessingException e) {
                LoggingContext.logWithContext("Failed to serialize request details: %s", e.getMessage());
            }
        }
    }
}
