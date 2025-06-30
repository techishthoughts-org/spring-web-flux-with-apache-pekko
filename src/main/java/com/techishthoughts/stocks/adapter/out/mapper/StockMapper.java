package com.techishthoughts.stocks.adapter.out.mapper;

import com.techishthoughts.stocks.adapter.out.finnhub.dto.CompanyProfile;
import com.techishthoughts.stocks.adapter.out.finnhub.dto.StockDetails;
import com.techishthoughts.stocks.domain.Stock;
import java.time.Instant;
import org.springframework.stereotype.Component;

/**
 * Manual implementation of StockMapper to fix MapStruct annotation processor issues.
 * This can be replaced with MapStruct-generated code once the processor works correctly.
 */
@Component
public class StockMapper {

    // Default values for stock mapping
    private static final String DEFAULT_ASSET_TYPE = "Common Stock";
    private static final String DEFAULT_COUNTRY = "US";
    private static final String DEFAULT_CURRENCY = "USD";
    private static final String DEFAULT_EXCHANGE = "UNKNOWN";
    private static final String DEFAULT_INDUSTRY = "Technology";
    private static final String COMPANY_SUFFIX = " Inc.";
    private static final String EMPTY_STRING = "";
    private static final double DEFAULT_NUMERIC_VALUE = 0.0;

    public Stock toStock(StockDetails stockDetails) {
        if (stockDetails == null) {
            return null;
        }

        return new Stock(
                stockDetails.symbol(),
                stockDetails.name(),
                stockDetails.exchange(),
                stockDetails.assetType(),
                stockDetails.ipoDate(),
                stockDetails.country(),
                stockDetails.currency(),
                stockDetails.ipo(),
                stockDetails.marketCapitalization(),
                stockDetails.phone(),
                stockDetails.shareOutstanding(),
                stockDetails.ticker(),
                stockDetails.weburl(),
                stockDetails.logo(),
                stockDetails.finnhubIndustry(),
                Instant.now()
        );
    }

    public StockDetails toStockDetails(Stock stock) {
        if (stock == null) {
            return null;
        }

        return new StockDetails(
                stock.symbol(),
                stock.name(),
                stock.exchange(),
                stock.assetType(),
                stock.ipoDate(),
                stock.country(),
                stock.currency(),
                stock.ipo(),
                stock.marketCapitalization(),
                stock.phone(),
                stock.shareOutstanding(),
                stock.ticker(),
                stock.weburl(),
                stock.logo(),
                stock.finnhubIndustry()
        );
    }

    public Stock toStock(String symbol, CompanyProfile profile) {
        if (symbol == null || profile == null) {
            return null;
        }

        return new Stock(
                symbol,
                profile.name(),
                profile.exchange(),
                DEFAULT_ASSET_TYPE,
                profile.ipo(),
                profile.country(),
                profile.currency(),
                profile.ipo(),
                profile.marketCapitalization(),
                profile.phone(),
                profile.shareOutstanding(),
                profile.ticker(),
                profile.weburl(),
                profile.logo(),
                profile.finnhubIndustry(),
                Instant.now()
        );
    }

    public Stock toBasicStock(String symbol) {
        if (symbol == null) {
            return null;
        }

        return new Stock(
                symbol,
                symbol + COMPANY_SUFFIX,
                DEFAULT_EXCHANGE,
                DEFAULT_ASSET_TYPE,
                EMPTY_STRING,
                DEFAULT_COUNTRY,
                DEFAULT_CURRENCY,
                EMPTY_STRING,
                DEFAULT_NUMERIC_VALUE,
                EMPTY_STRING,
                DEFAULT_NUMERIC_VALUE,
                symbol,
                EMPTY_STRING,
                EMPTY_STRING,
                DEFAULT_INDUSTRY,
                Instant.now()
        );
    }
}
