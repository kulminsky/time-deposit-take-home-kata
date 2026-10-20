# Time Deposit API (Java solution)

Spring Boot 4.1.1 service on Java 17 for the XA Bank time deposit kata. It keeps the legacy
`TimeDeposit` class and `TimeDepositCalculator.updateBalance(List<TimeDeposit>)` unchanged from the
outside and adds a Postgres database and two REST endpoints around them.

| Method | Path | What it does |
|---|---|---|
| `GET` | `/time-deposits` | Lists all time deposits (`id`, `planType`, `balance`, `days`, `withdrawals`) |
| `POST` | `/time-deposits/balance-updates` | Adds one month of interest to the balance of every time deposit (204) |

The API contract is [`src/main/resources/static/openapi/time-deposits.yaml`](src/main/resources/static/openapi/time-deposits.yaml)
(contract first: the Spring interface and DTOs are generated from it at build time).

## Requirements

- To run the application: Docker with Docker Compose only. No JDK needed.
- To build and test it: JDK 17 or newer (the build compiles for Java 17) and Docker (local runs start Postgres
  with Docker Compose, and the integration tests use Testcontainers). No local Maven needed: use the wrapper
  `./mvnw` (Maven 3.9.16).

## Run it

### With Docker only

```bash
cd java
docker compose up
```

The first start builds the application image from the [`Dockerfile`](Dockerfile) (Maven and a JDK run inside
Docker), then starts Postgres and the application from [`compose.yaml`](compose.yaml). Flyway applies the
migrations plus sample data (`local` profile), and the API is served on http://localhost:8080.
Ctrl+C stops both containers. After changing the code, run `docker compose up --build` to rebuild the image.

### With a JDK

```bash
cd java
./mvnw spring-boot:run
```

This starts only the Postgres service from [`compose.yaml`](compose.yaml) (Spring Boot's Docker Compose support),
applies the same migrations and sample data, and serves the API on http://localhost:8080. The Postgres container
is stopped when the application stops.

Both ways use port 8080, so run one at a time. They share the same Postgres data volume, so the data survives
restarts; `docker compose down -v` (in `java/`) removes the containers and the volume, and the next start begins
again from the sample data.

## Call the endpoints with Swagger UI

1. Open http://localhost:8080/swagger-ui/index.html (http://localhost:8080/ redirects there).
   Swagger UI shows the contract file itself.
2. Open **GET /time-deposits**, click **Try it out**, then **Execute**. You get the 8 sample deposits.
3. Open **POST /time-deposits/balance-updates**, click **Try it out**, then **Execute**. The response is `204`.
4. Run **GET /time-deposits** again. The balances have grown by one month of interest:

| id | planType | days | balance before | balance after one POST | why |
|---|---|---|---|---|---|
| 1 | basic | 30 | 1000.00 | 1000.00 | no interest in the first 30 days |
| 2 | basic | 31 | 1000.00 | 1000.83 | 1% a year |
| 3 | student | 365 | 1000.00 | 1002.50 | 3% a year |
| 4 | student | 366 | 1000.00 | 1000.00 | no interest after one year |
| 5 | premium | 45 | 1000.00 | 1000.00 | interest starts after 45 days |
| 6 | premium | 46 | 1000.00 | 1004.17 | 5% a year |
| 7 | basic | 120 | 1234567.00 | 1235595.81 | 1% a year |
| 8 | gold | 100 | 500.00 | 500.00 | unknown plan type |

Every POST adds another month (the update is not idempotent, which is why it is a POST).

The same with curl:

```bash
curl -s http://localhost:8080/time-deposits
```

```bash
curl -s -i -X POST http://localhost:8080/time-deposits/balance-updates
```

## Tests

```bash
cd java
./mvnw test
```

Unit tests, no Docker needed: the calculator characterization and golden-master tests, the interest rules,
the application services, the REST controller (`@WebMvcTest`) and the ArchUnit architecture rules.

```bash
cd java
./mvnw verify
```

Adds the integration tests (`*IT`) against Postgres in Docker (Testcontainers): persistence, row locking
and end-to-end HTTP tests, including a check that the application has exactly the two endpoints of the contract.

## Design

Hexagonal architecture (ports and adapters), checked by ArchUnit (`HexagonalArchitectureTest`):

```
org.ikigaidigital
├── TimeDeposit, TimeDepositCalculator   legacy domain classes, unchanged API
├── domain                               Withdrawal, TimeDepositWithWithdrawals (read model)
│   └── interest                         one InterestRule per plan, InterestRules lookup
├── application
│   ├── port.in                          UpdateAllBalancesUseCase, ListTimeDepositsUseCase
│   ├── port.out                         TimeDepositRepository, WithdrawalRepository
│   └── service                          the two use case implementations
├── adapter.in.web                       TimeDepositController implements the generated TimeDepositsApi
├── adapter.out.persistence              JPA entities and repositories
└── config                               bean wiring
```

- **Legacy behaviour kept.** Before the refactoring, characterization tests pinned the exact results of the
  original calculator, and a golden-master test compares the refactored calculator bit for bit with a frozen
  copy of the original (`LegacyTimeDepositCalculator`) over 100,000 random deposits and 12 monthly updates.
  The double arithmetic and the `new BigDecimal(double)` HALF_UP rounding are unchanged.
- **Extensible interest rules.** The if/else chain became one `InterestRule` per plan type
  (`BasicPlanInterestRule`, `StudentPlanInterestRule`, `PremiumPlanInterestRule`), looked up by exact plan type
  in `InterestRules`. A new plan is a new rule class registered in `InterestRules.defaults()`; the calculator
  does not change. `TimeDepositCalculator` still has its no-arg constructor and its `updateBalance` signature.
- **Concurrent updates.** The balance update reads all deposits with `SELECT ... FOR NO KEY UPDATE` (ordered by
  id) and writes them in the same transaction. A second update waits for the first to commit and then adds its
  month on top, so no update is lost; reads are never blocked. `BalanceUpdateLockingIT` proves this.
- **Database.** Flyway creates the README tables with their exact camelCase names (`"timeDeposits"`,
  `"withdrawals"`, quoted in Postgres). Money columns are `NUMERIC(19,2)`.

## Assumptions

Each one is also written as a comment in the code where it applies.

- Plan types are matched exactly and case-sensitively; any other plan type earns no interest (legacy behaviour).
- "No interest for the first 30 days" belongs to the three existing plans: day 31 is the first day with interest.
  Student: one year is 365 days, so day 366 and later earn nothing. Premium: day 46 is the first day with interest.
- Withdrawals are history only: they do not change balances or interest (the legacy calculator ignores them).
- Balances are stored with 2 decimals. The legacy `TimeDeposit` keeps the balance as a `double`; it is converted
  to and from `NUMERIC(19,2)` exactly for balances below 10^13.
- The update is a `POST` that returns `204` without a body; `GET` shows the new balances.
- Withdrawals are nested in their time deposit (without `timeDepositId`) and listed oldest first; deposits are
  listed by id.
- Swagger UI and the contract file are static documentation, not API endpoints. springdoc is not used, because
  its Swagger UI needs its own `/v3/api-docs` document generated from the code.
- Invalid input is not handled (the task says so); a `null` plan type gets no interest.
