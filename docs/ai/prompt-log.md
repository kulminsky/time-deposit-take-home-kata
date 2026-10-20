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

