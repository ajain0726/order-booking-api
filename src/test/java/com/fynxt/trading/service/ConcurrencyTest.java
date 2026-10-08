package com.fynxt.trading.service;

import com.fynxt.trading.domain.Order;
import com.fynxt.trading.domain.OrderSide;
import com.fynxt.trading.domain.OrderStatus;
import com.fynxt.trading.exception.InsufficientHoldingsException;
import com.fynxt.trading.exception.InvalidOrderStateException;
import com.fynxt.trading.exception.PendingOrderLimitExceededException;
import com.fynxt.trading.repository.OrderRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Fires many requests for the same trader at the same instant and checks the
 * invariants still hold. Without the per-trader lock these fail intermittently
 * (e.g. 4+ pending orders, double fills, lost holding updates).
 */
@SpringBootTest
public class ConcurrencyTest {

    private static final int THREADS = 16;

    @Autowired
    private OrderService orderService;

    @Autowired
    private PortfolioService portfolioService;

    @Autowired
    private OrderRepository orderRepository;

    private ExecutorService pool;
    private String trader;

    @BeforeEach
    void setUp() {
        pool = Executors.newFixedThreadPool(THREADS);
        trader = "T-" + UUID.randomUUID().toString().substring(0, 12);
    }

    @AfterEach
    void tearDown() {
        pool.shutdownNow();
    }

    @RepeatedTest(3)
    void pendingLimitHoldsUnderConcurrentPlacement() throws Exception {
        List<Outcome> outcomes = runConcurrently(THREADS, i ->
                orderService.placeOrder(new PlaceOrderCommand(trader, "AAPL", "TECH", 1, OrderSide.BUY)));

        assertThat(count(outcomes, null)).isEqualTo(3);
        assertThat(count(outcomes, PendingOrderLimitExceededException.class)).isEqualTo(THREADS - 3);
        assertThat(orderRepository.countByTraderIdAndStatus(trader, OrderStatus.PENDING)).isEqualTo(3);
    }

    @RepeatedTest(3)
    void concurrentSellsNeverOversellHoldings() throws Exception {
        portfolioService.addHolding(new AddHoldingCommand(trader, "AAPL", "TECH", 100));

        List<Outcome> outcomes = runConcurrently(THREADS, i ->
                orderService.placeOrder(new PlaceOrderCommand(trader, "AAPL", "TECH", 40, OrderSide.SELL)));

        // 100 shares / 40 per order -> only two SELLs can ever be reserved.
        assertThat(count(outcomes, null)).isEqualTo(2);
        assertThat(count(outcomes, InsufficientHoldingsException.class)).isEqualTo(THREADS - 2);
        assertThat(orderRepository.sumQuantity(trader, "AAPL", OrderSide.SELL, OrderStatus.PENDING))
                .isEqualTo(80);
    }

    @RepeatedTest(3)
    void anOrderIsFilledExactlyOnce() throws Exception {
        Order order = orderService.placeOrder(new PlaceOrderCommand(trader, "AAPL", "TECH", 25, OrderSide.BUY));

        List<Outcome> outcomes = runConcurrently(THREADS, i -> orderService.fillOrder(order.getId()));

        assertThat(count(outcomes, null)).isEqualTo(1);
        assertThat(count(outcomes, InvalidOrderStateException.class)).isEqualTo(THREADS - 1);
        assertThat(portfolioService.getPortfolio(trader).positions()).containsEntry("AAPL", 25L);
    }

    @RepeatedTest(3)
    void fillAndCancelRaceHasOneWinner() throws Exception {
        Order order = orderService.placeOrder(new PlaceOrderCommand(trader, "AAPL", "TECH", 10, OrderSide.BUY));

        List<Outcome> outcomes = runConcurrently(THREADS, i -> i % 2 == 0
                ? orderService.fillOrder(order.getId())
                : orderService.cancelOrder(order.getId()));

        assertThat(count(outcomes, null)).isEqualTo(1);
        OrderStatus finalStatus = orderService.getOrder(order.getId()).getStatus();
        long expectedPosition = finalStatus == OrderStatus.FILLED ? 10 : 0;
        assertThat(portfolioService.getPortfolio(trader).positions().getOrDefault("AAPL", 0L))
                .isEqualTo(expectedPosition);
    }

    @Test
    void concurrentAdditionsAreNotLost() throws Exception {
        // A brand-new stock and trader: also exercises the first-use registration race.
        String newStock = "Z" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        List<Outcome> outcomes = runConcurrently(THREADS, i ->
                portfolioService.addHolding(new AddHoldingCommand(trader, newStock, "OTHER", 5)));

        assertThat(count(outcomes, null)).isEqualTo(THREADS);
        assertThat(portfolioService.getPortfolio(trader).positions()).containsEntry(newStock, 5L * THREADS);
    }

    // ---------------------------------------------------------------- helpers

    private record Outcome(Class<? extends Throwable> failure) {
    }

    @FunctionalInterface
    private interface Task {
        Object run(int index) throws Exception;
    }

    /** Releases all tasks at the same instant via a start gate, then collects outcomes. */
    private List<Outcome> runConcurrently(int n, Task task) throws InterruptedException {
        CountDownLatch ready = new CountDownLatch(n);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<Object>> futures = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            int index = i;
            Callable<Object> callable = () -> {
                ready.countDown();
                go.await();
                return task.run(index);
            };
            futures.add(pool.submit(callable));
        }
        assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
        go.countDown();

        List<Outcome> outcomes = new ArrayList<>();
        for (Future<Object> future : futures) {
            try {
                future.get(30, TimeUnit.SECONDS);
                outcomes.add(new Outcome(null));
            } catch (ExecutionException e) {
                outcomes.add(new Outcome(e.getCause().getClass()));
            } catch (java.util.concurrent.TimeoutException e) {
                throw new AssertionError("Task did not finish (possible deadlock)", e);
            }
        }
        return outcomes;
    }

    private static long count(List<Outcome> outcomes, Class<? extends Throwable> failure) {
        return outcomes.stream().filter(o -> o.failure() == failure).count();
    }
}
