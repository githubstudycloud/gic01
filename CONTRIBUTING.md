# Contributing

This repo is a modular, enterprise-grade Spring Boot 4.x platform foundation (`platform-*`).
We optimize for strict boundaries, easy upgrades, and fast verification.

## Prerequisites

- JDK: 17+ (recommended runtime: 21)
- Maven: 3.9+
- Git
- Optional (for adapter ITs / local labs): Docker Desktop, kubectl, Helm

## Build / Test

```bash
# Fast default (unit tests only)
mvn -q test

# Enable integration tests (failsafe)
mvn -q -Pit verify

# Compile with Java 21 target (requires JDK 21+)
mvn -q -Pjava21 test
```

## Module Rules (Non-Negotiable)

- `platform-kernel`:
  - pure Java (no Spring, no Jakarta EE)
  - holds shared primitives/conventions used everywhere
- `platform-spi-*`:
  - ports (interfaces + small DTOs)
  - MUST NOT depend on Spring / Jakarta EE
- `platform-adapter-*`:
  - implementations of SPIs (redis/kafka/s3/...)
  - can use third-party clients; keep Spring out unless the adapter is explicitly Spring-based
- `platform-autoconfigure-*`:
  - Spring Boot auto-configuration and `@ConfigurationProperties`
  - keep “policy defaults” here; keep business logic out
- `platform-starter-*`:
  - dependency bundle + opinionated defaults
  - every starter should have a minimal context test

If you need to break a rule, document the reason in an ADR (see `docs/adr/`).

## “Minimal Release Verification”

Default checks should stay fast and deterministic. Put heavier verification behind profiles and/or
dedicated workflows:

- `mvn test`: always fast
- `-Pit verify`: adapter integration tests (Testcontainers etc.)
- load tests / cluster tests: live under `platform-loadtest/` and are run manually or nightly

## Dependency Policy (36 months)

Design constraint: dependencies should be released within the last 36 months.
If a dependency is older, it must be vendored into `platform-thirdparty-repo/` and recorded in
`platform-deps-metadata/` with release date/source/license/replacement plan.

Enforcement may be staged behind opt-in profiles initially to keep PR validation minimal.

## Multi-Agent Collaboration

See `AGENTS.md` for collaboration rules and the required `docs/QA.md` session archive entry.

