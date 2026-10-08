package com.fynxt.trading.overlap;

import java.util.Objects;
import java.util.Set;

/**
 * A named benchmark basket of stock tickers. Immutable.
 */
public record Basket(String name, Set<String> stocks) {

    public Basket {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(stocks, "stocks");
        if (stocks.isEmpty()) {
            throw new IllegalArgumentException("Basket " + name + " must contain at least one stock");
        }
        stocks = Set.copyOf(stocks);
    }

    public static Basket of(String name, String... stocks) {
        return new Basket(name, Set.of(stocks));
    }
}
