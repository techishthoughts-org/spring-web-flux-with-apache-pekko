# Getting Started Guide

This guide will help you quickly get the Stock Service up and running with full observability.

## 🚀 Quick Start

### 1. Prerequisites

Ensure you have the following installed:

- **Java 21+** (JDK)
- **Maven 3.8+**
- **Docker or Podman**
- **Git**

### 2. Clone and Setup

```bash
git clone <repository-url>
cd stocks-demo

# Copy environment configuration
cp env.example .env
```

### 3. Start the Application

**Option A: Local Development (Recommended)**

```bash
# Start monitoring stack in containers
make dev

# Run application locally (in another terminal)
make run
```

**Option B: Full Container Mode**

```bash
# Start everything in containers
make full
```

### 4. Verify Everything is Working

```bash
# Check all services
make status

# Test the API
curl http://localhost:8080/health-simple
curl http://localhost:8080/stocks/AAPL
```

### 5. Access the Monitoring Stack

- **Grafana**: http://localhost:3000 (admin/admin)
- **Prometheus**: http://localhost:9090
- **Jaeger**: http://localhost:16686
- **ClickHouse**: http://localhost:8123

## 📁 Project Structure

```
stocks-demo/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/techishthoughts/stocks/
│   │   │       ├── adapter/          # Web controllers and external adapters
│   │   │       ├── application/      # Business logic and use cases
│   │   │       ├── domain/          # Domain entities and value objects
│   │   │       ├── infrastructure/  # Technical infrastructure
│   │   │       └── config/          # Configuration classes
│   │   └── resources/
│   │       ├── application.yml      # Main configuration
│   │       └── logback-spring.xml   # Logging configuration
│   └── test/                        # Test files
├── monitoring/                      # Monitoring configurations
├── scripts/                         # Utility scripts
├── docs/                           # Documentation
├── docker-compose.yml              # Single Docker Compose with profiles
├── Dockerfile                      # Application container
├── Makefile                        # Build and deployment commands
└── pom.xml                         # Maven configuration
```

## 🐳 Deployment Options

### Single Docker Compose with Profiles

This project uses a **single `docker-compose.yml`** with profiles for different scenarios:

| Profile | Purpose | Command |
|---------|---------|---------|
| `monitoring` | Observability stack for local development | `make dev` |
| `app` | Application service only | `docker-compose --profile app up` |
| `full` | Complete stack in containers | `make full` |
| `cadvisor` | Optional container metrics | `make dev-with-cadvisor` |

### Development Workflow

#### Local Development (Recommended)

This mode runs the monitoring stack in containers and the application locally:

```bash
# Start monitoring stack
make dev

# Run application locally (new terminal)
make run
```

**Benefits:**
- Easy debugging with IDE
- Fast application restarts
- Full observability stack
- Hot reload during development

#### Full Container Mode

Run everything in containers for production-like testing:

```bash
# Start everything in containers
make full
```

**Benefits:**
- Production-like environment
- Container resource monitoring
- Consistent environment across teams

#### Container Metrics (Optional)

Add container metrics monitoring with cAdvisor:

```bash
# Local development with container metrics
make dev-with-cadvisor

# Full stack with container metrics
make full-with-cadvisor
```

## 🔧 Configuration

### Environment Variables

Create a `.env` file or set environment variables:

```bash
# Required
FINNHUB_KEY=your_finnhub_api_key

# Optional
OTEL_EXPORTER_OTLP_ENDPOINT=http://localhost:4317
DEPLOYMENT_ENVIRONMENT=development
```

### Application Configuration

Key settings in `src/main/resources/application.yml`:

```yaml
# Server configuration
server:
  port: 8080

# Finnhub API
finnhub:
  base-url: https://finnhub.io/api/v1
  api-key: ${FINNHUB_KEY:demo}

# Management endpoints
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus

# OpenTelemetry
otel:
  exporter:
    otlp:
      endpoint: ${OTEL_EXPORTER_OTLP_ENDPOINT:http://localhost:4317}
```

## 📊 Observability Setup

### Complete Logging Pipeline

**Application → OTEL Collector → ClickHouse → Grafana**

1. **Application** generates structured JSON logs with OpenTelemetry integration
2. **OTEL Collector** receives logs via OTLP protocol and processes them
3. **ClickHouse** stores logs in a high-performance columnar database
4. **Grafana** provides log visualization and dashboards with SQL queries

### Monitoring Stack Components

| Service | Purpose | Port | Health Check |
|---------|---------|------|--------------|
| **Grafana** | Dashboards and visualization | 3000 | http://localhost:3000 |
| **Prometheus** | Metrics collection | 9090 | http://localhost:9090 |
| **Jaeger** | Distributed tracing | 16686 | http://localhost:16686 |
| **Loki** | Log aggregation | 3100 | http://localhost:3100/ready |
| **Alertmanager** | Alert management | 9093 | http://localhost:9093 |
| **Promtail** | Log collection | 9080 | http://localhost:9080 |
| **OTEL Collector** | Telemetry collection | 4317-4318 | http://localhost:8889 |
| **Node Exporter** | System metrics | 9100 | http://localhost:9100 |

### Verification

```bash
# Check all services
make status

# Check application health
make health

# View application logs
make logs

# Test API endpoints
curl http://localhost:8080/health-simple
curl http://localhost:8080/stocks/AAPL
```

## 🧪 Testing

### Running Tests

```bash
# Run all tests
make test

# Run specific test types
make test-unit              # Unit tests only
make test-integration       # Integration tests only
make test-performance       # Performance tests

# Clean and test
make clean && make test
```

### Test Structure

- **Unit Tests**: Fast, isolated tests
- **Integration Tests**: Database and external service tests
- **Component Tests**: End-to-end API tests using Cucumber
- **Performance Tests**: Load and stress testing
- **Smoke Tests**: Basic functionality validation

## 🔍 Troubleshooting

### Common Issues

#### Port Conflicts

```bash
# Check what's using port 8080
lsof -i :8080

# Stop all services
make stop

# Clean everything
make clean
```

#### Application Not Starting

```bash
# Check logs
make logs

# Check health
make health

# Rebuild and restart
make clean && make build && make dev
```

#### Missing Observability Data

```bash
# Check service status
make status

# Check Promtail logs
podman logs stocks-promtail

# Check Loki status
curl http://localhost:3100/ready

# Query logs directly
curl "http://localhost:3100/loki/api/v1/query?query={service=\"stocks-service\"}"
```

#### Container Issues

```bash
# Check container logs
podman logs stocks-grafana
podman logs stocks-prometheus
podman logs stocks-loki

# Restart specific service
podman restart stocks-grafana
```

### Performance Issues

```bash
# Check JVM metrics
curl http://localhost:8080/actuator/prometheus | grep jvm

# Check actor system metrics
curl http://localhost:8080/actuator/prometheus | grep pekko

# View metrics in Prometheus
open http://localhost:9090
```

## 📈 Next Steps

1. **Explore the API**: Check out [API Documentation](API.md)
2. **Learn the Architecture**: Read [Architecture Documentation](ARCHITECTURE.md)
3. **Set up Monitoring**: Follow [Monitoring Guide](MONITORING.md)
4. **Run Tests**: See [Testing Guide](TESTING.md)
5. **Deploy to Production**: Check [Deployment Guide](DEPLOYMENT.md)

## 🎯 Development Tips

### IDE Setup

**IntelliJ IDEA:**
1. Import as Maven project
2. Enable annotation processing
3. Install Lombok plugin
4. Configure Java 21 SDK

**VS Code:**
1. Install Java Extension Pack
2. Install Spring Boot Extension Pack
3. Configure Java 21 in settings

### Hot Reload

```bash
# For local development
make dev    # Start monitoring stack
make run    # Run application with hot reload enabled
```

### Debugging

```bash
# Run with debug enabled
MAVEN_OPTS="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005" make run
```

### Log Levels

```bash
# Set log level via environment
export LOGGING_LEVEL_COM_TECHISHTHOUGHTS_STOCKS=DEBUG
make run
```

## 🤝 Getting Help

- **Issues**: Check logs with `make logs`
- **Documentation**: Browse `docs/` directory
- **Health Checks**: Use `make health` and `make status`
- **Community**: Submit issues via GitHub

---

**You're now ready to start developing with the Stock Service! 🚀**
