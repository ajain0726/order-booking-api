package com.fynxt.trading.domain;

import com.fynxt.trading.exception.InsufficientHoldingsException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HoldingTest {

    private final Holding holding = new Holding("T1", new Stock("AAPL", "TECH"));

    @Test
    void increaseAndDecrease() {
        holding.increase(100);
        holding.decrease(40);
        assertThat(holding.getQuantity()).isEqualTo(60);
    }

    @Test
    void cannotGoNegative() {
        holding.increase(10);
        assertThatThrownBy(() -> holding.decrease(11)).isInstanceOf(InsufficientHoldingsException.class);
        assertThat(holding.getQuantity()).isEqualTo(10);
    }

    @Test
    void rejectsNonPositiveAmounts() {
        assertThatThrownBy(() -> holding.increase(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> holding.decrease(-1)).isInstanceOf(IllegalArgumentException.class);
    }
}
