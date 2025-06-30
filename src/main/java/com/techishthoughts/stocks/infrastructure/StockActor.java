package com.techishthoughts.stocks.infrastructure;

import com.techishthoughts.stocks.adapter.out.finnhub.dto.StockDetails;
import com.techishthoughts.stocks.adapter.out.mapper.StockMapper;
import com.techishthoughts.stocks.application.port.out.StockRepository;
import com.techishthoughts.stocks.domain.Stock;
import com.techishthoughts.stocks.infrastructure.logging.LoggingContext;
import com.techishthoughts.stocks.infrastructure.logging.RouteStep;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.Behavior;
import org.apache.pekko.actor.typed.javadsl.AbstractBehavior;
import org.apache.pekko.actor.typed.javadsl.ActorContext;
import org.apache.pekko.actor.typed.javadsl.Behaviors;
import org.apache.pekko.actor.typed.javadsl.Receive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Actor that manages the state of a single stock symbol.
 * It fetches data from the repository when needed and caches it.
 */
public class StockActor extends AbstractBehavior<StockActor.Command> {

    private static final Logger log = LoggerFactory.getLogger(StockActor.class);

    private final String symbol;
    private final StockRepository repository;
    private final StockMapper stockMapper;
    private Stock currentStock;

    // Commands that this actor can handle
    public sealed interface Command {
    }

    public record GetStock(ActorRef<Response> replyTo) implements Command {
    }

    public record InitializeFromListing(StockDetails listing) implements Command {
    }

    // Response message
    public record Response(Stock stock) {
    }

    private StockActor(ActorContext<Command> context,
            String symbol,
            StockRepository repository,
            StockMapper stockMapper) {
        super(context);
        this.symbol = symbol;
        this.repository = repository;
        this.stockMapper = stockMapper;

        log.info("StockActor created for symbol: {}", symbol);
    }

    public static Behavior<Command> create(String symbol,
            StockRepository repository,
            StockMapper stockMapper) {
        return Behaviors.setup(context ->
                new StockActor(context, symbol, repository, stockMapper)
        );
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(GetStock.class, this::onGetStock)
                .onMessage(InitializeFromListing.class, this::onInitializeFromListing)
                .build();
    }

    private Behavior<Command> onGetStock(GetStock command) {
        log.debug("Received GetStock command for symbol: {}", symbol);

        // Create route step for actor call
        RouteStep actorStep = RouteStep.actorCall("stock-actor-get", "StockActor");
        LoggingContext.addRouteStep(actorStep);
        LoggingContext.addMetadata("stockSymbol", symbol);

        if (currentStock != null) {
            actorStep.complete(200, "Retrieved cached stock: " + symbol);
            LoggingContext.logWithContext("Retrieved cached stock for symbol: %s", symbol);
            command.replyTo().tell(new Response(currentStock));
            return this;
        }

        repository.find(symbol)
                .subscribe(stock -> {
                    currentStock = stock;
                    actorStep.complete(200, "Retrieved stock from repository: " + symbol);
                    LoggingContext.logWithContext("Retrieved stock from repository for symbol: %s", symbol);
                    command.replyTo().tell(new Response(stock));
                }, error -> {
                    log.error("Error fetching stock data for symbol: {}", symbol, error);
                    actorStep.fail("Error fetching stock data: " + error.getMessage());
                    LoggingContext.logWithContext("Error fetching stock data for symbol: %s, error: %s", symbol, error.getMessage());
                    Stock defaultStock = stockMapper.toBasicStock(symbol);
                    currentStock = defaultStock;
                    command.replyTo().tell(new Response(defaultStock));
                }, () -> {
                    if (currentStock == null) {
                        Stock defaultStock = stockMapper.toBasicStock(symbol);
                        currentStock = defaultStock;
                        actorStep.complete(200, "Created default stock: " + symbol);
                        LoggingContext.logWithContext("Created default stock for symbol: %s", symbol);
                        command.replyTo().tell(new Response(defaultStock));
                    }
                });

        return this;
    }

    private Behavior<Command> onInitializeFromListing(InitializeFromListing command) {
        log.info("Initializing stock from listing: {}", command.listing().symbol());

        // Create route step for actor initialization
        RouteStep initStep = RouteStep.actorCall("stock-actor-init", "StockActor");
        LoggingContext.addRouteStep(initStep);
        LoggingContext.addMetadata("stockSymbol", command.listing().symbol());

        StockDetails listing = command.listing();

        // Use MapStruct mapper instead of manual object creation
        Stock stock = stockMapper.toStock(listing);

        repository.save(stock).subscribe(
                savedStock -> {
                    initStep.complete(200, "Initialized and saved stock: " + listing.symbol());
                    LoggingContext.logWithContext("Successfully initialized stock from listing: %s", listing.symbol());
                },
                error -> {
                    initStep.fail("Failed to save stock: " + error.getMessage());
                    LoggingContext.logWithContext("Failed to save stock during initialization: %s, error: %s", listing.symbol(), error.getMessage());
                }
        );
        currentStock = stock;

        return this;
    }
}
