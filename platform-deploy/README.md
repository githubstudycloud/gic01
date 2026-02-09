# platform-deploy

Goal: one artifact, many targets, with minimal "release verification" hooks.

This folder provides deployment templates and one-command wrappers:
- `docker` (single container)
- `compose` (local multi-dependency topology)
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

Build an image from a jar (Dockerfile) and deploy:

```powershell
./platform-deploy/deploy.ps1 -Mode docker -ServiceName platform-sample -JarPath platform-sample-app/target/platform-sample-app-0.1.0-SNAPSHOT.jar
```

K8s (Helm) deploy:

```powershell
./platform-deploy/deploy.ps1 -Mode k8s -ServiceName platform-sample -Image your-registry/platform-sample:0.1.0 -Namespace dev
```

## Bash

```bash
./platform-deploy/verify-http.sh http://localhost:8080/actuator/health/readiness
./platform-deploy/deploy.sh docker platform-sample platform-sample:local
```

## Notes

- K8s templates assume Spring Boot actuator probes:
  - `/actuator/health/liveness`
  - `/actuator/health/readiness`
- For production you will likely layer in:
  - Ingress/Gateway, service mesh, pod security policies, PDB, network policies
  - secret management, observability exporters, rollout strategies

