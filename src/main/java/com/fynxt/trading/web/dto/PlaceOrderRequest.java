package com.fynxt.trading.web.dto;

import com.fynxt.trading.domain.OrderSide;
import com.fynxt.trading.service.PlaceOrderCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record PlaceOrderRequest(
        @NotBlank @Size(max = 32) String traderId,
        @NotBlank @Size(max = 16) String stock,
        @NotBlank @Size(max = 32) String sector,
        @NotNull @Positive Long quantity,
        @NotNull OrderSide side) {

    public PlaceOrderCommand toCommand() {
        return new PlaceOrderCommand(traderId, stock, sector, quantity, side);
    }
}
