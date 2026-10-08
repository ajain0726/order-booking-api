package com.fynxt.trading.service;

import com.fynxt.trading.domain.Order;
import com.fynxt.trading.domain.OrderSide;
import com.fynxt.trading.domain.OrderStatus;
import com.fynxt.trading.exception.InsufficientHoldingsException;
import com.fynxt.trading.exception.InvalidOrderStateException;
import com.fynxt.trading.exception.OrderNotFoundException;
import com.fynxt.trading.exception.PendingOrderLimitExceededException;
import com.fynxt.trading.exception.StockSectorMismatchException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Business rules against a real (in-memory) database, so constraints,
 * queries and locking are exercised exactly as in production.
 */
@SpringBootTest
public class OrderServiceTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private PortfolioService portfolioService;

    private String trader;

    @BeforeEach
    void newTrader() {
        trader = "T-" + UUID.randomUUID().toString().substring(0, 12);
    }

    private Order place(String stock, OrderSide side, long qty) {
        return orderService.placeOrder(new PlaceOrderCommand(trader, stock, "TECH", qty, side));
    }

    private long position(String stock) {
        return portfolioService.getPortfolio(trader).positions().getOrDefault(stock, 0L);
    }

    @Test
    void buyFillIncreasesHolding() {
        Order order = place("AAPL", OrderSide.BUY, 50);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(position("AAPL")).isZero();

        Order filled = orderService.fillOrder(order.getId());

        assertThat(filled.getStatus()).isEqualTo(OrderStatus.FILLED);
        assertThat(position("AAPL")).isEqualTo(50);
    }

    @Test
    void sellFillDecreasesHolding() {
        orderService.fillOrder(place("AAPL", OrderSide.BUY, 100).getId());

        orderService.fillOrder(place("AAPL", OrderSide.SELL, 30).getId());

        assertThat(position("AAPL")).isEqualTo(70);
    }

    @Test
    void sellingEntirePositionRemovesItFromPortfolio() {
        orderService.fillOrder(place("AAPL", OrderSide.BUY, 10).getId());
        orderService.fillOrder(place("AAPL", OrderSide.SELL, 10).getId());

        assertThat(portfolioService.getPortfolio(trader).positions()).doesNotContainKey("AAPL");
    }

    @Test
    void fourthPendingOrderIsRejected() {
        place("AAPL", OrderSide.BUY, 1);
        place("MSFT", OrderSide.BUY, 1);
        place("NVDA", OrderSide.BUY, 1);

        assertThatThrownBy(() -> place("TSLA", OrderSide.BUY, 1))
                .isInstanceOf(PendingOrderLimitExceededException.class);
    }

    @Test
    void fillingOrCancellingFreesAPendingSlot() {
        Order a = place("AAPL", OrderSide.BUY, 1);
        Order b = place("MSFT", OrderSide.BUY, 1);
        place("NVDA", OrderSide.BUY, 1);

        orderService.fillOrder(a.getId());
        place("TSLA", OrderSide.BUY, 1);
        orderService.cancelOrder(b.getId());
        place("GOOGL", OrderSide.BUY, 1);

        assertThatThrownBy(() -> place("AMD", OrderSide.BUY, 1))
                .isInstanceOf(PendingOrderLimitExceededException.class);
    }

    @Test
    void pendingLimitIsPerTrader() {
        place("AAPL", OrderSide.BUY, 1);
        place("AAPL", OrderSide.BUY, 1);
        place("AAPL", OrderSide.BUY, 1);

        String other = trader + "-B";
        Order otherOrder = orderService.placeOrder(new PlaceOrderCommand(other, "AAPL", "TECH", 1, OrderSide.BUY));
        assertThat(otherOrder.getStatus()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    void sellWithoutHoldingIsRejected() {
        assertThatThrownBy(() -> place("AAPL", OrderSide.SELL, 1))
                .isInstanceOf(InsufficientHoldingsException.class)
                .hasMessageContaining("only 0 shares available");
    }

    @Test
    void sellMoreThanHeldIsRejected() {
        portfolioService.addHolding(new AddHoldingCommand(trader, "AAPL", "TECH", 20));

        assertThatThrownBy(() -> place("AAPL", OrderSide.SELL, 21))
                .isInstanceOf(InsufficientHoldingsException.class);
        assertThat(place("AAPL", OrderSide.SELL, 20).getStatus()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    void pendingSellsReserveShares() {
        portfolioService.addHolding(new AddHoldingCommand(trader, "AAPL", "TECH", 100));
        place("AAPL", OrderSide.SELL, 60);

        assertThatThrownBy(() -> place("AAPL", OrderSide.SELL, 50))
                .isInstanceOf(InsufficientHoldingsException.class)
                .hasMessageContaining("only 40 shares available");
    }

    @Test
    void cancellingASellReleasesReservedShares() {
        portfolioService.addHolding(new AddHoldingCommand(trader, "AAPL", "TECH", 100));
        Order sell = place("AAPL", OrderSide.SELL, 100);
        orderService.cancelOrder(sell.getId());

        assertThat(place("AAPL", OrderSide.SELL, 100).getStatus()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    void pendingBuyDoesNotCountAsHeldForSell() {
        place("AAPL", OrderSide.BUY, 100);

        assertThatThrownBy(() -> place("AAPL", OrderSide.SELL, 1))
                .isInstanceOf(InsufficientHoldingsException.class);
    }

    @Test
    void cannotFillOrCancelTwice() {
        Order filled = place("AAPL", OrderSide.BUY, 5);
        orderService.fillOrder(filled.getId());
        Order cancelled = place("AAPL", OrderSide.BUY, 5);
        orderService.cancelOrder(cancelled.getId());

        assertThatThrownBy(() -> orderService.fillOrder(filled.getId()))
                .isInstanceOf(InvalidOrderStateException.class);
        assertThatThrownBy(() -> orderService.cancelOrder(filled.getId()))
                .isInstanceOf(InvalidOrderStateException.class)
                .hasMessageContaining("is FILLED");
        assertThatThrownBy(() -> orderService.fillOrder(cancelled.getId()))
                .isInstanceOf(InvalidOrderStateException.class)
                .hasMessageContaining("is CANCELLED");
        assertThat(position("AAPL")).isEqualTo(5);
    }

    @Test
    void unknownOrderIsNotFound() {
        assertThatThrownBy(() -> orderService.fillOrder(Long.MAX_VALUE)).isInstanceOf(OrderNotFoundException.class);
        assertThatThrownBy(() -> orderService.cancelOrder(Long.MAX_VALUE)).isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    void tickerAndSectorAreNormalised() {
        Order order = orderService.placeOrder(new PlaceOrderCommand(trader, " aapl ", "tech", 5, OrderSide.BUY));

        assertThat(order.getStock().getSymbol()).isEqualTo("AAPL");
        assertThat(order.getStock().getSector()).isEqualTo("TECH");
    }

    @Test
    void conflictingSectorForKnownStockIsRejected() {
        place("AAPL", OrderSide.BUY, 1);

        assertThatThrownBy(() -> orderService.placeOrder(
                new PlaceOrderCommand(trader, "AAPL", "FINANCE", 1, OrderSide.BUY)))
                .isInstanceOf(StockSectorMismatchException.class);
    }
}
