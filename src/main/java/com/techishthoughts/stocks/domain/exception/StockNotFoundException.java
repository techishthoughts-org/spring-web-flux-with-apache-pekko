package com.techishthoughts.stocks.domain.exception;

/**
 * Exception thrown when a requested stock is not found.
 */
public class StockNotFoundException extends RuntimeException {

    private final String symbol;

    public StockNotFoundException(String symbol) {
        super("Stock not found for symbol: " + symbol);
        this.symbol = symbol;
    }

    public StockNotFoundException(String symbol, Throwable cause) {
        super("Stock not found for symbol: " + symbol, cause);
        this.symbol = symbol;
    }

    public String getSymbol() {
        return symbol;
    }
}
