package com.techishthoughts.stocks.infrastructure;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.ActorSystem;
import org.apache.pekko.actor.typed.Behavior;
import org.apache.pekko.actor.typed.javadsl.AbstractBehavior;
import org.apache.pekko.actor.typed.javadsl.ActorContext;
import org.apache.pekko.actor.typed.javadsl.Behaviors;
import org.apache.pekko.actor.typed.javadsl.Receive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.techishthoughts.stocks.adapter.out.finnhub.dto.StockDetails;
import com.techishthoughts.stocks.adapter.out.mapper.StockMapper;
import com.techishthoughts.stocks.application.port.out.StockRepository;

/**
 * Configuration for Pekko Actor System.
 * This class sets up the actor system and provides beans for dependency injection.
 */
@Configuration
public class PekkoConfig {

    private static final Logger log = LoggerFactory.getLogger(PekkoConfig.class);

    // Commands that the root actor can handle
    public sealed interface Command {
    }

    public record GetStockActorCommand(String symbol, ActorRef<ActorRef<StockActor.Command>> replyTo) implements Command {
    }

    public record GetAllStockActorsCommand(ActorRef<List<ActorRef<StockActor.Command>>> replyTo) implements Command {
    }

    public record CreateActorsForSymbolsCommand(List<StockDetails> listings) implements Command {
    }

    /**
     * Creates the actor system and returns it as a Spring bean.
     */
    @Bean
    public ActorSystem<Command> actorSystem(StockRepository repository,
            StockMapper stockMapper) {
        log.info("Creating actor system");
        return ActorSystem.create(
                rootBehavior(repository, stockMapper),
                "stock-system"
        );
    }

    /**
     * Creates the root behavior for the actor system.
     */
    private Behavior<Command> rootBehavior(StockRepository repository,
            StockMapper stockMapper) {
        return Behaviors.setup(context -> new RootBehavior(context, repository, stockMapper));
    }

    /**
     * The root actor that manages all stock actors.
     */
    private static class RootBehavior extends AbstractBehavior<Command> {

        private final Map<String, ActorRef<StockActor.Command>> stockActors = new HashMap<>();
        private final StockRepository repository;
        private final StockMapper stockMapper;

        private RootBehavior(ActorContext<Command> context,
                StockRepository repository,
                StockMapper stockMapper) {
            super(context);
            this.repository = repository;
            this.stockMapper = stockMapper;
            log.info("Root actor created");
        }

        @Override
        public Receive<Command> createReceive() {
            return newReceiveBuilder()
                    .onMessage(GetStockActorCommand.class, this::onGetStockActor)
                    .onMessage(GetAllStockActorsCommand.class, this::onGetAllStockActors)
                    .onMessage(CreateActorsForSymbolsCommand.class, this::onCreateActorsForSymbols)
                    .build();
        }

        private Behavior<Command> onGetStockActor(GetStockActorCommand command) {
            String symbol = command.symbol().toUpperCase();
            log.debug("Getting stock actor for symbol: {}", symbol);

            ActorRef<StockActor.Command> stockActor = getOrCreateStockActor(symbol);

            command.replyTo().tell(stockActor);
            return this;
        }

        private Behavior<Command> onGetAllStockActors(GetAllStockActorsCommand command) {
            log.debug("Getting all stock actors, count: {}", stockActors.size());
            command.replyTo().tell(new ArrayList<>(stockActors.values()));
            return this;
        }

        private Behavior<Command> onCreateActorsForSymbols(CreateActorsForSymbolsCommand command) {
            List<StockDetails> listings = command.listings();
            log.info("Creating actors for {} symbols", listings.size());

            for (StockDetails listing : listings) {
                String symbol = listing.symbol().toUpperCase();
                ActorRef<StockActor.Command> actor = getOrCreateStockActor(symbol);
                actor.tell(new StockActor.InitializeFromListing(listing));
            }

            return this;
        }

        private ActorRef<StockActor.Command> getOrCreateStockActor(String symbol) {
            return stockActors.computeIfAbsent(
                    symbol,
                    s -> {
                        log.debug("Creating new stock actor for symbol: {}", s);
                        String sanitizedSymbol = sanitizeSymbol(s);
                        return getContext().spawn(
                                StockActor.create(s, repository, stockMapper),
                                sanitizedSymbol
                        );
                    }
            );
        }

        /**
         * Helper method to sanitize the symbol string so it contains only valid characters for actor names.
         * Allowed characters: ASCII letters, digits, and the special characters - _ . * $ + : @ & = , ! ~ ' ;
         */
        private String sanitizeSymbol(String symbol) {
            return symbol.replaceAll("[^A-Za-z0-9\\-_.\\*\\$\\+\\:\\@\\&\\=\\,\\!\\~\\';]", "_");
        }
    }
}
