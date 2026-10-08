package com.fynxt.trading.domain;

import com.fynxt.trading.exception.InvalidOrderStateException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderTest {

    private final Stock aapl = new Stock("AAPL", "TECH");

    @Test
    void newOrderIsPending() {
        assertThat(new Order("T1", aapl, OrderSide.BUY, 10).getStatus()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    void pendingCanBeFilled() {
        Order order = new Order("T1", aapl, OrderSide.BUY, 10);
        order.markFilled();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.FILLED);
    }

    @Test
    void pendingCanBeCancelled() {
        Order order = new Order("T1", aapl, OrderSide.BUY, 10);
        order.markCancelled();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void filledOrderCannotBeFilledOrCancelled() {
        Order order = new Order("T1", aapl, OrderSide.BUY, 10);
        order.markFilled();

        assertThatThrownBy(order::markFilled).isInstanceOf(InvalidOrderStateException.class)
                .hasMessageContaining("because it is FILLED");
        assertThatThrownBy(order::markCancelled).isInstanceOf(InvalidOrderStateException.class);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.FILLED);
    }

    @Test
    void cancelledOrderCannotBeFilled() {
        Order order = new Order("T1", aapl, OrderSide.SELL, 10);
        order.markCancelled();

        assertThatThrownBy(order::markFilled).isInstanceOf(InvalidOrderStateException.class)
                .hasMessageContaining("because it is CANCELLED");
    }

    @Test
    void quantityMustBePositive() {
        assertThatThrownBy(() -> new Order("T1", aapl, OrderSide.BUY, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
