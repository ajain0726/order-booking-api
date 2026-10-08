package com.fynxt.trading.exception;

import org.springframework.http.HttpStatus;

/**
 * Stable, machine-readable error codes and the HTTP status each maps to.
 */
public enum ErrorCode {
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST),
    MALFORMED_REQUEST(HttpStatus.BAD_REQUEST),
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND),
    INVALID_ORDER_STATE(HttpStatus.CONFLICT),
    PENDING_ORDER_LIMIT_EXCEEDED(HttpStatus.UNPROCESSABLE_ENTITY),
    INSUFFICIENT_HOLDINGS(HttpStatus.UNPROCESSABLE_ENTITY),
    STOCK_SECTOR_MISMATCH(HttpStatus.CONFLICT),
    CONCURRENT_MODIFICATION(HttpStatus.CONFLICT),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR);

    private final HttpStatus httpStatus;

    ErrorCode(HttpStatus httpStatus) {
        this.httpStatus = httpStatus;
    }

    public HttpStatus httpStatus() {
        return httpStatus;
    }
}
