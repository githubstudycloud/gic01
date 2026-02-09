# platform-* foundation (WIP)

Enterprise-grade platform foundation built around strict module boundaries, strong verification gates,
and "starter-first" ergonomics for Spring Boot 4.x.

## New here?

- Start with `docs/getting-started.md`
- Architecture blueprint: `docs/platform-blueprint.md`
- API contracts/registry: `docs/api-contracts-and-registry.md`
- Vue frontend: `platform-frontend-vue/`
- Python tests: `platform-test-python/`
- Contribution rules: `CONTRIBUTING.md` (module boundaries + dependency policy)
- Multi-agent guide: `AGENTS.md` (humans + AIs)

## Quick start (Maven)

Build everything (unit tests only by default):

```bash
mvn -q test
```

Run integration tests (if/when added):

```bash
mvn -q -Pit verify
```

Enable Java 21 compilation (optional, requires JDK 21+):

```bash
mvn -q -Pjava21 test
```

## Gradle consumption

This repo is built with Maven as the source of truth. Gradle projects can consume the published BOM
and modules. See `platform-example-gradle-consumer/`.

## Namespace changes (groupId / base package)

Use `scripts/rename-namespace.ps1` to rename `com.test.platform` to your real coordinates later.
