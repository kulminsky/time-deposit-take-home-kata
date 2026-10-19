# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this repo is

XA Bank "time deposit" refactoring kata (take-home). The same starting code exists in five languages (`java/`, `kotlin/`, `c#/`, `python/`, `typescript/`); the solution is built in `java/` only. The full task is in `README.md`.

The starting point is tiny: `TimeDeposit` (plain data class) and `TimeDepositCalculator.updateBalance(List<TimeDeposit>)` in package `org.ikigaidigital`. There is no framework, database or API yet; those have to be added.

## Commands (Java)

Run from `java/`:

```bash
mvn test                                                       # all tests
mvn test -Dtest=TimeDepositCalculatorTest                      # one class
mvn test -Dtest=TimeDepositCalculatorTest#updateBalance_Test   # one method
mvn package
```

Plain Maven project targeting Java 17, JUnit 5 + AssertJ. No Maven wrapper, linter or formatter is configured yet.

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

The existing test is a placeholder (`assertThat(1).isEqualTo(1)`) and checks nothing, so pin down the current behaviour with tests before refactoring.

- No interest unless `days > 30`.
- Plan is matched by exact, case-sensitive string: `"basic"` 1%/yr; `"student"` 3%/yr only while `days < 366`; `"premium"` 5%/yr only when `days > 45`. Any other plan gets 0 interest. Monthly interest = `balance * rate / 12`.
- Interest is computed in `double`, rounded with `new BigDecimal(double).setScale(2, HALF_UP)` (not `BigDecimal.valueOf`), then added to the `double` balance. Switching to `BigDecimal` arithmetic or `valueOf` can change results at rounding edges, so keep the exact arithmetic or prove equality with a golden-master test.
- It mutates the deposits in the passed list (`setBalance`) and returns nothing.

## Project decisions (from the repo owner)

- Spring Boot 4.1.1 on a Java 17 baseline.
- Leave `c#/`, `kotlin/`, `python/` and `typescript/` unchanged; they stay as the original kata.
