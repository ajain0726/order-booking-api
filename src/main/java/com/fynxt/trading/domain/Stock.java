package com.fynxt.trading.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Reference data: a ticker and the single sector it belongs to.
 */
@Entity
@Table(name = "stocks")
public class Stock {

    @Id
    @Column(name = "symbol", length = 16)
    private String symbol;

    @Column(name = "sector", nullable = false, length = 32)
    private String sector;

    protected Stock() {
    }

    public Stock(String symbol, String sector) {
        this.symbol = symbol;
        this.sector = sector;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getSector() {
        return sector;
    }
}
