#!/usr/bin/env bash
set -euo pipefail

CLUSTER_NAME="${CLUSTER_NAME:-platform-lab}"
NAMESPACE="${NAMESPACE:-platform-lab}"
RELEASE="${RELEASE:-platform-sample-app}"
REPLICAS="${REPLICAS:-3}"
JAVA_VERSION="${JAVA_VERSION:-21}"

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
KIND_CONFIG="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/kind-config.yaml"
CHART_PATH="${ROOT_DIR}/platform-deploy/helm/platform-service"
DOCKERFILE="${ROOT_DIR}/platform-deploy/docker/Dockerfile.jvm"

command -v kind >/dev/null 2>&1 || { echo "kind not found in PATH" >&2; exit 2; }
command -v docker >/dev/null 2>&1 || { echo "docker not found in PATH" >&2; exit 2; }
command -v kubectl >/dev/null 2>&1 || { echo "kubectl not found in PATH" >&2; exit 2; }
command -v helm >/dev/null 2>&1 || { echo "helm not found in PATH" >&2; exit 2; }
command -v mvn >/dev/null 2>&1 || { echo "mvn not found in PATH" >&2; exit 2; }

if ! kind get clusters | grep -q "^${CLUSTER_NAME}$"; then
  echo "Creating kind cluster: ${CLUSTER_NAME}"
  kind create cluster --name "${CLUSTER_NAME}" --config "${KIND_CONFIG}"
else
  echo "Kind cluster already exists: ${CLUSTER_NAME}"
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

echo "Loading image into kind: ${IMAGE}"
kind load docker-image "${IMAGE}" --name "${CLUSTER_NAME}"

echo "Deploying via Helm: ${RELEASE} ns=${NAMESPACE} replicas=${REPLICAS}"
helm upgrade --install "${RELEASE}" "${CHART_PATH}" \
  -n "${NAMESPACE}" --create-namespace \
  --set "replicaCount=${REPLICAS}" \
  --set "image.repository=platform-sample-app" \
  --set "image.tag=local" \
  --set "service.port=8080"

kubectl rollout status "deployment/${RELEASE}" -n "${NAMESPACE}" --timeout "120s"

echo ""
echo "Next (port-forward the service):"
echo "  kubectl -n ${NAMESPACE} port-forward svc/${RELEASE} 8080:8080"

