package com.techishthoughts.stocks.application.port.out;

import com.techishthoughts.stocks.domain.Stock;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface StockRepository {
    Mono<Stock> find(String symbol);

    Flux<Stock> findAll();

    Mono<Void> save(Stock stock);
}
