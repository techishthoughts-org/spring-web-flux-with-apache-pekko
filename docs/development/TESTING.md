# Testing Strategy & Guide

This document outlines the comprehensive testing strategy for the Spring Boot +
Pekko Actors Stock Service, covering all test levels from unit to performance
testing.

## 🎯 Testing Philosophy

Our testing strategy follows the **Test Pyramid** principle with multiple
specialized test categories:

```text
                    ▲
                   / \
              E2E /   \ Manual
                 /     \
            Smoke/-------\Performance
           Component/-----\Mutation
          Integration/-----\Sanity
             Unit/----------\BDD
                /____________\
               Comprehensive Coverage

```text

## 📊 Test Categories Overview

| Test Type | Purpose | Speed | Coverage | Tools |
|-----------|---------|-------|----------|-------|
| **Unit** | Individual component logic | ⚡ Fast | High | JUnit 5, Mockito |
| **Integration** | Component interactions | 🚶 Medium | Medium | Spring Test, TestContainers |
| **Component** | Business scenarios (BDD) | 🚶 Medium | Business Logic | Cucumber, Gherkin |
| **Smoke** | Basic functionality | ⚡ Fast | Critical Paths | Spring Boot Test |
| **Sanity** | Core business rules | ⚡ Fast | Business Logic | Custom Assertions |
| **Performance** | System performance | 🐌 Slow | Bottlenecks | JMH Benchmarks |
| **Mutation** | Test quality | 🐌 Slow | Test Effectiveness | PITest |

## 🧪 Test Structure

### Directory Organization

```text
src/test/java/
├── unit/                          # Fast, isolated tests
│   ├── domain/                    # Domain logic tests
│   ├── application/               # Use case tests
│   ├── adapter/                   # Adapter tests
│   └── infrastructure/            # Infrastructure tests
├── integration/                   # End-to-end integration tests
│   ├── api/                       # REST API tests
│   ├── external/                  # External service tests
│   └── database/                  # Data persistence tests
├── component/                     # BDD business scenario tests
│   ├── features/                  # Cucumber features
│   ├── steps/                     # Step definitions
│   └── support/                   # Test support utilities
├── smoke/                         # Basic functionality tests
├── sanity/                        # Core business logic tests
└── performance/                   # JMH performance benchmarks

```text

## 📝 Test Categories in Detail

### 1. Unit Tests (`src/test/java/**/unit/**`)

**Purpose**: Test individual components in isolation
**Speed**: ⚡ Very Fast (< 100ms per test)
**Coverage**: High code coverage, focused on business logic

#### Domain Layer Tests

```java
// Example: StockSymbolTest.java
@Test
void shouldValidateValidStockSymbol() {
    // Given
    String validSymbol = "AAPL";

    // When & Then
    assertDoesNotThrow(() -> new StockSymbol(validSymbol));
}

@Test
void shouldRejectInvalidStockSymbol() {
    // Given
    String invalidSymbol = "";

    // When & Then
    InvalidStockSymbolException exception = assertThrows(
        InvalidStockSymbolException.class,
        () -> new StockSymbol(invalidSymbol)
    );
    assertEquals("Stock symbol cannot be empty", exception.getMessage());
}

```text

#### Application Layer Tests

```java
// Example: StockUseCasesTest.java
@ExtendWith(MockitoExtension.class)
class StockUseCasesTest {

    @Mock private StockActorAdapter actorAdapter;
    @Mock private StockRepository repository;

    @InjectMocks private StockUseCases stockUseCases;

    @Test
    void shouldGetStockSuccessfully() {
        // Given
        String symbol = "AAPL";
        Stock expectedStock = new Stock(symbol, Instant.now());
        when(actorAdapter.askStockActor(symbol))
            .thenReturn(Mono.just(expectedStock));

        // When
        Mono<Stock> result = stockUseCases.getStock(symbol);

        // Then
        StepVerifier.create(result)
            .expectNext(expectedStock)
            .verifyComplete();
    }
}

```text

#### Actor Tests

```java
// Example: StockActorTest.java
class StockActorTest {

    @Test
    void shouldRespondToGetStockCommand() {
        // Given
        TestKit<StockActor.Command> testKit = ActorTestKit.create();
        ActorRef<StockActor.Command> stockActor = testKit.spawn(
            StockActor.create("AAPL", mockClient, mockRepository, mockMapper)
        );
        TestProbe<StockActor.Response> probe = testKit.createTestProbe();

        // When
        stockActor.tell(new StockActor.GetStock(probe.getRef()));

        // Then
        StockActor.Response response = probe.receiveMessage();
        assertNotNull(response.stock());
        assertEquals("AAPL", response.stock().symbol());
    }
}

```text
**Run Unit Tests:**

```bash
./mvnw test -Dtest="**/unit/**"

```text

### 2. Integration Tests (`src/test/java/**/integration/**`)

**Purpose**: Test component interactions and external integrations
**Speed**: 🚶 Medium (1-5 seconds per test)
**Coverage**: Integration points, API contracts

#### API Integration Tests

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {"finnhub.api-key=test-key"})
class StockServiceIntegrationTest {

    @Autowired private WebTestClient webTestClient;

    @Test
    void shouldGetStockViaRestApi() {
        webTestClient
            .get()
            .uri("/stocks/AAPL")
            .exchange()
            .expectStatus().isOk()
            .expectBody(Stock.class)
            .value(stock -> {
                assertEquals("AAPL", stock.symbol());
                assertNotNull(stock.lastUpdated());
            });
    }
}

```text

#### External Service Integration

```java
@SpringBootTest
@Testcontainers
class ExternalApiIntegrationTest {

    @Container
static WireMockContainer wiremock = new WireMockContainer("wiremock/wiremock")
        .withMappingFromResource("finnhub-api-mocks.json");

    @Test
    void shouldIntegrateWithFinnhubApi() {
        // Test actual HTTP integration with mocked external service
    }
}

```text
**Run Integration Tests:**

```bash
./mvnw test -Dtest="**/integration/**"

```text

### 3. Component Tests (BDD) (`src/test/java/**/component/**`)

**Purpose**: Business scenario validation using BDD
**Speed**: 🚶 Medium (2-10 seconds per scenario)
**Coverage**: Business requirements and user stories

#### Feature Files

```gherkin

# src/test/resources/features/stock-retrieval.feature

Feature: Stock Information Retrieval
  As a user
  I want to retrieve stock information
  So that I can make informed investment decisions

  Scenario: Retrieve valid stock information
    Given the stock market is accessible
    When I request information for stock "AAPL"
    Then I should receive stock details
    And the stock symbol should be "AAPL"
    And the stock should have a valid last updated timestamp

  Scenario: Handle invalid stock symbol
    Given the stock market is accessible
    When I request information for invalid stock "INVALID"
    Then I should receive an error response
    And the error should indicate the stock was not found

```text

#### Step Definitions

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class StockRetrievalSteps {

    @Autowired private WebTestClient webTestClient;
    private ResponseSpec lastResponse;

    @Given("the stock market is accessible")
    public void theStockMarketIsAccessible() {
        // Verify system health
        webTestClient.get()
            .uri("/actuator/health")
            .exchange()
            .expectStatus().isOk();
    }

    @When("I request information for stock {string}")
    public void iRequestInformationForStock(String symbol) {
        lastResponse = webTestClient.get()
            .uri("/stocks/{symbol}", symbol)
            .exchange();
    }

    @Then("I should receive stock details")
    public void iShouldReceiveStockDetails() {
        lastResponse.expectStatus().isOk()
            .expectBody(Stock.class);
    }
}

```text
**Run Component Tests:**

```bash
./mvnw test -Dtest="**/component/**"

```text

### 4. Smoke Tests (`src/test/java/**/smoke/**`)

**Purpose**: Verify basic application functionality
**Speed**: ⚡ Fast (< 1 second per test)
**Coverage**: Critical application paths

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SmokeTest {

    @Autowired private WebTestClient webTestClient;

    @Test
    void applicationContextLoads() {
        // Verifies Spring context loads successfully
    }

    @Test
    void healthEndpointIsAccessible() {
        webTestClient.get()
            .uri("/actuator/health")
            .exchange()
            .expectStatus().isOk();
    }

    @Test
    void stocksEndpointResponds() {
        webTestClient.get()
            .uri("/stocks")
            .exchange()
            .expectStatus().isOk();
    }
}

```text
**Run Smoke Tests:**

```bash
./mvnw test -Dtest="**/smoke/**"

```text

### 5. Sanity Tests (`src/test/java/**/sanity/**`)

**Purpose**: Verify core business logic integrity
**Speed**: ⚡ Fast (< 2 seconds per test)
**Coverage**: Essential business rules

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class StockServiceSanityTest {

    @Autowired private WebTestClient webTestClient;

    @Test
    void shouldHandleValidStockSymbol() {
        String validSymbol = "AAPL";

        webTestClient.get()
            .uri("/stocks/{symbol}", validSymbol)
            .exchange()
            .expectStatus().isOk()
            .expectBody(Stock.class)
            .value(stock -> {
                assertEquals(validSymbol, stock.symbol());
                assertNotNull(stock.lastUpdated());
            });
    }

    @Test
    void shouldHandleCaseInsensitiveSymbols() {
        webTestClient.get()
            .uri("/stocks/msft")  // lowercase
            .exchange()
            .expectStatus().isOk()
            .expectBody(Stock.class)
            .value(stock -> assertEquals("MSFT", stock.symbol())); // uppercase
    }
}

```text
**Run Sanity Tests:**

```bash
./mvnw test -Dtest="**/sanity/**"

```text

### 6. Performance Tests (`src/test/java/**/performance/**`)

**Purpose**: Measure and validate system performance
**Speed**: 🐌 Slow (10+ seconds per test)
**Coverage**: Performance-critical code paths

#### JMH Benchmarks

```java
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
public class StockServiceBenchmark {

    private StockService stockService;

    @Setup
    public void setup() {
        // Initialize service with test configuration
    }

    @Benchmark
    public Stock benchmarkGetStock() {
        return stockService.getStock("AAPL").block();
    }

    @Benchmark
    public List<Stock> benchmarkGetAllStocks() {
        return stockService.getAllStocks()
            .collectList()
            .block();
    }
}

```text

#### Load Testing

```java
@Test
@Timeout(30)
void shouldHandleConcurrentRequests() {
    int numberOfThreads = 10;
    int requestsPerThread = 100;

    ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
    CountDownLatch latch = new CountDownLatch(numberOfThreads);

    for (int i = 0; i < numberOfThreads; i++) {
        executor.submit(() -> {
            try {
                for (int j = 0; j < requestsPerThread; j++) {
                    webTestClient.get()
                        .uri("/stocks/AAPL")
                        .exchange()
                        .expectStatus().isOk();
                }
            } finally {
                latch.countDown();
            }
        });
    }

    assertDoesNotThrow(() -> latch.await(30, TimeUnit.SECONDS));
}

```text
**Run Performance Tests:**

```bash
./mvnw test -Pperformance

```text

### 7. Mutation Tests

**Purpose**: Evaluate test suite quality by introducing code mutations
**Speed**: 🐌 Very Slow (minutes)
**Coverage**: Test effectiveness measurement

```bash
./mvnw test -Pmutation

```text
PITest configuration in `pom.xml`:

```xml
<plugin>
    <groupId>org.pitest</groupId>
    <artifactId>pitest-maven</artifactId>
    <version>1.15.8</version>
    <configuration>
        <targetClasses>
            <param>com.techishthoughts.stocks.*</param>
        </targetClasses>
        <targetTests>
            <param>com.techishthoughts.stocks.*</param>
        </targetTests>
    </configuration>
</plugin>

```text

## 🏃‍♂️ Test Execution

### Individual Test Categories

```bash

# Unit tests (fastest)

./mvnw test -Dtest="**/unit/**"

# Integration tests

./mvnw test -Dtest="**/integration/**"

# Component/BDD tests

./mvnw test -Dtest="**/component/**"

# Smoke tests

./mvnw test -Dtest="**/smoke/**"

# Sanity tests

./mvnw test -Dtest="**/sanity/**"

```text

### Test Profiles

```bash

# All tests except performance

./mvnw test -Pall-tests

# CI profile (optimized for build pipelines)

./mvnw test -Pci

# Performance tests only

./mvnw test -Pperformance

# Mutation testing

./mvnw test -Pmutation

```text

### Continuous Testing

```bash

# Watch mode for development

./mvnw test -Dtest="**/unit/**" -Dspring-boot.run.fork=false -Drun.watch=true

```text

## 📊 Test Configuration

### Maven Configuration

```xml
<!-- Surefire Plugin for Unit Tests -->
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <configuration>
        <includes>
            <include>**/unit/**/*Test.java</include>
            <include>**/smoke/**/*Test.java</include>
            <include>**/sanity/**/*Test.java</include>
        </includes>
    </configuration>
</plugin>

<!-- Failsafe Plugin for Integration Tests -->
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-failsafe-plugin</artifactId>
    <configuration>
        <includes>
            <include>**/integration/**/*Test.java</include>
            <include>**/component/**/*Test.java</include>
        </includes>
    </configuration>
</plugin>

```text

### Test Properties

#### application-test.yml

```yaml
spring:
  profiles:
    active: test

# Fast startup for tests

logging:
  level:
    com.techishthoughts.stocks: WARN
    org.apache.pekko: WARN

# Test-specific configurations

finnhub:
  api-key: test-key

# Disable external services in tests

external:
  api:
    enabled: false

```text

## 🎯 Test Best Practices

### 1. Test Naming Convention

```java
// Pattern: shouldDoSomethingWhenCondition()
@Test
void shouldReturnStockWhenValidSymbolProvided() { }

@Test
void shouldThrowExceptionWhenInvalidSymbolProvided() { }

@Test
void shouldHandleTimeoutWhenExternalServiceSlow() { }

```text

### 2. Test Structure (AAA Pattern)

```java
@Test
void shouldCalculateCorrectPrice() {
    // Arrange (Given)
    Stock stock = new Stock("AAPL", Instant.now());
    PriceCalculator calculator = new PriceCalculator();

    // Act (When)
    BigDecimal price = calculator.calculate(stock);

    // Assert (Then)
    assertThat(price).isGreaterThan(BigDecimal.ZERO);
}

```text

### 3. Mock Strategy

```java
// Use @Mock for external dependencies
@Mock private StockMarketClient stockMarketClient;

// Use @Spy for partial mocking
@Spy private StockMapper stockMapper;

// Use real objects for value objects and domain entities
private Stock createTestStock() {
    return new Stock("AAPL", Instant.now());
}

```text

### 4. Reactive Testing

```java
@Test
void shouldHandleReactiveStream() {
    // Given
    Flux<Stock> stockStream = stockService.getAllStocks();

    // When & Then
    StepVerifier.create(stockStream)
        .expectNextMatches(stock -> "AAPL".equals(stock.symbol()))
        .expectNextMatches(stock -> "MSFT".equals(stock.symbol()))
        .verifyComplete();
}

```text

### 5. Actor Testing

```java
@Test
void shouldTestActorBehavior() {
    // Given
    TestKit<StockActor.Command> testKit = ActorTestKit.create();
ActorRef<StockActor.Command> actor = testKit.spawn(StockActor.create("AAPL"));
    TestProbe<StockActor.Response> probe = testKit.createTestProbe();

    // When
    actor.tell(new StockActor.GetStock(probe.getRef()));

    // Then
    StockActor.Response response = probe.receiveMessage();
    assertEquals("AAPL", response.stock().symbol());
}

```text

## 📈 Test Coverage

### Coverage Goals

- **Unit Tests**: > 85% line coverage
- **Integration Tests**: > 70% integration path coverage
- **Component Tests**: 100% critical business scenario coverage
- **Mutation Tests**: > 80% mutation score

### Coverage Reports

```bash

# Generate Jacoco coverage report

./mvnw jacoco:report

# View coverage report

open target/site/jacoco/index.html

```text

### Coverage Configuration

```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <configuration>
        <rules>
            <rule>
                <element>BUNDLE</element>
                <limits>
                    <limit>
                        <counter>LINE</counter>
                        <value>COVEREDRATIO</value>
                        <minimum>0.85</minimum>
                    </limit>
                </limits>
            </rule>
        </rules>
    </configuration>
</plugin>

```text

## 🚨 Continuous Integration

### CI Pipeline Configuration

```yaml

# .github/workflows/ci.yml

name: CI

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:

      - uses: actions/checkout@v3

      - name: Set up JDK 21
        uses: actions/setup-java@v3
        with:
          java-version: '21'

      - name: Cache Maven dependencies
        uses: actions/cache@v3
        with:
          path: ~/.m2
          key: ${{ runner.os }}-m2-${{ hashFiles('**/pom.xml') }}

      - name: Run tests
        run: ./mvnw test -Pci

      - name: Generate coverage report
        run: ./mvnw jacoco:report

      - name: Upload coverage to Codecov
        uses: codecov/codecov-action@v3

```text

### Test Quality Gates

```bash

# Fail build if coverage below threshold

./mvnw verify

# Fail build if mutation score too low

./mvnw test -Pmutation -Dpitest.threshold=80

```text

## 🔧 Troubleshooting

### Common Test Issues

#### 1. Flaky Tests

```java
// Use proper timeouts for async operations
@Test
@Timeout(5)
void shouldCompleteWithinTimeout() {
    StepVerifier.create(stockService.getStock("AAPL"))
        .expectNextCount(1)
        .verifyComplete();
}

// Use Awaitility for complex conditions
await().atMost(5, SECONDS)
    .until(() -> stockService.isReady(), equalTo(true));

```text

#### 2. Actor Test Issues

```java
// Ensure proper actor lifecycle management
@AfterEach
void tearDown() {
    testKit.shutdownTestKit();
}

```text

#### 3. Resource Cleanup

```java
// Use @DirtiesContext for tests that modify Spring context
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)

```text
This comprehensive testing strategy ensures high quality, maintainable code with
excellent coverage across all application layers and scenarios.
