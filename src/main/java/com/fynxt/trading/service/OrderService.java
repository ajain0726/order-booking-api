package com.fynxt.trading.service;

import com.fynxt.trading.config.TradingProperties;
import com.fynxt.trading.domain.Holding;
import com.fynxt.trading.domain.Order;
import com.fynxt.trading.domain.OrderSide;
import com.fynxt.trading.domain.OrderStatus;
import com.fynxt.trading.domain.Stock;
import com.fynxt.trading.exception.InsufficientHoldingsException;
import com.fynxt.trading.exception.OrderNotFoundException;
import com.fynxt.trading.exception.PendingOrderLimitExceededException;
import com.fynxt.trading.repository.HoldingRepository;
import com.fynxt.trading.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.function.Consumer;

/**
 * Order lifecycle: place, fill, cancel.
 *
 * <p>Concurrency model: every mutation takes the trader's row lock first, then
 * re-reads state and checks business rules while holding it. Rules are
 * therefore evaluated against state no other request can change until commit.
 */
@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final HoldingRepository holdingRepository;
    private final ReferenceDataRegistrar registrar;
    private final TraderLock traderLock;
    private final TradingProperties properties;
    private final TransactionTemplate tx;
    private final TransactionTemplate readOnlyTx;

    public OrderService(OrderRepository orderRepository,
                        HoldingRepository holdingRepository,
                        ReferenceDataRegistrar registrar,
                        TraderLock traderLock,
                        TradingProperties properties,
                        PlatformTransactionManager transactionManager) {
        this.orderRepository = orderRepository;
        this.holdingRepository = holdingRepository;
        this.registrar = registrar;
        this.traderLock = traderLock;
        this.properties = properties;
        this.tx = Transactions.readCommitted(transactionManager);
        this.readOnlyTx = Transactions.readOnly(transactionManager);
    }

    public Order placeOrder(PlaceOrderCommand command) {
        String traderId = Identifiers.traderId(command.traderId());
        // Reference data is registered before the main transaction (see ReferenceDataRegistrar).
        registrar.ensureTrader(traderId);
        Stock stock = registrar.ensureStock(Identifiers.ticker(command.stock()), Identifiers.sector(command.sector()));

        Order placed = tx.execute(status -> {
            traderLock.acquire(traderId);
            enforcePendingLimit(traderId);
            if (command.side() == OrderSide.SELL) {
                enforceSellable(traderId, stock.getSymbol(), command.quantity());
            }
            return orderRepository.save(new Order(traderId, stock, command.side(), command.quantity()));
        });

        log.info("Placed order {}: trader={} {} {} {}", placed.getId(), traderId, placed.getSide(),
                placed.getQuantity(), stock.getSymbol());
        return placed;
    }

    public Order fillOrder(Long orderId) {
        Order filled = mutatePendingOrder(orderId, order -> {
            order.markFilled();
            applyFillToHolding(order);
        });
        log.info("Filled order {}: trader={} {} {} {}", orderId, filled.getTraderId(), filled.getSide(),
                filled.getQuantity(), filled.getStock().getSymbol());
        return filled;
    }

    public Order cancelOrder(Long orderId) {
        Order cancelled = mutatePendingOrder(orderId, Order::markCancelled);
        log.info("Cancelled order {} for trader {}", orderId, cancelled.getTraderId());
        return cancelled;
    }

    public Order getOrder(Long orderId) {
        return readOnlyTx.execute(status -> orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId)));
    }

    /** Lock the owning trader, then load the order fresh and apply the transition. */
    private Order mutatePendingOrder(Long orderId, Consumer<Order> transition) {
        return tx.execute(status -> {
            String traderId = orderRepository.findTraderIdById(orderId)
                    .orElseThrow(() -> new OrderNotFoundException(orderId));
            traderLock.acquire(traderId);
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new OrderNotFoundException(orderId));
            transition.accept(order);
            return order;
        });
    }

    private void enforcePendingLimit(String traderId) {
        int limit = properties.maxPendingOrdersPerTrader();
        if (orderRepository.countByTraderIdAndStatus(traderId, OrderStatus.PENDING) >= limit) {
            throw new PendingOrderLimitExceededException(traderId, limit);
        }
    }

    /**
     * A SELL may only be placed against shares that are held and not already
     * reserved by other PENDING SELL orders, so the trader can never commit to
     * selling the same shares twice.
     */
    private void enforceSellable(String traderId, String symbol, long quantity) {
        long held = holdingRepository.findByTraderAndStock(traderId, symbol)
                .map(Holding::getQuantity)
                .orElse(0L);
        long reserved = orderRepository.sumQuantity(traderId, symbol, OrderSide.SELL, OrderStatus.PENDING);
        long available = held - reserved;
        if (quantity > available) {
            throw new InsufficientHoldingsException(traderId, symbol, quantity, Math.max(available, 0));
        }
    }

    private void applyFillToHolding(Order order) {
        Holding holding = holdingRepository.findByTraderAndStock(order.getTraderId(), order.getStock().getSymbol())
                .orElseGet(() -> new Holding(order.getTraderId(), order.getStock()));
        switch (order.getSide()) {
            case BUY -> holding.increase(order.getQuantity());
            case SELL -> holding.decrease(order.getQuantity());
        }
        holdingRepository.save(holding);
    }
}
