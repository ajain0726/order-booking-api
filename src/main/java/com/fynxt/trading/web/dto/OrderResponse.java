package com.fynxt.trading.web.dto;

import com.fynxt.trading.domain.Order;
import com.fynxt.trading.domain.OrderSide;
import com.fynxt.trading.domain.OrderStatus;

import java.time.Instant;

public record OrderResponse(
        Long id,
        String traderId,
        String stock,
        String sector,
        long quantity,
        OrderSide side,
        OrderStatus status,
        Instant createdAt,
        Instant updatedAt) {

    public static OrderResponse from(Order order) {
        return new OrderResponse(order.getId(), order.getTraderId(), order.getStock().getSymbol(),
                order.getStock().getSector(), order.getQuantity(), order.getSide(), order.getStatus(),
                order.getCreatedAt(), order.getUpdatedAt());
    }
}
