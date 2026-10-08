package com.fynxt.trading.overlap;

import java.util.List;

/**
 * The benchmark baskets defined by the specification, in display order.
 */
public final class BenchmarkBaskets {

    public static final List<Basket> DEFAULT = List.of(
            Basket.of("TECH_HEAVY", "AAPL", "MSFT", "GOOGL", "TSLA", "NVDA"),
            Basket.of("FINANCE_HEAVY", "JPM", "GS", "BAC", "MS", "WFC"),
            Basket.of("BALANCED", "AAPL", "JPM", "XOM", "JNJ", "TSLA"));

    private BenchmarkBaskets() {
    }
}
