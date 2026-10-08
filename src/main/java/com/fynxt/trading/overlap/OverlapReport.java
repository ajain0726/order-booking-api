package com.fynxt.trading.overlap;

import java.util.List;
import java.util.Optional;

/**
 * Result of comparing a portfolio against all benchmark baskets.
 *
 * @param overlaps       one entry per basket, in basket order
 * @param dominantBasket basket with the highest overlap; empty when every overlap is zero
 * @param riskFlag       risk derived from the highest overlap
 */
public record OverlapReport(List<BasketOverlap> overlaps, Optional<String> dominantBasket, RiskFlag riskFlag) {

    public OverlapReport {
        overlaps = List.copyOf(overlaps);
    }
}
