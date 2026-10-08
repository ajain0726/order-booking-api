-- =====================================================================
-- Demo seed data, loaded on every start.
-- Traders also come into existence automatically on their first order
-- or holding, so this file is only a convenience for trying the API.
-- Disable it with: --spring.sql.init.data-locations=
--
-- Every insert is guarded with NOT EXISTS, so running the script against
-- a database that already holds the seed (e.g. a MySQL volume that
-- survived a restart) changes nothing. Plain SELECT ... UNION ALL keeps it
-- portable between MySQL and H2.
-- =====================================================================

INSERT INTO traders (id)
SELECT v.id FROM (
    SELECT 'T001' AS id UNION ALL SELECT 'T002' UNION ALL SELECT 'T003'
) v
WHERE NOT EXISTS (SELECT 1 FROM traders t WHERE t.id = v.id);

INSERT INTO stocks (symbol, sector)
SELECT v.symbol, v.sector FROM (
              SELECT 'AAPL' AS symbol, 'TECH' AS sector
    UNION ALL SELECT 'TSLA', 'TECH'
    UNION ALL SELECT 'NVDA', 'TECH'
    UNION ALL SELECT 'MSFT', 'TECH'
    UNION ALL SELECT 'GOOGL', 'TECH'
    UNION ALL SELECT 'JPM', 'FINANCE'
    UNION ALL SELECT 'GS', 'FINANCE'
    UNION ALL SELECT 'BAC', 'FINANCE'
    UNION ALL SELECT 'MS', 'FINANCE'
    UNION ALL SELECT 'WFC', 'FINANCE'
    UNION ALL SELECT 'XOM', 'ENERGY'
    UNION ALL SELECT 'JNJ', 'HEALTHCARE'
) v
WHERE NOT EXISTS (SELECT 1 FROM stocks s WHERE s.symbol = v.symbol);

-- T001: tech-heavy (worked example in the spec -> HIGH risk, TECH_HEAVY 75%)
-- T002: finance-heavy (-> HIGH risk, FINANCE_HEAVY 75%)
-- T003: energy + healthcare (-> MEDIUM risk, BALANCED 57.14%)
INSERT INTO holdings (trader_id, stock_symbol, quantity)
SELECT v.trader_id, v.stock_symbol, v.quantity FROM (
              SELECT 'T001' AS trader_id, 'AAPL' AS stock_symbol, 150 AS quantity
    UNION ALL SELECT 'T001', 'TSLA', 80
    UNION ALL SELECT 'T001', 'NVDA', 100
    UNION ALL SELECT 'T002', 'JPM', 200
    UNION ALL SELECT 'T002', 'GS', 50
    UNION ALL SELECT 'T002', 'BAC', 120
    UNION ALL SELECT 'T003', 'XOM', 90
    UNION ALL SELECT 'T003', 'JNJ', 30
) v
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
SELECT v.trader_id, v.stock_symbol, v.side, v.quantity, v.status FROM (
              SELECT 1 AS seq, 'T001' AS trader_id, 'AAPL' AS stock_symbol, 'BUY' AS side,
                     150 AS quantity, 'FILLED' AS status
    UNION ALL SELECT 2, 'T001', 'MSFT', 'BUY', 20, 'PENDING'
    UNION ALL SELECT 3, 'T001', 'AAPL', 'SELL', 50, 'PENDING'
    UNION ALL SELECT 4, 'T002', 'WFC', 'BUY', 100, 'PENDING'
    UNION ALL SELECT 5, 'T002', 'GS', 'SELL', 10, 'CANCELLED'
) v
WHERE NOT EXISTS (SELECT 1 FROM orders o WHERE o.trader_id IN ('T001', 'T002', 'T003'))
ORDER BY v.seq;
