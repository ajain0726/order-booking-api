package com.fynxt.trading.config;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Externalised business limits, bound from the {@code trading.*} properties.
 */
@Validated
@ConfigurationProperties(prefix = "trading")
public record TradingProperties(@Min(1) int maxPendingOrdersPerTrader) {
}
