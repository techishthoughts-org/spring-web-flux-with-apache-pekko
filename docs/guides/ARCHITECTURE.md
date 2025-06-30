# Stocks Demo - Hexagonal Architecture

This project demonstrates a Clean Architecture implementation using Hexagonal
Architecture (Ports & Adapters) principles with Spring Boot, Pekko Actors, and
reactive programming.

## Architecture Overview

The application follows the Hexagonal Architecture pattern, which promotes
separation of concerns, testability, and maintainability by organizing code into
layers with clear dependencies.

```text
┌─────────────────────────────────────────────────────────────┐
│                     Hexagonal Architecture                  │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌─────────────────┐    ┌─────────────────┐                │
│  │   Inbound       │    │   Outbound      │                │
│  │   Adapters      │    │   Adapters      │                │
│  └─────────────────┘    └─────────────────┘                │
│           │                       │                        │
│           ▼                       ▼                        │
│  ┌─────────────────────────────────────────────────────────┐│
│  │              Application Layer                          ││
│  │  ┌─────────────┐    ┌─────────────────────────────────┐ ││
│  │  │   Ports     │    │        Use Cases                │ ││
│  │  │  (In/Out)   │    │     (Business Logic)           │ ││
│  │  └─────────────┘    └─────────────────────────────────┘ ││
│  └─────────────────────────────────────────────────────────┘│
│                             │                              │
│                             ▼                              │
│  ┌─────────────────────────────────────────────────────────┐│
│  │                 Domain Layer                            ││
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────────┐ ││
│  │  │  Entities   │  │Value Objects│  │    Exceptions   │ ││
│  │  └─────────────┘  └─────────────┘  └─────────────────┘ ││
│  └─────────────────────────────────────────────────────────┘│
│                                                             │
│  ┌─────────────────────────────────────────────────────────┐│
│  │              Infrastructure Layer                       ││
│  │  ┌─────────────┐    ┌─────────────────────────────────┐ ││
│  │  │Actor System │    │         Configuration           │ ││
│  │  └─────────────┘    └─────────────────────────────────┘ ││
│  └─────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────┘

```text

## Layers Description

### 🎯 Domain Layer (`src/main/java/com/techishthoughts/stocks/domain/`)

The core business layer containing:

- **Entities**: `Stock.java` - Core business entities
- **Value Objects**: `StockSymbol.java` - Immutable objects representing domain concepts
- **Exceptions**: Domain-specific exceptions for business rule violations

**Key Principles**:

- No dependencies on external frameworks
- Contains business rules and domain logic
- Framework-agnostic and testable

### 🔌 Application Layer (`src/main/java/com/techishthoughts/stocks/application/`)

Contains the application's use cases and port definitions:

#### Ports (`application/port/`)

- **Inbound Ports** (`in/`): `StockService.java` - Application's public interface
- **Outbound Ports** (`out/`): `StockRepository.java`, `StockMarketClient.java` - Dependencies contracts

#### Use Cases

- **`StockUseCases.java`**: Implements business flows and orchestrates domain operations

**Key Principles**:

- Defines application behavior without implementation details
- Depends only on domain layer
- Technology-agnostic interfaces

### 🔧 Adapter Layer (`src/main/java/com/techishthoughts/stocks/adapter/`)

Implements the ports defined in the application layer:

#### Inbound Adapters (`in/`)

- **`web/StockController.java`**: REST API endpoints
- **`web/GlobalExceptionHandler.java`**: Error handling for HTTP responses

#### Outbound Adapters (`out/`)

- **`finnhub/FinnhubClient.java`**: External API integration
- **`persistence/InMemoryStockRepository.java`**: Data persistence
- **`pekko/StockActorAdapter.java`**: Actor system integration
- **`mapper/StockMapper.java`**: MapStruct object mapping

**Key Principles**:

- Translate between external world and application
- Implement port interfaces
- Handle external concerns (HTTP, databases, APIs)

### ⚙️ Infrastructure Layer (`src/main/java/com/techishthoughts/stocks/infrastructure/`)

Framework-specific components and configurations:

- **`PekkoConfig.java`**: Actor system configuration
- **`StockActor.java`**: Actor implementation for managing stock state

**Key Principles**:

- Framework-specific implementations
- Configuration and wiring
- Cross-cutting concerns

## 🎭 Actor Model Integration

### Actor System Architecture

The application integrates Pekko Actors with Spring using a **Bridge Pattern**:

```text
┌─────────────────────────────────────────────────────────────┐
│                    Actor System Integration                 │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  Spring Context          │         Actor System            │
│  ┌─────────────────┐     │     ┌─────────────────────────┐  │
│  │ StockUseCases   │────────→  │   StockActorAdapter     │  │
│  │ (Application)   │     │     │   (Bridge/Adapter)      │  │
│  └─────────────────┘     │     └─────────────────────────┘  │
│                          │                │                │
│                          │                ▼                │
│                          │     ┌─────────────────────────┐  │
│                          │     │      Root Actor         │  │
│                          │     │   (Actor Management)    │  │
│                          │     └─────────────────────────┘  │
│                          │                │                │
│                          │                ▼                │
│                          │     ┌─────────────────────────┐  │
│                          │     │    Stock Actors         │  │
│                          │     │  (One per Symbol)       │  │
│                          │     └─────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘

```text

### Actor Lifecycle and State Management

#### 1. Actor Hierarchy

```text
RootActor (System Management)
    ├── StockActor[AAPL] (Apple Stock State)
    ├── StockActor[MSFT] (Microsoft Stock State)
    ├── StockActor[GOOGL] (Google Stock State)
    └── ... (One actor per stock symbol)

```text

#### 2. Message Flow

```java
// Request Flow: REST → Use Case → Adapter → Actor
REST Request → StockUseCases.getStock() → StockActorAdapter.askStockActor()
    → RootActor → StockActor → Response

// Response Flow: Actor → Adapter → Use Case → REST
StockActor.Response → StockActorAdapter.Mono → StockUseCases.Mono
    → StockController.Mono → REST Response

```text

#### 3. State Isolation

Each Stock Actor maintains:

- Current stock data
- Last update timestamp
- Error state and recovery information
- Isolated state (no shared mutable state)

### MapStruct Integration

The application uses MapStruct to eliminate manual object mapping:

```java
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface StockMapper {
    @Mapping(target = "lastUpdated", expression = "java(Instant.now())")
    Stock toStock(StockDetails stockDetails);

    Stock toBasicStock(String symbol);

    Stock toStock(String symbol, CompanyProfile profile);
}

```text
**Benefits**:

- Compile-time code generation
- Type-safe mapping
- Performance optimization
- Reduced boilerplate code

## Testing Strategy

The application includes comprehensive testing at multiple levels:

### Unit Tests (`src/test/java/**/unit/**`)

```text
unit/
├── domain/
│   └── StockSymbolTest.java          # Domain validation tests
├── application/
│   └── StockUseCasesTest.java        # Business logic tests
└── adapter/
    ├── in/web/
    │   └── StockControllerTest.java  # HTTP adapter tests
    └── out/mapper/
        └── StockMapperTest.java      # MapStruct mapping tests

```text
**Focus**: Individual components in isolation
**Tools**: JUnit 5, Mockito, AssertJ
**Run**: `./mvnw test -Dtest="**/unit/**"`

### Integration Tests (`src/test/java/**/integration/**`)

- **`StockServiceIntegrationTest.java`**: End-to-end API tests with real Spring context
- **Database Integration**: Tests with real repositories
- **External API Integration**: Tests with WireMock

**Focus**: Component interaction and integration points
**Tools**: Spring Boot Test, WebTestClient, Testcontainers
**Run**: `./mvnw test -Dtest="**/integration/**"`

### Component Tests (`src/test/java/**/component/**`)

- **Behavior-driven tests**: User scenarios and business workflows
- **Cross-layer testing**: Domain through adapters
- **Contract testing**: API contract validation

**Focus**: Business scenarios and user journeys
**Tools**: Cucumber, Spring Boot Test
**Run**: `./mvnw test -Dtest="**/component/**"`

### Smoke Tests (`src/test/java/**/smoke/**`)

- **Application startup**: Verify context loads correctly
- **Basic connectivity**: Health checks and essential endpoints
- **Deployment verification**: Post-deployment validation

**Focus**: Basic application functionality
**Tools**: Spring Boot Test, WebTestClient
**Run**: `./mvnw test -Dtest="**/smoke/**"`

### Sanity Tests (`src/test/java/**/sanity/**`)

- **Core business logic**: Essential functionality verification
- **Data integrity**: Business rule enforcement
- **Error handling**: Graceful failure scenarios

**Focus**: Business logic correctness and system stability
**Tools**: JUnit 5, Spring Boot Test
**Run**: `./mvnw test -Dtest="**/sanity/**"`

### Performance Tests (`src/test/java/**/performance/**`)

- **JMH Benchmarks**: Microbenchmark critical paths
- **Load testing**: System behavior under load
- **Memory profiling**: Resource usage analysis

**Focus**: Performance characteristics and scalability
**Tools**: JMH (Java Microbenchmark Harness)
**Run**: `./mvnw test -Pperformance`

### Mutation Tests

- **Code quality**: Test effectiveness measurement
- **Coverage analysis**: Identify untested code paths
- **Logic verification**: Ensure tests catch logic errors

**Focus**: Test quality and coverage effectiveness
**Tools**: PITest
**Run**: `./mvnw test -Pmutation`

## Key Design Decisions

### 1. **Reactive Programming**

- Uses Project Reactor (`Mono`/`Flux`) for non-blocking operations
- Handles backpressure and provides better resource utilization
- Seamless integration with Actor ask patterns

### 2. **Actor Model with Pekko**

- Manages state through actors for concurrent access
- Provides isolation and fault tolerance
- Scales horizontally
- Integrated with Spring through adapter pattern

### 3. **Immutable Data Structures**

- Uses Java Records for DTOs and Value Objects
- Ensures thread safety and prevents accidental mutations
- Actor messages are immutable for safe concurrent access

### 4. **Validation Strategy**

- Domain objects self-validate (e.g., `StockSymbol`)
- Fail-fast approach with meaningful error messages
- Input validation at adapter boundaries

### 5. **Error Handling**

- Domain-specific exceptions
- Global exception handler for consistent API responses
- Actor supervision strategies for fault tolerance
- Proper logging and monitoring

### 6. **MapStruct for Object Mapping**

- Compile-time code generation
- Eliminates manual mapping boilerplate
- Type-safe transformations
- Performance optimized

## Dependency Flow

```text
External World → Adapters → Application → Domain
              ←           ←              ←

```text
**Dependencies always point inward:**

- Adapters depend on Application ports
- Application depends on Domain
- Domain has no external dependencies
- Infrastructure provides technical capabilities

## Best Practices Implemented

### ✅ **SOLID Principles**

- **Single Responsibility**: Each class has one reason to change
- **Open/Closed**: Extensible without modification
- **Liskov Substitution**: Proper interface implementations
- **Interface Segregation**: Focused, cohesive interfaces
- **Dependency Inversion**: Depend on abstractions, not concretions

### ✅ **Clean Code**

- Meaningful names and small methods
- Comprehensive test coverage
- Clear separation of concerns
- Consistent error handling

### ✅ **Reactive Best Practices**

- Non-blocking I/O operations
- Proper error handling with operators
- Resource management and backpressure
- Integration with Actor model

### ✅ **Actor Model Best Practices**

- Immutable messages
- State isolation
- Proper supervision strategies
- Resource cleanup

### ✅ **Security**

- Input validation at boundaries
- Proper error message handling
- No sensitive data exposure

## Running the Application

1. **Start the application**:

   ```bash
   ./mvnw spring-boot:run
   ```

1. **Run tests**:

   ```bash
   # All tests
   ./mvnw test

   # Specific test categories
   ./mvnw test -Dtest="**/unit/**"
   ./mvnw test -Dtest="**/integration/**"
   ./mvnw test -Dtest="**/component/**"
   ./mvnw test -Dtest="**/smoke/**"
   ./mvnw test -Dtest="**/sanity/**"

   # Performance tests
   ./mvnw test -Pperformance

   # Mutation tests
   ./mvnw test -Pmutation

   # All test profiles
   ./mvnw test -Pall-tests

   # CI profile (excludes performance)
   ./mvnw test -Pci
   ```

1. **Access endpoints**:

   - `GET /stocks` - Get all stocks
   - `GET /stocks/{symbol}` - Get specific stock
   - `GET /actuator/health` - Health check
   - `GET /actuator/prometheus` - Metrics

## Future Enhancements

- [ ] Add caching layer with Redis
- [ ] Implement OpenAPI documentation
- [ ] Add metrics and monitoring
- [ ] Implement circuit breaker pattern
- [ ] Add database persistence
- [ ] Implement event sourcing
- [ ] Add security with OAuth2/JWT
- [ ] Add more actor types (OrderBook, Portfolio, etc.)
- [ ] Implement actor clustering for scalability
