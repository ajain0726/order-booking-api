package com.fynxt.trading.web.dto;

import com.fynxt.trading.service.AddHoldingCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record AddHoldingRequest(
        @NotBlank @Size(max = 16) String stock,
        @NotBlank @Size(max = 32) String sector,
        @NotNull @Positive Long quantity) {

    public AddHoldingCommand toCommand(String traderId) {
        return new AddHoldingCommand(traderId, stock, sector, quantity);
    }
}
