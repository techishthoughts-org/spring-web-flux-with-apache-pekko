# Testing Guide

## Overview

This guide covers the testing strategy and implementation for the Stock Service application. The testing approach follows the testing pyramid with unit tests, integration tests, component tests, and end-to-end tests.

## 🧪 Testing Strategy

### Testing Pyramid

```
                    ┌─────────────────┐
                    │   End-to-End    │  ← Few, Slow, Expensive
                    │     Tests       │
                    └─────────────────┘
                ┌─────────────────────────┐
                │    Integration Tests    │  ← Some, Medium Speed
                │   (Component Tests)     │
                └─────────────────────────┘
        ┌─────────────────────────────────────────┐
        │              Unit Tests                 │  ← Many, Fast, Cheap
        │        (70% of total tests)             │
        └─────────────────────────────────────────┘
```

### Test Categories

1. **Unit Tests** (70%): Test individual components in isolation
2. **Integration Tests** (20%): Test component interactions
3. **Component Tests** (8%): Test complete features
4. **End-to-End Tests** (2%): Test complete user workflows

## 🗂️ Test Structure

### Directory Structure

```
src/test/java/com/techishthoughts/stocks/
├── unit/                    # Unit tests
│   ├── adapter/
│   │   ├── in/web/         # Controller tests
│   │   └── out/            # External adapter tests
│   ├── application/        # Use case tests
│   ├── domain/            # Domain logic tests
│   └── infrastructure/    # Infrastructure tests
├── integration/           # Integration tests
│   ├── actuator/         # Actuator endpoint tests
│   └── api/              # API integration tests
├── component/            # Component tests (BDD)
│   ├── stepdefs/        # Cucumber step definitions
│   └── CucumberTestRunner.java
├── performance/          # Performance tests
├── smoke/               # Smoke tests
└── resources/
    ├── features/        # Cucumber feature files
    └── wiremock/       # Mock configurations
```

## 🔧 Test Technologies

### Core Testing Framework
- **JUnit 5**: Main testing framework
- **AssertJ**: Fluent assertions
- **Mockito**: Mocking framework
- **Reactor Test**: Reactive stream testing

### Integration Testing
- **Spring Boot Test**: Spring context integration
- **TestContainers**: Containerized dependencies
- **WireMock**: HTTP service mocking

### BDD Testing
- **Cucumber**: Behavior-driven development
- **Gherkin**: Feature specification language

### Performance Testing
- **JMH**: Java Microbenchmark Harness

## 🧪 Unit Tests

### Controller Tests

Testing REST controllers with mocked dependencies:

```java
@ExtendWith(MockitoExtension.class)
class StockControllerTest {

    @Mock
    private StockUseCases stockUseCases;

    private StockController stockController;

    @BeforeEach
    void setUp() {
        stockController = new StockController(stockUseCases);
    }

    @Test
    void shouldReturnStockBySymbol() {
        // Given
        String symbol = "AAPL";
        Stock expectedStock = createTestStock(symbol);
        when(stockUseCases.getStock(symbol)).thenReturn(Mono.just(expectedStock));

        // When
        Mono<Stock> result = stockController.getStock(symbol);

        // Then
        StepVerifier.create(result)
                .expectNext(expectedStock)
                .verifyComplete();

        verify(stockUseCases).getStock(symbol);
    }
}
```

### Domain Tests

Testing domain entities and value objects:

```java
class StockSymbolTest {

    @Test
    void shouldCreateValidStockSymbol() {
        // Given
        String validSymbol = "AAPL";

        // When
        StockSymbol symbol = StockSymbol.of(validSymbol);

        // Then
        assertThat(symbol.value()).isEqualTo("AAPL");
    }

    @Test
    void shouldRejectInvalidSymbol() {
        // Given
        String invalidSymbol = "invalid@symbol";

        // When & Then
        assertThatThrownBy(() -> StockSymbol.of(invalidSymbol))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must contain only uppercase letters");
    }
}
```

### Reactive Testing

Testing reactive streams with StepVerifier:

```java
@Test
void shouldHandleReactiveStream() {
    // Given
    Flux<Stock> stockStream = Flux.just(
        createTestStock("AAPL"),
        createTestStock("GOOGL")
    );

    // When & Then
    StepVerifier.create(stockStream)
        .expectNextMatches(stock -> "AAPL".equals(stock.symbol()))
        .expectNextMatches(stock -> "GOOGL".equals(stock.symbol()))
        .verifyComplete();
}
```

## 🔗 Integration Tests

### API Integration Tests

Testing complete request/response cycles:

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class StockServiceIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void shouldRetrieveStockBySymbol() {
        // When & Then
        webTestClient.get()
            .uri("/stocks/{symbol}", "AAPL")
            .exchange()
            .expectStatus().isOk()
            .expectBody(Stock.class)
            .value(stock -> {
                assertThat(stock.symbol()).isEqualTo("AAPL");
                assertThat(stock.name()).isNotNull();
            });
    }

    @Test
    void shouldReturnNotFoundForInvalidSymbol() {
        // When & Then
        webTestClient.get()
            .uri("/stocks/{symbol}", "INVALID")
            .exchange()
            .expectStatus().isNotFound()
            .expectBody()
            .jsonPath("$.code").isEqualTo("STOCK_NOT_FOUND");
    }
}
```

### Actuator Tests

Testing Spring Boot Actuator endpoints:

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ActuatorEndpointsTests {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void shouldReturnHealthStatus() {
        webTestClient.get()
            .uri("/actuator/health")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.status").isEqualTo("UP");
    }

    @Test
    void shouldReturnPrometheusMetrics() {
        webTestClient.get()
            .uri("/actuator/prometheus")
            .exchange()
            .expectStatus().isOk()
            .expectHeader().contentType(MediaType.TEXT_PLAIN);
    }
}
```

## 🎭 Component Tests (BDD)

### Cucumber Feature Files

```gherkin
# src/test/resources/features/stock_retrieval.feature
Feature: Stock Retrieval
  As a user of the stocks application
  I want to retrieve stock information
  So that I can make informed investment decisions

  Scenario: Retrieve existing stock by symbol
    Given the stock service is running
    When I request stock information for "AAPL"
    Then I should receive a valid stock response
    And the response should contain the symbol "AAPL"
    And the response should contain a company name

  Scenario: Handle non-existent stock symbol
    Given the stock service is running
    When I request stock information for "INVALID"
    Then I should receive a not found response
    And the error code should be "STOCK_NOT_FOUND"

  Scenario: Retrieve all available stocks
    Given the stock service is running
    When I request all available stocks
    Then I should receive a list of stocks
    And each stock should have a valid symbol
```

### Step Definitions

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class StockRetrievalStepDefs {

    @Autowired
    private WebTestClient webTestClient;

    private WebTestClient.ResponseSpec response;

    @Given("the stock service is running")
    public void theStockServiceIsRunning() {
        webTestClient.get()
            .uri("/actuator/health")
            .exchange()
            .expectStatus().isOk();
    }

    @When("I request stock information for {string}")
    public void iRequestStockInformationFor(String symbol) {
        response = webTestClient.get()
            .uri("/stocks/{symbol}", symbol)
            .exchange();
    }

    @Then("I should receive a valid stock response")
    public void iShouldReceiveAValidStockResponse() {
        response.expectStatus().isOk();
    }

    @Then("the response should contain the symbol {string}")
    public void theResponseShouldContainTheSymbol(String expectedSymbol) {
        response.expectBody()
            .jsonPath("$.symbol").isEqualTo(expectedSymbol);
    }
}
```

## 🚀 Performance Tests

### JMH Benchmarks

```java
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
public class StockServicePerformanceTest {

    private StockService stockService;

    @Setup
    public void setup() {
        stockService = new StockServiceImpl();
    }

    @Benchmark
    public void benchmarkGetStock() {
        Mono<Stock> result = stockService.getStock("AAPL");
        result.block();
    }

    @Benchmark
    public void benchmarkGetAllStocks() {
        Flux<Stock> result = stockService.getAllStocks();
        result.collectList().block();
    }
}
```

### Load Testing

```java
@Test
void shouldHandleHighLoad() {
    // Given
    int numberOfRequests = 1000;
    int concurrentUsers = 50;

    // When
    Flux<String> requests = Flux.range(0, numberOfRequests)
        .flatMap(i ->
            webTestClient.get()
                .uri("/stocks/AAPL")
                .exchange()
                .expectStatus().isOk()
                .returnResult(String.class)
                .getResponseBody()
                .take(1)
        , concurrentUsers);

    // Then
    StepVerifier.create(requests)
        .expectNextCount(numberOfRequests)
        .verifyComplete();
}
```

## 🔍 Test Configuration

### Test Properties

```yaml
# src/test/resources/application-test.yml
spring:
  profiles:
    active: test

logging:
  level:
    com.techishthoughts.stocks: DEBUG
    org.springframework.web: DEBUG

management:
  endpoints:
    web:
      exposure:
        include: "*"

finnhub:
  api-key: test-key
```

### Test Containers

```java
@Testcontainers
@SpringBootTest
class DatabaseIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:13")
        .withDatabaseName("testdb")
        .withUsername("test")
        .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
}
```

## 🎯 Testing Commands

### Running Tests

```bash
# Run all tests
make test

# Run specific test categories
make test-unit
make test-integration

# Run tests with Maven
./mvnw test

# Run specific test class
./mvnw test -Dtest=StockControllerTest

# Run tests with coverage
./mvnw test jacoco:report
```

### Test Profiles

```bash
# Run with test profile
./mvnw test -Dspring.profiles.active=test

# Run with debug logging
./mvnw test -Dlogging.level.com.techishthoughts.stocks=DEBUG

# Run performance tests
./mvnw test -Dtest=**/*PerformanceTest
```

## 📊 Test Coverage

### Coverage Configuration

```xml
<!-- pom.xml -->
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.7</version>
    <executions>
        <execution>
            <goals>
                <goal>prepare-agent</goal>
            </goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>test</phase>
            <goals>
                <goal>report</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

### Coverage Reports

```bash
# Generate coverage report
./mvnw test jacoco:report

# View coverage report
open target/site/jacoco/index.html
```

### Coverage Targets

- **Unit Tests**: 80%+ code coverage
- **Integration Tests**: 70%+ integration coverage
- **Overall**: 75%+ total coverage

## 🔄 Test Automation

### Continuous Integration

```yaml
# .github/workflows/test.yml
name: Tests
on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - name: Set up JDK 21
        uses: actions/setup-java@v2
        with:
          java-version: '21'
      - name: Run tests
        run: ./mvnw test
      - name: Generate coverage report
        run: ./mvnw jacoco:report
```

### Test Quality Gates

```bash
# Fail build if coverage drops below threshold
./mvnw test jacoco:check

# Run mutation testing
./mvnw pitest:mutationCoverage
```

## 🎪 Mocking and Test Doubles

### WireMock Configuration

```java
@SpringBootTest
class ExternalApiTest {

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
        .port(8089)
        .build();

    @Test
    void shouldMockExternalApi() {
        // Given
        wireMock.stubFor(get(urlEqualTo("/api/stocks/AAPL"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                        "symbol": "AAPL",
                        "name": "Apple Inc."
                    }
                """)));

        // When & Then
        webTestClient.get()
            .uri("/stocks/AAPL")
            .exchange()
            .expectStatus().isOk();
    }
}
```

## 🔍 Test Best Practices

### Test Naming

```java
// Good: Descriptive test names
@Test
void shouldReturnStockWhenValidSymbolProvided() { }

@Test
void shouldThrowExceptionWhenInvalidSymbolProvided() { }

// Bad: Vague test names
@Test
void testStock() { }

@Test
void test1() { }
```

### Test Structure (AAA Pattern)

```java
@Test
void shouldCalculateStockValue() {
    // Arrange (Given)
    Stock stock = createTestStock("AAPL");
    BigDecimal price = new BigDecimal("150.00");

    // Act (When)
    BigDecimal value = stock.calculateValue(price);

    // Assert (Then)
    assertThat(value).isEqualTo(new BigDecimal("1500.00"));
}
```

### Test Data Management

```java
// Test data builders
public class StockTestDataBuilder {

    public static Stock createTestStock(String symbol) {
        return new Stock(
            symbol,
            symbol + " Inc.",
            "NASDAQ",
            "Common Stock",
            "2000-01-01",
            "US",
            "USD",
            "2000-01-01",
            1000000.0,
            "+1-555-0123",
            1000000.0,
            symbol,
            "https://example.com",
            "https://example.com/logo.png",
            "Technology",
            Instant.now()
        );
    }
}
```

## 🔧 Test Utilities

### Custom Assertions

```java
public class StockAssertions {

    public static void assertValidStock(Stock stock) {
        assertThat(stock).isNotNull();
        assertThat(stock.symbol()).isNotBlank();
        assertThat(stock.name()).isNotBlank();
        assertThat(stock.lastUpdated()).isNotNull();
    }

    public static void assertErrorResponse(ErrorResponse error, String expectedCode) {
        assertThat(error.code()).isEqualTo(expectedCode);
        assertThat(error.message()).isNotBlank();
        assertThat(error.timestamp()).isNotNull();
    }
}
```

### Test Slices

```java
// Test only web layer
@WebFluxTest(StockController.class)
class StockControllerSliceTest {

    @MockBean
    private StockUseCases stockUseCases;

    @Autowired
    private WebTestClient webTestClient;
}

// Test only JPA layer
@DataJpaTest
class StockRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private StockRepository stockRepository;
}
```

## 📈 Test Metrics

### Key Metrics

- **Test Coverage**: Code coverage percentage
- **Test Execution Time**: Time taken to run tests
- **Test Stability**: Flaky test detection
- **Test Maintainability**: Code quality metrics

### Monitoring

```bash
# Test execution time
./mvnw test -Dtest.time=true

# Test stability (run multiple times)
./mvnw test -Dsurefire.rerunFailingTestsCount=3

# Memory usage during tests
./mvnw test -Xmx2g -XX:+PrintGCDetails
```

---

This comprehensive testing strategy ensures high code quality, reliability, and maintainability of the Stock Service application. Regular testing practices help catch bugs early and maintain confidence in the system's behavior.
