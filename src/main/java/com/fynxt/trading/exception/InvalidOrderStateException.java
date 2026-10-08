package com.fynxt.trading.exception;

import com.fynxt.trading.domain.OrderStatus;

public class InvalidOrderStateException extends TradingException {

    public InvalidOrderStateException(Long orderId, OrderStatus currentStatus, String attemptedAction) {
        super(ErrorCode.INVALID_ORDER_STATE,
                "Order " + orderId + " cannot be " + attemptedAction + " because it is " + currentStatus
                        + "; only PENDING orders can be " + attemptedAction);
    }
}
