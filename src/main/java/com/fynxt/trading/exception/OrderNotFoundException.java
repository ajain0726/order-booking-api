package com.fynxt.trading.exception;

public class OrderNotFoundException extends TradingException {

    public OrderNotFoundException(Long orderId) {
        super(ErrorCode.ORDER_NOT_FOUND, "Order " + orderId + " does not exist");
    }
}
