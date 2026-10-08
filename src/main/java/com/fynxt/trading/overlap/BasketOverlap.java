package com.fynxt.trading.overlap;

import java.math.BigDecimal;

/**
 * Overlap of a portfolio with one basket, as a percentage with scale 2.
 */
public record BasketOverlap(String basket, BigDecimal percentage) {
}
