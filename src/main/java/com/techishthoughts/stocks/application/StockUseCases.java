package com.techishthoughts.stocks.application;

import com.techishthoughts.stocks.adapter.out.pekko.StockActorAdapter;
import com.techishthoughts.stocks.application.port.in.StockService;
import com.techishthoughts.stocks.domain.Stock;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class StockUseCases implements StockService {

    private final StockActorAdapter adapter;

    public StockUseCases(StockActorAdapter adapter) {
        this.adapter = adapter;
    }

    @Override
    public Mono<Stock> getStock(String symbol) {
        return adapter.askStockActor(symbol);
    }

    @Override
    public Flux<Stock> getAllStocks() {
        return adapter.askAllStockActors();
    }
}
