package com.fynxt.trading.service;

import java.util.Locale;

/**
 * Canonical forms for user-supplied identifiers, so " aapl" and "AAPL"
 * resolve to the same stock.
 */
final class Identifiers {

    private Identifiers() {
    }

    static String traderId(String raw) {
        return raw.trim();
    }

    static String ticker(String raw) {
        return raw.trim().toUpperCase(Locale.ROOT);
    }

    static String sector(String raw) {
        return raw.trim().toUpperCase(Locale.ROOT);
    }
}
