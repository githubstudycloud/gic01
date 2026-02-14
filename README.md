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

Note: some ITs use Testcontainers; Docker daemon is required to actually run them (otherwise they are skipped).

Enable Java 21 compilation (optional, requires JDK 21+):

```bash
mvn -q -Pjava21 test
```

## Release verify (optional)

```powershell
./scripts/release-verify.ps1 -Level standard
```

## Gradle consumption

This repo is built with Maven as the source of truth. Gradle projects can consume the published BOM
and modules. See `platform-example-gradle-consumer/`.

## Namespace changes (groupId / base package)

Use `scripts/rename-namespace.ps1` to rename `com.test.platform` to your real coordinates later.
