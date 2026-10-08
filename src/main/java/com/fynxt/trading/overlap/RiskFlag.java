package com.fynxt.trading.overlap;

import java.math.BigDecimal;

/**
 * Concentration risk derived from the highest basket overlap.
 */
public enum RiskFlag {
    HIGH(new BigDecimal("60")),
    MEDIUM(new BigDecimal("40")),
    LOW(BigDecimal.ZERO);

    private final BigDecimal minimumOverlap;

    RiskFlag(BigDecimal minimumOverlap) {
        this.minimumOverlap = minimumOverlap;
    }

    /** Declaration order is highest first, so the first threshold met wins. */
    public static RiskFlag forHighestOverlap(BigDecimal highestOverlap) {
        for (RiskFlag flag : values()) {
            if (highestOverlap.compareTo(flag.minimumOverlap) >= 0) {
                return flag;
            }
        }
        return LOW;
    }
}
