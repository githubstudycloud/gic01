# Repo Agent Guide (Humans + AIs)

This repo is a strict, modular Spring Boot 4.x platform foundation. Multiple AI agents may work in
parallel; use this guide to avoid conflicts and keep changes reviewable.

## Golden Rules

- Keep modules small and composable; prefer adding a new `platform-*` module over “one big module”.
- Preserve strict boundaries:
  - `platform-kernel` and `platform-spi-*` must NOT depend on Spring / Jakarta EE.
  - Spring Boot auto-configuration lives in `platform-autoconfigure-*`.
  - Dependency “bundles” live in `platform-starter-*`.
  - App entrypoints (runnable) are in `platform-*-app` / `platform-*-entry` modules.
- Maven is the source-of-truth build for this repo; Gradle support means “easy to consume” (BOM +
  examples), not “dual-build the platform sources”.
- Default validation must be fast (`mvn test`). Heavy checks belong to opt-in profiles / workflows.
- Never use destructive git commands (e.g. `reset --hard`) unless explicitly requested by the user.

## Session Archiving (Required)

After each session, append a short Q/A entry to `docs/QA.md`:
- Q: what the user asked (bullet summary)
- A: what was implemented/decided (bullet summary)

This is part of the “platform governance” requirement.

## Working Agreements (Multi-Agent)

- Prefer one feature per PR/commit series (docs-only, build-only, a new starter, etc.).
- Avoid large reformat-only commits mixed with functional changes.
- When changing shared build files (`pom.xml`, `platform-parent/pom.xml`, `platform-bom/pom.xml`),
  keep the diff minimal and explain the rationale in the commit message.
- When adding new modules:
  - add it to root `pom.xml` in dependency order (parent/bom first, then libs, then apps)
  - include a minimal unit test or a minimal Spring context test
  - add a short note to `docs/platform-blueprint.md` if it affects conventions

## Quick Commands

```bash
# Unit tests (default / PR-fast)
mvn -q test

# Integration tests (opt-in)
mvn -q -Pit verify

# Compile with Java 21 target (opt-in)
mvn -q -Pjava21 test
```

