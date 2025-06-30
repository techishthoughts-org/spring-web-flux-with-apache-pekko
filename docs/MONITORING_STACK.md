# 📊 Complete Grafana Monitoring Stack

This document provides a comprehensive guide to the complete observability
stack for the Stocks application, featuring enterprise-grade monitoring,
logging, tracing, and alerting capabilities.

## 🎯 Stack Overview

```text
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│  Stocks App     │───▶│ OTEL Collector   │───▶│    Jaeger       │
│  (Spring Boot)  │    │  (Traces)        │    │   (Tracing)     │
│                 │    │                  │    │                 │
│                 │    ┌──────────────────┐    ┌─────────────────┐
│                 │───▶│   Prometheus     │───▶│   Grafana       │
│                 │    │  (Metrics)       │    │ (Dashboards)    │
│                 │    │                  │    │                 │
│                 │    ┌──────────────────┐    │                 │
│                 │───▶│   Promtail       │───▶│   Loki          │
│                 │    │ (Log Collection) │    │  (Logs)         │
└─────────────────┘    └──────────────────┘    └─────────────────┘
         │                        │                       │
         ▼                        ▼                       ▼
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│  Alertmanager   │    │   cAdvisor       │    │ Node Exporter   │
│   (Alerts)      │    │ (Containers)     │    │  (System)       │
└─────────────────┘    └──────────────────┘    └─────────────────┘

```text

## 🚀 Quick Start

### Option 1: Automated Startup (Recommended)

```bash

# Start the complete stack with automatic service orchestration

./scripts/start-monitoring.sh

```text

### Option 2: Manual Startup

```bash

# Start all services

docker-compose up -d

# Or start services in stages

docker-compose up -d prometheus grafana loki jaeger
docker-compose up -d alertmanager node-exporter cadvisor
docker-compose up -d stocks-service

```text

## 📊 Service Portfolio

### Core Application

| Service | Port | Purpose | Health Check |
|---------|------|---------|-------------|
| **Stocks App** | 8080 | Main application | <http://localhost:8080/actuator/health> |
| **Actuator** | 8081 | Application metrics | <http://localhost:8081/actuator/prometheus> |

### Monitoring & Observability

| Service | Port | Purpose | Health Check |
|---------|------|---------|-------------|
| **Grafana** | 3000 | Dashboards & visualization | <http://localhost:3000/api/health> |
| **Prometheus** | 9090 | Metrics collection & queries | <http://localhost:9090/-/healthy> |
| **Jaeger** | 16686 | Distributed tracing UI | <http://localhost:16686/health> |
| **Alertmanager** | 9093 | Alert management | <http://localhost:9093/-/healthy> |

### Logging Stack

| Service | Port | Purpose | Health Check |
|---------|------|---------|-------------|
| **Loki** | 3100 | Log aggregation | <http://localhost:3100/ready> |
| **Promtail** | 9080 | Log collection | <http://localhost:9080/ready> |

### System Monitoring

| Service | Port | Purpose | Health Check |
|---------|------|---------|-------------|
| **cAdvisor** | 8082 | Container metrics | <http://localhost:8082/healthz> |
| **Node Exporter** | 9100 | System metrics | <http://localhost:9100/metrics> |
| **OTEL Collector** | 4318 | OpenTelemetry gateway | <http://localhost:8888/metrics> |

### Supporting Services

| Service | Port | Purpose | Health Check |
|---------|------|---------|-------------|
| **Redis** | 6379 | Application cache | N/A |
| **WireMock** | 8089 | API mocking | <http://localhost:8089/__admin/health> |
| **PostgreSQL** | 5432 | Database (optional) | N/A |

## 🎛️ Pre-configured Dashboards

### Application Dashboards

- **Stocks Overview**: Application metrics, request rates, response times
- **JVM Metrics**: Memory usage, garbage collection, thread pools
- **Spring Boot**: Actuator metrics, health indicators
- **Business Metrics**: Stock-specific KPIs and performance indicators

### Infrastructure Dashboards

- **System Overview**: CPU, memory, disk, network usage
- **Container Metrics**: Docker container performance
- **Database Performance**: Connection pools, query performance
- **Network Monitoring**: Traffic analysis, latency metrics

### Observability Dashboards

- **Distributed Tracing**: Request flow visualization
- **Log Analysis**: Centralized log searching and analysis
- **Alert Overview**: Active alerts and incident management
- **SLA Monitoring**: Service level objectives tracking

## 🔍 Key Features

### 1. **Distributed Tracing**

- **OpenTelemetry** integration with automatic instrumentation
- **Jaeger** for trace visualization and analysis
- **Correlation ID** propagation across services
- **Context propagation** between reactive and imperative code

### 2. **Comprehensive Metrics**

- **Application metrics**: Request rates, response times, error rates
- **JVM metrics**: Memory, GC, thread pools
- **System metrics**: CPU, memory, disk, network
- **Container metrics**: Resource usage, health status
- **Business metrics**: Custom application KPIs

### 3. **Centralized Logging**

- **Structured JSON logging** with trace correlation
- **Log aggregation** via Loki
- **Automatic log collection** from containers
- **Cross-service log correlation** with trace IDs

### 4. **Intelligent Alerting**

- **Prometheus alerting rules** for proactive monitoring
- **Alertmanager** for alert routing and management
- **Multi-channel notifications** (email, webhook, Slack)
- **Alert deduplication** and grouping

### 5. **Service Discovery**

- **Auto-discovery** of services and endpoints
- **Dynamic configuration** updates
- **Health check** integration
- **Load balancing** metrics

## 🔧 Configuration Highlights

### Prometheus Configuration

```yaml

# Enhanced scrape configurations for all services

scrape_configs:

  - job_name: 'stocks-app'
    static_configs:

      - targets: ['stocks-service:8081']
    metrics_path: /actuator/prometheus
    scrape_interval: 15s

```text

### Grafana Provisioning

```yaml

# Auto-provisioned datasources

datasources:

  - name: Prometheus
    type: prometheus
    url: <http://prometheus:9090>
    isDefault: true

  - name: Loki
    type: loki
    url: <http://loki:3100>

  - name: Jaeger
    type: jaeger
    url: <http://jaeger:16686>

```text

### OpenTelemetry Integration

```yaml

# OTLP configuration for traces

receivers:
  otlp:
    protocols:
      grpc:
        endpoint: 0.0.0.0:4317
      http:
        endpoint: 0.0.0.0:4318

```text

## 📈 Sample Queries

### Prometheus Queries

```promql

# Request rate by endpoint

rate(http_server_requests_seconds_count[5m])

# 95th percentile response time

histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m]))

# Error rate

rate(http_server_requests_seconds_count{status=~"5.."}[5m])

# JVM memory usage

(jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"}) * 100

```text

### Loki Queries

```logql

# Application logs with trace correlation

{job="stocks-app"} |= "traceId"

# Error logs only

{job="stocks-app"} | json | level="ERROR"

# Logs for specific trace

{job="stocks-app"} | json | traceId="abc123def456"

```text

### Jaeger Queries

- Search by service name: `stocks-service`
- Search by operation: `GET /stocks/{symbol}`
- Search by trace ID: `abc123def456ghi789`
- Search by tags: `http.status_code=200`

## 🚨 Default Alerting Rules

### Application Alerts

- **Application Down**: Service unavailable for 30s
- **High Error Rate**: >10% errors for 2 minutes
- **High Response Time**: >2s 95th percentile for 5 minutes
- **High Memory Usage**: >80% JVM heap for 5 minutes

### Infrastructure Alerts

- **High CPU Usage**: >80% for 5 minutes
- **Low Disk Space**: <20% available for 5 minutes
- **Container Resource Limits**: Near memory/CPU limits
- **Service Health Check Failures**: Health checks failing

## 🔐 Security Configuration

### Access Control

- **Grafana**: admin/admin (change in production)
- **Prometheus**: No authentication (internal network)
- **Jaeger**: No authentication (internal network)
- **Alertmanager**: No authentication (internal network)

### Network Security

- All services run in isolated Docker network
- Only necessary ports exposed to host
- Internal service communication only

## 🎯 Production Considerations

### Scalability

- **Horizontal scaling**: Multiple Prometheus instances
- **Long-term storage**: Remote write to external systems
- **High availability**: Clustered Grafana/Alertmanager

### Data Retention

- **Prometheus**: 30 days (configurable)
- **Loki**: 30 days (configurable)
- **Jaeger**: 7 days (configurable)

### Performance Tuning

- **Scrape intervals**: Optimized for each service type
- **Storage optimization**: Configured retention and compression
- **Resource limits**: Appropriate CPU/memory allocation

## 🛠️ Troubleshooting

### Common Issues

1. **Services not starting**

   ```bash
   # Check logs
   docker-compose logs [service-name]

   # Check resource usage
   docker stats
   ```

1. **Missing metrics**

   ```bash
   # Verify Prometheus targets
   curl <http://localhost:9090/api/v1/targets>

   # Check service health
   curl <http://localhost:8081/actuator/health>
   ```

1. **Tracing not working**

   ```bash
   # Check OTEL Collector
   curl <http://localhost:8888/metrics>

   # Verify Jaeger traces
   curl <http://localhost:16686/api/traces>
   ```

### Health Check Commands

```bash

# Check all services

./scripts/health-check.sh

# Individual service checks

curl <http://localhost:3000/api/health>      # Grafana
curl <http://localhost:9090/-/healthy>       # Prometheus
curl <http://localhost:16686/health>         # Jaeger
curl <http://localhost:3100/ready>           # Loki

```text

## 📚 Additional Resources

- [OpenTelemetry Documentation](<https://opentelemetry.io/docs/>)
- [Prometheus Best Practices](<https://prometheus.io/docs/practices/>)
- [Grafana Dashboard Best Practices](<https://grafana.com/docs/grafana/latest/best-practices/>)
- [Jaeger Performance Tuning](<https://www.jaegertracing.io/docs/1.35/performance-tuning/>)

## 🎉 What's Next?

After starting the stack:

1. **Explore Grafana** dashboards at <http://localhost:3000>
1. **Make API calls** to generate metrics and traces
1. **Create custom dashboards** for your specific needs
1. **Set up alerting** for your production environment
1. **Integrate with external systems** (Slack, PagerDuty, etc.)

Happy monitoring! 🚀
