# platform-deploy

Goal: one artifact, many targets, with minimal "release verification" hooks.

This folder provides deployment templates and one-command wrappers:
- `docker` (single container)
- `compose` (local multi-dependency topology)
- `swarm` (Docker Swarm stack)
- `k8s` (Helm chart)
- `baremetal` (systemd template)

## PowerShell

Verify a HTTP health endpoint:

```powershell
./platform-deploy/verify-http.ps1 -Url http://localhost:8080/actuator/health/readiness
```

Deploy an existing image via Docker and verify readiness:

```powershell
./platform-deploy/deploy.ps1 -Mode docker -ServiceName platform-sample -Image platform-sample:local
```

Remote Docker via Docker context:

```powershell
./platform-deploy/deploy.ps1 -Mode docker -ServiceName platform-sample -Image your-registry/platform-sample:0.1.0 `
  -DockerContext platform-remote -HealthUrl http://your-host:8080/actuator/health/readiness
```

Build an image from a jar (Dockerfile) and deploy:

```powershell
./platform-deploy/deploy.ps1 -Mode docker -ServiceName platform-sample -JarPath platform-sample-app/target/platform-sample-app-0.1.0-SNAPSHOT.jar
```

Docker Swarm:

```powershell
./platform-deploy/deploy.ps1 -Mode swarm -ServiceName platform-sample -Image your-registry/platform-sample:0.1.0
```

K8s (Helm) deploy:

```powershell
./platform-deploy/deploy.ps1 -Mode k8s -ServiceName platform-sample -Image your-registry/platform-sample:0.1.0 -Namespace dev
```

Notes:
- By default, the Helm release name is also used as the Kubernetes `Deployment`/`Service` name (so `deployment/<release>` matches).

## Bash

```bash
./platform-deploy/verify-http.sh http://localhost:8080/actuator/health/readiness
./platform-deploy/deploy.sh docker platform-sample platform-sample:local
```

Remote Docker via Docker context (health URL must be reachable from where you run the script):

```bash
DOCKER_CONTEXT=platform-remote \
  HEALTH_URL=http://your-host:8080/actuator/health/readiness \
  ./platform-deploy/deploy.sh docker platform-sample your-registry/platform-sample:0.1.0
```

Docker Swarm:

```bash
./platform-deploy/deploy.sh swarm platform-sample your-registry/platform-sample:0.1.0
```

## WSL (local)

If you're on Windows and want to run the deploy + smoke tests from WSL (Ubuntu), use the WSL wrapper script:

```bash
./platform-deploy/wsl/smoke.sh
```

It builds the sample app JAR, builds a Docker image, deploys it via Docker, gates on readiness,
and runs the Python black-box test suite against the deployed service.

## Notes

- K8s templates assume Spring Boot actuator probes:
  - `/actuator/health/liveness`
  - `/actuator/health/readiness`
- For production you will likely layer in:
  - Ingress/Gateway, service mesh, pod security policies, PDB, network policies
  - secret management, observability exporters, rollout strategies

## Local observability stack (optional)

Bring up a local stack for metrics + traces + logs (Prometheus + Grafana + Tempo + Loki + Promtail + OTLP collector):

```powershell
docker compose -f ./platform-deploy/compose/docker-compose.observability.yml up -d
```

Run apps on the host (ports: sample 8080, hub 8081) and enable OTLP export:

```powershell
$env:PLATFORM_TRACING_OTEL_EXPORT_ENABLED="true"
$env:OTEL_EXPORTER_OTLP_ENDPOINT="http://localhost:4317"

mvn -q -pl platform-sample-app spring-boot:run
mvn -q -pl platform-observability-hub spring-boot:run
```

Note: Loki/Promtail in this compose file scrape Docker container logs. If you want your app logs to
show up in Grafana (Loki), run the app via Docker/Compose (`./platform-deploy/deploy.ps1 -Mode docker|compose ...`)
instead of `spring-boot:run`.

Open:
- Grafana: `http://localhost:3000` (admin / `${GRAFANA_ADMIN_PASSWORD:-admin}`)
- Prometheus: `http://localhost:9090`
- Tempo: `http://localhost:3200`
- Loki: `http://localhost:3100` (datasource provisioned in Grafana; use Explore)
