#!/usr/bin/env bash
set -euo pipefail

LEVEL="${LEVEL:-standard}" # fast|standard|full
DEPLOY_MODE="${DEPLOY_MODE:-docker}" # docker|compose|swarm|k8s
JAVA_VERSION="${JAVA_VERSION:-21}"
HOST_PORT="${HOST_PORT:-18080}"
DOCKER_CONTEXT="${DOCKER_CONTEXT:-}"

SKIP_DOCKER="${SKIP_DOCKER:-0}"
SKIP_K6="${SKIP_K6:-0}"
SKIP_PYTHON="${SKIP_PYTHON:-0}"
SKIP_BUILD_IMAGE="${SKIP_BUILD_IMAGE:-0}"
CLEANUP="${CLEANUP:-1}"

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

cd "${ROOT_DIR}"

echo "Release verify"
echo "  Level:      ${LEVEL}"
echo "  DeployMode: ${DEPLOY_MODE}"
echo "  Java:       ${JAVA_VERSION}"
echo "  HostPort:   ${HOST_PORT}"
echo "  Context:    ${DOCKER_CONTEXT:-<default>}"
echo "  SkipDocker: ${SKIP_DOCKER}"
echo "  SkipK6:     ${SKIP_K6}"
echo "  SkipPython: ${SKIP_PYTHON}"
echo "  SkipBuild:  ${SKIP_BUILD_IMAGE}"
echo "  Cleanup:    ${CLEANUP}"

command -v mvn >/dev/null 2>&1 || { echo "mvn not found in PATH" >&2; exit 2; }

if [[ "${LEVEL}" == "fast" ]]; then
  echo ""
  echo "[1/2] Maven unit tests..."
  mvn -q test

  echo ""
  echo "[2/2] Dependency age audit (direct runtime deps)..."
  ./scripts/deps-age-audit.sh
  exit 0
fi

echo ""
echo "[1/5] Maven verify (unit + IT)..."
mvn -q -Pit verify

echo ""
echo "[2/5] Dependency age audit (direct runtime deps)..."
./scripts/deps-age-audit.sh

if [[ "${SKIP_DOCKER}" == "1" ]]; then
  echo ""
  echo "Docker steps skipped."
  exit 0
fi

docker_cmd=(docker)
if [[ -n "${DOCKER_CONTEXT}" ]]; then
  docker_cmd+=(--context "${DOCKER_CONTEXT}")
fi

needs_docker=0
if [[ "${DEPLOY_MODE}" != "k8s" ]]; then
  needs_docker=1
fi
if [[ "${SKIP_K6}" != "1" ]]; then
  needs_docker=1
fi
if [[ "${needs_docker}" == "1" ]]; then
  command -v docker >/dev/null 2>&1 || { echo "docker not found in PATH" >&2; exit 2; }
fi

SERVICE_NAME="${SERVICE_NAME:-platform-sample-verify}"
BASE_URL_INPUT="${BASE_URL-}"
BASE_URL="${BASE_URL_INPUT:-http://localhost:${HOST_PORT}}"
HEALTH_URL_INPUT="${HEALTH_URL-}"
HEALTH_URL="${HEALTH_URL_INPUT:-${BASE_URL}/actuator/health/readiness}"
IMAGE_INPUT="${IMAGE-}"
IMAGE="${IMAGE_INPUT:-platform-sample-app:verify}"
STACK_NAME="${STACK_NAME:-$SERVICE_NAME}"
NAMESPACE="${NAMESPACE:-default}"
RELEASE="${RELEASE:-$SERVICE_NAME}"

cleanup() {
  if [[ "${CLEANUP}" != "1" ]]; then
    return 0
  fi

  case "${DEPLOY_MODE}" in
    docker)
      "${docker_cmd[@]}" rm -f "${SERVICE_NAME}" >/dev/null 2>&1 || true
      ;;
    compose)
      "${docker_cmd[@]}" compose -f "${ROOT_DIR}/platform-deploy/compose/docker-compose.yml" --project-name "${SERVICE_NAME}" down -v >/dev/null 2>&1 || true
      ;;
    swarm)
      "${docker_cmd[@]}" stack rm "${STACK_NAME}" >/dev/null 2>&1 || true
      ;;
    k8s)
      if command -v helm >/dev/null 2>&1; then
        helm uninstall "${RELEASE}" -n "${NAMESPACE}" >/dev/null 2>&1 || true
      fi
      ;;
  esac
}
trap cleanup EXIT

if [[ "${DEPLOY_MODE}" == "k8s" && -z "${BASE_URL_INPUT}" ]]; then
  echo "For DEPLOY_MODE=k8s, set BASE_URL (ingress or port-forward URL), e.g.:" >&2
  echo "  BASE_URL=http://localhost:8080 DEPLOY_MODE=k8s IMAGE=your-registry/app:1.0.0 ./scripts/release-verify.sh" >&2
  exit 2
fi

if [[ "${DEPLOY_MODE}" == "k8s" && -z "${IMAGE_INPUT}" ]]; then
  echo "For DEPLOY_MODE=k8s, set IMAGE explicitly (registry image recommended)." >&2
  exit 2
fi

JAR_FILE="$(ls -t "${ROOT_DIR}/platform-sample-app/target/platform-sample-app-"*.jar 2>/dev/null | head -n 1 || true)"
if [[ -z "${JAR_FILE}" ]]; then
  echo ""
  echo "[3/5] Building sample app jar..."
  mvn -q -pl platform-sample-app -DskipTests package
  JAR_FILE="$(ls -t "${ROOT_DIR}/platform-sample-app/target/platform-sample-app-"*.jar | head -n 1)"
fi

if [[ "${DEPLOY_MODE}" != "k8s" && "${SKIP_BUILD_IMAGE}" != "1" ]]; then
  DOCKERFILE="${ROOT_DIR}/platform-deploy/docker/Dockerfile.jvm"

  echo ""
  echo "[3/5] Build Docker image..."
  "${docker_cmd[@]}" build \
    -f "${DOCKERFILE}" \
    --build-arg "JAVA_VERSION=${JAVA_VERSION}" \
    --build-arg "JAR_FILE=platform-sample-app/target/$(basename "${JAR_FILE}")" \
    -t "${IMAGE}" \
    "${ROOT_DIR}"
fi

echo ""
echo "[3/5] Deploy (${DEPLOY_MODE}) + readiness gate..."
HOST_PORT="${HOST_PORT}" HEALTH_URL="${HEALTH_URL}" \
  DOCKER_CONTEXT="${DOCKER_CONTEXT}" STACK_NAME="${STACK_NAME}" \
  NAMESPACE="${NAMESPACE}" RELEASE="${RELEASE}" \
  ./platform-deploy/deploy.sh "${DEPLOY_MODE}" "${SERVICE_NAME}" "${IMAGE}"

if [[ "${SKIP_K6}" != "1" ]]; then
  echo ""
  echo "[4/5] k6 smoke..."
  VUS=10 DURATION=10s ./platform-loadtest/run-k6.sh ping-smoke "${BASE_URL}"
  VUS=20 DURATION=10s ./platform-loadtest/run-k6.sh lock-contention "${BASE_URL}"
else
  echo ""
  echo "[4/5] k6 skipped."
fi

if [[ "${SKIP_PYTHON}" != "1" ]]; then
  echo ""
  echo "[5/5] Python black-box tests..."
  command -v python3 >/dev/null 2>&1 || { echo "python3 not found in PATH" >&2; exit 2; }

  pushd platform-test-python >/dev/null
  if [[ ! -x ".venv/bin/python" ]]; then
    python3 -m venv .venv
    .venv/bin/python -m pip install -r requirements.txt
  fi
  PLATFORM_BASE_URL="${BASE_URL}" .venv/bin/python -m pytest
  popd >/dev/null
else
  echo ""
  echo "[5/5] Python tests skipped."
fi

if [[ "${LEVEL}" == "full" ]]; then
  echo ""
  echo "Full level note: for cluster verification, run:"
  echo "  ./platform-loadtest/kind/lab.sh"
  echo "  ./platform-loadtest/run-k6.sh ping-smoke http://localhost:8080 ..."
fi
