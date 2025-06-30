package com.techishthoughts.stocks.adapter.out.finnhub.dto;

public record StockSymbols(
String currency,
String description,
String displaySymbol,
String figi,
String mic,
String symbol,
String type
) {
}
