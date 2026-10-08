package com.fynxt.trading.domain;

import com.fynxt.trading.exception.InsufficientHoldingsException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * A trader's position in one stock. Quantity never goes below zero.
 */
@Entity
@Table(name = "holdings")
public class Holding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "trader_id", nullable = false, length = 32)
    private String traderId;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "stock_symbol", nullable = false)
    private Stock stock;

    @Column(name = "quantity", nullable = false)
    private long quantity;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Holding() {
    }

    public Holding(String traderId, Stock stock) {
        this.traderId = traderId;
        this.stock = stock;
        this.quantity = 0;
        this.updatedAt = Timestamps.now();
    }

    public void increase(long amount) {
        requirePositive(amount);
        this.quantity = Math.addExact(quantity, amount);
        this.updatedAt = Timestamps.now();
    }

    public void decrease(long amount) {
        requirePositive(amount);
        if (amount > quantity) {
            throw new InsufficientHoldingsException(traderId, stock.getSymbol(), amount, quantity);
        }
        this.quantity -= amount;
        this.updatedAt = Timestamps.now();
    }

    private static void requirePositive(long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive but was " + amount);
        }
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

    public long getQuantity() {
        return quantity;
    }
}
