# Trading Desk Backend

Spring Boot backend for a stock trading desk using Java 17, Spring Data JPA, transactions, validation, H2, and a clean layered structure.

## Setup

Prerequisites:

- Java 17+
- Maven 3.9+

Run:

```bash
mvn spring-boot:run
```

Test:

```bash
mvn test
```

The app uses an in-memory H2 relational database. Schema is defined in `src/main/resources/schema.sql`.

## APIs

- `POST /api/orders` place order
- `POST /api/orders/{orderId}/fill` fill a pending order
- `POST /api/orders/{orderId}/cancel` cancel a pending order
- `GET /api/traders/{traderId}/portfolio` get portfolio
- `POST /api/traders/{traderId}/portfolio` add holdings
- `GET /api/traders/{traderId}/portfolio/sector-overlap` run basket overlap analysis

Example place order:

```json
{
  "traderId": "T001",
  "stock": "AAPL",
  "sector": "TECH",
  "quantity": 50,
  "side": "BUY"
}
```

Example add to portfolio:

```json
{
  "stock": "NVDA",
  "sector": "TECH",
  "quantity": 100
}
```

## Design Decisions

- Per-trader writes are serialized with `PESSIMISTIC_WRITE` locks on the trader row. This keeps pending-order limits, reserved SELL quantity, fills, cancels, and portfolio mutations consistent under concurrent requests.
- SELL placement validates against available quantity: current holdings minus already pending SELL orders for the same stock.
- Order state transitions are explicit domain methods. Only `PENDING -> FILLED` and `PENDING -> CANCELLED` are allowed.
- Sector overlap is pure Java in `SectorOverlapCalculator`, independent of Spring and persistence.
- DTOs isolate API payloads from JPA entities.
- `schema.sql` owns the relational schema and constraints; Hibernate DDL generation is disabled.

## Tradeoffs

- Pessimistic locking is simple and robust for desk-style correctness, but it reduces parallelism for multiple operations by the same trader. Operations for different traders can still proceed concurrently.
- H2 is used for local/demo delivery. For production, point the datasource to PostgreSQL or another relational database and validate lock behavior with that database.
- There is no authentication/authorization layer because the assignment focuses on trading, portfolio, and concurrency behavior.

## Assumptions

- Stock and sector symbols are stored uppercase.
- Adding to portfolio is an administrative/bootstrap operation for a specific trader path.
- Pending SELL orders reserve holdings to prevent over-selling before fills.
- Zero-quantity positions are retained in the database but omitted from portfolio responses and overlap calculations.

## Skipped Items

- No market-price, cash balance, matching engine, audit trail, or external broker integration.
- No Docker setup; the app is self-contained with H2 for easy evaluation.
