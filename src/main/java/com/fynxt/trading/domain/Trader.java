package com.fynxt.trading.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * A trader. Besides identity, the row acts as the per-trader lock that
 * serialises every state-changing operation for that trader.
 */
@Entity
@Table(name = "traders")
public class Trader {

    @Id
    @Column(name = "id", length = 32)
    private String id;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Trader() {
    }

    public Trader(String id) {
        this.id = id;
        this.createdAt = Timestamps.now();
    }

    public String getId() {
        return id;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
