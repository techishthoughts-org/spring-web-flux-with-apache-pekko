package com.techishthoughts.stocks.application.port.in;

import com.techishthoughts.stocks.domain.Stock;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface StockService {
    Mono<Stock> getStock(String symbol);

    Flux<Stock> getAllStocks();
}
