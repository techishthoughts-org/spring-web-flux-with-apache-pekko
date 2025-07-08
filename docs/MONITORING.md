# Monitoring and Observability Guide

## Overview

This guide covers the comprehensive observability solution for the Stock Service application, including monitoring, logging, tracing, and alerting. The observability stack follows the three pillars of observability: metrics, logs, and traces.

## 📊 Observability Stack

### Components Overview

```
┌─────────────────────────────────────────────────────────────┐
│                  Observability Stack                        │
├─────────────────────────────────────────────────────────────┤
│  Grafana (3000)     │  Visualization & Dashboards          │
│  Prometheus (9090)  │  Metrics Collection & Alerting       │
│  Jaeger (16686)     │  Distributed Tracing                 │
│  ClickHouse (8123)  │  High-Performance Log Storage        │
│  AlertManager (9093)│  Alert Management                    │
│  OTEL Collector     │  Telemetry Data Pipeline             │
│  Node Exporter      │  System Metrics                      │
│  cAdvisor (8082)    │  Container Metrics (Optional)        │
└─────────────────────────────────────────────────────────────┘
```

### Service Ports

| Service | Port | Purpose | Health Check |
|---------|------|---------|--------------|
| Application | 8080 | Main API & Actuator | http://localhost:8080/actuator/health |
| Grafana | 3000 | Visualization | http://localhost:3000 |
| Prometheus | 9090 | Metrics | http://localhost:9090 |
| Jaeger | 16686 | Tracing UI | http://localhost:16686 |
| ClickHouse | 8123 | Log Storage API | http://localhost:8123/ping |
| ClickHouse TCP | 9000 | Native Protocol | tcp://localhost:9000 |
| AlertManager | 9093 | Alerts | http://localhost:9093 |
| OTEL Collector | 8889 | Telemetry | http://localhost:8889 |
| Node Exporter | 9100 | System Metrics | http://localhost:9100 |
| cAdvisor | 8082 | Container Metrics | http://localhost:8082 |

## 🚀 Quick Start

### Starting Observability Stack

```bash
# Start monitoring stack for local development
make dev

# Start with container metrics
make dev-with-cadvisor

# Start everything in containers
make full

# Check all services
make status
```

### Accessing Services

```bash
# Direct access
open http://localhost:3000    # Grafana (admin/admin)
open http://localhost:9090    # Prometheus
open http://localhost:16686   # Jaeger
open http://localhost:8123    # ClickHouse HTTP API
```

## 📈 Metrics Collection

### Prometheus Configuration

The metrics collection is configured in `monitoring/prometheus.yml`:

```yaml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

rule_files:
  - "rules/*.yml"

scrape_configs:
  # Application metrics
  - job_name: 'stocks-app'
    static_configs:
      - targets: ['stocks-service:8080']
    metrics_path: /actuator/prometheus
    scrape_interval: 30s

  # System metrics
  - job_name: 'node-exporter'
    static_configs:
      - targets: ['node-exporter:9100']

  # Container metrics (optional)
  - job_name: 'cadvisor'
    static_configs:
      - targets: ['cadvisor:8080']

  # OpenTelemetry Collector
  - job_name: 'otel-collector'
    static_configs:
      - targets: ['otel-collector:8888']
```

### Application Metrics

The application exposes metrics through Spring Boot Actuator:

```bash
# View all available metrics
curl http://localhost:8080/actuator/metrics

# Specific metric
curl http://localhost:8080/actuator/metrics/http.server.requests

# Prometheus format
curl http://localhost:8080/actuator/prometheus
```

### Key Metrics Categories

#### HTTP Metrics
- `http_server_requests_seconds_count`: Request count
- `http_server_requests_seconds_sum`: Total request time
- `http_server_requests_seconds_max`: Maximum request time

#### JVM Metrics
- `jvm_memory_used_bytes`: Memory usage
- `jvm_gc_pause_seconds`: Garbage collection pauses
- `jvm_threads_live_threads`: Active thread count

#### Actor System Metrics
- `pekko_actor_message_processing_time_seconds`: Message processing time
- `pekko_actor_mailbox_size`: Actor mailbox size
- `pekko_actor_failures_total`: Actor failures

#### Business Metrics
- `stocks_processed_total`: Total stocks processed
- `stocks_cache_hits_total`: Cache hit count
- `stocks_cache_misses_total`: Cache miss count

## 📋 Logging Pipeline ✅

### Complete Logging Architecture

**Application → OTEL Collector → ClickHouse → Grafana**

The logging pipeline is **fully operational** and provides:

1. **Application** generates structured JSON logs with OpenTelemetry integration
2. **OTEL Collector** receives logs via OTLP protocol and processes them
3. **ClickHouse** stores logs in a high-performance columnar database with advanced analytics capabilities
4. **Grafana** provides log visualization and dashboards with ClickHouse data source

### Log Configuration

#### Application Logging (`logback-spring.xml`)
```xml
<configuration>
    <appender name="JSON_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/application.json</file>
        <encoder class="net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder">
            <providers>
                <timestamp/>
                <logLevel/>
                <loggerName/>
                <message/>
                <mdc/>
                <arguments/>
            </providers>
        </encoder>
    </appender>

    <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <root level="INFO">
        <appender-ref ref="JSON_FILE"/>
        <appender-ref ref="STDOUT"/>
    </root>
</configuration>
```

#### OTEL Collector Configuration (`monitoring/otel-collector.yml`)
```yaml
receivers:
  otlp:
    protocols:
      grpc:
        endpoint: 0.0.0.0:4317
      http:
        endpoint: 0.0.0.0:4318

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
  clickhouse:
    endpoint: tcp://clickhouse:9000?dial_timeout=10s&compress=lz4&async_insert=1&username=default&password=clickhouse123
    database: logs
    logs_table_name: otel_logs
    ttl: 720h
    create_schema: true

service:
  pipelines:
    logs:
      receivers: [otlp]
      processors: [resource, batch]
      exporters: [clickhouse, debug]
```

#### ClickHouse Configuration

ClickHouse provides high-performance log storage with:

- **Compression**: ZSTD level 3 for optimal storage efficiency
- **Partitioning**: By date for efficient querying and data management
- **TTL**: 30-day retention policy (configurable)
- **Indexing**: Bloom filters for fast text searches
- **Schema**: OpenTelemetry standard fields plus custom application metadata
- **Performance**: Sub-second queries on millions of log entries
- **Cost-Effective**: 10-14x compression, significantly cheaper than traditional log solutions

### Viewing Logs

#### Grafana Log Exploration
1. Go to http://localhost:3000
2. Navigate to "Explore" → Select "ClickHouse" data source
3. Use SQL queries like:
   - `SELECT * FROM otel_logs WHERE ServiceName = 'stocks-service' ORDER BY Timestamp DESC LIMIT 100`
   - `SELECT * FROM otel_logs WHERE SeverityText = 'ERROR' ORDER BY Timestamp DESC`
   - `SELECT * FROM otel_logs WHERE Body LIKE '%stock%' ORDER BY Timestamp DESC`

#### Direct ClickHouse Queries
```bash
# Query recent logs
curl -s "http://localhost:8123/" --data-binary "SELECT Timestamp, ServiceName, SeverityText, Body FROM logs.otel_logs ORDER BY Timestamp DESC LIMIT 10" --user "default:clickhouse123"

# Query with time range (last hour)
curl -s "http://localhost:8123/" --data-binary "SELECT * FROM logs.otel_logs WHERE Timestamp >= now() - INTERVAL 1 HOUR ORDER BY Timestamp DESC" --user "default:clickhouse123"

# Count logs by service
curl -s "http://localhost:8123/" --data-binary "SELECT ServiceName, count() FROM logs.otel_logs GROUP BY ServiceName" --user "default:clickhouse123"

# Check table schema
curl -s "http://localhost:8123/" --data-binary "DESCRIBE logs.otel_logs" --user "default:clickhouse123"
```

### Log Verification

```bash
# Check OTEL Collector is collecting logs
docker logs stocks-otel-collector

# Check ClickHouse is ready
curl http://localhost:8123/ping

# Check logs are being ingested
curl -s "http://localhost:8123/" --data-binary "SELECT COUNT(*) FROM logs.otel_logs" --user "default:clickhouse123"
```

## 📊 Grafana Dashboards

### Pre-configured Dashboards

The application includes several pre-built dashboards:

1. **Spring Boot + Pekko Overview** (`spring-pekko-stocks.json`)
2. **Application Metrics** (`stocks-overview.json`)

### Dashboard Categories

#### Application Overview
- Health status indicator
- Request rate and response times
- Error rate trends
- Active connections

#### JVM Metrics
- Heap and non-heap memory usage
- Garbage collection statistics
- Thread pool status
- Class loading metrics

#### Actor System
- Message processing rates
- Actor lifecycle events
- Mailbox queue sizes
- Supervision events

#### Logs Dashboard
- Real-time log streaming
- Log level distribution
- Error rate trends
- Search and filtering capabilities

#### Business Metrics
- Stock processing rates
- Cache hit/miss ratios
- API usage patterns
- External service calls

### Importing Dashboards

```bash
# Import main dashboard
curl -X POST http://admin:admin@localhost:3000/api/dashboards/db \
  -H 'Content-Type: application/json' \
  -d @monitoring/grafana/dashboards/application/spring-pekko-stocks.json

# Import via API
curl -X POST http://admin:admin@localhost:3000/api/dashboards/import \
  -H 'Content-Type: application/json' \
  -d '{"dashboard": {...}, "overwrite": true}'
```

### Custom Dashboard Creation

```json
{
  "dashboard": {
    "title": "Custom Stock Metrics",
    "panels": [
      {
        "title": "Stock Processing Rate",
        "type": "graph",
        "targets": [
          {
            "expr": "rate(stocks_processed_total[5m])",
            "legendFormat": "Processing Rate"
          }
        ]
      }
    ]
  }
}
```

## 🔍 Distributed Tracing

### OpenTelemetry Integration

The application uses OpenTelemetry for distributed tracing:

```yaml
# application.yml
management:
  tracing:
    sampling:
      probability: 1.0
  otlp:
    tracing:
      endpoint: ${OTEL_EXPORTER_OTLP_ENDPOINT:http://localhost:4317}
```

### Jaeger UI

Access Jaeger at http://localhost:16686 to:
- View distributed traces
- Analyze request flows
- Identify performance bottlenecks
- Debug service interactions

### Trace Correlation

Logs are correlated with traces using:
- Trace ID in log messages
- Span ID for request correlation
- Baggage for context propagation

## 🎯 Note on Profiling

Continuous profiling capabilities were initially planned using Pyroscope but have been removed from the current implementation due to compatibility issues. The current observability stack focuses on the three core pillars: metrics (Prometheus), traces (Jaeger), and logs (Loki), which provide comprehensive monitoring capabilities for the application.

## 🚨 Alerting

### Alert Rules

Configured in `monitoring/rules/stocks-alerts.yml`:

```yaml
groups:
  - name: stocks-service
    rules:
      - alert: HighErrorRate
        expr: rate(http_server_requests_seconds_count{status=~"5.."}[5m]) > 0.1
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High error rate detected"
          description: "Error rate is {{ $value }} errors per second"

      - alert: HighMemoryUsage
        expr: jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"} > 0.8
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "High memory usage"
          description: "Memory usage is {{ $value | humanizePercentage }}"
```

### Alert Manager

Access AlertManager at http://localhost:9093 to:
- View active alerts
- Configure notification channels
- Manage alert silencing
- Review alert history

## 🔧 Configuration

### Environment Variables

```bash
# OpenTelemetry
OTEL_EXPORTER_OTLP_ENDPOINT=http://localhost:4317
OTEL_RESOURCE_ATTRIBUTES=service.name=stocks-service

# Logging
LOGGING_LEVEL_COM_TECHISHTHOUGHTS_STOCKS=INFO
LOGGING_FILE_PATH=logs/application.log
```

### Monitoring Configuration Files

```
monitoring/
├── prometheus.yml          # Prometheus configuration
├── loki.yml               # Loki configuration
├── promtail.yml           # Promtail configuration
├── alertmanager.yml       # AlertManager configuration
├── otel-collector.yml     # OpenTelemetry Collector
├── grafana/
│   ├── provisioning/
│   │   ├── datasources/   # Data source definitions
│   │   └── dashboards/    # Dashboard provisioning
│   └── dashboards/
│       └── application/   # Dashboard definitions
└── rules/
    └── stocks-alerts.yml  # Alert rules
```

## 📈 Performance Monitoring

### Key Performance Indicators

#### Response Time
```promql
# 95th percentile response time
histogram_quantile(0.95,
  rate(http_server_requests_seconds_bucket[5m])
)
```

#### Throughput
```promql
# Requests per second
rate(http_server_requests_seconds_count[5m])
```

#### Error Rate
```promql
# Error rate percentage
rate(http_server_requests_seconds_count{status=~"5.."}[5m]) /
rate(http_server_requests_seconds_count[5m]) * 100
```

#### Actor Performance
```promql
# Actor message processing time
histogram_quantile(0.95,
  rate(pekko_actor_message_processing_time_seconds_bucket[5m])
)
```

### SLA Monitoring

```yaml
# SLA definitions
sla:
  response_time_95th: 500ms
  error_rate_max: 1%
  availability_min: 99.9%
```

## 🔍 Troubleshooting

### Common Issues

#### Missing Metrics
```bash
# Check Prometheus targets
curl http://localhost:9090/api/v1/targets

# Verify application metrics endpoint
curl http://localhost:8080/actuator/prometheus
```

#### Log Ingestion Issues
```bash
# Check Promtail logs
podman logs stocks-promtail

# Check Loki status
curl http://localhost:3100/ready

# Verify log files
ls -la logs/
```

#### Tracing Issues
```bash
# Check OTEL Collector
curl http://localhost:8889/api/v1/traces

# Verify Jaeger
curl http://localhost:16686/api/traces
```

### Debug Commands

```bash
# Check all services
make status

# View logs
make logs

# Check health
make health

# Restart monitoring stack
make stop && make dev
```

## 📊 Dashboards and Queries

### Useful Prometheus Queries

```promql
# JVM Memory Usage
jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"} * 100

# Request Rate by Endpoint
rate(http_server_requests_seconds_count[5m])

# Error Rate by Status
rate(http_server_requests_seconds_count{status=~"5.."}[5m])

# Actor Mailbox Size
pekko_actor_mailbox_size

# GC Pause Time
rate(jvm_gc_pause_seconds_sum[5m])
```

### Useful Loki Queries

```logql
# All application logs
{service="stocks-service"}

# Error logs only
{service="stocks-service", level="ERROR"}

# Logs with specific message
{service="stocks-service"} |= "Processing symbol"

# Log rate by level
rate({service="stocks-service"}[5m])
```

## 🎯 Best Practices

### Metrics
- Use appropriate metric types (counter, gauge, histogram)
- Add meaningful labels for aggregation
- Monitor key business metrics
- Set up SLA-based alerting

### Logging
- Use structured logging (JSON)
- Include correlation IDs
- Log at appropriate levels
- Avoid sensitive data in logs

### Tracing
- Enable sampling for performance
- Use meaningful span names
- Add custom attributes
- Correlate with logs

### Dashboards
- Group related metrics
- Use consistent time ranges
- Add annotations for context
- Design for different audiences

## 🔗 Resources

### Documentation
- [Prometheus Documentation](https://prometheus.io/docs/)
- [Grafana Documentation](https://grafana.com/docs/)
- [Jaeger Documentation](https://www.jaegertracing.io/docs/)
- [Loki Documentation](https://grafana.com/docs/loki/)

### Tutorials
- [OpenTelemetry Java](https://opentelemetry.io/docs/instrumentation/java/)
- [Spring Boot Actuator](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)

---

**The observability stack is fully operational and provides comprehensive monitoring, logging, and tracing capabilities for the Stock Service application! 🚀**
