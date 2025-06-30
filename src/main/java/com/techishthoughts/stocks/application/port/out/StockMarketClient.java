package com.techishthoughts.stocks.application.port.out;

import com.techishthoughts.stocks.adapter.out.finnhub.dto.CompanyProfile;
import com.techishthoughts.stocks.adapter.out.finnhub.dto.StockDetails;
import com.techishthoughts.stocks.adapter.out.finnhub.dto.StockSymbols;
import java.util.List;
import reactor.core.publisher.Mono;

public interface StockMarketClient {

    Mono<List<StockSymbols>> getStockSymbols();

    Mono<CompanyProfile> getCompanyProfile(String stockSymbol);

    StockDetails getStockDetails(StockSymbols stockSymbols, CompanyProfile companyProfile);
}
