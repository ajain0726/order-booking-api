package com.fynxt.trading.domain;

import com.fynxt.trading.exception.InvalidOrderStateException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * A trade order. The state machine (PENDING -> FILLED | CANCELLED) lives here,
 * so no caller can move an order through an illegal transition.
 */
@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "trader_id", nullable = false, length = 32)
    private String traderId;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "stock_symbol", nullable = false)
    private Stock stock;

    @Enumerated(EnumType.STRING)
    @Column(name = "side", nullable = false, length = 4)
    private OrderSide side;

    @Column(name = "quantity", nullable = false)
    private long quantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    private OrderStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Order() {
    }

    public Order(String traderId, Stock stock, OrderSide side, long quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive but was " + quantity);
        }
        this.traderId = traderId;
        this.stock = stock;
        this.side = side;
        this.quantity = quantity;
        this.status = OrderStatus.PENDING;
        this.createdAt = Timestamps.now();
        this.updatedAt = createdAt;
    }

    public void markFilled() {
        transitionTo(OrderStatus.FILLED, "filled");
    }

    public void markCancelled() {
        transitionTo(OrderStatus.CANCELLED, "cancelled");
    }

    private void transitionTo(OrderStatus target, String action) {
        if (status != OrderStatus.PENDING) {
            throw new InvalidOrderStateException(id, status, action);
        }
        this.status = target;
        this.updatedAt = Timestamps.now();
    }

    public boolean isPending() {
        return status == OrderStatus.PENDING;
    }

    public Long getId() {
        return id;
    }

    public String getTraderId() {
        return traderId;
    }

    public Stock getStock() {
        return stock;
    }

    public OrderSide getSide() {
        return side;
    }

    public long getQuantity() {
        return quantity;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
