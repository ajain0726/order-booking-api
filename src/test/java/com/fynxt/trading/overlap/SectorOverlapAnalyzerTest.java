package com.fynxt.trading.overlap;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SectorOverlapAnalyzerTest {

    private final SectorOverlapAnalyzer analyzer = new SectorOverlapAnalyzer(BenchmarkBaskets.DEFAULT);

    @Test
    void workedExampleFromSpecIsHighRiskTechHeavy() {
        OverlapReport report = analyzer.analyze(Set.of("AAPL", "TSLA", "NVDA"));

        assertThat(report.overlaps()).containsExactly(
                new BasketOverlap("TECH_HEAVY", new BigDecimal("75.00")),
                new BasketOverlap("FINANCE_HEAVY", new BigDecimal("0.00")),
                new BasketOverlap("BALANCED", new BigDecimal("50.00")));
        assertThat(report.dominantBasket()).contains("TECH_HEAVY");
        assertThat(report.riskFlag()).isEqualTo(RiskFlag.HIGH);
    }

    @Test
    void tieResolvesToFirstDeclaredBasket() {
        // |p|=2: TECH 2*2/(2+5)=57.14, BALANCED 2*2/7=57.14 -> tie, first declared wins
        OverlapReport report = analyzer.analyze(Set.of("AAPL", "TSLA"));

        assertThat(report.overlaps()).extracting(BasketOverlap::percentage)
                .containsExactly(new BigDecimal("57.14"), new BigDecimal("0.00"), new BigDecimal("57.14"));
        assertThat(report.dominantBasket()).contains("TECH_HEAVY");
        assertThat(report.riskFlag()).isEqualTo(RiskFlag.MEDIUM);
    }

    @Test
    void emptyPortfolioHasNoDominantBasketAndLowRisk() {
        OverlapReport report = analyzer.analyze(Set.of());

        assertThat(report.overlaps()).allSatisfy(o -> assertThat(o.percentage()).isEqualByComparingTo("0"));
        assertThat(report.dominantBasket()).isEmpty();
        assertThat(report.riskFlag()).isEqualTo(RiskFlag.LOW);
    }

    @Test
    void portfolioOutsideAllBasketsIsLowRisk() {
        OverlapReport report = analyzer.analyze(Set.of("IBM", "ORCL"));

        assertThat(report.dominantBasket()).isEmpty();
        assertThat(report.riskFlag()).isEqualTo(RiskFlag.LOW);
    }

    @Test
    void identicalPortfolioIsHundredPercent() {
        OverlapReport report = analyzer.analyze(Set.of("JPM", "GS", "BAC", "MS", "WFC"));

        assertThat(report.overlaps().get(1).percentage()).isEqualByComparingTo("100.00");
        assertThat(report.dominantBasket()).contains("FINANCE_HEAVY");
    }

    @Test
    void largePortfolioDilutesOverlap() {
        // 3 common out of 10 held: 2*3/(10+5) = 40.00 -> exactly MEDIUM boundary
        OverlapReport report = analyzer.analyze(Set.of(
                "AAPL", "MSFT", "GOOGL", "A1", "A2", "A3", "A4", "A5", "A6", "A7"));

        assertThat(report.overlaps().get(0).percentage()).isEqualByComparingTo("40.00");
        assertThat(report.riskFlag()).isEqualTo(RiskFlag.MEDIUM);
    }

    @Test
    void roundsHalfUpToTwoDecimals() {
        // 2*1/(1+5)*100 = 33.333...
        assertThat(SectorOverlapAnalyzer.overlap(Set.of("AAPL"), Set.of("AAPL", "B", "C", "D", "E")))
                .isEqualTo(new BigDecimal("33.33"));
        // 2*1/(2+1)*100 = 66.666... -> 66.67
        assertThat(SectorOverlapAnalyzer.overlap(Set.of("X", "Y"), Set.of("X")))
                .isEqualTo(new BigDecimal("66.67"));
    }

    @ParameterizedTest
    @CsvSource({"100.00,HIGH", "60.00,HIGH", "59.99,MEDIUM", "40.00,MEDIUM", "39.99,LOW", "0.00,LOW"})
    void riskFlagThresholdsAreInclusive(String overlap, RiskFlag expected) {
        assertThat(RiskFlag.forHighestOverlap(new BigDecimal(overlap))).isEqualTo(expected);
    }

    @Test
    void basketsAreDefensivelyCopiedAndValidated() {
        assertThatThrownBy(() -> new SectorOverlapAnalyzer(List.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Basket("EMPTY", Set.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
