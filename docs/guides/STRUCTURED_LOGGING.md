# Comprehensive Logging Guide

This document describes the complete logging system implementation including
structured logging with `InheritableThreadLocal`, enhanced HTTP request logging,
and observability integration.

## Overview

The logging system provides:

- **Request Context Management**: Track request metadata across thread boundaries
- **Route Step Tracking**: Monitor each HTTP call and internal operation
- **Enhanced HTTP Logging**: Detailed request/response logging in JSON format
- **Lifecycle Management**: Automatic context initialization and cleanup
- **Thread Safety**: Context inheritance across actor system threads
- **Security**: Automatic sanitization of sensitive information
- **Integration**: Works with OpenTelemetry, Grafana, and monitoring stack

## Architecture

### Core Components

1. **LoggingContext**: Static utility class managing the InheritableThreadLocal
context
1. **RequestContext**: Holds request lifecycle information and metadata
1. **RouteStep**: Represents individual operations (HTTP calls, actor calls,
database operations)
1. **LoggingWebFilter**: Manages context lifecycle and captures HTTP details
1. **LoggingWebClientInterceptor**: Tracks outbound HTTP calls
1. **LoggingMDCInterceptor**: MDC integration for structured logging

### Context Flow

```text
HTTP Request → LoggingWebFilter → Controller → Service → Actor → External API
     ↓              ↓               ↓          ↓        ↓          ↓
Initialize → Add metadata → Log business → Track actor → Track HTTP
Context      Enrich context   operations    calls       calls

```text

## Key Features

### 1. InheritableThreadLocal Context

The logging context uses `InheritableThreadLocal` to ensure request information
is available across all threads spawned during request processing, including
actor system threads.

```java
private static final InheritableThreadLocal<RequestContext> CONTEXT = new
InheritableThreadLocal<>();

```text

### 2. Enhanced HTTP Request Logging

Complete HTTP request/response information is captured in structured JSON
format:

```json
{
    "requestId": "550e8400-e29b-41d4-a716-446655440000",
    "method": "GET",
    "uri": "/stocks/AAPL",
    "headers": {
        "Content-Type": "application/json",
        "Authorization": "Bearer ***masked***",
        "User-Agent": "MyApp/1.0"
    },
    "statusCode": 200,
    "userAgent": "MyApp/1.0",
    "startTime": "2024-01-15 10:30:15.123",
    "endTime": "2024-01-15 10:30:15.456",
    "routeSteps": [
        {
            "stepName": "incoming-request",
            "stepType": "HTTP_IN",
            "method": "GET",
            "url": "<http://localhost:8080/stocks/AAPL",>
            "statusCode": 200,
            "startTime": "2024-01-15 10:30:15.123",
            "endTime": "2024-01-15 10:30:15.456",
            "durationMs": 333,
            "headers": {...},
            "metadata": {...}
        }
    ],
    "metadata": {
        "stockSymbol": "AAPL",
        "stockFound": true,
        "stockName": "Apple Inc."
    }
}

```text

### 3. Route Steps Tracking

Each significant operation is tracked as a route step:

- **HTTP_IN**: Incoming HTTP requests
- **HTTP_OUT**: Outbound HTTP calls to external services
- **ACTOR_CALL**: Actor system message processing
- **DATABASE**: Database operations

### 4. Security Features

- **Header Sanitization**: Authorization headers are automatically masked
- **Sensitive Data Protection**: PII and secrets are filtered from logs
- **Body Filtering**: Large payloads are truncated to prevent log pollution

### 5. Automatic Lifecycle Management

Context is automatically:

- Initialized on request start
- Populated with request metadata
- Cleared on request completion
- Cleaned up on errors

## Implementation Details

### LoggingWebFilter

The main filter handles complete HTTP request lifecycle:

```java
@Component
@Order(1)
public class LoggingWebFilter implements WebFilter {
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        return captureRequestResponse(exchange, chain)
            .doOnSuccess(v -> logRequestCompletion(exchange))
            .doOnError(throwable -> logRequestError(exchange, throwable))
            .doFinally(signalType -> LoggingContext.cleanup());
    }
}

```text

### WebClient Interceptor

For tracking outbound HTTP calls:

```java
@Component
public class LoggingWebClientInterceptor implements ClientHttpRequestInterceptor
{
    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body,
                                       ClientHttpRequestExecution execution) {
        RouteStep step = RouteStep.httpOut("external-api",
                                          request.getMethod().name(),
                                          request.getURI().toString());
        LoggingContext.addRouteStep(step);

        try {
            ClientHttpResponse response = execution.execute(request, body);
step.complete(response.getStatusCode().value(), "HTTP call completed");
            return response;
        } catch (Exception e) {
            step.fail("HTTP call failed: " + e.getMessage());
            throw e;
        }
    }
}

```text

## Usage Examples

### Basic Context Usage

```java
// Initialize context (done automatically by LoggingWebFilter)
LoggingContext.initialize("req-123", "GET", "/stocks/AAPL");

// Add metadata
LoggingContext.addMetadata("stockSymbol", "AAPL");
LoggingContext.addMetadata("userId", 12345);

// Log with context
LoggingContext.logWithContext("Processing stock request for symbol: %s",
"AAPL");

// Complete request (done automatically)
LoggingContext.complete(200, "{\"symbol\":\"AAPL\"}");

```text

### Controller Integration

```java
@GetMapping("/{symbol}")
public Mono<Stock> getStock(@PathVariable String symbol) {
    String upperSymbol = symbol.toUpperCase();

    LoggingContext.logWithContext("Fetching stock for symbol: %s", upperSymbol);
    LoggingContext.addMetadata("requestedSymbol", symbol);
    LoggingContext.addMetadata("normalizedSymbol", upperSymbol);

    return stockService.getStock(upperSymbol)
        .doOnNext(stock -> {
            LoggingContext.addMetadata("stockFound", true);
            LoggingContext.addMetadata("stockName", stock.name());
LoggingContext.logWithContext("Successfully retrieved stock: %s", stock.name());
        })
        .doOnError(error -> {
LoggingContext.addMetadata("errorType", error.getClass().getSimpleName());
        });
}

```text

### Actor Integration

```java
private Behavior<Command> onGetStock(GetStock command) {
    RouteStep actorStep = RouteStep.actorCall("stock-retrieval", "StockActor");
    LoggingContext.addRouteStep(actorStep);

    try {
        // Process stock request
        Stock stock = retrieveStock(command.symbol());
        actorStep.complete(200, "Stock retrieved: " + stock.symbol());
        command.replyTo().tell(new StockResponse(stock));
    } catch (Exception e) {
        actorStep.fail("Failed to retrieve stock: " + e.getMessage());
        command.replyTo().tell(new StockError(e.getMessage()));
    }

    return this;
}

```text

## Configuration

### Logback Configuration

The system uses specialized appenders for different log types:

```xml
<!-- Main application logs -->
<appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
<encoder class="net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder">
        <providers>
            <timestamp/>
            <logLevel/>
            <loggerName/>
            <mdc/>
            <message/>
        </providers>
    </encoder>
</appender>

<!-- Enhanced HTTP request logs -->
<appender name="HTTP_DETAILS"
class="ch.qos.logback.core.rolling.RollingFileAppender">
    <file>${LOG_PATH}/http-details.log</file>
    <filter class="ch.qos.logback.core.filter.EvaluatorFilter">
        <evaluator class="ch.qos.logback.classic.boolex.GEventEvaluator">
<expression>return message.contains("Request completed:");</expression>
        </evaluator>
        <OnMismatch>DENY</OnMismatch>
        <OnMatch>ACCEPT</OnMatch>
    </filter>
    <encoder class="ch.qos.logback.classic.encoder.PatternLayoutEncoder">
        <pattern>%replace(%msg){'Request completed: ', ''}%n</pattern>
    </encoder>
</appender>

```text

### Log File Structure

- **Application logs**: `logs/application.log` - General application logging
- **HTTP details**: `logs/http-details.log` - Detailed HTTP request/response logs
- **Error logs**: `logs/error.log` - Error-specific logs
- **Performance logs**: `logs/performance.log` - Performance metrics and traces

## Monitoring Integration

### OpenTelemetry Integration

The logging system integrates with OpenTelemetry for distributed tracing:

```java
// Trace ID and Span ID are automatically added to MDC
LoggingContext.addMetadata("traceId", traceId);
LoggingContext.addMetadata("spanId", spanId);

```text

### Grafana Dashboard Integration

Log data is automatically shipped to:

- **Loki**: For log aggregation and querying
- **Grafana**: For visualization and alerting
- **Prometheus**: For metrics derived from logs

### Query Examples

```bash

# View detailed HTTP logs

tail -f logs/http-details.log | jq '.'

# Filter specific endpoints

tail -f logs/http-details.log | jq 'select(.uri == "/stocks")'

# Monitor error rates

grep -c "ERROR" logs/application.log

# Track specific request

grep "req-123" logs/application.log

```text

## Testing

### Unit Tests

```java
@Test
void testRequestContextLogging() {
    LoggingContext.initialize("test-123", "GET", "/test");
    LoggingContext.addMetadata("testKey", "testValue");

    RequestContext context = LoggingContext.getCurrentContext();
    assertThat(context.getRequestId()).isEqualTo("test-123");
    assertThat(context.getMetadata()).containsEntry("testKey", "testValue");
}

```text

### Integration Tests

Run the logging format tests:

```bash
mvn test -Dtest=LoggingFormatTest
mvn test -Dtest=LoggingContextTest

```text

## Performance Considerations

1. **Async Logging**: All logging is asynchronous to prevent blocking
1. **Buffer Management**: Log buffers are sized appropriately
1. **Context Cleanup**: Automatic cleanup prevents memory leaks
1. **Filtering**: Log levels and filters prevent unnecessary overhead
1. **Rotation**: Log files are automatically rotated and compressed

## Troubleshooting

### Common Issues

1. **Missing Context**: Ensure LoggingWebFilter is properly configured
1. **Thread Context Loss**: Verify InheritableThreadLocal is working across
threads
1. **Memory Leaks**: Check that cleanup() is called in finally blocks
1. **Log File Size**: Monitor log rotation and cleanup policies

### Debug Configuration

```yaml
logging:
  level:
    com.techishthoughts.stocks.infrastructure.logging: DEBUG
    ROOT: INFO

```text

## File Locations

- **Implementation**: `src/main/java/com/techishthoughts/stocks/infrastructure/logging/`
- **Configuration**: `src/main/resources/logback-spring.xml`
- **Tests**: `src/test/java/com/techishthoughts/stocks/infrastructure/logging/`
- **Log Files**: `logs/` directory

---

This comprehensive logging system provides complete observability while
maintaining high performance and security standards.
