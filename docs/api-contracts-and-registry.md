# API Contracts & Registry (Frontend <-> Backend)

Goal: keep frontend/backend integration strict, auditable, and easy to verify.

## Contract First (OpenAPI)

Convention:
- Every service must expose an OpenAPI spec at a stable, well-known URL:
  - default: `/openapi.yaml`
- The spec is part of the release artifact (versioned with the code tag).

In this repo:
- `platform-sample-app` serves a static spec at:
  - `GET http://localhost:8080/openapi.yaml`

## API Registry (Optional)

`platform-api-registry` is an optional app that:
- keeps a configured catalog of services and their OpenAPI spec URLs
- fetches specs in parallel (timeout/parallelism controlled)
- exposes:
  - `GET /registry/apis` (configured list)
  - `GET /registry/snapshot` (status + sha256 digest per spec)
  - `GET /registry/spec/{name}` (proxy the raw spec)

### Run locally

Terminal A:

```bash
mvn -q -pl platform-sample-app spring-boot:run
```

Terminal B:

```bash
mvn -q -pl platform-api-registry spring-boot:run
```

Then configure `platform-api-registry` with the sample app:

```yaml
platform:
  api-registry:
    services:
      - name: sample
        base-uri: http://localhost:8080
        spec-path: /openapi.yaml
```

Verify:
- `GET http://localhost:8082/registry/apis`
- `GET http://localhost:8082/registry/snapshot`
- `GET http://localhost:8082/registry/spec/sample`

## Why this design

- Minimal release verification:
  - snapshot includes a digest (sha256) to detect unexpected spec changes quickly
- Strict upgrade isolation:
  - registry core is Spring-free (`platform-api-registry-core`)
- Frontend readiness:
  - OpenAPI spec becomes the input for generating typed clients (Vue step)

