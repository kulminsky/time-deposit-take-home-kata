# Prompt log

Every prompt sent to Claude Code in this repo. New entries are appended by the `UserPromptSubmit` hook in `.claude/settings.json`. The first entries were added by hand because they were sent before the hook existed.

## 2026-10-19 23:07:36

/init

## 2026-10-19 23:10:47

Add these rules to CLAUDE.md: never change TimeDeposit.java or the updateBalance signature; calculation results must stay the same; only two REST endpoints; write assumptions as code comments, small Conventional Commits, never push.

## 2026-10-19 23:18:18

Add .claude/settings.json: deny edits to TimeDeposit.java, git push, and git reset --hard. Also add a UserPromptSubmit hook with jq that appends every prompt with a timestamp to docs/ai/prompt-log.md, and add my earlier prompts from this session to that file.

## 2026-10-19 23:24:33

Not yet


## 2026-10-19 23:25:03

Create a code-reviewer subagent that reviews my changes against the rules in CLAUDE.md and reports problems by severity. Then commit CLAUDE.md and the .claude setup in small commits.


## 2026-10-19 23:44:25

Read README.md and the java/ code. Plan the full solution: Spring Boot 4.1.1, Java 17, Postgres, Flyway, JPA, hexagonal, OpenAPI contract-first, Testcontainers, ArchUnit. Start with tests that pin the current calculator results, then refactor.


## 2026-10-20 19:50:16

Add Docker so anyone can run the app with 'docker compose up'. 
No local JDK needed.


## 2026-10-20 21:22:40

Review all changes since c4ea358 like a strict senior reviewer. List findings by severity, don't change anything.


## 2026-10-20 21:36:43

No fixes for now, the review is enough.


## 2026-10-20 21:38:35

Fix 1, 2, 3, 5, and 9, plus the tests, Dockerfile, AI_USAGE wording, and the in-memory comment. 
1 - keep the new behaviour. 
2 - keep days as is and write it as an assumption.
9 - a short note in java/README is enough.


## 2026-10-20 21:49:43

Leave the CI workflow as it is.


## 2026-10-20 21:54:46

Use --retry-all-errors in the CI smoke test after all


## 2026-10-20 22:04:42

Update the docs to match the latest changes, then commit everything by following the style.

