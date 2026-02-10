#!/usr/bin/env bash
set -euo pipefail

CLUSTER_NAME="${CLUSTER_NAME:-platform-lab}"
NAMESPACE="${NAMESPACE:-platform-lab}"
RELEASE="${RELEASE:-platform-sample-app}"
REPLICAS="${REPLICAS:-3}"
JAVA_VERSION="${JAVA_VERSION:-21}"

# Optional: set to 1 to run readiness + k6 + python against a port-forwarded service.
RUN_SMOKE="${RUN_SMOKE:-0}"
LOCAL_PORT="${LOCAL_PORT:-8080}"

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
CHART_PATH="${ROOT_DIR}/platform-deploy/helm/platform-service"
DOCKERFILE="${ROOT_DIR}/platform-deploy/docker/Dockerfile.jvm"

KUBE_CONTEXT="k3d-${CLUSTER_NAME}"

command -v k3d >/dev/null 2>&1 || { echo "k3d not found in PATH" >&2; exit 2; }
command -v docker >/dev/null 2>&1 || { echo "docker not found in PATH" >&2; exit 2; }
command -v kubectl >/dev/null 2>&1 || { echo "kubectl not found in PATH" >&2; exit 2; }
command -v helm >/dev/null 2>&1 || { echo "helm not found in PATH" >&2; exit 2; }
command -v mvn >/dev/null 2>&1 || { echo "mvn not found in PATH" >&2; exit 2; }

if ! k3d cluster list | awk 'NR>1 {print $1}' | grep -qx "${CLUSTER_NAME}"; then
  echo "Creating k3d cluster: ${CLUSTER_NAME}"
  k3d cluster create "${CLUSTER_NAME}" --agents 2 --servers 1 --wait
else
  echo "k3d cluster already exists: ${CLUSTER_NAME}"
fi

echo "Building sample app jar..."
(cd "${ROOT_DIR}" && mvn -q -pl platform-sample-app -DskipTests package)

JAR_FILE="$(ls -t "${ROOT_DIR}/platform-sample-app/target/platform-sample-app-"*.jar | head -n 1)"
IMAGE="platform-sample-app:local"

echo "Building docker image: ${IMAGE}"
docker build \
  -f "${DOCKERFILE}" \
  --build-arg "JAVA_VERSION=${JAVA_VERSION}" \
  --build-arg "JAR_FILE=platform-sample-app/target/$(basename "${JAR_FILE}")" \
  -t "${IMAGE}" \
  "${ROOT_DIR}"

echo "Importing image into k3d: ${IMAGE}"
k3d image import -c "${CLUSTER_NAME}" "${IMAGE}"

echo "Deploying via Helm: ${RELEASE} ns=${NAMESPACE} replicas=${REPLICAS}"
helm upgrade --install "${RELEASE}" "${CHART_PATH}" \
  --kube-context "${KUBE_CONTEXT}" \
  -n "${NAMESPACE}" --create-namespace \
  --set "replicaCount=${REPLICAS}" \
  --set "image.repository=platform-sample-app" \
  --set "image.tag=local" \
  --set "service.port=8080"

kubectl --context "${KUBE_CONTEXT}" rollout status "deployment/${RELEASE}" -n "${NAMESPACE}" --timeout "120s"

if [[ "${RUN_SMOKE}" != "1" ]]; then
  echo ""
  echo "Next (port-forward the service):"
  echo "  kubectl --context ${KUBE_CONTEXT} -n ${NAMESPACE} port-forward svc/${RELEASE} ${LOCAL_PORT}:8080"
  exit 0
fi

pf_log="$(mktemp -t platform-k3d-portforward.XXXXXX.log)"
kubectl --context "${KUBE_CONTEXT}" -n "${NAMESPACE}" port-forward "svc/${RELEASE}" "${LOCAL_PORT}:8080" >"${pf_log}" 2>&1 &
pf_pid="$!"
trap 'kill "${pf_pid}" >/dev/null 2>&1 || true' EXIT

BASE_URL="http://localhost:${LOCAL_PORT}"
HEALTH_URL="${BASE_URL}/actuator/health/readiness"

echo ""
echo "Waiting for readiness: ${HEALTH_URL}"
"${ROOT_DIR}/platform-deploy/verify-http.sh" "${HEALTH_URL}" 120

echo ""
echo "k6 smoke..."
VUS=10 DURATION=10s "${ROOT_DIR}/platform-loadtest/run-k6.sh" ping-smoke "${BASE_URL}"

echo ""
echo "Python black-box tests..."
if command -v python3 >/dev/null 2>&1; then
  pushd "${ROOT_DIR}/platform-test-python" >/dev/null
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

