package com.techishthoughts.stocks.domain;

import java.time.Instant;

public record Stock(
String symbol,
String name,
String exchange,
String assetType,
String ipoDate,
String country,
String currency,
String ipo,
Double marketCapitalization,
String phone,
Double shareOutstanding,
String ticker,
String weburl,
String logo,
String finnhubIndustry,
Instant lastUpdated) {

    public Stock(String symbol, Instant now) {
        this(symbol, null, null, null, null, null, null, null, null, null, null, null, null, null, null, now);
    }

}
