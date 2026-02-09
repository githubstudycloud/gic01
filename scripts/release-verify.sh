#!/usr/bin/env bash
set -euo pipefail

LEVEL="${LEVEL:-standard}" # fast|standard|full
JAVA_VERSION="${JAVA_VERSION:-21}"
HOST_PORT="${HOST_PORT:-18080}"

SKIP_DOCKER="${SKIP_DOCKER:-0}"
SKIP_K6="${SKIP_K6:-0}"
SKIP_PYTHON="${SKIP_PYTHON:-0}"

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

cd "${ROOT_DIR}"

echo "Release verify"
echo "  Level:      ${LEVEL}"
echo "  Java:       ${JAVA_VERSION}"
echo "  HostPort:   ${HOST_PORT}"
echo "  SkipDocker: ${SKIP_DOCKER}"
echo "  SkipK6:     ${SKIP_K6}"
echo "  SkipPython: ${SKIP_PYTHON}"

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

command -v docker >/dev/null 2>&1 || { echo "docker not found in PATH" >&2; exit 2; }

SERVICE_NAME="platform-sample-verify"
BASE_URL="http://localhost:${HOST_PORT}"

JAR_FILE="$(ls -t "${ROOT_DIR}/platform-sample-app/target/platform-sample-app-"*.jar 2>/dev/null | head -n 1 || true)"
if [[ -z "${JAR_FILE}" ]]; then
  echo ""
  echo "[3/5] Building sample app jar..."
  mvn -q -pl platform-sample-app -DskipTests package
  JAR_FILE="$(ls -t "${ROOT_DIR}/platform-sample-app/target/platform-sample-app-"*.jar | head -n 1)"
fi

IMAGE="platform-sample-app:verify"
DOCKERFILE="${ROOT_DIR}/platform-deploy/docker/Dockerfile.jvm"

cleanup() {
  docker rm -f "${SERVICE_NAME}" >/dev/null 2>&1 || true
}
trap cleanup EXIT

echo ""
echo "[3/5] Docker deploy + readiness gate..."
docker build \
  -f "${DOCKERFILE}" \
  --build-arg "JAVA_VERSION=${JAVA_VERSION}" \
  --build-arg "JAR_FILE=platform-sample-app/target/$(basename "${JAR_FILE}")" \
  -t "${IMAGE}" \
  "${ROOT_DIR}"

docker rm -f "${SERVICE_NAME}" >/dev/null 2>&1 || true
docker run -d --name "${SERVICE_NAME}" -p "${HOST_PORT}:8080" "${IMAGE}" >/dev/null
./platform-deploy/verify-http.sh "http://localhost:${HOST_PORT}/actuator/health/readiness" 60

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

