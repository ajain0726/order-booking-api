-- =====================================================================
-- Order Booking & Portfolio API - relational schema (MySQL 8.0.16+).
-- Also runs unchanged on the embedded H2 database in MySQL mode.
-- InnoDB is required: it provides row locks, foreign keys and transactions.
-- =====================================================================

-- A trader. Rows are created on first use; the row doubles as the
-- per-trader lock (SELECT ... FOR UPDATE) that serialises order placement,
-- fills, cancels and direct portfolio additions for one trader.
CREATE TABLE IF NOT EXISTS traders (
    id          VARCHAR(32)  NOT NULL,
    created_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_traders PRIMARY KEY (id)
) ENGINE = InnoDB;

-- Reference data: every ticker belongs to exactly one sector.
CREATE TABLE IF NOT EXISTS stocks (
    symbol      VARCHAR(16)  NOT NULL,
    sector      VARCHAR(32)  NOT NULL,
    CONSTRAINT pk_stocks PRIMARY KEY (symbol)
) ENGINE = InnoDB;

-- Current position of a trader in a stock.
CREATE TABLE IF NOT EXISTS holdings (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    trader_id     VARCHAR(32)  NOT NULL,
    stock_symbol  VARCHAR(16)  NOT NULL,
    quantity      BIGINT       NOT NULL,
    updated_at    DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_holdings PRIMARY KEY (id),
    CONSTRAINT uq_holdings_trader_stock UNIQUE (trader_id, stock_symbol),
    CONSTRAINT fk_holdings_trader FOREIGN KEY (trader_id) REFERENCES traders (id),
    CONSTRAINT fk_holdings_stock  FOREIGN KEY (stock_symbol) REFERENCES stocks (symbol),
    -- Last line of defence: a position can never go short.
    CONSTRAINT ck_holdings_quantity_non_negative CHECK (quantity >= 0)
) ENGINE = InnoDB;

-- Orders. Lifecycle: PENDING -> FILLED | PENDING -> CANCELLED.
CREATE TABLE IF NOT EXISTS orders (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    trader_id     VARCHAR(32)  NOT NULL,
    stock_symbol  VARCHAR(16)  NOT NULL,
    side          VARCHAR(4)   NOT NULL,
    quantity      BIGINT       NOT NULL,
    status        VARCHAR(10)  NOT NULL,
    created_at    DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at    DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_orders PRIMARY KEY (id),
    CONSTRAINT fk_orders_trader FOREIGN KEY (trader_id) REFERENCES traders (id),
    CONSTRAINT fk_orders_stock  FOREIGN KEY (stock_symbol) REFERENCES stocks (symbol),
    CONSTRAINT ck_orders_side     CHECK (side IN ('BUY', 'SELL')),
    CONSTRAINT ck_orders_status   CHECK (status IN ('PENDING', 'FILLED', 'CANCELLED')),
    CONSTRAINT ck_orders_quantity CHECK (quantity > 0),
    -- Supports the "max 3 PENDING per trader" count and reserved-SELL sum.
    -- Declared inline because MySQL has no CREATE INDEX IF NOT EXISTS.
    INDEX ix_orders_trader_status (trader_id, status)
) ENGINE = InnoDB;
