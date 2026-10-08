package com.fynxt.trading.service;

import com.fynxt.trading.overlap.OverlapReport;
import com.fynxt.trading.overlap.RiskFlag;
import com.fynxt.trading.service.PortfolioService.Portfolio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class PortfolioServiceTest {

    @Autowired
    private PortfolioService portfolioService;

    private String trader;

    @BeforeEach
    void newTrader() {
        trader = "T-" + UUID.randomUUID().toString().substring(0, 12);
    }

    private void add(String stock, String sector, long qty) {
        portfolioService.addHolding(new AddHoldingCommand(trader, stock, sector, qty));
    }

    @Test
    void portfolioMatchesSpecExample() {
        add("AAPL", "TECH", 100);
        add("AAPL", "TECH", 50);
        add("TSLA", "TECH", 80);

        Portfolio portfolio = portfolioService.getPortfolio(trader);

        assertThat(portfolio.positions()).isEqualTo(Map.of("AAPL", 150L, "TSLA", 80L));
        assertThat(portfolio.sectorBreakdown()).isEqualTo(Map.of("TECH", 230L));
    }

    @Test
    void sectorBreakdownGroupsAcrossSectors() {
        add("AAPL", "TECH", 10);
        add("JPM", "FINANCE", 5);
        add("GS", "FINANCE", 7);

        assertThat(portfolioService.getPortfolio(trader).sectorBreakdown())
                .isEqualTo(Map.of("TECH", 10L, "FINANCE", 12L));
    }

    @Test
    void unknownTraderHasEmptyPortfolio() {
        Portfolio portfolio = portfolioService.getPortfolio("nobody-" + trader);

        assertThat(portfolio.positions()).isEmpty();
        assertThat(portfolio.sectorBreakdown()).isEmpty();
    }

    @Test
    void overlapUsesHeldStocks() {
        add("AAPL", "TECH", 1);
        add("TSLA", "TECH", 1);
        add("NVDA", "TECH", 1);

        OverlapReport report = portfolioService.analyzeOverlap(trader);

        assertThat(report.dominantBasket()).contains("TECH_HEAVY");
        assertThat(report.riskFlag()).isEqualTo(RiskFlag.HIGH);
    }
}
