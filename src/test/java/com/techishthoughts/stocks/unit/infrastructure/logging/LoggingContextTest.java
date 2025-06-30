package com.techishthoughts.stocks.unit.infrastructure.logging;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.techishthoughts.stocks.infrastructure.logging.LoggingContext;
import com.techishthoughts.stocks.infrastructure.logging.RequestContext;
import com.techishthoughts.stocks.infrastructure.logging.RouteStep;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for structured logging context functionality
 */
class LoggingContextTest {

    @BeforeEach
    void setUp() {
        // Ensure clean state before each test
        LoggingContext.clear();
    }

    @AfterEach
    void tearDown() {
        // Clean up after each test to prevent context leakage
        LoggingContext.clear();
    }

    @Test
    void shouldInitializeLoggingContext() {
        // Given
        String requestId = "test-request-123";
        String method = "GET";
        String uri = "/stocks/AAPL";

        // When
        LoggingContext.initialize(requestId, method, uri);

        // Then
        RequestContext context = LoggingContext.getContext();
        assertNotNull(context, "Context should be initialized");
        assertEquals(requestId, context.getRequestId(), "Request ID should match");
        assertEquals(method, context.getMethod(), "Method should match");
        assertEquals(uri, context.getUri(), "URI should match");
        assertNotNull(context.getStartTime(), "Start time should be set");
        assertNotNull(context.getRouteSteps(), "Route steps should be initialized");
        assertNotNull(context.getMetadata(), "Metadata should be initialized");
    }

    @Test
    void shouldGenerateRequestIdWhenNotProvided() {
        // When
        LoggingContext.initialize(null, "POST", "/stocks");

        // Then
        String requestId = LoggingContext.getRequestId();
        assertNotNull(requestId, "Request ID should be generated");
        assertFalse(requestId.isEmpty(), "Request ID should not be empty");
        assertNotEquals("unknown", requestId, "Request ID should not be 'unknown'");
    }

    @Test
    void shouldAddAndRetrieveMetadata() {
        // Given
        LoggingContext.initialize("test-123", "GET", "/test");

        // When
        LoggingContext.addMetadata("stockSymbol", "AAPL");
        LoggingContext.addMetadata("userId", 12345);
        LoggingContext.addMetadata("feature", "stock-lookup");

        // Then
        assertEquals("AAPL", LoggingContext.getMetadata("stockSymbol"));
        assertEquals(12345, LoggingContext.getMetadata("userId"));
        assertEquals("stock-lookup", LoggingContext.getMetadata("feature"));
    }

    @Test
    void shouldAddRouteSteps() {
        // Given
        LoggingContext.initialize("test-123", "GET", "/stocks/AAPL");

        // When
        RouteStep httpStep = RouteStep.httpOut("finnhub-api", "GET", "https://finnhub.io/api/v1/stock/profile2");
        RouteStep actorStep = RouteStep.actorCall("stock-actor", "StockActor");
        RouteStep dbStep = RouteStep.database("stock-save", "INSERT");

        LoggingContext.addRouteStep(httpStep);
        LoggingContext.addRouteStep(actorStep);
        LoggingContext.addRouteStep(dbStep);

        // Then
        RequestContext context = LoggingContext.getContext();
        assertEquals(3, context.getRouteStepCount(), "Should have 3 route steps");
        assertEquals("finnhub-api", context.getRouteSteps().get(0).getStepName());
        assertEquals("stock-actor", context.getRouteSteps().get(1).getStepName());
        assertEquals("stock-save", context.getRouteSteps().get(2).getStepName());
    }

    @Test
    void shouldCompleteRequestSuccessfully() {
        // Given
        LoggingContext.initialize("test-123", "GET", "/stocks/AAPL");
        LoggingContext.addMetadata("stockSymbol", "AAPL");

        // When
        LoggingContext.complete(200, "{\"symbol\":\"AAPL\",\"name\":\"Apple Inc.\"}");

        // Then
        RequestContext context = LoggingContext.getContext();
        assertEquals(200, context.getStatusCode());
        assertEquals("{\"symbol\":\"AAPL\",\"name\":\"Apple Inc.\"}", context.getResponseBody());
        assertNotNull(context.getEndTime(), "End time should be set");
        assertTrue(context.isSuccessful(), "Request should be marked as successful");
        assertTrue(context.getDurationMs() >= 0, "Duration should be calculated");
    }

    @Test
    void shouldHandleRequestFailure() {
        // Given
        LoggingContext.initialize("test-123", "GET", "/stocks/INVALID");
        Exception error = new RuntimeException("Stock not found");

        // When
        LoggingContext.fail(error);

        // Then
        RequestContext context = LoggingContext.getContext();
        assertEquals(500, context.getStatusCode());
        assertEquals("Stock not found", context.getError());
        assertNotNull(context.getEndTime(), "End time should be set");
        assertFalse(context.isSuccessful(), "Request should be marked as failed");
    }

    @Test
    void shouldGetContextAsJson() {
        // Given
        LoggingContext.initialize("test-123", "GET", "/stocks/AAPL");
        LoggingContext.addMetadata("stockSymbol", "AAPL");

        RouteStep step = RouteStep.httpOut("test-api", "GET", "https://api.example.com");
        step.complete(200, "Success");
        LoggingContext.addRouteStep(step);

        // When
        String json = LoggingContext.getContextAsJson();

        // Then
        assertNotNull(json, "JSON should not be null");
        assertFalse(json.isEmpty(), "JSON should not be empty");
        assertTrue(json.contains("test-123"), "JSON should contain request ID");
        assertTrue(json.contains("AAPL"), "JSON should contain stock symbol");
        assertTrue(json.contains("test-api"), "JSON should contain route step");
    }

    @Test
    void shouldReturnUnknownWhenNoContext() {
        // When/Then
        assertEquals("unknown", LoggingContext.getRequestId());
        assertNull(LoggingContext.getContext());
        assertNull(LoggingContext.getMetadata("any-key"));
    }

    @Test
    void shouldClearContext() {
        // Given
        LoggingContext.initialize("test-123", "GET", "/test");
        LoggingContext.addMetadata("key", "value");
        assertNotNull(LoggingContext.getContext(), "Context should exist");

        // When
        LoggingContext.clear();

        // Then
        assertNull(LoggingContext.getContext(), "Context should be cleared");
        assertEquals("unknown", LoggingContext.getRequestId(), "Request ID should be unknown");
    }

    @Test
    void shouldCreateRouteStepsWithDifferentTypes() {
        // When
        RouteStep httpOut = RouteStep.httpOut("external-api", "POST", "https://api.example.com");
        RouteStep httpIn = RouteStep.httpIn("incoming-request", "GET", "/stocks/AAPL");
        RouteStep actorCall = RouteStep.actorCall("stock-processing", "StockActor");
        RouteStep database = RouteStep.database("stock-query", "SELECT");

        // Then
        assertEquals("HTTP_OUT", httpOut.getStepType());
        assertEquals("HTTP_IN", httpIn.getStepType());
        assertEquals("ACTOR_CALL", actorCall.getStepType());
        assertEquals("DATABASE_OPERATION", database.getStepType());

        assertNotNull(httpOut.getStartTime(), "Start time should be set");
        assertNotNull(httpIn.getStartTime(), "Start time should be set");
        assertNotNull(actorCall.getStartTime(), "Start time should be set");
        assertNotNull(database.getStartTime(), "Start time should be set");
    }

    @Test
    void shouldCalculateRouteStepDuration() throws InterruptedException {
        // Given
        RouteStep step = RouteStep.httpOut("test-step", "GET", "https://example.com");

        // When
        Thread.sleep(10); // Small delay to ensure measurable duration
        step.complete(200, "Success");

        // Then
        assertTrue(step.getDurationMs() > 0, "Duration should be greater than 0");
        assertTrue(step.isSuccessful(), "Step should be successful");
    }

    @Test
    void shouldLogWithContext() {
        // Given
        LoggingContext.initialize("test-123", "GET", "/stocks/AAPL");

        // When/Then - This mainly tests that no exceptions are thrown
        assertDoesNotThrow(() -> {
            LoggingContext.logWithContext("Processing stock symbol: %s", "AAPL");
            LoggingContext.logWithContext("Simple message without parameters");
        });
    }
}
