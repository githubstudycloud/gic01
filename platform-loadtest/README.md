# platform-loadtest

Goal: reproducible cluster + concurrency verification, without bloating the default PR checks.

This folder is NOT wired into `mvn test`. Treat it as an opt-in lab:
- run locally before release
- run in nightly CI (optional)

## k6 (via Docker)

No local k6 installation required.

Prereq:
- Docker
- a running backend (example):

```bash
mvn -q -pl platform-sample-app spring-boot:run
```

Run ping smoke:

```powershell
./platform-loadtest/run-k6.ps1 -Script ping-smoke -BaseUrl http://localhost:8080 -Vus 10 -Duration 15s
```

Run lock contention (uses the optional `holdMillis` query param):

```powershell
./platform-loadtest/run-k6.ps1 -Script lock-contention -BaseUrl http://localhost:8080 -Vus 20 -Duration 15s
```

## Local K8s lab (kind) - optional

This is a starting point for cluster verification:
- create a kind cluster
- build the sample app image
- load it into kind
- deploy via the shared Helm chart (replicas configurable)

See:
- `platform-loadtest/kind/lab.ps1`
- `platform-loadtest/kind/lab.sh`

## Local K8s lab (k3d / k3s) - optional

Same idea as the kind lab, but using k3s (via k3d):

See:
- `platform-loadtest/k3d/lab.ps1`
- `platform-loadtest/k3d/lab.sh`
