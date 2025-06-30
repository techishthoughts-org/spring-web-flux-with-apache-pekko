package com.techishthoughts.stocks.adapter.out.persistence;

import com.techishthoughts.stocks.application.port.out.StockRepository;
import com.techishthoughts.stocks.domain.Stock;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * In-memory implementation of stock repository that replaces Redis.
 */
@Repository
public class InMemoryStockRepository implements StockRepository {

    private final Map<String, Stock> stocks = new ConcurrentHashMap<>();

    @Override
    public Mono<Stock> find(String symbol) {
        return Mono.justOrEmpty(stocks.get(key(symbol)));
    }

    @Override
    public Flux<Stock> findAll() {
        return Flux.fromIterable(stocks.values());
    }

    @Override
    public Mono<Void> save(Stock s) {
        stocks.put(key(s.symbol()), s);
        return Mono.empty();
    }

    private String key(String symbol) {
        return symbol.toUpperCase();
    }
}
