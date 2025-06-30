package com.techishthoughts.stocks.infrastructure.logging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class LoggingFormatTest {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @Test
    void testDetailedLoggingFormat() throws Exception {
        // Create a sample RequestContext with detailed information
        RequestContext context = new RequestContext();
        context.setMethod("GET");
        context.setUri("/stocks");
        context.setStatusCode(200);
        context.setRequestBody(null);

        // Set up headers
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("X-Parent-Span-Id", "12345");
        headers.put("Authorization", "Bearer token");
        headers.put("User-Agent", "MyApp/1.0");
        context.setHeaders(headers);

        // Set up response body
        String responseBody = """
            {
                "stocks": [
                    {
                        "symbol": "AAPL",
                        "price": 150.12,
                        "volume": 1000000
                    },
                    {
                        "symbol": "GOOGL",
                        "price": 2800.50,
                        "volume": 500000
                    }
                ]
            }
            """;
        context.setResponseBody(responseBody);

        // Add a route step
        context.addRouteStep("GET", "/", 200, null, responseBody);

        // Convert to log format
        Map<String, Object> logFormat = context.toLogFormat();

        // Convert to JSON string
        String jsonOutput = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(logFormat);

        System.out.println("=== Expected Logging Format ===");
        System.out.println(jsonOutput);

        // Verify the structure
        assertNotNull(logFormat.get("method"));
        assertNotNull(logFormat.get("uri"));
        assertNotNull(logFormat.get("headers"));
        assertNotNull(logFormat.get("status"));
        assertNotNull(logFormat.get("responseBody"));
        assertNotNull(logFormat.get("routesSteps"));

        assertEquals("GET", logFormat.get("method"));
        assertEquals("/stocks", logFormat.get("uri"));
        assertEquals(200, logFormat.get("status"));

        // Verify headers
        @SuppressWarnings("unchecked")
        Map<String, String> logHeaders = (Map<String, String>) logFormat.get("headers");
        assertEquals("application/json", logHeaders.get("Content-Type"));
        assertEquals("12345", logHeaders.get("X-Parent-Span-Id"));
        assertEquals("Bearer token", logHeaders.get("Authorization"));
        assertEquals("MyApp/1.0", logHeaders.get("User-Agent"));
    }

    @Test
    void testExactFormatExample() {
        // Create the exact format as requested by the user
        Map<String, Object> expectedFormat = new HashMap<>();
        expectedFormat.put("method", "GET");
        expectedFormat.put("uri", "/stocks");

        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("X-Parent-Span-Id", "12345");
        headers.put("Authorization", "Bearer token");
        headers.put("User-Agent", "MyApp/1.0");
        expectedFormat.put("headers", headers);

        expectedFormat.put("requestBody", null);
        expectedFormat.put("status", 200);

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("stocks", java.util.List.of(
                Map.of("symbol", "AAPL", "price", 150.12, "volume", 1000000),
                Map.of("symbol", "GOOGL", "price", 2800.50, "volume", 500000)
        ));
        expectedFormat.put("responseBody", responseBody);

        // Route steps
        Map<String, Object> routeStep = new HashMap<>();
        routeStep.put("method", "GET");
        routeStep.put("uri", "/");
        routeStep.put("status", 200);
        routeStep.put("requestBody", null);
        routeStep.put("responseBody", responseBody);
        expectedFormat.put("routesSteps", java.util.List.of(routeStep));

        try {
            String jsonOutput = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(expectedFormat);
            System.out.println("=== User's Expected Format ===");
            System.out.println(jsonOutput);
        } catch (Exception e) {
            fail("Failed to serialize expected format: " + e.getMessage());
        }
    }
}
