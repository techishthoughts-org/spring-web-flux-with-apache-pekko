package com.techishthoughts.stocks.domain;

/**
 * Value object representing a stock symbol.
 * Encapsulates validation and business rules for stock symbols.
 *
 * @param value the stock symbol string value
 */
public record StockSymbol(String value) {

    /** Maximum allowed length for stock symbols. */
    private static final int MAX_SYMBOL_LENGTH = 10;

    /**
     * Compact constructor with validation.
     * Validates that the stock symbol meets business requirements.
     */
    public StockSymbol {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "Stock symbol cannot be null or blank");
        }
        if (value.length() > MAX_SYMBOL_LENGTH) {
            throw new IllegalArgumentException(
                    "Stock symbol cannot be longer than " + MAX_SYMBOL_LENGTH
                            + " characters");
        }
        if (!value.matches("^[A-Z0-9\\.\\-]+$")) {
            throw new IllegalArgumentException(
                    "Stock symbol must contain only uppercase letters, numbers, "
                            + "dots, and hyphens");
        }
    }

    /**
     * Creates a StockSymbol from a string value.
     * Automatically converts to uppercase and trims whitespace.
     *
     * @param value the stock symbol string
     * @return new StockSymbol instance
     */
    public static StockSymbol of(final String value) {
        return new StockSymbol(value.toUpperCase().trim());
    }

    /**
     * Returns the string representation of the stock symbol.
     *
     * @return the stock symbol value
     */
    @Override
    public String toString() {
        return value;
    }
}
