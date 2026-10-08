package com.fynxt.trading.exception;

public class InsufficientHoldingsException extends TradingException {

    public InsufficientHoldingsException(String traderId, String stock, long requested, long available) {
        super(ErrorCode.INSUFFICIENT_HOLDINGS,
                "Trader " + traderId + " cannot sell " + requested + " " + stock
                        + ": only " + available + " shares available");
    }
}
