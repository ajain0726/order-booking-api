package com.fynxt.trading.exception;

/**
 * Base type for every expected business failure. Each subtype carries an
 * {@link ErrorCode}, so the web layer maps them without instanceof chains.
 */
public abstract class TradingException extends RuntimeException {

    private final ErrorCode errorCode;

    protected TradingException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
