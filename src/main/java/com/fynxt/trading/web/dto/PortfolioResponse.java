package com.fynxt.trading.web.dto;

import com.fynxt.trading.service.PortfolioService.Portfolio;

import java.util.Map;

public record PortfolioResponse(String traderId, Map<String, Long> positions, Map<String, Long> sectorBreakdown) {

    public static PortfolioResponse from(Portfolio portfolio) {
        return new PortfolioResponse(portfolio.traderId(), portfolio.positions(), portfolio.sectorBreakdown());
    }
}
