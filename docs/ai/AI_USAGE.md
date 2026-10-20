# AI-assisted development

This solution was built with an AI coding agent. This page lists the tools, the configuration in this
repository, how the work was done, and which parts were AI-assisted.

## Tools

| Tool | Use |
|---|---|
| [Claude Code](https://claude.com/claude-code), desktop app (Code tab) | Agent that read the code, planned, wrote code and tests, ran Maven and Docker, and made the commits |
| Claude Opus 5.5 (`claude-opus-5-5`) | Model behind the agent |
| Claude Code plan mode | Read-only planning and the final review; the plan was approved (and corrected once) before any code changed |
| Claude Code subagent `code-reviewer` | Review against the project rules before larger commits; it reports and never edits or commits |
| Built-in browser of the Claude desktop app | Checking Swagger UI of the locally running application |

No other LLMs, code generators or agent frameworks were used. The only other code generator is
openapi-generator, which produces the Spring interface and DTOs from the OpenAPI contract during the build.

## Configuration in this repository

All of it is committed, so the setup is reproducible: open the repository in Claude Code and it is used.

- [`CLAUDE.md`](../../CLAUDE.md): project context and rules the agent must follow. Never change `TimeDeposit.java` or
  the `updateBalance` signature; keep calculation results exactly the same; only two REST endpoints; write
  assumptions as code comments; small Conventional Commits; never push. It also describes the legacy behaviour
  in detail (double arithmetic, `new BigDecimal(double)` rounding), so the agent pins it with tests before refactoring.
- [`.claude/settings.json`](../../.claude/settings.json):
  - `deny` rules that stop the agent's file-edit tools from changing `TimeDeposit.java` and block `git push` and
    `git reset --hard`. They back up the rules in `CLAUDE.md` but are not a complete guard: a shell command could
    still change the file, so the `code-reviewer` agent also checks that it is unchanged;
  - a `UserPromptSubmit` hook (`jq`) that appends each of the developer's prompts with its date and time to
    [`prompt-log.md`](prompt-log.md) and skips automated messages (subagent reports, background task notifications).
- [`.claude/agents/code-reviewer.md`](../../.claude/agents/code-reviewer.md): a reviewer subagent. It checks
  the changes against `CLAUDE.md` (unchanged `TimeDeposit`, unchanged results, exactly two endpoints, untouched
  other languages, assumption comments, commit style), runs the full build (`./mvnw -q verify`, including the
  Testcontainers integration tests) and reports findings by severity. Its instructions say to only read and
  report, never edit, stage or commit. That is not enforced: it has Bash for git and the build, and the build
  writes `target/` and starts containers.

## Prompts

[`prompt-log.md`](prompt-log.md) has the developer's prompts in order, each under its local date and time. The first
entries were added by hand because they were sent before the hook existed. Two things to know when reading it:

- Automated messages reach the agent the same way as prompts: subagent reports (`<agent-message>` blocks) and notices
  that a background command finished (`<task-notification>` blocks). The hook skips them, so the log only holds prompts
  the developer wrote.
- An answer given while rejecting a plan in plan mode is not a prompt, so the hook did not record it. After the
  first plan the reply was: "Use the latest stable versions that work with Spring Boot 4.1.1, lock the rows when
  updating balances so two POSTs can't overlap, and keep the CI step."
- A few entries are replies the developer picked from the buttons the desktop app offers at the end of a turn
  (for example "Leave the CI workflow as it is."), not typed text.

## How the work was done

1. **Setup.** `/init` created `CLAUDE.md`; the rules, deny list, prompt-log hook and reviewer subagent were added
   and committed first.
2. **Plan.** In plan mode the agent read the README and the Java code and wrote a step-by-step plan (stack,
   package layout, commit sequence, assumptions, verification). The developer corrected it (latest compatible
   versions, row locking for the update, keep CI) before approving it.
3. **Pin, then refactor.** The agent wrote characterization tests with values taken from running the original
   calculator, and a golden-master test against a frozen copy of it, before touching the calculator. Both test
   classes were checked by mutation (changing a day limit or switching to `BigDecimal.valueOf` makes them fail).
4. **Build the rest in small commits:** ports and services, Flyway schema, JPA adapter, locking test, OpenAPI
   contract and controller, Swagger UI, end-to-end test, ArchUnit rules, local run setup, CI, docs. Each commit was
   built and tested before it was made; the row-locking test was also checked by removing the lock (3 of 4 tests
   then fail, including a real lost update).
5. **Review.** The `code-reviewer` subagent reviewed the refactor, then everything up to the ArchUnit rules, then
   the finished result. It found no problems with the calculation or the API rules; its findings on the code and
   setup (clearer assumption comments, a stronger "exactly two endpoints" test, a Docker volume left behind by local
   runs, running the full build in the reviewer) were fixed in the following commits.
6. **Run it.** The agent started the application with `./mvnw spring-boot:run`, called GET, POST and GET again with
   curl, checked the balances against the expected values, and opened Swagger UI in the Claude desktop app's
   built-in browser to run the GET request there.
7. **Docker.** On request, the agent added a `Dockerfile` and an `app` service in `compose.yaml`, so
   `docker compose up` runs everything without a local JDK, plus a CI step that smoke-tests that image.
8. **Final review, then fixes.** The developer asked for a strict senior review of everything since `c4ea358`
   without changes. In plan mode the agent read the whole diff and reported 2 major findings, 7 minor and 8 nits
   (nothing critical). The developer chose what to fix and how:
   - the `null` plan type change was kept, documented as the one deliberate change and pinned against the legacy
     copy;
   - `days` stays unchanged by the update and is written down as an assumption;
   - the locked read and the write now require a transaction;
   - money columns became `NUMERIC(15,2)` so balances convert to `double` and back exactly;
   - smaller test, Dockerfile and doc fixes.

   While checking the Docker run, the agent found that the CI smoke test did not retry on a connection reset.
   The developer first left it, then asked for `--retry-all-errors`. Each fix has its own commits, and the full
   build and a Docker Compose run passed afterwards. Left open on purpose: a CI check that `TimeDeposit.java` and
   the other language folders are unchanged, the sample data as a versioned migration, and the prompt-log hook
   writing into a tracked file.

## Which parts were AI-assisted, and why

| Part | AI-assisted | Developer |
|---|---|---|
| Rules, guards, prompt log, reviewer agent (`CLAUDE.md`, `.claude/`) | Written by the agent from short instructions | Chose the rules and guards |
| Plan, architecture, commit sequence | Proposed by the agent in plan mode | Chose the stack and versions policy, required row locking and CI, approved the plan |
| Characterization and golden-master tests | Written and mutation-checked by the agent | Required "pin first, then refactor" |
| Production code (domain, application, adapters, config, contract, migrations) | Written by the agent | Set the requirements through the approved plan |
| Integration tests (Testcontainers), ArchUnit rules | Written by the agent | |
| CI workflow, Dockerfile and Docker Compose, docs | Written by the agent | |
| Final review and the fixes after it | Review written by the agent in plan mode; fixes written and tested by the agent | Chose which findings to fix and how |
| Pushing to GitHub | Not allowed for the agent (deny rule) | Pushes |

Why an agent: most of the work is careful, mechanical and easy to verify (tests that pin exact doubles,
boilerplate around Spring, JPA and OpenAPI, many small commits). The repository rules, deny rules and reviewer
subagent keep the agent inside the constraints of the task, and the tests, mutation checks and reviews verify
what it produced.
