package com.techishthoughts.stocks.infrastructure.logging;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.lang.NonNull;

/**
 * HTTP client interceptor for logging outbound requests and responses.
 * This interceptor captures detailed information about HTTP calls made
 * by the application and integrates with the logging context system.
 */
public final class LoggingWebClientInterceptor implements ClientHttpRequestInterceptor {

    /**
     * Intercepts HTTP requests to add logging capabilities.
     *
     * @param request the HTTP request
     * @param body the request body
     * @param execution the request execution chain
     * @return the HTTP response
     * @throws IOException if an I/O error occurs
     */
    @Override
    @NonNull
    public ClientHttpResponse intercept(@NonNull final HttpRequest request,
            @NonNull final byte[] body,
            @NonNull final ClientHttpRequestExecution execution)
            throws IOException {

        final LocalDateTime startTime = LocalDateTime.now();
        final String requestBody = new String(body, StandardCharsets.UTF_8);

        // Create route step for this HTTP call
        final RouteStep step = RouteStep.forHttpRequest(
                "HTTP_OUT_" + request.getMethod(),
                request.getMethod().name(),
                request.getURI().toString()
        );
        step.setRequestBody(requestBody);
        step.setStartTime(startTime);

        try {
            // Execute the request
            final ClientHttpResponse response = execution.execute(request, body);

            // Log successful response
            step.completeSuccess(
                    response.getStatusCode() != null ? response.getStatusCode().value() : 200,
                    "Response received"
            );

            // Add step to current logging context if available
            final RequestContext context = LoggingContext.getContext();
            if (context != null) {
                context.getRouteSteps().add(step);
            }

            return response;

        } catch (final IOException e) {
            // Log failed request
            step.completeError("HTTP request failed: " + e.getMessage());

            // Add step to current logging context if available
            final RequestContext context = LoggingContext.getContext();
            if (context != null) {
                context.getRouteSteps().add(step);
            }

            throw e;
        }
    }
}
