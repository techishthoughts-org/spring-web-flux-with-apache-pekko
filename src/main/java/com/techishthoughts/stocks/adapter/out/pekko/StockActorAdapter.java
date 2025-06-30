package com.techishthoughts.stocks.adapter.out.pekko;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletionStage;

import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.ActorSystem;
import org.apache.pekko.actor.typed.Scheduler;
import org.apache.pekko.actor.typed.javadsl.AskPattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.google.common.util.concurrent.RateLimiter;
import com.techishthoughts.stocks.adapter.out.finnhub.dto.StockDetails;
import com.techishthoughts.stocks.adapter.out.finnhub.dto.StockSymbols;
import com.techishthoughts.stocks.application.port.out.StockMarketClient;
import com.techishthoughts.stocks.infrastructure.PekkoConfig;
import com.techishthoughts.stocks.infrastructure.StockActor;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Adapter that provides a reactive interface to the actor system.
 * It translates between the reactive world (Mono/Flux) and the actor world (ask patterns).
 */
@Component
public class StockActorAdapter {

    private static final Logger log = LoggerFactory.getLogger(StockActorAdapter.class);
    private static final Duration ASK_TIMEOUT = Duration.ofSeconds(5);

    private final ActorSystem<PekkoConfig.Command> actorSystem;
    private final Scheduler scheduler;
    private final StockMarketClient stockMarketClient;

    // Track initialization status
    private volatile boolean initializationStarted = false;
    private volatile boolean initializationCompleted = false;
    private volatile int totalSymbolsToProcess = 0;
    private volatile int symbolsProcessed = 0;

    public StockActorAdapter(ActorSystem<PekkoConfig.Command> actorSystem, StockMarketClient stockMarketClient) {
        this.actorSystem = actorSystem;
        this.scheduler = actorSystem.scheduler();
        this.stockMarketClient = stockMarketClient;
        log.info("StockActorAdapter initialized");
    }

    /**
     * Initialize the actor system with all available stock symbols.
     * This method is called when the application is ready and runs asynchronously
     * in the background to avoid blocking application startup.
     */
    @Async
    @EventListener(ApplicationReadyEvent.class)
    public void initializeActorsForAllSymbols() {
        log.info("Starting background initialization of actors for all symbols");
        initializationStarted = true;

        try {
            log.info("Background task: Initializing actors for all symbols");
            RateLimiter rateLimiter = RateLimiter.create(1.0); // 1 requests per second

            stockMarketClient.getStockSymbols()
                    .doOnSubscribe(sub -> log.info("Background task: Fetching stock symbols"))
                    .doOnNext(symbols -> {
                        totalSymbolsToProcess = symbols.size();
                        log.info("Background task: Found {} symbols to process", totalSymbolsToProcess);
                    })
                    .flatMapMany(Flux::fromIterable)
                    .flatMap(symbol -> processSymbolWithRateLimit(symbol, rateLimiter))
                    .doOnNext(stockDetails -> {
                        symbolsProcessed++;
                        if (symbolsProcessed % 10 == 0) { // Log progress every 10 symbols
                            log.info("Background task: Processed {}/{} symbols", symbolsProcessed, totalSymbolsToProcess);
                        }
                    })
                    .collectList()
                    .doOnNext(list -> log.info("Background task: Collected {} stock details, creating actors", list.size()))
                    .doOnError(error -> log.error("Background task: Error collecting stock details", error))
                    .doOnSuccess(list -> {
                        log.info("Background task: Successfully collected all stock details");
                        initializationCompleted = true;
                    })
                    .subscribeOn(Schedulers.boundedElastic()) // Ensure it runs on background thread
                    .subscribe(
                            this::createActorsForSymbols,
                            error -> {
                                log.error("Background task: Failed to create actors for symbols", error);
                                initializationCompleted = true; // Mark as completed even on error
                            }
                    );

            log.info("Background initialization task started successfully - application startup continues");
        } catch (Exception e) {
            log.error("Failed to start background initialization task", e);
            initializationCompleted = true; // Mark as completed even on error
        }
    }

    /**
     * Process a single symbol with rate limiting and error handling.
     */
    private Mono<StockDetails> processSymbolWithRateLimit(StockSymbols symbol, RateLimiter rateLimiter) {

        return Mono.fromCallable(() -> {
            rateLimiter.acquire(); // This blocks, but on a different thread
            return symbol;
        })
                .subscribeOn(Schedulers.boundedElastic()) // Move blocking to elastic scheduler
                .flatMap(s -> processSymbol(s))
                .onErrorResume(error -> {
                    log.error("Failed to process symbol: {}", symbol.symbol(), error);
                    return Mono.empty();
                });
    }

    /**
     * Process a single symbol by fetching its company profile and creating stock details.
     */
    private Mono<StockDetails> processSymbol(StockSymbols symbol) {
        log.info("Processing symbol: {}", symbol.symbol());

        return stockMarketClient.getCompanyProfile(symbol.symbol())
                .doOnNext(profile -> log.info("Fetched company profile for symbol: {}", symbol.symbol()))
                .map(profile -> createStockDetails(symbol, profile))
                .doOnNext(details -> log.info("Created stock details for symbol: {}", symbol.symbol()))
                .checkpoint("After creating stock details for " + symbol.symbol());
    }

    /**
     * Create stock details from symbol and company profile.
     */
    private StockDetails createStockDetails(
            StockSymbols symbol,
            com.techishthoughts.stocks.adapter.out.finnhub.dto.CompanyProfile profile) {

        return stockMarketClient.getStockDetails(symbol, profile);
    }

    /**
     * Create actors for the given list of symbols.
     *
     * @param listings the list of symbols to create actors for
     */
    public void createActorsForSymbols(List<StockDetails> listings) {
        log.info("Creating actors for {} symbols", listings.size());
        actorSystem.tell(new PekkoConfig.CreateActorsForSymbolsCommand(listings));
    }

    /**
     * Ask a specific stock actor for its data.
     *
     * @param symbol the stock symbol
     * @return a Mono with the stock data
     */
    public Mono<com.techishthoughts.stocks.domain.Stock> askStockActor(String symbol) {
        log.debug("Asking stock actor for symbol: {}", symbol);

        return Mono.fromCompletionStage(() -> {
            @SuppressWarnings("unchecked")
            CompletionStage<ActorRef<StockActor.Command>> actorStage =
                    AskPattern.<PekkoConfig.Command, ActorRef<StockActor.Command>>ask(
                            actorSystem,
                            replyTo -> new PekkoConfig.GetStockActorCommand(symbol,
                                    (ActorRef<ActorRef<StockActor.Command>>) (ActorRef<?>) replyTo),
                            ASK_TIMEOUT,
                            scheduler
                    );

            return actorStage.thenCompose(this::askForStock);
        });
    }

    /**
     * Ask all stock actors for their data.
     *
     * @return a Flux with all stocks data
     */
    public Flux<com.techishthoughts.stocks.domain.Stock> askAllStockActors() {
        log.debug("Asking for all stock actors");

        return Mono.defer(() -> {
            @SuppressWarnings("unchecked")
            CompletionStage<List<ActorRef<StockActor.Command>>> actorsStage =
                    AskPattern.<PekkoConfig.Command, List<ActorRef<StockActor.Command>>>ask(
                            actorSystem,
                            replyTo -> new PekkoConfig.GetAllStockActorsCommand(
                                    (ActorRef<List<ActorRef<StockActor.Command>>>) (ActorRef<?>) replyTo),
                            ASK_TIMEOUT,
                            scheduler
                    );

            return Mono.fromCompletionStage(actorsStage);
        })
                .flatMapMany(actors -> Flux.fromIterable(actors)
                        .flatMap(actor -> Mono.fromCompletionStage(askForStock(actor))));
    }

    @SuppressWarnings("unchecked")
    private CompletionStage<com.techishthoughts.stocks.domain.Stock> askForStock(ActorRef<StockActor.Command> actorRef) {
        return AskPattern.ask(
                actorRef,
                replyTo -> new StockActor.GetStock((ActorRef<StockActor.Response>) (ActorRef<?>) replyTo),
                ASK_TIMEOUT,
                scheduler
        ).thenApply(response -> ((StockActor.Response) response).stock());
    }

    /**
     * Check if the background initialization has started.
     *
     * @return true if initialization has started, false otherwise
     */
    public boolean isInitializationStarted() {
        return initializationStarted;
    }

    /**
     * Check if the background initialization has completed.
     *
     * @return true if initialization has completed (successfully or with errors), false otherwise
     */
    public boolean isInitializationCompleted() {
        return initializationCompleted;
    }

    /**
     * Get the current initialization progress.
     *
     * @return a string describing the current progress
     */
    public String getInitializationProgress() {
        if (!initializationStarted) {
            return "Not started";
        } else if (initializationCompleted) {
            return String.format("Completed (%d/%d symbols processed)", symbolsProcessed, totalSymbolsToProcess);
        } else {
            return String.format("In progress (%d/%d symbols processed)", symbolsProcessed, totalSymbolsToProcess);
        }
    }

    /**
     * Get the total number of symbols to process.
     *
     * @return total symbols count, 0 if not yet determined
     */
    public int getTotalSymbolsToProcess() {
        return totalSymbolsToProcess;
    }

    /**
     * Get the number of symbols processed so far.
     *
     * @return processed symbols count
     */
    public int getSymbolsProcessed() {
        return symbolsProcessed;
    }
}
