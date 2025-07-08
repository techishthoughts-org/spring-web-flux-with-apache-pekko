# Stock Service - Spring Boot + Apache Pekko

A reactive microservice for stock data management built with Spring Boot 3, Apache Pekko actors, and comprehensive observability.

## 🏗️ Overview

This application provides a reactive REST API for stock data retrieval using:

- **Spring Boot 3** with WebFlux for reactive web layer
- **Apache Pekko** actor system for concurrent stock data processing
- **Hexagonal Architecture** with clear separation of concerns
- **Comprehensive Observability** with metrics, tracing, and logging
- **Single Docker Compose** with profiles for different deployment scenarios

## 🚀 Quick Start

```bash
# Start monitoring stack for local development
make dev

# Run application locally
make run

# Or run everything in containers
make full

# Check health
make health

# Run tests
make test
```

**Access the application:**

- **API**: http://localhost:8080
- **Health**: http://localhost:8080/actuator/health
- **Simple Health**: http://localhost:8080/health-simple
- **Metrics**: http://localhost:8080/actuator/prometheus

**Monitoring Stack:**

- **Grafana**: http://localhost:3000 (admin/admin)
- **Prometheus**: http://localhost:9090
- **Jaeger**: http://localhost:16686
- **ClickHouse**: http://localhost:8123

## 🎯 Features

### Core Functionality

- **Stock Retrieval**: Get individual stock data by symbol
- **Stock Listing**: Retrieve all available stocks
- **Real-time Processing**: Asynchronous stock data processing with Pekko actors
- **Error Handling**: Comprehensive error handling with proper HTTP status codes

### Technical Features

- **Reactive Programming**: Non-blocking I/O with Reactor
- **Actor System**: Concurrent processing with Apache Pekko
- **Health Checks**: Application and component health monitoring
- **Metrics Collection**: Prometheus metrics export
- **Distributed Tracing**: OpenTelemetry integration with Jaeger
- **Structured Logging**: JSON logging with correlation IDs exported to ClickHouse

## 📚 API Documentation

### Endpoints

#### Get Stock by Symbol

```
GET /stocks/{symbol}
```

Returns stock information for the specified symbol.

**Example:**

```bash
curl http://localhost:8080/stocks/AAPL
```

#### Get All Stocks

```
GET /stocks
```

Returns all available stock information.

**Example:**

```bash
curl http://localhost:8080/stocks
```

#### Health Check

```
GET /health-simple
```

Simple health check endpoint.

**Example:**

```bash
curl http://localhost:8080/health-simple
```

### Response Format

**Stock Entity:**

```json
{
  "symbol": "AAPL",
  "name": "Apple Inc.",
  "exchange": "NASDAQ",
  "assetType": "Common Stock",
  "ipoDate": "1980-12-12",
  "country": "US",
  "currency": "USD",
  "ipo": "1980-12-12",
  "marketCapitalization": 3000000000000.0,
  "phone": "+1-408-996-1010",
  "shareOutstanding": 16000000000.0,
  "ticker": "AAPL",
  "weburl": "https://www.apple.com",
  "logo": "https://static.finnhub.io/img/87768.png",
  "finnhubIndustry": "Technology",
  "lastUpdated": "2024-01-01T00:00:00Z"
}
```

**Error Response:**

```json
{
  "code": "STOCK_NOT_FOUND",
  "message": "Stock not found for symbol: INVALID",
  "timestamp": "2024-01-01T00:00:00Z",
  "path": "/stocks/INVALID"
}
```

## 🏛️ Architecture

### Layer Structure

```
┌─────────────────────┐
│   Web Layer (REST)  │  ← Controllers, Exception Handlers
├─────────────────────┤
│  Application Layer  │  ← Use Cases, Business Logic
├─────────────────────┤
│   Domain Layer      │  ← Entities, Value Objects, Exceptions
├─────────────────────┤
│ Infrastructure Layer│  ← Actors, External APIs, Config
└─────────────────────┘
```

### Key Components

- **StockController**: REST API endpoints
- **StockUseCases**: Business logic orchestration
- **Stock**: Domain entity representing stock data
- **StockSymbol**: Value object for stock symbols with validation
- **StockActor**: Pekko actor for concurrent stock processing
- **FinnhubClient**: External API integration

## 🐳 Deployment

### Single Docker Compose with Profiles

This project uses a single `docker-compose.yml` with profiles for different deployment scenarios:

| Profile | Purpose | Command |
|---------|---------|---------|
| `monitoring` | Observability stack for local development | `make dev` |
| `app` | Application service only | `docker-compose --profile app up` |
| `full` | Complete stack in containers | `make full` |
| `cadvisor` | Optional container metrics | `make dev-with-cadvisor` |

### Development Workflow

1. **Local Development**: `make dev` + `make run`
   - Monitoring stack in containers
   - Application running locally (easy debugging)

2. **Full Container Testing**: `make full`
   - Everything containerized
   - Production-like environment

3. **Container Metrics**: `make dev-with-cadvisor`
   - Includes container resource metrics
   - Additional monitoring for containers

## 🔧 Development

### Prerequisites

- Java 21+
- Maven 3.8+
- Docker/Podman
- jq (optional, for JSON formatting)

### Available Commands

```bash
# Build & Run
make build                  # Build application
make run                    # Run application locally

# Development Environments
make dev                    # Start monitoring stack for local development
make dev-with-cadvisor      # Include container metrics
make full                   # Start everything in containers
make full-with-cadvisor     # Full stack with container metrics

# Management
make status                 # Check status of all services
make health                 # Check application health
make logs                   # View application logs
make stop                   # Stop all services
make clean                  # Clean up everything

# Testing
make test                   # Run all tests
make test-unit              # Run unit tests only
make test-integration       # Run integration tests only
make test-performance       # Run performance tests
```

### Configuration

Key configuration properties in `application.yml`:

- `finnhub.api-key`: Finnhub API key
- `spring.application.name`: Application name
- `management.endpoints.web.exposure.include`: Actuator endpoints
- `otel.exporter.otlp.endpoint`: OpenTelemetry collector endpoint

### Environment Variables

- `FINNHUB_KEY`: Finnhub API key
- `OTEL_EXPORTER_OTLP_ENDPOINT`: OpenTelemetry endpoint
- `DEPLOYMENT_ENVIRONMENT`: Environment name

## 📊 Observability

### Complete Logging Pipeline ✅

**Application → Promtail → Loki → Grafana**

- **Structured Logging**: JSON format with correlation IDs
- **Log Aggregation**: Loki collects and indexes logs
- **Real-time Monitoring**: Grafana dashboards for log analysis
- **Tracing Integration**: Logs linked to traces via correlation IDs

### Monitoring Stack

| Service | Purpose | URL |
|---------|---------|-----|
| **Grafana** | Dashboards and visualization | http://localhost:3000 |
| **Prometheus** | Metrics collection | http://localhost:9090 |
| **Jaeger** | Distributed tracing | http://localhost:16686 |
| **Loki** | Log aggregation | http://localhost:3100 |
| **Alertmanager** | Alert management | http://localhost:9093 |

### Dashboards

- **Application Overview**: General health and performance
- **JVM Metrics**: Memory, GC, threads
- **HTTP Metrics**: Request rates, response times
- **Actor System**: Pekko actor metrics
- **Logs**: Real-time log monitoring with filtering

### Verification

```bash
make status  # Check all services
make health  # Application health
make logs    # View logs
```

## 🧪 Testing

### Test Structure

```
src/test/java/
├── unit/           # Unit tests
├── integration/    # Integration tests
├── component/      # Component tests (Cucumber)
├── performance/    # Performance tests
└── smoke/         # Smoke tests
```

### Running Tests

```bash
make test              # All tests
make test-unit         # Unit tests only
make test-integration  # Integration tests only
make test-performance  # Performance tests
```

### Test Features

- **Cucumber BDD**: Behavior-driven development tests
- **Reactive Testing**: StepVerifier for reactive streams
- **Test Containers**: Integration testing with real dependencies
- **Performance Testing**: JMH benchmarks

## 🔍 Troubleshooting

### Common Issues

**Application won't start:**

```bash
make health  # Check health
make logs    # View logs
make clean   # Clean and restart
```

**Port conflicts:**

```bash
make stop    # Stop all services
lsof -i :8080  # Check what's using port 8080
```

**Missing metrics:**

```bash
make status  # Check observability stack
curl http://localhost:8080/actuator/prometheus
```

**Log ingestion issues:**

```bash
# Check Promtail logs
podman logs stocks-promtail

# Check Loki status
curl http://localhost:3100/ready

# Query logs directly
curl "http://localhost:3100/loki/api/v1/query?query={service=\"stocks-service\"}"
```

### Health Endpoints

- Application: http://localhost:8080/actuator/health
- Simple health: http://localhost:8080/health-simple
- Metrics: http://localhost:8080/actuator/prometheus

## 📈 Performance

### Optimizations

- **ZGC**: Low-latency garbage collection
- **Reactive Streams**: Non-blocking I/O
- **Actor Concurrency**: Parallel processing
- **Connection Pooling**: Efficient resource usage
- **Caching**: In-memory stock data caching

### Note on Profiling

Continuous profiling capabilities were initially planned but have been removed from the current implementation due to compatibility issues. The current observability stack focuses on metrics, traces, and logs which provide comprehensive monitoring capabilities.

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Write tests for new functionality
4. Ensure all tests pass: `make test`
5. Update documentation if needed
6. Submit a pull request

## 📄 License

This project is licensed under the MIT License.

## 🔗 Additional Resources

- [Architecture Documentation](docs/ARCHITECTURE.md)
- [API Documentation](docs/API.md)
- [Getting Started Guide](docs/GETTING_STARTED.md)
- [Testing Guide](docs/TESTING.md)
- [Deployment Guide](docs/DEPLOYMENT.md)
- [Monitoring Guide](docs/MONITORING.md)

---

**Built with ❤️ using Spring Boot 3, Apache Pekko, and modern observability practices**
