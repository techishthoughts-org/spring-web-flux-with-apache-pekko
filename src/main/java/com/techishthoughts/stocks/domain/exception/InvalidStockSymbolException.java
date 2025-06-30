package com.techishthoughts.stocks.domain.exception;

/**
 * Exception thrown when a stock symbol is invalid.
 */
public class InvalidStockSymbolException extends RuntimeException {

    private final String symbol;

    public InvalidStockSymbolException(String symbol, String message) {
        super("Invalid stock symbol '" + symbol + "': " + message);
        this.symbol = symbol;
    }

    public InvalidStockSymbolException(String symbol, String message, Throwable cause) {
        super("Invalid stock symbol '" + symbol + "': " + message, cause);
        this.symbol = symbol;
    }

    public String getSymbol() {
        return symbol;
    }
}
