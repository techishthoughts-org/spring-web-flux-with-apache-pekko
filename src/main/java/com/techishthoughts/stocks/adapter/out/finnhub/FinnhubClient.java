package com.techishthoughts.stocks.adapter.out.finnhub;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.techishthoughts.stocks.adapter.out.finnhub.dto.CompanyProfile;
import com.techishthoughts.stocks.adapter.out.finnhub.dto.StockDetails;
import com.techishthoughts.stocks.adapter.out.finnhub.dto.StockSymbols;
import com.techishthoughts.stocks.application.port.out.StockMarketClient;
import com.techishthoughts.stocks.infrastructure.logging.LoggingContext;
import com.techishthoughts.stocks.infrastructure.logging.RouteStep;

import reactor.core.publisher.Mono;

/**
 * Minimal wrapper for Finnhub API endpoints.
 * Docs: https://finnhub.io/docs/api
 */
@Component
@Primary
public class FinnhubClient implements StockMarketClient {

    private static final Logger log = LoggerFactory.getLogger(FinnhubClient.class);

    // API Constants
    private static final String FINNHUB_BASE_URL = "https://finnhub.io";
    private static final String STOCK_SYMBOLS_PATH = "/api/v1/stock/symbol";
    private static final String COMPANY_PROFILE_PATH = "/api/v1/stock/profile2";
    private static final String AUTH_HEADER = "X-Finnhub-Token";

    // Market Constants
    private static final String US_EXCHANGE = "US";
    private static final String NYSE_MIC = "XNYS";


    // Error Messages
    private static final String CLIENT_ERROR_MESSAGE = "Client error when fetching stock symbols";
    private static final String SERVER_ERROR_MESSAGE = "Server error when fetching stock symbols";

    private final WebClient webClient;
    private final String apiKey;

    public FinnhubClient(
            WebClient.Builder builder,
            @Value("${finnhub.api-key}") String apiKey) {
        this.webClient = builder.baseUrl(FINNHUB_BASE_URL).build();
        this.apiKey = apiKey;
    }

    @Override
    public Mono<List<StockSymbols>> getStockSymbols() {
        log.info("Fetching stock symbols for US in the New York Stock Exchange ({})", NYSE_MIC);

        // Create route step for this HTTP call
        RouteStep routeStep = RouteStep.httpOut(
                "finnhub-get-stock-symbols",
                "GET",
                FINNHUB_BASE_URL + STOCK_SYMBOLS_PATH + "?exchange=" + US_EXCHANGE + "&mic=" + NYSE_MIC
        );
        LoggingContext.addRouteStep(routeStep);

        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(STOCK_SYMBOLS_PATH)
                        .queryParam("exchange", US_EXCHANGE)
                        .queryParam("mic", NYSE_MIC)
                        .build())
                .header(AUTH_HEADER, apiKey)
                .retrieve()
                .onStatus(
                        HttpStatusCode::is4xxClientError,
                        response -> {
                            routeStep.fail(CLIENT_ERROR_MESSAGE);
                            return Mono.error(new FinnhubApiException(CLIENT_ERROR_MESSAGE));
                        }
                )
                .onStatus(
                        HttpStatusCode::is5xxServerError,
                        response -> {
                            routeStep.fail(SERVER_ERROR_MESSAGE);
                            return Mono.error(new FinnhubApiException(SERVER_ERROR_MESSAGE));
                        }
                )
                .bodyToMono(new ParameterizedTypeReference<List<StockSymbols>>() {
                })
                .doOnNext(result -> {
                    routeStep.complete(200, "Retrieved " + result.size() + " stock symbols");
                    LoggingContext.logWithContext("Successfully fetched %d stock symbols", result.size());
                })
                .doOnError(error -> {
                    routeStep.fail(error.getMessage());
                    LoggingContext.logWithContext("Failed to fetch stock symbols: %s", error.getMessage());
                });
    }


    @Override
    public Mono<CompanyProfile> getCompanyProfile(String stockSymbol) {
        // Create route step for this HTTP call
        RouteStep routeStep = RouteStep.httpOut(
                "finnhub-get-company-profile",
                "GET",
                FINNHUB_BASE_URL + COMPANY_PROFILE_PATH + "?symbol=" + stockSymbol
        );
        LoggingContext.addRouteStep(routeStep);

        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(COMPANY_PROFILE_PATH)
                        .queryParam("symbol", stockSymbol)
                        .build())
                .header(AUTH_HEADER, apiKey)
                .retrieve()
                .bodyToMono(CompanyProfile.class)
                .doOnNext(profile -> {
                    routeStep.complete(200, "Retrieved company profile for ".formatted(stockSymbol));
                    LoggingContext.logWithContext("Successfully fetched company profile for symbol: %s".formatted(stockSymbol), stockSymbol);
                })
                .doOnError(error -> {
                    routeStep.fail(error.getMessage());
                    LoggingContext.logWithContext("Failed to fetch company profile for symbol: %s, error: %s".formatted(stockSymbol, error.getMessage()), stockSymbol, error.getMessage());
                    log.warn("Failed to fetch company profile for symbol: {} in reason of {}", stockSymbol, error.getMessage(), error);
                });
    }

    @Override
    public StockDetails getStockDetails(StockSymbols symbol, CompanyProfile profile) {
        if (symbol == null && profile == null) {
            return null;
        }

        String symbolValue = symbol != null ? symbol.symbol() : null;
        String assetType = symbol != null ? symbol.type() : null;
        String name = profile != null ? profile.name() : null;
        String exchange = profile != null ? profile.exchange() : null;
        String ipoDate = profile != null ? profile.ipo() : null;
        String country = profile != null ? profile.country() : null;
        String currency = profile != null ? profile.currency() : null;
        String ipo = profile != null ? profile.ipo() : null;
        Double marketCapitalization = profile != null ? profile.marketCapitalization() : null;
        String phone = profile != null ? profile.phone() : null;
        Double shareOutstanding = profile != null ? profile.shareOutstanding() : null;
        String ticker = profile != null ? profile.ticker() : null;
        String weburl = profile != null ? profile.weburl() : null;
        String logo = profile != null ? profile.logo() : null;
        String finnhubIndustry = profile != null ? profile.finnhubIndustry() : null;

        return new StockDetails(
                symbolValue, name, exchange, assetType, ipoDate, country, currency, ipo,
                marketCapitalization, phone, shareOutstanding, ticker, weburl, logo, finnhubIndustry
        );
    }

    /**
     * Custom exception for Finnhub API errors
     */
    public static class FinnhubApiException extends RuntimeException {
        public FinnhubApiException(String message) {
            super(message);
        }

        public FinnhubApiException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
