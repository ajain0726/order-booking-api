-- =====================================================================
-- Demo seed data, loaded on every start of the in-memory database.
-- Traders also come into existence automatically on their first order
-- or holding, so this file is only a convenience for trying the API.
-- Disable it with: --spring.sql.init.data-locations=
--
-- Every insert is guarded with NOT EXISTS so the script is safe to run
-- again against a database that already holds the seed (portable SQL,
-- works on H2 and PostgreSQL).
-- =====================================================================

INSERT INTO traders (id)
SELECT v.id FROM (VALUES ('T001'), ('T002'), ('T003')) AS v(id)
WHERE NOT EXISTS (SELECT 1 FROM traders t WHERE t.id = v.id);

INSERT INTO stocks (symbol, sector)
SELECT v.symbol, v.sector FROM (VALUES
    ('AAPL', 'TECH'), ('TSLA', 'TECH'), ('NVDA', 'TECH'), ('MSFT', 'TECH'), ('GOOGL', 'TECH'),
    ('JPM', 'FINANCE'), ('GS', 'FINANCE'), ('BAC', 'FINANCE'), ('MS', 'FINANCE'), ('WFC', 'FINANCE'),
    ('XOM', 'ENERGY'), ('JNJ', 'HEALTHCARE')) AS v(symbol, sector)
WHERE NOT EXISTS (SELECT 1 FROM stocks s WHERE s.symbol = v.symbol);

-- T001: tech-heavy (worked example in the spec -> HIGH risk, TECH_HEAVY 75%)
-- T002: finance-heavy (-> HIGH risk, FINANCE_HEAVY 75%)
-- T003: energy + healthcare (-> MEDIUM risk, BALANCED 57.14%)
INSERT INTO holdings (trader_id, stock_symbol, quantity)
SELECT v.trader_id, v.stock_symbol, v.quantity FROM (VALUES
    ('T001', 'AAPL', 150), ('T001', 'TSLA', 80), ('T001', 'NVDA', 100),
    ('T002', 'JPM', 200), ('T002', 'GS', 50), ('T002', 'BAC', 120),
    ('T003', 'XOM', 90), ('T003', 'JNJ', 30)) AS v(trader_id, stock_symbol, quantity)
WHERE NOT EXISTS (SELECT 1 FROM holdings h
                  WHERE h.trader_id = v.trader_id AND h.stock_symbol = v.stock_symbol);

-- On an empty database these get ids 1..5 (orders have no natural key, so the
-- whole batch is skipped if the seeded traders already have any orders):
--   1 T001 BUY  AAPL 150 FILLED     (fill/cancel -> 409 INVALID_ORDER_STATE)
--   2 T001 BUY  MSFT  20 PENDING    (fill it to add MSFT to T001)
--   3 T001 SELL AAPL  50 PENDING    (reserves 50 of T001's 150 AAPL)
--   4 T002 BUY  WFC  100 PENDING
--   5 T002 SELL GS    10 CANCELLED
INSERT INTO orders (trader_id, stock_symbol, side, quantity, status)
SELECT v.trader_id, v.stock_symbol, v.side, v.quantity, v.status FROM (VALUES
    (1, 'T001', 'AAPL', 'BUY', 150, 'FILLED'),
    (2, 'T001', 'MSFT', 'BUY', 20, 'PENDING'),
    (3, 'T001', 'AAPL', 'SELL', 50, 'PENDING'),
    (4, 'T002', 'WFC', 'BUY', 100, 'PENDING'),
    (5, 'T002', 'GS', 'SELL', 10, 'CANCELLED'))
    AS v(seq, trader_id, stock_symbol, side, quantity, status)
WHERE NOT EXISTS (SELECT 1 FROM orders o WHERE o.trader_id IN ('T001', 'T002', 'T003'))
ORDER BY v.seq;
