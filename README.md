# Order Booking & Portfolio API

Spring Boot 3.3 / Java 17+ backend for a stock trading desk: traders place BUY/SELL
orders, orders are filled or cancelled, and each trader has a portfolio that can be
analysed for overlap with benchmark baskets.

## How to run

Requirements: JDK 17 or newer, Maven 3.9+.

```bash
mvn test                      # 101 tests; the 31 MySQL ones need Docker (skipped without it)
mvn spring-boot:run           # starts on http://localhost:8080 with an embedded database
# or
mvn package && java -jar target/order-booking-api-1.0.0.jar
```

### Database: MySQL

The schema targets **MySQL 8** (`src/main/resources/schema.sql`). There are two ways to run:

| | Command | Database |
|---|---|---|
| Quick start (default) | `mvn spring-boot:run` | Embedded H2 in **MySQL compatibility mode**, in memory. Nothing to install; data resets on restart. Console at `http://localhost:8080/h2-console` (JDBC URL `jdbc:h2:mem:trading`, user `sa`, no password). |
| Real MySQL | `docker compose up -d` then `mvn spring-boot:run -Dspring-boot.run.profiles=mysql` | MySQL 8.4 from `docker-compose.yml` on `localhost:3306`, database/user/password `trading`. Data survives restarts. |

To use your own MySQL instead of Docker, create a database and run with the `mysql` profile and
`DB_URL`, `DB_USER`, `DB_PASSWORD` environment variables
(e.g. `DB_URL=jdbc:mysql://localhost:3306/trading`). The schema and demo data are applied on
startup and are safe to re-run.

**Why H2 by default and MySQL for real?** Reviewers can run the app with one command and no
database install, while the same `schema.sql` runs on real MySQL. H2's MySQL mode is not a
perfect copy of MySQL (locking and isolation differ), so the business-rule and concurrency
tests also run against a real MySQL 8.4 in Docker via Testcontainers (`MySql*Test`).
`CURLS.md` has a curl for every endpoint, in a sequence that works on a fresh start, with real
responses. `requests.http` has the same walkthrough for IntelliJ / VS Code REST Client.

### Demo data

`src/main/resources/data.sql` seeds three traders on startup so every endpoint returns
something immediately:

| Trader | Holdings | Orders | Overlap |
|---|---|---|---|
| `T001` | AAPL 150, TSLA 80, NVDA 100 (TECH) | #1 BUY AAPL 150 FILLED, #2 BUY MSFT 20 PENDING, #3 SELL AAPL 50 PENDING | TECH_HEAVY 75%, HIGH |
| `T002` | JPM 200, GS 50, BAC 120 (FINANCE) | #4 BUY WFC 100 PENDING, #5 SELL GS 10 CANCELLED | FINANCE_HEAVY 75%, HIGH |
| `T003` | XOM 90 (ENERGY), JNJ 30 (HEALTHCARE) | none | BALANCED 57.14%, MEDIUM |

The inserts are guarded with `NOT EXISTS`, so re-running the script is harmless. Start
with an empty database instead with `--spring.sql.init.data-locations=`.

### Adding a trader

The spec defines no "create trader" endpoint, so there isn't one: a trader is created
automatically the first time any request names it. Either of these creates `T004`:

```bash
curl -X POST localhost:8080/api/v1/traders/T004/portfolio/holdings -H 'Content-Type: application/json' \
  -d '{"stock":"AAPL","sector":"TECH","quantity":10}'
curl -X POST localhost:8080/api/v1/orders -H 'Content-Type: application/json' \
  -d '{"traderId":"T004","stock":"AAPL","sector":"TECH","quantity":10,"side":"BUY"}'
```

## Endpoints

| # | Method & path | Purpose | Success |
|---|---|---|---|
| 1 | `POST /api/v1/orders` | Place an order | 201 + `Location` |
| 2 | `POST /api/v1/orders/{id}/fill` | Fill a PENDING order | 200 |
| 3 | `POST /api/v1/orders/{id}/cancel` | Cancel a PENDING order | 200 |
| 4 | `GET /api/v1/traders/{traderId}/portfolio` | Positions + sector breakdown | 200 |
| 5 | `GET /api/v1/traders/{traderId}/portfolio/overlap` | Sector overlap analysis | 200 |
| 6 | `POST /api/v1/traders/{traderId}/portfolio/holdings` | Add holdings directly | 200 + portfolio |
| – | `GET /api/v1/orders/{id}` | Read one order (convenience) | 200 |

```bash
# Against the demo data (see below):
curl localhost:8080/api/v1/traders/T001/portfolio
# {"traderId":"T001","positions":{"AAPL":150,"NVDA":100,"TSLA":80},"sectorBreakdown":{"TECH":330}}
curl localhost:8080/api/v1/traders/T001/portfolio/overlap
curl -X POST localhost:8080/api/v1/orders/2/fill          # seeded PENDING BUY MSFT 20
curl -X POST localhost:8080/api/v1/orders -H 'Content-Type: application/json' \
  -d '{"traderId":"T005","stock":"AAPL","sector":"TECH","quantity":150,"side":"BUY"}'
```

### Errors

Every error has the same shape:

```json
{"timestamp":"…","status":409,"error":"Conflict","code":"INVALID_ORDER_STATE",
 "message":"Order 1 cannot be cancelled because it is FILLED; only PENDING orders can be cancelled",
 "path":"/api/v1/orders/1/cancel"}
```

| Code | HTTP | When |
|---|---|---|
| `VALIDATION_FAILED` | 400 | Missing/blank field, non-positive quantity (`details` lists each field) |
| `MALFORMED_REQUEST` | 400 | Bad JSON, wrong type, `side` not BUY/SELL |
| `ORDER_NOT_FOUND` | 404 | Unknown order id |
| `INVALID_ORDER_STATE` | 409 | Fill/cancel of an order that is not PENDING |
| `STOCK_SECTOR_MISMATCH` | 409 | Stock already registered under a different sector |
| `CONCURRENT_MODIFICATION` | 409 | Lock wait timed out under extreme contention; safe to retry |
| `PENDING_ORDER_LIMIT_EXCEEDED` | 422 | Trader already has 3 PENDING orders |
| `INSUFFICIENT_HOLDINGS` | 422 | SELL larger than the shares available |

409 means "the resource's current state forbids this"; 422 means "the request is
well-formed but breaks a business rule".

## Project layout

```
com.fynxt.trading
├── overlap/      Pure Java: Basket, BenchmarkBaskets, SectorOverlapAnalyzer, RiskFlag, OverlapReport
├── domain/       JPA entities with behaviour: Order (state machine), Holding (never negative), Stock, Trader
├── repository/   Spring Data interfaces, incl. the trader row lock
├── service/      OrderService, PortfolioService, ReferenceDataRegistrar, TraderLock
├── web/          Controllers, request/response DTOs, GlobalExceptionHandler
├── exception/    TradingException hierarchy + ErrorCode (code -> HTTP status)
└── config/       TradingProperties (max pending = 3), overlap bean wiring
```

## Key design decisions

### Concurrency: one row lock per trader

Every rule in the spec is scoped to one trader (pending count, shares held), so every
write for a trader first runs `SELECT … FOR UPDATE` on that trader's row
(`TraderRepository.lockById`), then reads state and checks rules while holding it.

* **Correct across instances.** The lock lives in the database, so it still holds
  with several app instances behind a load balancer. A JVM `synchronized`/`ReentrantLock`
  would not.
* **Parallel across traders.** Different traders never wait for each other.
* **No deadlocks.** Every transaction locks exactly one trader row and always takes it
  first, so there is no lock-ordering cycle.
* **Fill/cancel load the order only after the lock.** `findTraderIdById` is a scalar
  query, so the order entity is read fresh once the lock is held; a racing fill and
  cancel cannot both see PENDING.
* **READ COMMITTED isolation, set explicitly.** MySQL/InnoDB defaults to REPEATABLE READ,
  where a plain `SELECT` reads from a snapshot taken at the transaction's first read.
  Fill/cancel read the order's trader id *before* taking the lock, so under REPEATABLE READ
  the order re-read after the lock could still show PENDING for an order another request
  just filled. Every write transaction therefore runs at READ COMMITTED
  (`service/Transactions.java`). This is not theoretical: with the default isolation,
  `MySqlConcurrencyTest` fails (orders filled more than once); with READ COMMITTED it
  passes. H2 defaults to READ COMMITTED, which is why the H2 tests could not catch it.
* **The DB is the final safety net.** `CHECK (quantity >= 0)` on holdings and the
  status/side CHECKs reject anything the application logic ever missed.

`ConcurrencyTest` fires 16 simultaneous requests at one trader and asserts: exactly 3
orders become PENDING, SELLs never exceed holdings, an order is filled exactly once,
a fill/cancel race has one winner, and concurrent additions are not lost. With the
lock swapped for a plain read, 11 of its 13 runs fail; with it, all pass.
`MySqlConcurrencyTest` runs the same cases against real MySQL.

**Alternative considered: optimistic locking (`@Version`) with retry.** It scales
better under low contention, but the pending-limit rule is an invariant over a *set*
of rows (count of PENDING orders), which a version on one order row does not protect
without bumping a version on the trader anyway. Pessimistic locking of the trader is
simpler to reason about and to explain, and contention per trader is naturally low.

### SELL orders reserve shares

The spec says a SELL is rejected if the trader "does not hold enough shares". Checking
only the current holding would let a trader with 100 shares place two PENDING SELLs of
100 each, and the second fill would fail. So available shares =
`held − Σ(PENDING SELL quantity for that stock)`, checked at placement. The fill
re-checks against the actual holding as defence in depth. PENDING BUYs do not count
as held shares.

### Sector overlap is framework-free

`com.fynxt.trading.overlap` imports nothing outside `java.*`: no Spring, no JPA, no
I/O. `SectorOverlapAnalyzer` takes a `Set<String>` of tickers and returns an immutable
`OverlapReport`. It is stateless and thread-safe. Spring only wires it as a bean in
`OverlapConfig`. Percentages use `BigDecimal`, rounded HALF_UP to 2 decimals.

### Other choices

* **Rich domain objects.** `Order.markFilled()/markCancelled()` own the state machine
  and `Holding.decrease()` refuses to go negative, so services cannot bypass the rules.
* **Traders and stocks are registered on first use.** The spec has no endpoints to
  create them, so adding one would be API surface nobody asked for. `ReferenceDataRegistrar` inserts in its own short transaction *before*
  the main one; if two first requests race, the loser catches the unique-key violation
  and uses the winner's row. Running it before (not inside) the main transaction
  avoids needing two pooled connections per request.
* **One sector per stock.** Stocks live in a `stocks` table (symbol → sector). A
  request that names a known stock with a different sector is rejected with 409
  rather than silently re-classifying it.
* **Programmatic transactions (`TransactionTemplate`).** They make transaction
  boundaries explicit in the service code, which matters here because registration
  must happen outside the locking transaction.
* **Config over constants.** The pending limit is `trading.max-pending-orders-per-trader`.
* **Logging.** INFO on every state change (placed/filled/cancelled/added), WARN for
  rejected business requests, ERROR with stack trace for unexpected failures. Clients
  never see internal exception messages.

## Assumptions (where the spec is ambiguous)

| Question | Choice |
|---|---|
| Is "portfolio" for overlap a set of tickers or weighted by quantity? | Set of tickers with quantity > 0, as in the worked example |
| `dominantBasket` on a tie | First basket in spec order (TECH_HEAVY, FINANCE_HEAVY, BALANCED) |
| `dominantBasket` when every overlap is 0% | `null`: no basket dominates |
| Risk thresholds | Inclusive (≥ 60 HIGH, ≥ 40 MEDIUM), applied to the rounded 2-dp value the client sees |
| Unknown trader on GET portfolio/overlap | 200 with empty portfolio (LOW risk), not 404, since traders exist implicitly |
| Position that reaches 0 after a SELL | Omitted from `positions` and from the overlap set |
| Ticker / sector case | Trimmed and upper-cased (`" aapl"` → `AAPL`); trader ids are trimmed, case-sensitive |
| Fill price | Not modelled: the spec has no prices, so a fill only moves quantity |
| URL scheme | Versioned `/api/v1`; fill/cancel are `POST` sub-resources (they are commands, not idempotent PUTs) |

## Trade-offs and things intentionally skipped

* **H2 as the default runtime database.** Zero setup for reviewers. The production target
  is MySQL (`mysql` profile), and the tests that depend on database behaviour also run
  against real MySQL via Testcontainers.
* **Enums stored as VARCHAR + CHECK, not MySQL `ENUM`.** Adding a status later is then a
  constraint change rather than a column-type change, and the schema stays portable.
* **No migration tool (Flyway/Liquibase).** The deliverable is a single `schema.sql`;
  in production I would put it under Flyway as `V1__init.sql`. Hibernate runs with
  `ddl-auto=validate`, so entity/schema drift fails at startup.
* **No authentication/authorization.** Out of scope; in production `traderId` would come
  from the authenticated principal, not the request body.
* **No idempotency keys.** A retried `POST /orders` creates a second order. A client-supplied
  `Idempotency-Key` with a unique constraint would fix this.
* **No pagination/listing of orders, no OpenAPI UI, no Docker image of the app.** Not asked for; kept
  the dependency set minimal.
* **Lock wait.** H2 `LOCK_TIMEOUT` is 10 s; on MySQL it is InnoDB's
  `innodb_lock_wait_timeout` (50 s by default). If exceeded, the client gets a retryable
  409 `CONCURRENT_MODIFICATION`.
* **Service tests use the real (in-memory) database** rather than mocks, because the
  behaviour under test (row locks, constraints, aggregate queries) lives in the database.
  The overlap logic and domain state machine have pure unit tests with no Spring context.

## Tests

| Class | Kind | Covers |
|---|---|---|
| `SectorOverlapAnalyzerTest` | Pure unit | Worked example, ties, empty/unrelated portfolio, rounding, inclusive thresholds |
| `OrderTest`, `HoldingTest` | Pure unit | State machine, illegal transitions, non-negative holdings |
| `OrderServiceTest` | Service + H2 | Pending limit (per trader, freed by fill/cancel), SELL checks and reservation, fill effects, not-found, normalisation, sector mismatch |
| `PortfolioServiceTest` | Service + H2 | Spec portfolio example, multi-sector breakdown, unknown trader, overlap from holdings |
| `ConcurrencyTest` | 16 threads, start gate | Limit, overselling, double fill, fill-vs-cancel, lost updates, first-use registration race |
| `ApiTest` | MockMvc | All endpoints, status codes, error body, validation details |
| `SeedDataTest` | MockMvc | Demo traders load with the documented holdings, orders and risk |
| `MySqlOrderServiceTest`, `MySqlConcurrencyTest`, `MySqlSeedDataTest` | Testcontainers, MySQL 8.4 | The three classes above re-run against real MySQL; skipped automatically when Docker is not available |

Testcontainers is pinned to 1.21.4 because older releases cannot talk to Docker Engine 29+.

Note: `pom.xml` loads Mockito's Byte Buddy agent via `-javaagent` for tests. JDK 21+
warns about (and some sandboxes block) the dynamic self-attach Mockito otherwise uses.
