#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

JAVA_VERSION="${JAVA_VERSION:-21}"
HOST_PORT="${HOST_PORT:-18080}"
SERVICE_NAME="${SERVICE_NAME:-platform-sample-wsl}"
IMAGE="${IMAGE:-platform-sample-app:wsl}"

BASE_URL="http://localhost:${HOST_PORT}"
HEALTH_URL="${HEALTH_URL:-${BASE_URL}/actuator/health/readiness}"

cd "${ROOT_DIR}"

echo "WSL smoke deploy"
echo "  JavaVersion:  ${JAVA_VERSION}"
echo "  Image:        ${IMAGE}"
echo "  ServiceName:  ${SERVICE_NAME}"
echo "  HostPort:     ${HOST_PORT}"
echo "  HealthUrl:    ${HEALTH_URL}"

command -v java >/dev/null 2>&1 || { echo "java not found in PATH" >&2; exit 2; }
command -v mvn >/dev/null 2>&1 || { echo "mvn not found in PATH" >&2; exit 2; }
command -v docker >/dev/null 2>&1 || { echo "docker not found in PATH" >&2; exit 2; }

echo ""
echo "[1/4] Build sample app jar..."
mvn -q -pl platform-sample-app -am -DskipTests package

JAR_FILE="$(ls -t platform-sample-app/target/platform-sample-app-*.jar | head -n 1)"
echo "  Jar: ${JAR_FILE}"

echo ""
echo "[2/4] Build Docker image..."
docker build \
  -f "./platform-deploy/docker/Dockerfile.jvm" \
  --build-arg "JAVA_VERSION=${JAVA_VERSION}" \
  --build-arg "JAR_FILE=${JAR_FILE}" \
  -t "${IMAGE}" \
  .

cleanup() {
  docker rm -f "${SERVICE_NAME}" >/dev/null 2>&1 || true
}
trap cleanup EXIT

echo ""
echo "[3/4] Deploy via Docker (readiness gate)..."
HOST_PORT="${HOST_PORT}" HEALTH_URL="${HEALTH_URL}" ./platform-deploy/deploy.sh docker "${SERVICE_NAME}" "${IMAGE}"

echo ""
echo "[4/4] Python black-box tests..."
if command -v python3 >/dev/null 2>&1; then
  pushd platform-test-python >/dev/null
  if [[ ! -x ".venv/bin/python" ]]; then
    python3 -m venv .venv
    .venv/bin/python -m pip install -r requirements.txt
  fi
  PLATFORM_BASE_URL="${BASE_URL}" .venv/bin/python -m pytest
  popd >/dev/null
else
  echo "python3 not found; skipping."
fi

echo ""
echo "OK:"
echo "  ${BASE_URL}/demo/ping"
echo "  ${BASE_URL}/flows"
echo "  ${BASE_URL}/crud/todos"
