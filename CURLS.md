# Sample curl commands

Run these in order against a **fresh start** of the app (`mvn spring-boot:run`), which loads the demo traders T001 to T003 and orders 1 to 5 from `data.sql`. Each response below is real output from running the command. Timestamps will differ.

## 1. Get a trader's portfolio (endpoint 4)

```bash
curl http://localhost:8080/api/v1/traders/T001/portfolio
```

HTTP 200

```json
{"traderId": "T001", "positions": {"AAPL": 150, "NVDA": 100, "TSLA": 80}, "sectorBreakdown": {"TECH": 330}}
```

## 2. Sector overlap analysis (endpoint 5)

```bash
curl http://localhost:8080/api/v1/traders/T001/portfolio/overlap
```

HTTP 200

```json
{"overlaps": [{"basket": "TECH_HEAVY", "overlap": "75.00%"}, {"basket": "FINANCE_HEAVY", "overlap": "0.00%"}, {"basket": "BALANCED", "overlap": "50.00%"}], "dominantBasket": "TECH_HEAVY", "riskFlag": "HIGH"}
```

## 3. Place a BUY order (endpoint 1). T001 already has 2 PENDING orders, so this is the 3rd

```bash
curl -X POST http://localhost:8080/api/v1/orders -H 'Content-Type: application/json' -d '{"traderId":"T001","stock":"GOOGL","sector":"TECH","quantity":10,"side":"BUY"}'
```

HTTP 201

```json
{"id": 6, "traderId": "T001", "stock": "GOOGL", "sector": "TECH", "quantity": 10, "side": "BUY", "status": "PENDING", "createdAt": "2026-10-09T03:28:24.604192Z", "updatedAt": "2026-10-09T03:28:24.604192Z"}
```

## 4. Error: a 4th PENDING order is rejected (422)

```bash
curl -X POST http://localhost:8080/api/v1/orders -H 'Content-Type: application/json' -d '{"traderId":"T001","stock":"MSFT","sector":"TECH","quantity":5,"side":"BUY"}'
```

HTTP 422

```json
{"timestamp": "2026-10-09T03:28:24.702514385Z", "status": 422, "error": "Unprocessable Entity", "code": "PENDING_ORDER_LIMIT_EXCEEDED", "message": "Trader T001 already has 3 PENDING orders (maximum is 3); fill or cancel one before placing another", "path": "/api/v1/orders"}
```

## 5. Fill a PENDING order (endpoint 2): order 2 is BUY MSFT 20

```bash
curl -X POST http://localhost:8080/api/v1/orders/2/fill
```

HTTP 200

```json
{"id": 2, "traderId": "T001", "stock": "MSFT", "sector": "TECH", "quantity": 20, "side": "BUY", "status": "FILLED", "createdAt": "2026-10-09T03:28:20.138778Z", "updatedAt": "2026-10-09T03:28:24.752643Z"}
```

## 6. Error: filling it again is rejected (409)

```bash
curl -X POST http://localhost:8080/api/v1/orders/2/fill
```

HTTP 409

```json
{"timestamp": "2026-10-09T03:28:24.805462948Z", "status": 409, "error": "Conflict", "code": "INVALID_ORDER_STATE", "message": "Order 2 cannot be filled because it is FILLED; only PENDING orders can be filled", "path": "/api/v1/orders/2/fill"}
```

## 7. Cancel a PENDING order (endpoint 3): order 3 is SELL AAPL 50

```bash
curl -X POST http://localhost:8080/api/v1/orders/3/cancel
```

HTTP 200

```json
{"id": 3, "traderId": "T001", "stock": "AAPL", "sector": "TECH", "quantity": 50, "side": "SELL", "status": "CANCELLED", "createdAt": "2026-10-09T03:28:20.138778Z", "updatedAt": "2026-10-09T03:28:24.841973Z"}
```

## 8. Error: cancelling a FILLED order is rejected (409)

```bash
curl -X POST http://localhost:8080/api/v1/orders/1/cancel
```

HTTP 409

```json
{"timestamp": "2026-10-09T03:28:24.894955551Z", "status": 409, "error": "Conflict", "code": "INVALID_ORDER_STATE", "message": "Order 1 cannot be cancelled because it is FILLED; only PENDING orders can be cancelled", "path": "/api/v1/orders/1/cancel"}
```

## 9. Read one order

```bash
curl http://localhost:8080/api/v1/orders/6
```

HTTP 200

```json
{"id": 6, "traderId": "T001", "stock": "GOOGL", "sector": "TECH", "quantity": 10, "side": "BUY", "status": "PENDING", "createdAt": "2026-10-09T03:28:24.604192Z", "updatedAt": "2026-10-09T03:28:24.604192Z"}
```

## 10. Error: SELL more shares than held (422)

```bash
curl -X POST http://localhost:8080/api/v1/orders -H 'Content-Type: application/json' -d '{"traderId":"T003","stock":"XOM","sector":"ENERGY","quantity":500,"side":"SELL"}'
```

HTTP 422

```json
{"timestamp": "2026-10-09T03:28:24.990964175Z", "status": 422, "error": "Unprocessable Entity", "code": "INSUFFICIENT_HOLDINGS", "message": "Trader T003 cannot sell 500 XOM: only 90 shares available", "path": "/api/v1/orders"}
```

## 11. Add holdings directly (endpoint 6)

```bash
curl -X POST http://localhost:8080/api/v1/traders/T003/portfolio/holdings -H 'Content-Type: application/json' -d '{"stock":"JPM","sector":"FINANCE","quantity":40}'
```

HTTP 200

```json
{"traderId": "T003", "positions": {"JNJ": 30, "JPM": 40, "XOM": 90}, "sectorBreakdown": {"ENERGY": 90, "FINANCE": 40, "HEALTHCARE": 30}}
```

## 12. Portfolio after the fill: MSFT 20 now appears

```bash
curl http://localhost:8080/api/v1/traders/T001/portfolio
```

HTTP 200

```json
{"traderId": "T001", "positions": {"AAPL": 150, "MSFT": 20, "NVDA": 100, "TSLA": 80}, "sectorBreakdown": {"TECH": 350}}
```

## 13. Overlap after the fill

```bash
curl http://localhost:8080/api/v1/traders/T001/portfolio/overlap
```

HTTP 200

```json
{"overlaps": [{"basket": "TECH_HEAVY", "overlap": "88.89%"}, {"basket": "FINANCE_HEAVY", "overlap": "0.00%"}, {"basket": "BALANCED", "overlap": "44.44%"}], "dominantBasket": "TECH_HEAVY", "riskFlag": "HIGH"}
```

## 14. New trader: created automatically on first use

```bash
curl -X POST http://localhost:8080/api/v1/orders -H 'Content-Type: application/json' -d '{"traderId":"T004","stock":"AAPL","sector":"TECH","quantity":25,"side":"BUY"}'
```

HTTP 201

```json
{"id": 7, "traderId": "T004", "stock": "AAPL", "sector": "TECH", "quantity": 25, "side": "BUY", "status": "PENDING", "createdAt": "2026-10-09T03:28:25.183279Z", "updatedAt": "2026-10-09T03:28:25.183279Z"}
```

## 15. Error: invalid request body (400)

```bash
curl -X POST http://localhost:8080/api/v1/orders -H 'Content-Type: application/json' -d '{"traderId":"T001","stock":"AAPL","sector":"TECH","quantity":0,"side":"HOLD"}'
```

HTTP 400

```json
{"timestamp": "2026-10-09T03:28:25.220776219Z", "status": 400, "error": "Bad Request", "code": "MALFORMED_REQUEST", "message": "Malformed request: check JSON syntax, field types and enum values (side must be BUY or SELL)", "path": "/api/v1/orders"}
```

## 16. Error: unknown order (404)

```bash
curl -X POST http://localhost:8080/api/v1/orders/999/fill
```

HTTP 404

```json
{"timestamp": "2026-10-09T03:28:25.256449092Z", "status": 404, "error": "Not Found", "code": "ORDER_NOT_FOUND", "message": "Order 999 does not exist", "path": "/api/v1/orders/999/fill"}
```

