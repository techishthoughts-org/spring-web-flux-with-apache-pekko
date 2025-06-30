package com.techishthoughts.stocks.unit.domain;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.techishthoughts.stocks.domain.StockSymbol;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class StockSymbolTest {

    @Test
    void shouldCreateValidStockSymbol() {
        // Given
        String validSymbol = "AAPL";

        // When
        StockSymbol stockSymbol = new StockSymbol(validSymbol);

        // Then
        assertEquals(validSymbol, stockSymbol.value());
        assertEquals(validSymbol, stockSymbol.toString());
    }

    @Test
    void shouldCreateStockSymbolWithFactoryMethod() {
        // Given
        String lowercaseSymbol = "aapl";

        // When
        StockSymbol stockSymbol = StockSymbol.of(lowercaseSymbol);

        // Then
        assertEquals("AAPL", stockSymbol.value());
    }

    @Test
    void shouldTrimWhitespaceWhenCreatingWithFactory() {
        // Given
        String symbolWithSpaces = "  MSFT  ";

        // When
        StockSymbol stockSymbol = StockSymbol.of(symbolWithSpaces);

        // Then
        assertEquals("MSFT", stockSymbol.value());
    }

    @ParameterizedTest
    @ValueSource(strings = {"AAPL", "MSFT", "GOOGL", "AMZN", "TSLA", "A", "BRK.A", "BRK-B"})
    void shouldAcceptValidSymbols(String validSymbol) {
        assertDoesNotThrow(() -> new StockSymbol(validSymbol));
    }

    @Test
    void shouldRejectNullSymbol() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new StockSymbol(null)
        );
        assertEquals("Stock symbol cannot be null or blank", exception.getMessage());
    }

    @Test
    void shouldRejectBlankSymbol() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new StockSymbol("   ")
        );
        assertEquals("Stock symbol cannot be null or blank", exception.getMessage());
    }

    @Test
    void shouldRejectEmptySymbol() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new StockSymbol("")
        );
        assertEquals("Stock symbol cannot be null or blank", exception.getMessage());
    }

    @Test
    void shouldRejectTooLongSymbol() {
        // Given
        String tooLongSymbol = "VERYLONGSYMBOL";

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new StockSymbol(tooLongSymbol)
        );
        assertEquals("Stock symbol cannot be longer than 10 characters", exception.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"aapl", "123@", "GOOGL!", "AMZN#", "TSLA$", "test symbol"})
    void shouldRejectInvalidCharacters(String invalidSymbol) {
        assertThrows(IllegalArgumentException.class, () -> new StockSymbol(invalidSymbol));
    }

    @Test
    void shouldAcceptNumbersInSymbol() {
        assertDoesNotThrow(() -> new StockSymbol("BRK123"));
    }

    @Test
    void shouldAcceptDotsInSymbol() {
        assertDoesNotThrow(() -> new StockSymbol("BRK.A"));
    }

    @Test
    void shouldAcceptHyphensInSymbol() {
        assertDoesNotThrow(() -> new StockSymbol("BRK-B"));
    }
}
