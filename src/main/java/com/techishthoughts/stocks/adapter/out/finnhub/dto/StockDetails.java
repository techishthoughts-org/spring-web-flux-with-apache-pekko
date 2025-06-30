package com.techishthoughts.stocks.adapter.out.finnhub.dto;

/**
 * Represents a Stock.
 */
public record StockDetails(
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
String finnhubIndustry
) {

}
