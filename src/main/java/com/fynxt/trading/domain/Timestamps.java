package com.fynxt.trading.domain;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Current time at the database's TIMESTAMP precision (microseconds), so a value
 * returned before and after a round-trip through the database is identical.
 */
final class Timestamps {

    private Timestamps() {
    }

    static Instant now() {
        return Instant.now().truncatedTo(ChronoUnit.MICROS);
    }
}
