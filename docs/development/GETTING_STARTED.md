# Getting Started Guide

Welcome to the Spring Boot + Pekko Actors Stock Service! This guide will help
you set up your development environment and make your first contribution.

## 🚀 Quick Start

### Prerequisites

- **Java 21** or higher
- **Maven 3.8+**
- **Docker** (optional, for containerized deployment)
- **IDE** with Java support (IntelliJ IDEA, Eclipse, VS Code)

### Initial Setup

```bash

# Clone the repository

git clone https://github.com/techishthoughts-org/spring-web-flux-with-apache-pekko.git
cd stocks-demo

# Verify Java version

java --version

# Run tests to verify setup

./mvnw test

# Run the application

./mvnw spring-boot:run

```text

### Verify Installation

```bash

# Health check

curl <http://localhost:8080/actuator/health>

# Get all stocks

curl <http://localhost:8080/stocks>

# Get specific stock

curl <http://localhost:8080/stocks/AAPL>

# Expected response

{
  "symbol": "AAPL",
  "name": "Apple Inc.",
  "exchange": "NASDAQ",
  "lastUpdated": "2024-01-15T10:30:15.123Z"
}

```text

## 🏗️ Development Setup

### IDE Configuration

#### IntelliJ IDEA

1. **Import Project**: Open as Maven project
1. **Java Version**: Set Project SDK to Java 21
1. **Annotation Processing**: Enable for MapStruct

   - Settings → Build → Compiler → Annotation Processors
   - Check "Enable annotation processing"
1. **Lombok Plugin**: Install if using Lombok annotations

#### VS Code

1. **Extensions**: Install Java Extension Pack
1. **Settings**: Configure Java 21 as default
1. **Maven**: Ensure Maven for Java extension is active

### Environment Variables

Create `.env` file in project root:

```bash

# API Keys

FINNHUB_KEY=your_finnhub_api_key
ALPHA_VANTAGE_KEY=your_alpha_vantage_key

# Application Settings

SPRING_PROFILES_ACTIVE=dev
LOGGING_LEVEL_COM_TECHISHTHOUGHTS_STOCKS=DEBUG

# Actor System Configuration

PEKKO_ACTOR_TIMEOUT=5s
PEKKO_ACTOR_DISPATCHER_PARALLELISM=4

```text

### Development Profiles

#### Application Profiles

- **`default`**: Production-like settings
- **`dev`**: Development with enhanced logging
- **`test`**: Test environment configuration

```bash

# Run with development profile

./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# Run tests with specific profile

./mvnw test -Dspring.profiles.active=test

```text

## 🔄 Adding New Actor Flows

Follow this comprehensive guide to add new business functionality using the
Actor Model:

### Step 1: Define Domain Model

Create domain entities in the `domain` package:

```java
// src/main/java/com/techishthoughts/stocks/domain/OrderBook.java
package com.techishthoughts.stocks.domain;

import java.time.Instant;
import java.util.List;

public record OrderBook(
    String symbol,
    List<Order> bids,
    List<Order> asks,
    Instant lastUpdated
) {
    public OrderBook {
        // Validation logic
        if (symbol == null || symbol.isEmpty()) {
throw new IllegalArgumentException("Symbol cannot be null or empty");
        }
    }
}

public record Order(
    String id,
    String symbol,
    OrderType type,
    double price,
    double quantity,
    Instant timestamp
) {}

public enum OrderType {
    BUY, SELL
}

```text

### Step 2: Create Actor Commands and Responses

Define message protocols for actor communication:

```java
// src/main/java/com/techishthoughts/stocks/infrastructure/OrderBookActor.java
public class OrderBookActor extends AbstractBehavior<OrderBookActor.Command> {

    // Command interface
    public sealed interface Command {}

    // Commands
public record GetOrderBook(ActorRef<OrderBookResponse> replyTo) implements
Command {}
    public record UpdateOrderBook(OrderBook orderBook) implements Command {}
public record AddOrder(Order order, ActorRef<OrderResponse> replyTo) implements
Command {}

    // Responses
    public record OrderBookResponse(OrderBook orderBook) {}
    public record OrderResponse(boolean success, String message) {}

    // Implementation...
}

```text

### Step 3: Implement Actor Behavior

```java
public class OrderBookActor extends AbstractBehavior<OrderBookActor.Command> {
private static final Logger log = LoggerFactory.getLogger(OrderBookActor.class);

    private final String symbol;
    private OrderBook currentOrderBook;

    private OrderBookActor(ActorContext<Command> context, String symbol) {
        super(context);
        this.symbol = symbol;
this.currentOrderBook = new OrderBook(symbol, List.of(), List.of(),
Instant.now());
        log.info("OrderBookActor created for symbol: {}", symbol);
    }

    public static Behavior<Command> create(String symbol) {
        return Behaviors.setup(context -> new OrderBookActor(context, symbol));
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
            .onMessage(GetOrderBook.class, this::onGetOrderBook)
            .onMessage(UpdateOrderBook.class, this::onUpdateOrderBook)
            .onMessage(AddOrder.class, this::onAddOrder)
            .build();
    }

    private Behavior<Command> onGetOrderBook(GetOrderBook command) {
        // Add structured logging
RouteStep actorStep = RouteStep.actorCall("orderbook-get", "OrderBookActor");
        LoggingContext.addRouteStep(actorStep);

        command.replyTo().tell(new OrderBookResponse(currentOrderBook));
        actorStep.complete(200, "Retrieved order book for " + symbol);

        return this;
    }

    private Behavior<Command> onUpdateOrderBook(UpdateOrderBook command) {
        this.currentOrderBook = command.orderBook();
        log.info("Updated order book for symbol: {}", symbol);
        return this;
    }

    private Behavior<Command> onAddOrder(AddOrder command) {
        // Business logic for adding order
        Order order = command.order();

        // Validate order
        if (!order.symbol().equals(symbol)) {
            command.replyTo().tell(new OrderResponse(false, "Symbol mismatch"));
            return this;
        }

        // Add to order book (simplified)
        List<Order> newBids = new ArrayList<>(currentOrderBook.bids());
        List<Order> newAsks = new ArrayList<>(currentOrderBook.asks());

        if (order.type() == OrderType.BUY) {
            newBids.add(order);
        } else {
            newAsks.add(order);
        }

        this.currentOrderBook = new OrderBook(
            symbol,
            newBids,
            newAsks,
            Instant.now()
        );

command.replyTo().tell(new OrderResponse(true, "Order added successfully"));
log.info("Added order {} to order book for symbol: {}", order.id(), symbol);

        return this;
    }
}

```text

### Step 4: Add to Root Actor Management

Update the `PekkoConfig` to manage the new actors:

```java
// src/main/java/com/techishthoughts/stocks/infrastructure/PekkoConfig.java
public static class RootBehavior extends AbstractBehavior<Command> {
private final Map<String, ActorRef<StockActor.Command>> stockActors = new
HashMap<>();
private final Map<String, ActorRef<OrderBookActor.Command>> orderBookActors =
new HashMap<>(); // Add this

    // Add method to get or create OrderBook actors
private ActorRef<OrderBookActor.Command> getOrCreateOrderBookActor(String
symbol) {
        return orderBookActors.computeIfAbsent(symbol, s ->
            getContext().spawn(OrderBookActor.create(s), "orderbook-" + s));
    }

    // Add command handling for OrderBook operations
public record GetOrderBookActorCommand(String symbol,
ActorRef<ActorRef<OrderBookActor.Command>> replyTo) implements Command {}

private Behavior<Command> onGetOrderBookActor(GetOrderBookActorCommand command)
{
ActorRef<OrderBookActor.Command> actor =
getOrCreateOrderBookActor(command.symbol());
        command.replyTo().tell(actor);
        return this;
    }
}

```text

### Step 5: Create Adapter Bridge

Create adapter to bridge reactive world with actor system:

```java
//
src/main/java/com/techishthoughts/stocks/adapter/out/pekko/OrderBookActorAdapter.java
package com.techishthoughts.stocks.adapter.out.pekko;

import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.ActorSystem;
import org.apache.pekko.actor.typed.Scheduler;
import org.apache.pekko.actor.typed.javadsl.AskPattern;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import com.techishthoughts.stocks.domain.OrderBook;
import com.techishthoughts.stocks.infrastructure.OrderBookActor;
import com.techishthoughts.stocks.infrastructure.PekkoConfig;

import java.time.Duration;

@Component
public class OrderBookActorAdapter {

    private static final Duration ASK_TIMEOUT = Duration.ofSeconds(5);

    private final ActorSystem<PekkoConfig.Command> actorSystem;
    private final Scheduler scheduler;

    public OrderBookActorAdapter(ActorSystem<PekkoConfig.Command> actorSystem) {
        this.actorSystem = actorSystem;
        this.scheduler = actorSystem.scheduler();
    }

    public Mono<OrderBook> getOrderBook(String symbol) {
        return Mono.defer(() -> {
            // First get the actor reference
            CompletionStage<ActorRef<OrderBookActor.Command>> actorStage =
                AskPattern.ask(
                    actorSystem,
replyTo -> new PekkoConfig.GetOrderBookActorCommand(symbol, replyTo),
                    ASK_TIMEOUT,
                    scheduler
                );

            // Then ask the actor for the order book
            return Mono.fromCompletionStage(actorStage)
                .flatMap(actor -> {
CompletionStage<OrderBookActor.OrderBookResponse> responseStage =
                        AskPattern.ask(
                            actor,
                            OrderBookActor.GetOrderBook::new,
                            ASK_TIMEOUT,
                            scheduler
                        );

                    return Mono.fromCompletionStage(responseStage)
                        .map(OrderBookActor.OrderBookResponse::orderBook);
                });
        }).subscribeOn(Schedulers.boundedElastic());
    }
}

```text

### Step 6: Add Application Service

Create use case implementation:

```java
//
src/main/java/com/techishthoughts/stocks/application/port/in/OrderBookService.java
package com.techishthoughts.stocks.application.port.in;

import com.techishthoughts.stocks.domain.OrderBook;
import reactor.core.publisher.Mono;

public interface OrderBookService {
    Mono<OrderBook> getOrderBook(String symbol);
}

// src/main/java/com/techishthoughts/stocks/application/OrderBookUseCases.java
package com.techishthoughts.stocks.application;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import com.techishthoughts.stocks.adapter.out.pekko.OrderBookActorAdapter;
import com.techishthoughts.stocks.application.port.in.OrderBookService;
import com.techishthoughts.stocks.domain.OrderBook;
import com.techishthoughts.stocks.infrastructure.logging.LoggingContext;

@Service
public class OrderBookUseCases implements OrderBookService {

    private final OrderBookActorAdapter adapter;

    public OrderBookUseCases(OrderBookActorAdapter adapter) {
        this.adapter = adapter;
    }

    @Override
    public Mono<OrderBook> getOrderBook(String symbol) {
LoggingContext.logWithContext("Fetching order book for symbol: %s", symbol);
        LoggingContext.addMetadata("operation", "getOrderBook");
        LoggingContext.addMetadata("symbol", symbol);

        return adapter.getOrderBook(symbol)
            .doOnNext(orderBook -> {
                LoggingContext.addMetadata("orderBookFound", true);
                LoggingContext.addMetadata("bidCount", orderBook.bids().size());
                LoggingContext.addMetadata("askCount", orderBook.asks().size());
            })
            .doOnError(error -> {
                LoggingContext.addMetadata("orderBookFound", false);
LoggingContext.addMetadata("errorType", error.getClass().getSimpleName());
            });
    }
}

```text

### Step 7: Add REST Endpoint

Create controller for REST API:

```java
//
src/main/java/com/techishthoughts/stocks/adapter/in/web/OrderBookController.java
package com.techishthoughts.stocks.adapter.in.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import com.techishthoughts.stocks.application.port.in.OrderBookService;
import com.techishthoughts.stocks.domain.OrderBook;
import com.techishthoughts.stocks.infrastructure.logging.LoggingContext;

@RestController
@RequestMapping("/orderbooks")
public class OrderBookController {

    private final OrderBookService service;

    public OrderBookController(OrderBookService service) {
        this.service = service;
    }

    @GetMapping("/{symbol}")
    public Mono<OrderBook> getOrderBook(@PathVariable String symbol) {
        String upperSymbol = symbol.toUpperCase();

LoggingContext.logWithContext("REST request for order book: %s", upperSymbol);
        LoggingContext.addMetadata("endpoint", "getOrderBook");
        LoggingContext.addMetadata("requestedSymbol", symbol);
        LoggingContext.addMetadata("normalizedSymbol", upperSymbol);

        return service.getOrderBook(upperSymbol)
            .doOnNext(orderBook -> {
LoggingContext.logWithContext("Successfully retrieved order book for: %s",
upperSymbol);
            });
    }
}

```text

## 🧪 Development Workflow

### Daily Development

```bash

# Start development session

./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# Run tests continuously

./mvnw test -Dtest="**/unit/**" --watch

# Run specific test category

./mvnw test -Dtest="**/integration/**"

```text

### Testing Your Changes

```bash

# Run all tests

./mvnw test

# Run specific test categories

./mvnw test -Dtest="**/unit/**"        # Unit tests
./mvnw test -Dtest="**/integration/**" # Integration tests
./mvnw test -Dtest="**/component/**"   # Component tests

# Run performance tests

./mvnw test -Pperformance

# Run mutation tests

./mvnw test -Pmutation

```text

### Code Quality Checks

```bash

# Compile and verify

./mvnw clean compile

# Run static analysis (if configured)

./mvnw verify

# Generate test coverage

./mvnw jacoco:report

```text

## 🔧 Troubleshooting

### Common Issues

#### 1. Port Already in Use

```bash

# Find process using port 8080

lsof -i :8080

# Kill the process

kill -9 <PID>

```text

#### 2. Maven Build Issues

```bash

# Clean and reinstall dependencies

./mvnw clean install -U

# Skip tests if needed

./mvnw clean install -DskipTests

```text

#### 3. Actor System Not Starting

- Check Java version (must be 21+)
- Verify Pekko configuration in `application.yml`
- Review logs for actor system initialization errors

#### 4. External API Issues

- Verify API keys in environment variables
- Check network connectivity
- Review rate limiting configuration

### Debug Configuration

Add to `application-dev.yml`:

```yaml
logging:
  level:
    com.techishthoughts.stocks: DEBUG
    org.apache.pekko: INFO
    org.springframework.web: DEBUG
    reactor.core: DEBUG

management:
  endpoints:
    web:
      exposure:
        include: "*"
  endpoint:
    health:
      show-details: always

```text

## 📚 Next Steps

1. **Explore Architecture**: Read [ARCHITECTURE.md](./ARCHITECTURE.md) for deep
dive
1. **Study Testing**: Review [TESTING.md](./TESTING.md) for testing strategies
1. **Learn Logging**: Check [STRUCTURED_LOGGING.md](./STRUCTURED_LOGGING.md) for
observability
1. **Production Ready**: See [DEPLOYMENT.md](./DEPLOYMENT.md) for deployment
strategies

## 🤝 Contributing

1. **Fork** the repository
1. **Create** a feature branch (`git checkout -b feature/new-feature`)
1. **Follow** the 7-step actor flow guide above
1. **Test** your changes thoroughly
1. **Document** new features and APIs
1. **Submit** a pull request

Ready to build something amazing with actors and reactive programming! 🚀
