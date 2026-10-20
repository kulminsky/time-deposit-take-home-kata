# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this repo is

XA Bank "time deposit" refactoring kata (take-home). The same starting code exists in five languages (`java/`, `kotlin/`, `c#/`, `python/`, `typescript/`); the solution is built in `java/` only. The full task is in `README.md`.

The legacy code is `TimeDeposit` (plain data class) and `TimeDepositCalculator.updateBalance(List<TimeDeposit>)` in package `org.ikigaidigital`. The solution around it is a Spring Boot app with a hexagonal layout (`domain`, `application`, `adapter.in.web`, `adapter.out.persistence`, `config`); `java/README.md` describes it, and `HexagonalArchitectureTest` (ArchUnit) enforces the boundaries.

## Commands (Java)

Run from `java/`:

```bash
./mvnw test                                                       # unit tests (no Docker)
./mvnw test -Dtest=TimeDepositCalculatorTest                      # one class
./mvnw test -Dtest=TimeDepositCalculatorTest#updateBalance_Test   # one method
./mvnw verify                                                     # unit + integration tests (*IT, Testcontainers, needs Docker)
./mvnw verify -Dtest=none -Dsurefire.failIfNoSpecifiedTests=false -Dit.test=TimeDepositApiIT   # one IT
./mvnw spring-boot:run                                            # run locally (Docker Compose Postgres + sample data)
docker compose up --build                                         # app + Postgres in Docker, no local JDK (Dockerfile)
```

`compose.yaml` serves both runs: `docker compose up` starts `postgres` and `app`, while `spring-boot:run` starts only `postgres` because of `spring.docker.compose.start.arguments` in `application.yaml` (keep it, or both would use port 8080).

Maven wrapper (Maven 3.9.16), Spring Boot 4.1.1 parent, Java 17 (`release 17`). The Spring interface and DTOs are generated from `src/main/resources/static/openapi/time-deposits.yaml` at build time (`target/generated-sources/openapi`); change the contract, not generated code. No linter or formatter is configured.

## Rules

- Never change `java/src/main/java/org/ikigaidigital/TimeDeposit.java` or the signature of `TimeDepositCalculator.updateBalance`.
- Calculation results must stay exactly the same (see "Legacy behaviour to preserve" below).
- Only two REST endpoints: one updates the balances of all time deposits, one lists all time deposits (`id`, `planType`, `balance`, `days`, `withdrawals`). Do not add others.
- Write assumptions as code comments where they apply.
- Make small commits using Conventional Commits (`feat:`, `fix:`, `test:`, `refactor:`, `docs:`, `chore:`).
- Never push; the owner pushes.

## Other requirements from the task

- DB tables `timeDeposits` (`id`, `planType`, `days`, `balance`) and `withdrawals` (`id`, `timeDepositId` FK, `amount`, `date`), named as in the README.
- Interest rules must be extensible (new plan types / rules without growing the if/else chain).
- Invalid-input and exception handling is not required.
- Preferred stack: OpenAPI (Swagger) contract, hexagonal architecture, Testcontainers.
- The submission must document the AI setup and usage (tools, prompts, agent config, which parts were AI-assisted) and explain how to call the endpoints via Swagger.

## Legacy behaviour to preserve (`TimeDepositCalculator.updateBalance`)

`TimeDepositCalculatorTest` pins these results (values taken from the original code), and `TimeDepositCalculatorGoldenMasterTest` compares the calculator bit for bit with `LegacyTimeDepositCalculator`, a frozen copy of the original. Never edit that copy; keep both tests green.

- No interest unless `days > 30`.
- Plan is matched by exact, case-sensitive string: `"basic"` 1%/yr; `"student"` 3%/yr only while `days < 366`; `"premium"` 5%/yr only when `days > 45`. Any other plan gets 0 interest. Monthly interest = `balance * rate / 12`.
- Interest is computed in `double`, rounded with `new BigDecimal(double).setScale(2, HALF_UP)` (not `BigDecimal.valueOf`), then added to the `double` balance. Switching to `BigDecimal` arithmetic or `valueOf` can change results at rounding edges, so keep the exact arithmetic or prove equality with a golden-master test.
- It mutates the deposits in the passed list (`setBalance`) and returns nothing.
- One deliberate change, accepted by the owner: a `null` plan type gets no interest and the rest of the list is still updated (the original threw a `NullPointerException` once `days > 30`). Both test classes pin it; don't "fix" it back.

## Project decisions (from the repo owner)

- Spring Boot 4.1.1 on a Java 17 baseline.
- Leave `c#/`, `kotlin/`, `python/` and `typescript/` unchanged; they stay as the original kata.
