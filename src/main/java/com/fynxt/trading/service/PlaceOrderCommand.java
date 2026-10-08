package com.fynxt.trading.service;

import com.fynxt.trading.domain.OrderSide;

public record PlaceOrderCommand(String traderId, String stock, String sector, long quantity, OrderSide side) {
}
