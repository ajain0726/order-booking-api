package com.fynxt.trading.exception;

public class PendingOrderLimitExceededException extends TradingException {

    public PendingOrderLimitExceededException(String traderId, int limit) {
        super(ErrorCode.PENDING_ORDER_LIMIT_EXCEEDED,
                "Trader " + traderId + " already has " + limit + " PENDING orders (maximum is " + limit
                        + "); fill or cancel one before placing another");
    }
}
