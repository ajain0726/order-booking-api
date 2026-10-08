package com.fynxt.trading.overlap;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Pure-Java sector overlap analysis: no I/O, no framework, no shared mutable
 * state, so it is thread-safe and trivially unit-testable.
 *
 * <pre>Overlap = [ 2 x |common| / (|portfolio| + |basket|) ] x 100</pre>
 */
public final class SectorOverlapAnalyzer {

    private static final int SCALE = 2;
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final List<Basket> baskets;

    public SectorOverlapAnalyzer(List<Basket> baskets) {
        Objects.requireNonNull(baskets, "baskets");
        if (baskets.isEmpty()) {
            throw new IllegalArgumentException("At least one basket is required");
        }
        this.baskets = List.copyOf(baskets);
    }

    /**
     * @param portfolioStocks distinct tickers the trader currently holds (quantity &gt; 0)
     */
    public OverlapReport analyze(Set<String> portfolioStocks) {
        Objects.requireNonNull(portfolioStocks, "portfolioStocks");

        List<BasketOverlap> overlaps = baskets.stream()
                .map(basket -> new BasketOverlap(basket.name(), overlap(portfolioStocks, basket.stocks())))
                .toList();

        // Strictly-greater comparison: on a tie the basket declared first wins.
        BasketOverlap highest = overlaps.stream()
                .reduce((best, next) -> next.percentage().compareTo(best.percentage()) > 0 ? next : best)
                .orElseThrow();

        Optional<String> dominant = highest.percentage().signum() > 0
                ? Optional.of(highest.basket())
                : Optional.empty();

        return new OverlapReport(overlaps, dominant, RiskFlag.forHighestOverlap(highest.percentage()));
    }

    static BigDecimal overlap(Set<String> portfolio, Set<String> basket) {
        int denominator = portfolio.size() + basket.size();
        if (denominator == 0) {
            return BigDecimal.ZERO.setScale(SCALE);
        }
        Set<String> common = new HashSet<>(portfolio);
        common.retainAll(basket);
        return BigDecimal.valueOf(2L * common.size())
                .multiply(HUNDRED)
                .divide(BigDecimal.valueOf(denominator), SCALE, RoundingMode.HALF_UP);
    }
}
