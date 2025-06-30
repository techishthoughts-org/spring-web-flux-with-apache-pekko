package com.techishthoughts.stocks.performance;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.techishthoughts.stocks.adapter.out.pekko.StockActorAdapter;
import com.techishthoughts.stocks.application.StockUseCases;
import com.techishthoughts.stocks.domain.Stock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OperationsPerInvocation;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import reactor.core.publisher.Mono;

/**
 * Performance tests using JMH (Java Microbenchmark Harness).
 * These tests measure the performance characteristics of stock operations.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Fork(value = 1, jvmArgs = {"-Xms2G", "-Xmx2G"})
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
public class StockServicePerformanceTest {

    private StockUseCases stockUseCases;
    private Stock testStock;

    @Setup
    public void setup() {
        // Mock the adapter for performance testing
        StockActorAdapter mockAdapter = mock(StockActorAdapter.class);

        testStock = new Stock(
                "AAPL",
                "Apple Inc.",
                "NASDAQ",
                "Common Stock",
                "1980-12-12",
                "US",
                "USD",
                "1980-12-12",
                3000000000.0,
                "+1-408-996-1010",
                16000000000.0,
                "AAPL",
                "https://www.apple.com",
                "https://example.com/logo.png",
                "Technology",
                Instant.now()
        );

        when(mockAdapter.askStockActor(anyString()))
                .thenReturn(Mono.just(testStock));

        stockUseCases = new StockUseCases(mockAdapter);
    }

    @Benchmark
    public Stock benchmarkGetSingleStock() {
        return stockUseCases.getStock("AAPL")
                .block(Duration.ofSeconds(5));
    }

    @Benchmark
    @OperationsPerInvocation(10)
    public void benchmarkMultipleStockRequests() {
        for (int i = 0; i < 10; i++) {
            stockUseCases.getStock("STOCK" + i)
                    .block(Duration.ofSeconds(1));
        }
    }

    @Benchmark
    public Stock benchmarkStockCreation() {
        return new Stock(
                "TEST",
                "Test Corporation",
                "NYSE",
                "Common Stock",
                "2000-01-01",
                "US",
                "USD",
                "2000-01-01",
                1000000.0,
                "+1-555-0123",
                1000000.0,
                "TEST",
                "https://test.com",
                "https://test.com/logo.png",
                "Technology",
                Instant.now()
        );
    }

    /**
     * Run performance tests programmatically.
     * This method can be called from other test classes or CI/CD pipelines.
     */
    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(StockServicePerformanceTest.class.getSimpleName())
                .build();

        new Runner(opt).run();
    }
}
