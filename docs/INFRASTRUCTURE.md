# Infrastructure & Observability Stack

This document describes the complete infrastructure setup for the Spring WebFlux

+ Apache Pekko Stocks application, including the comprehensive observability
stack with Grafana, Prometheus, Loki, Jaeger, and supporting services.

## 🏗️ Architecture Overview

The infrastructure consists of several layers:

### Application Layer

+ **stocks-service**: Main Spring Boot application with Pekko actors
+ **External API**: Consumes real-time stock data from Finnhub API
+ **In-Memory Processing**: All data processing happens in-memory using Pekko actors

### Observability Stack

+ **prometheus**: Metrics collection and storage
+ **loki**: Log aggregation and storage
+ **jaeger**: Distributed tracing
+ **otel-collector**: OpenTelemetry collector for telemetry data processing
+ **grafana**: Unified dashboards and visualization

### System Monitoring

+ **node-exporter**: Host system metrics
+ **cadvisor**: Container metrics

### Development Tools

+ **swagger-ui**: External Swagger UI for API documentation
+ **mailhog**: Email testing service

### Testing Services

+ **wiremock**: API mocking for integration tests (optional)

## 🚀 Quick Start

### Full Observability Stack (Recommended)

```bash
make stack-observability

```text

### Individual Services

```bash
make docker-build       # Build application image
make docker-run         # Run application only

```text

### Environment Check

```bash
make env-check          # Check prerequisites (Docker/Podman)
make urls               # Show all service URLs

```text

## 🐳 Container Runtime Support

The infrastructure supports both **Docker** and **Podman** with automatic
detection:

### Automatic Detection

```bash

# The Makefile automatically detects your container runtime

make env-check                    # Shows detected runtime
make stack-observability         # Works with Docker or Podman

```text

### Manual Override (if needed)

```bash
CONTAINER_RUNTIME=podman make stack-observability
COMPOSE_CMD=podman-compose make stack-observability

```text

### Supported Runtimes

- **✅ Docker** with docker-compose
- **✅ Podman** with podman-compose (fully tested)

## 📋 Available Commands

### Infrastructure Management

```bash
make stack-observability # Start complete observability stack
make docker-build        # Build Docker image
make docker-run          # Run application container
make docker-stop         # Stop all services
make docker-clean        # Clean Docker resources
make docker-logs         # Show application logs

```text

### Environment & Health

```bash
make env-check           # Check environment setup
make urls                # Show all service URLs
make health-check        # Check application health
make status              # Show service status

```text

### Development

```bash
make build               # Build the application
make run                 # Run application locally
make clean               # Clean build artifacts

```text

## 🌐 Service URLs

### Application

- **Main App**: <http://localhost:8080>
- **Health Check**: <http://localhost:8081/actuator/health>
- **Metrics**: <http://localhost:8081/actuator/metrics>
- **Prometheus Metrics**: <http://localhost:8081/actuator/prometheus>

### Observability Stack

- **Grafana**: <http://localhost:3000> (admin/admin)
- **Prometheus**: <http://localhost:9090>
- **Jaeger**: <http://localhost:16686>
- **Loki**: <http://localhost:3100>

### System Monitoring

- **cAdvisor**: <http://localhost:8082>
- **Node Exporter**: <http://localhost:9100>
- **OTEL Collector**: <http://localhost:8888> (metrics), <http://localhost:8889> (health)

### Testing Services (Optional)

- **WireMock**: <http://localhost:8089>

## 📊 OpenTelemetry Configuration

### ✅ Verified Working Configuration

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

### OTEL Collector Pipeline

```yaml

# Receivers

- OTLP gRPC: port 4317
- OTLP HTTP: port 4318 ✅ (Used by application)

# Processors

- Batch processing
- Resource attribute enhancement
- Transform for correlation IDs

# Exporters

- Jaeger (OTLP): traces
- Prometheus: metrics
- Debug: development logging

```text

## 🔧 Performance Optimizations

### JVM Configuration

- **ZGC Garbage Collector**: Ultra-low latency
- **Memory Management**: 80% MaxRAMPercentage
- **Hardware Intrinsics**: AES, SHA, CRC optimizations
- **Code Cache**: 256MB reserved
- **NUMA Awareness**: Enabled

### Container Optimizations

- **Multi-stage build**: Minimal runtime image
- **Non-root user**: Security best practices
- **Health checks**: Kubernetes-ready probes
- **Resource limits**: Proper memory/CPU constraints

## 📊 Grafana Dashboards

The Grafana instance comes pre-configured with datasources:

### Datasources

- **Prometheus**: <http://prometheus:9090>
- **Loki**: <http://loki:3100>
- **Jaeger**: <http://jaeger:16686>

### Available Dashboards

- **Application Metrics**: Spring Boot Actuator metrics
- **Container Metrics**: cAdvisor container performance
- **System Metrics**: Node Exporter host metrics
- **Logs**: Loki log aggregation
- **Traces**: Jaeger distributed tracing

## 🔍 Monitoring & Alerting

### Health Checks

```bash

# Application health

curl <http://localhost:8081/actuator/health>

# Service status

make status

# Container health

podman-compose ps  # or docker-compose ps

```text

### Log Aggregation

- **Application logs**: Structured JSON format
- **Container logs**: Docker/Podman log driver
- **System logs**: Node Exporter metrics
- **Centralized**: Loki aggregation

### Metrics Collection

- **Application**: Micrometer + Spring Actuator
- **Container**: cAdvisor
- **System**: Node Exporter
- **Custom**: Business metrics via Micrometer

## 🚀 Deployment Scenarios

### Development

```bash

# Local development with observability

make stack-observability

```text

### Testing

```bash

# With WireMock for API mocking

make stack-observability

# WireMock available at <http://localhost:8089>

```text

### Production Considerations

- **Resource limits**: Set appropriate CPU/memory limits
- **Persistent storage**: Configure volumes for Prometheus/Grafana data
- **Security**: Use proper authentication and TLS
- **Scaling**: Consider horizontal scaling for high load

## 🔧 Configuration Files

### Key Files

- **docker-compose.yml**: Single compose file with all services
- **monitoring/otel-collector.yml**: OpenTelemetry collector configuration
- **monitoring/prometheus.yml**: Prometheus scraping configuration
- **monitoring/grafana/**: Grafana provisioning and dashboards
- **Makefile**: Infrastructure automation commands

### Environment Variables

```bash

# Application

SPRING_PROFILES_ACTIVE=docker
MANAGEMENT_SERVER_PORT=8081

# OpenTelemetry (Verified Working)

OTEL_EXPORTER_OTLP_ENDPOINT=<http://otel-collector:4318>
OTEL_EXPORTER_OTLP_PROTOCOL=http/protobuf
OTEL_SERVICE_NAME=stocks-service

# External API

FINNHUB_API_KEY=your-api-key-here

```text

## 🛠️ Troubleshooting

### Common Issues

#### Container Runtime Not Found

```bash

# Check available runtime

make env-check

# Install Podman (if needed)

brew install podman
podman machine init
podman machine start

```text

#### OpenTelemetry Not Working

```bash

# Check OTEL collector logs

podman-compose logs otel-collector

# Verify application configuration

podman exec stocks-service env | grep OTEL

```text

#### Service Not Starting

```bash

# Check service logs

podman-compose logs <service-name>

# Check service status

podman-compose ps

# Restart specific service

podman-compose restart <service-name>

```text

### Log Analysis

```bash

# Application logs

make docker-logs

# All service logs

podman-compose logs

# Specific service logs

podman-compose logs <service-name>

```text

## 📈 Monitoring Best Practices

### Metrics

- **Golden Signals**: Latency, traffic, errors, saturation
- **Business Metrics**: Stock processing rates, API call success rates
- **Infrastructure**: CPU, memory, disk, network

### Logging

- **Structured**: JSON format for machine parsing
- **Context**: Correlation IDs for request tracing
- **Levels**: Appropriate log levels for different environments

### Tracing

- **End-to-end**: From HTTP request to external API calls
- **Actor integration**: Trace continuity through Pekko actors
- **Performance**: Identify bottlenecks and optimization opportunities

---

For detailed observability configuration, see
[OBSERVABILITY.md](./OBSERVABILITY.md)
