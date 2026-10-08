package com.fynxt.trading.service;

public record AddHoldingCommand(String traderId, String stock, String sector, long quantity) {
}
