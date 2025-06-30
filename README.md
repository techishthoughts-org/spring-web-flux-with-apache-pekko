# Spring WebFlux with Apache Pekko Integration :rocket:

A demonstration of **Spring WebFlux** with **Apache Pekko Actors** integration,
showcasing how to leverage the **Actor Model** for building reactive,
fault-tolerant applications.

This project consumes the **Finnhub Stocks API** to demonstrate real-world
actor-based patterns in a
**Spring Boot 3** environment with **comprehensive observability** using
OpenTelemetry, Grafana, Prometheus, and Jaeger.

**🎯 Primary Goal**: Demonstrate how the Actor Model can be effectively
integrated with Spring WebFlux to handle concurrent stock data processing with
fault tolerance, scalability, and full observability.

**🏗️ Architecture**: Hexagonal Architecture with Ports & Adapters pattern,
showcasing clean separation between business logic and external integrations.

## 🚀 Quick Start

### Prerequisites

- **Java 21+**
- **Maven 3.8+**
- **Docker or Podman** (with docker-compose or podman-compose)
- **Act** (for local GitHub Actions testing)

### Run the Application

```bash

# Clone and run

git clone <repository-url>
cd stocks-demo

# Start the complete observability stack (recommended)

make stack-observability

# Or run just the application

./mvnw spring-boot:run

# Test the Actor Model integration

curl <http://localhost:8080/stocks/AAPL>    # Single stock via actor
curl <http://localhost:8080/stocks>         # Multiple stocks concurrently
curl <http://localhost:8081/actuator/health> # Health check (management port)
```

### 🔍 **Access the Observability Stack**

```bash

# View all available URLs

make urls

# Key endpoints:


# Application:     <http://localhost:8080>


# Health Check:    <http://localhost:8081/actuator/health>


# Grafana:         <http://localhost:3000> (admin/admin)


# Prometheus:      <http://localhost:9090>


# Jaeger:          <http://localhost:16686>


# Loki:            <http://localhost:3100>
```

### 🧪 **See the Actor Model in Action**

```bash

# Multiple concurrent requests to demonstrate actor concurrency

curl <http://localhost:8080/stocks/AAPL> &
curl <http://localhost:8080/stocks/GOOGL> &
curl <http://localhost:8080/stocks/MSFT> &
curl <http://localhost:8080/stocks/TSLA> &
wait

# View distributed traces in Jaeger

open <http://localhost:16686>

# Monitor metrics in Prometheus

open <http://localhost:9090>

# View logs in Grafana/Loki

open <http://localhost:3000>
```

## 🏗️ Architecture Highlights

**🎯 Focus**: Demonstrating Actor Model integration with Spring WebFlux through
real-world Finnhub API consumption with full observability

- **🎭 Actor Model Integration** - Apache Pekko Actors handle concurrent Finnhub API calls with fault tolerance
- **🌊 Reactive Programming** - Spring WebFlux provides non-blocking request handling
- **🔌 Hexagonal Architecture** - Clean separation between business logic and external API integration
- **🔄 Ask Pattern Bridge** - Seamless integration between WebFlux Mono/Flux and Actor messaging
- **📊 Structured Logging** - Track request flow from HTTP → Actor → Finnhub API
- **🔍 Distributed Tracing** - OpenTelemetry with Jaeger for end-to-end request tracing
- **📈 Comprehensive Monitoring** - Prometheus metrics, Grafana dashboards, Loki logs
- **🧪 Comprehensive Testing** - Unit, Integration, Component (BDD), Performance, Mutation tests

## 📚 Documentation & Guides

Complete documentation organized by category for developers, architects, and
DevOps teams:

### 👨‍💻 **Development**

| Document | Description |
|----------|-------------|
| **[🚀 Getting Started](./docs/development/GETTING_STARTED.md)** | Environment setup, development workflow, adding new features |
| **[🧪 Testing Strategy](./docs/development/TESTING.md)** | Multi-level testing approach with examples |

### 🏗️ **Technical Guides**

| Document | Description |
|----------|-------------|
| **[🏛️ Architecture](./docs/guides/ARCHITECTURE.md)** | Hexagonal architecture, actor patterns, design decisions |
| **[📊 Structured Logging](./docs/guides/STRUCTURED_LOGGING.md)** | Request tracing, observability patterns |

### 🚀 **Operations**

| Document | Description |
|----------|-------------|
| **[🚀 Deployment](./docs/operations/DEPLOYMENT.md)** | Production deployment, Kubernetes, configuration |
| **[🐳 Docker](./docs/operations/DOCKER.md)** | Container setup, Docker Compose, optimization |
| **[⚙️ CI/CD](./docs/operations/CICD.md)** | GitHub Actions, automated releases, pipeline |
| **[🤖 Dependabot Setup](./docs/operations/DEPENDABOT_SETUP.md)** | Automated dependency management |

## ⚡ Key Features

### 🎭 **Actor Model with Finnhub API Integration**

Demonstrates how Apache Pekko Actors handle concurrent stock data requests from
the Finnhub API:

```java
// Reactive bridge between Spring WebFlux and Pekko Actors
@Service
public class StockService {

    public Mono<Stock> getStock(String symbol) {
        return Mono.defer(() -> {
            // Ask pattern: WebFlux → Actor System
            CompletionStage<ActorRef<StockActor.Command>> actorStage =
                AskPattern.ask(actorSystem, replyTo ->
                    new GetStockActorCommand(symbol, replyTo));
            return Mono.fromCompletionStage(actorStage)
                .flatMap(this::askForStock);
        });
    }
}

// Actor handles Finnhub API calls with fault tolerance
public class StockActor extends AbstractBehavior<StockActor.Command> {

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
            .onMessage(GetStock.class, this::onGetStock)
            .build();
    }

    private Behavior<Command> onGetStock(GetStock command) {
        // Concurrent, fault-tolerant API calls to Finnhub
        finnhubClient.getStock(command.symbol())
            .whenComplete((stock, throwable) -> {
                if (throwable != null) {
                    // Actor supervision handles failures
command.replyTo().tell(new StockError(throwable.getMessage()));
                } else {
                    command.replyTo().tell(new StockResponse(stock));
                }
            });
        return this;
    }
}
```

**🔥 Actor Model Benefits Demonstrated:**

- **Concurrency**: Handle multiple stock requests simultaneously
- **Fault Tolerance**: Supervisor strategies for API failures
- **Isolation**: Actor state isolation prevents shared state issues
- **Scalability**: Lightweight actors scale to thousands of concurrent requests

### 🔍 Structured Logging with Route Steps

```java
// Automatic request context tracking
LoggingContext.initialize(requestId, "GET", "/stocks/AAPL");
LoggingContext.addMetadata("stockSymbol", "AAPL");

// Track HTTP calls and actor operations
RouteStep httpStep = RouteStep.httpOut("finnhub-api", "GET", url);
LoggingContext.addRouteStep(httpStep);

// Complete request with full trace
LoggingContext.complete(200, responseBody);
```

### 🏗️ Hexagonal Architecture

```text
┌─────────────────────────────────────────┐
│              Web Layer                  │
│    (REST Controllers & Filters)         │
├─────────────────────────────────────────┤
│           Application Layer             │
│     (Use Cases & Port Definitions)      │
├─────────────────────────────────────────┤
│            Domain Layer                 │
│    (Entities, Value Objects, Rules)     │
├─────────────────────────────────────────┤
│          Infrastructure Layer           │
│  (Actors, External APIs, Persistence)   │
└─────────────────────────────────────────┘
```

## 🧪 Testing

Multi-level testing strategy with comprehensive coverage:

```bash

# All test categories

./mvnw test -Pall-tests

# Individual categories

./mvnw test -Dtest="**/unit/**"        # Unit tests
./mvnw test -Dtest="**/integration/**" # Integration tests
./mvnw test -Dtest="**/component/**"   # BDD scenarios
./mvnw test -Pperformance              # JMH benchmarks
./mvnw test -Pmutation                 # PITest quality

# Local GitHub Actions testing

./scripts/act-helper.sh unit           # Test unit tests workflow
./scripts/act-helper.sh integration    # Test integration workflow
./scripts/act-helper.sh pr             # Test PR validation
npm run act:list                       # List all available workflows
```

## 🚀 Deployment

Ready for production with multiple deployment options:

```bash

# Docker/Podman with full observability stack

make stack-observability

# Individual services

make docker-build
make docker-run

# Kubernetes

kubectl apply -f k8s/

# Environment management

make env-check                         # Check prerequisites
make urls                             # Show all service URLs
make docker-logs                      # View application logs
make docker-stop                      # Stop all services
```

## 🔍 Observability & Monitoring

**Enterprise-grade observability with comprehensive monitoring and distributed
tracing:**

### 📊 **Complete Observability Stack**

- **✅ OpenTelemetry**: Distributed tracing with service name `stocks-service`
- **✅ Jaeger**: Trace visualization and analysis
- **✅ Prometheus**: Metrics collection and alerting
- **✅ Grafana**: Unified dashboards and visualization
- **✅ Loki**: Centralized log aggregation
- **✅ cAdvisor**: Container resource monitoring
- **✅ Node Exporter**: System metrics collection

### 🔍 **Distributed Tracing**

```yaml

# OpenTelemetry Configuration (Verified Working)

OTEL_EXPORTER_OTLP_ENDPOINT: <http://otel-collector:4318>
OTEL_EXPORTER_OTLP_PROTOCOL: http/protobuf
OTEL_SERVICE_NAME: stocks-service
OTEL_RESOURCE_ATTRIBUTES:
service.name=stocks-service,service.version=1.0.0,deployment.environment=docker
```

**🎯 Tracing Features:**

- **Service Name**: `stocks-service` (properly configured)
- **Span Context**: Cross-service trace correlation
- **Actor Integration**: Trace continuity through actor system
- **External API Tracking**: Finnhub API call monitoring
- **HTTP/gRPC Support**: Multiple protocol tracing

### 📈 **Metrics & Health Checks**

- **Spring Actuator**: Health endpoints, metrics, and application insights
- **Micrometer**: Application metrics collection and export
- **Custom Metrics**: Business-specific measurements and KPIs
- **Liveness/Readiness**: Kubernetes-ready health probes

### 📊 **Structured Logging**

- **InheritableThreadLocal**: Context preservation across async operations
- **Route Step Tracking**: End-to-end request tracing with detailed steps
- **JSON Format**: Machine-readable logs for analysis
- **Correlation IDs**: Request correlation across service boundaries

## 🎭 Apache Pekko Integration

Leveraging the power of the Actor Model for concurrent, fault-tolerant
processing:

### 🏗️ **Actor System Architecture**

```java
// Reactive bridge between Spring WebFlux and Pekko Actors
@Service
public class StockService {

    private final ActorSystem<Void> actorSystem;

    public Mono<Stock> getStock(String symbol) {
        return Mono.defer(() -> {
            CompletionStage<ActorRef<StockActor.Command>> actorStage =
                AskPattern.ask(actorSystem, replyTo ->
                    new GetStockActorCommand(symbol, replyTo));
            return Mono.fromCompletionStage(actorStage)
                .flatMap(this::askForStock);
        });
    }
}
```

### ⚡ **Fault Tolerance Features**

- **Supervision Strategy**: Automatic recovery from failures
- **Circuit Breaker**: Protection against cascading failures
- **Backpressure Handling**: Graceful degradation under load
- **State Isolation**: Actor-based state management

### 🔄 **Concurrency Model**

- **Message Passing**: Thread-safe actor communication
- **Non-blocking I/O**: Reactive streams integration
- **Parallel Processing**: Concurrent request handling
- **Resource Management**: Efficient thread pool utilization

## 🏛️ Spring Boot 3 Modern Features

Built on the latest Spring Boot with cutting-edge Java capabilities:

### ☕ **Java 21 Integration**

- **ZGC Garbage Collector**: Ultra-low latency GC (optimized configuration)
- **Virtual Threads**: Lightweight concurrency (Project Loom)
- **Pattern Matching**: Modern language features
- **Record Classes**: Immutable data structures
- **Text Blocks**: Improved string handling

### 🌐 **Reactive Web Stack**

- **Spring WebFlux**: Non-blocking web layer
- **Reactive Streams**: Backpressure-aware processing
- **Functional Endpoints**: Declarative request handling
- **WebClient**: Reactive HTTP client integration

### 🔧 **Configuration & Dependency Injection**

- **Native Configuration**: GraalVM ready
- **Conditional Beans**: Environment-aware components
- **Property Binding**: Type-safe configuration
- **Profile Management**: Environment-specific setups

## 🛠️ Technology Stack

### Core Framework

- **Spring Boot 3** - Modern Java framework with reactive support
- **Spring WebFlux** - Reactive web programming
- **Pekko Actors** - Fault-tolerant actor system (Akka successor)

### Data & Integration

- **MapStruct 1.5.5** - Compile-time object mapping
- **Finnhub API** - Real-time financial data
- **Jackson** - JSON processing

### Testing & Quality

- **JUnit 5** - Modern testing framework
- **Cucumber 7** - BDD testing with Gherkin
- **Testcontainers** - Integration testing with Docker
- **JMH** - Performance benchmarking
- **PITest** - Mutation testing
- **WireMock** - API mocking

### Observability Stack

- **OpenTelemetry** - Distributed tracing (✅ Verified working with service name `stocks-service`)
- **Jaeger** - Trace visualization and analysis
- **Prometheus** - Metrics collection and alerting
- **Grafana** - Unified dashboards and visualization
- **Loki** - Centralized log aggregation
- **OTEL Collector** - Telemetry data processing and routing
- **Spring Actuator** - Health checks and metrics
- **Micrometer** - Metrics collection
- **Structured Logging** - Custom InheritableThreadLocal implementation

### Container Runtime Support

- **Docker** - Traditional container runtime
- **Podman** - Rootless container alternative (✅ Fully supported)
- **Auto-detection** - Makefile automatically detects available runtime

## 🐳 Container & Infrastructure

### **Multi-Runtime Support**

The application supports both Docker and Podman with automatic detection:

```bash

# Automatic runtime detection

make env-check                    # Shows detected container runtime
make stack-observability         # Works with Docker or Podman

# Manual runtime selection (if needed)

CONTAINER_RUNTIME=podman make stack-observability
COMPOSE_CMD=podman-compose make stack-observability
```

### **Performance Optimizations**

- **ZGC Garbage Collector**: Ultra-low latency configuration
- **JVM Tuning**: Optimized for containerized environments
- **Memory Management**: 80% MaxRAMPercentage with pre-touching
- **Hardware Intrinsics**: AES, SHA, CRC optimizations enabled
- **Code Cache**: 256MB reserved for optimal performance

## 📊 Project Stats

- **Architecture**: Hexagonal (Ports & Adapters)
- **Test Coverage**: >85% line coverage
- **Test Categories**: 7 distinct test types
- **Documentation**: 5 comprehensive guides
- **Deployment Options**: Docker, Podman, Kubernetes, Cloud platforms
- **Observability**: Full OpenTelemetry integration with 7 monitoring services
- **Container Support**: Docker + Podman with auto-detection

## 🤝 Contributing

1. **Read the docs**: Start with [Getting Started](./docs/development/GETTING_STARTED.md)
1. **Follow patterns**: Use the [7-step Actor Flow Guide](./docs/development/GETTING_STARTED.md#adding-new-actor-flows)
1. **Test thoroughly**: Follow the [Testing Strategy](./docs/development/TESTING.md)
1. **Document changes**: Update relevant documentation

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for
details.

---

For detailed documentation, architecture deep-dives, and operational guides,
visit **[`/docs`](./docs)** 📚
