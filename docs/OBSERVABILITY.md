# Observability & Monitoring

This document outlines the comprehensive observability setup for the Stocks
application, including distributed tracing, metrics, logs, and monitoring. The
implementation follows OpenTelemetry standards with **verified working
configuration** using service name `stocks-service`.

## 🎯 Architecture Overview

```text
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│  Stocks App     │───▶│ OTEL Collector   │───▶│    Jaeger       │
│  (Spring Boot)  │    │  (Processor)     │    │   (Storage)     │
│ stocks-service  │    │   Port 4318      │    │  Port 16686     │
└─────────────────┘    └──────────────────┘    └─────────────────┘
         │                        │                       │
         ▼                        ▼                       ▼
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   Prometheus    │    │   Structured     │    │   Grafana       │
│   (Metrics)     │    │   Logs (JSON)    │    │ (Dashboards)    │
│   Port 9090     │    │   via Loki       │    │   Port 3000     │
└─────────────────┘    └──────────────────┘    └─────────────────┘

```text

## ✅ Verified Configuration

### OpenTelemetry Setup (Working)

```yaml

# Environment Variables (Verified in Container)

OTEL_EXPORTER_OTLP_ENDPOINT: <http://otel-collector:4318>
OTEL_EXPORTER_OTLP_PROTOCOL: http/protobuf
OTEL_SERVICE_NAME: stocks-service
OTEL_RESOURCE_ATTRIBUTES:
service.name=stocks-service,service.version=1.0.0,deployment.environment=docker
OTEL_LOGS_EXPORTER: otlp
OTEL_METRICS_EXPORTER: otlp

```text

### Service Endpoints

| Service | Port | Purpose | Status |
|---------|------|---------|--------|
| Stocks App | 8080 | Main application | ✅ Running |
| Stocks App | 8081 | Actuator endpoints | ✅ Running |
| Jaeger UI | 16686 | Distributed tracing UI | ✅ Running |
| Prometheus | 9090 | Metrics collection | ✅ Running |
| Grafana | 3000 | Dashboards (admin/admin) | ✅ Running |
| OTEL Collector | 4318 | OTLP HTTP endpoint | ✅ Running |
| OTEL Collector | 8888 | Prometheus metrics | ✅ Running |
| Loki | 3100 | Log aggregation | ✅ Running |

## 🔧 Implementation Details

### Dependencies Added

```xml
<!-- OpenTelemetry Support -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-tracing-bridge-otel</artifactId>
</dependency>
<dependency>
    <groupId>io.opentelemetry</groupId>
    <artifactId>opentelemetry-exporter-otlp</artifactId>
</dependency>
<!-- Context Propagation Library for Reactor -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>context-propagation</artifactId>
</dependency>

```text

### Context Propagation Configuration

Following Spring Boot 3 best practices:

1. **Automatic Instrumentation**: Spring Boot auto-configuration handles
OpenTelemetry setup
1. **Reactor Context Integration**: Using `contextCapture()` to bridge
ThreadLocal values
1. **MDC Population**: Automatic population of Logback MDC with tracing context

### Tracing Fields

All logs include these OpenTelemetry-compliant fields:

- **`traceId`**: Unique identifier for the entire request trace
- **`spanId`**: Unique identifier for the current operation
- **`parentId`**: Identifier of the parent span (if applicable)
- **`correlationId`**: Custom correlation ID for business logic tracing
- **`requestId`**: Unique identifier for the HTTP request

## 🐳 Docker Configuration

### OpenTelemetry Collector (Verified Working)

```yaml
otel-collector:
  image: otel/opentelemetry-collector-contrib:latest
  ports:

    - "4317:4317"   # OTLP gRPC receiver
    - "4318:4318"   # OTLP HTTP receiver ✅ (Used by application)
    - "8888:8888"   # Prometheus metrics
    - "8889:8889"   # Health check endpoint
  volumes:

    - ./monitoring/otel-collector.yml:/etc/otelcol-contrib/otel-collector.yml

```text

### Jaeger (OpenTelemetry Compatible)

```yaml
jaeger:
  image: jaegertracing/all-in-one:latest
  ports:

    - "16686:16686"  # Jaeger UI ✅
    - "14250:14250"  # gRPC collector
  environment:

    - COLLECTOR_OTLP_ENABLED=true

```text

### OTEL Collector Configuration

```yaml

# monitoring/otel-collector.yml

receivers:
  otlp:
    protocols:
      grpc:
        endpoint: 0.0.0.0:4317
      http:
        endpoint: 0.0.0.0:4318  # ✅ Used by application

processors:
  batch:
    timeout: 1s
    send_batch_size: 1024
  resource:
    attributes:

      - key: deployment.environment
        value: "docker"
        action: insert

exporters:
  otlp/jaeger:
    endpoint: jaeger:4317
    tls:
      insecure: true
  prometheus:
    endpoint: "0.0.0.0:8888"
  debug:
    verbosity: basic

service:
  pipelines:
    traces:
      receivers: [otlp]
      processors: [batch, resource]
      exporters: [otlp/jaeger, debug]
    metrics:
      receivers: [otlp]
      processors: [batch, resource]
      exporters: [prometheus, debug]

```text

## 🔍 Usage Examples

### In Controllers

```java
@GetMapping("/{symbol}")
public Mono<Stock> one(@PathVariable String symbol) {
    return service.getStock(symbol)
        .doOnNext(stock -> {
            LoggingContext.addMetadata("operation", "getStock");
LoggingContext.logWithContext("Retrieved stock: %s", stock.symbol());
        })
        .contextCapture(); // Automatic context propagation
}

```text

### In Services

```java
public Mono<Stock> getStock(String symbol) {
    return webClient.get()
        .uri("/stock/{symbol}", symbol)
        .retrieve()
        .bodyToMono(Stock.class)
        .tap(() -> new DefaultSignalListener<>() {
            @Override
            public void doOnComplete() {
LoggingContext.logWithContext("Successfully fetched stock: %s", symbol);
            }
        });
}

```text

## 📋 Log Formats

### Console Logs

```text
2024-01-20 10:30:45.123 INFO [reactor-http-nio-2] [abc123,def456,ghi789]
c.t.s.StockController req-001 - Retrieved stock: AAPL

```text

### JSON Logs

```json
{
  "timestamp": "2024-01-20T10:30:45.123Z",
  "level": "INFO",
  "message": "Retrieved stock: AAPL",
  "tracing": {
    "traceId": "abc123",
    "spanId": "def456",
    "parentId": "ghi789",
    "correlationId": "corr-001"
  },
  "request": {
    "id": "req-001",
    "method": "GET",
    "uri": "/stocks/AAPL",
    "statusCode": "200",
    "durationMs": "150"
  },
  "application": {
    "name": "stocks-service",
    "environment": "docker"
  }
}

```text

## 🚀 Getting Started

### Start Complete Observability Stack

```bash

# Start all services

make stack-observability

# Check service status

make status

# View all URLs

make urls

```text

### Access Observability Tools

```bash

# Grafana (Dashboards)

open <http://localhost:3000>  # admin/admin

# Jaeger (Distributed Tracing)

open <http://localhost:16686>

# Prometheus (Metrics)

open <http://localhost:9090>

# Application Health

curl <http://localhost:8081/actuator/health>

```text

### Generate Test Data

```bash

# Make some requests to generate traces

curl <http://localhost:8080/stocks/AAPL>
curl <http://localhost:8080/stocks/GOOGL>
curl <http://localhost:8080/stocks/MSFT>

# View traces in Jaeger

open <http://localhost:16686>

```text

## 📊 Monitoring Capabilities

### Distributed Tracing

- **Service Name**: `stocks-service` ✅
- **End-to-end tracing**: HTTP requests → Actor system → External APIs
- **Span correlation**: Automatic trace context propagation
- **Performance analysis**: Identify bottlenecks and latency issues

### Metrics Collection

- **Application metrics**: Spring Boot Actuator + Micrometer
- **Business metrics**: Stock processing rates, API success rates
- **Infrastructure metrics**: Container and system metrics
- **Custom metrics**: Domain-specific measurements

### Log Aggregation

- **Structured logging**: JSON format with trace correlation
- **Centralized collection**: Loki aggregation
- **Context preservation**: Trace IDs in all log entries
- **Search and filtering**: Grafana log exploration

### Health Monitoring

- **Application health**: Spring Boot health indicators
- **Service dependencies**: External API health checks
- **Infrastructure health**: Container and system monitoring
- **Custom health checks**: Business logic validation

## 🔧 Configuration Management

### Environment Variables

```bash

# Application Configuration

SPRING_PROFILES_ACTIVE=docker
MANAGEMENT_SERVER_PORT=8081

# OpenTelemetry Configuration (Verified Working)

OTEL_EXPORTER_OTLP_ENDPOINT=<http://otel-collector:4318>
OTEL_EXPORTER_OTLP_PROTOCOL=http/protobuf
OTEL_SERVICE_NAME=stocks-service
OTEL_RESOURCE_ATTRIBUTES=service.name=stocks-service,service.version=1.0.0,deployment.environment=docker
OTEL_LOGS_EXPORTER=otlp
OTEL_METRICS_EXPORTER=otlp

# External API Configuration

FINNHUB_API_KEY=your-api-key-here

```text

### Grafana Datasources

Pre-configured datasources:

```yaml

# Prometheus

- name: Prometheus
  type: prometheus
  url: <http://prometheus:9090>
  access: proxy

# Loki

- name: Loki
  type: loki
  url: <http://loki:3100>
  access: proxy

# Jaeger

- name: Jaeger
  type: jaeger
  url: <http://jaeger:16686>
  access: proxy

```text

## 🛠️ Troubleshooting

### Common Issues

#### No Traces in Jaeger

```bash

# Check OTEL collector logs

podman-compose logs otel-collector

# Verify application configuration

podman exec stocks-service env | grep OTEL

# Check Jaeger connectivity

curl <http://localhost:16686/api/services>

```text

#### Missing Metrics in Prometheus

```bash

# Check Prometheus targets

open <http://localhost:9090/targets>

# Verify application metrics endpoint

curl <http://localhost:8081/actuator/prometheus>

```text

#### Grafana Dashboard Issues

```bash

# Check Grafana logs

podman-compose logs grafana

# Verify datasource connectivity


# Go to Grafana → Configuration → Data Sources → Test

```text

### Health Checks

```bash

# Application health

curl <http://localhost:8081/actuator/health>

# OTEL Collector health

curl <http://localhost:8889>

# Prometheus health

curl <http://localhost:9090/-/healthy>

# Jaeger health

curl <http://localhost:16686>

```text

## 📈 Best Practices

### Tracing

- **Meaningful span names**: Use descriptive operation names
- **Proper context propagation**: Ensure trace continuity across async operations
- **Sampling strategy**: Configure appropriate sampling rates for production
- **Error handling**: Capture exceptions and error details in spans

### Metrics

- **Golden signals**: Monitor latency, traffic, errors, and saturation
- **Business metrics**: Track domain-specific KPIs
- **Cardinality management**: Avoid high-cardinality labels
- **Alerting rules**: Set up meaningful alerts for critical metrics

### Logging

- **Structured format**: Use JSON for machine-readable logs
- **Correlation IDs**: Include trace context in all log entries
- **Appropriate levels**: Use correct log levels for different environments
- **Sensitive data**: Avoid logging sensitive information

### Performance

- **Async processing**: Use non-blocking operations where possible
- **Resource limits**: Set appropriate memory and CPU limits
- **Batch processing**: Configure batching for telemetry data
- **Monitoring overhead**: Keep observability overhead minimal

---

For infrastructure setup details, see [INFRASTRUCTURE.md](./INFRASTRUCTURE.md)
