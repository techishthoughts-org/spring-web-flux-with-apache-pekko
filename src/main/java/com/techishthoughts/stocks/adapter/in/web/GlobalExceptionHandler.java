package com.techishthoughts.stocks.adapter.in.web;

import com.techishthoughts.stocks.domain.exception.InvalidStockSymbolException;
import com.techishthoughts.stocks.domain.exception.StockNotFoundException;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ServerWebExchange;

/**
 * Global exception handler for REST API endpoints.
 * Provides consistent error responses across the application.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(StockNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleStockNotFoundException(
            StockNotFoundException ex, ServerWebExchange exchange) {

        log.warn("Stock not found: {}", ex.getSymbol());

        ErrorResponse errorResponse = new ErrorResponse(
                "STOCK_NOT_FOUND",
                ex.getMessage(),
                Instant.now(),
                exchange.getRequest().getPath().pathWithinApplication().value()
        );

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(errorResponse);
    }

    @ExceptionHandler(InvalidStockSymbolException.class)
    public ResponseEntity<ErrorResponse> handleInvalidStockSymbolException(
            InvalidStockSymbolException ex, ServerWebExchange exchange) {

        log.warn("Invalid stock symbol: {}", ex.getSymbol());

        ErrorResponse errorResponse = new ErrorResponse(
                "INVALID_STOCK_SYMBOL",
                ex.getMessage(),
                Instant.now(),
                exchange.getRequest().getPath().pathWithinApplication().value()
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(errorResponse);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException ex, ServerWebExchange exchange) {

        log.warn("Invalid argument: {}", ex.getMessage());

        ErrorResponse errorResponse = new ErrorResponse(
                "INVALID_ARGUMENT",
                ex.getMessage(),
                Instant.now(),
                exchange.getRequest().getPath().pathWithinApplication().value()
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, ServerWebExchange exchange) {

        log.error("Unexpected error occurred", ex);

        ErrorResponse errorResponse = new ErrorResponse(
                "INTERNAL_SERVER_ERROR",
                "An unexpected error occurred. Please try again later.",
                Instant.now(),
                exchange.getRequest().getPath().pathWithinApplication().value()
        );

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorResponse);
    }

    /**
     * Standard error response format.
     */
    public record ErrorResponse(
    String code,
    String message,
    Instant timestamp,
    String path
    ) {
    }
}
