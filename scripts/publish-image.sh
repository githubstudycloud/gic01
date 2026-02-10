#!/usr/bin/env bash
set -euo pipefail

# Build a Spring Boot app JAR, build an OCI image, and optionally push it.
# Designed to be a small, composable primitive for "one artifact, many targets".

APP_MODULE="${APP_MODULE:-platform-sample-app}"
JAVA_VERSION="${JAVA_VERSION:-21}"

IMAGE_REPO="${IMAGE_REPO:-}"
IMAGE_TAG="${IMAGE_TAG:-}"
PUSH="${PUSH:-0}" # 0|1

DOCKER_CONTEXT="${DOCKER_CONTEXT:-}"

SERVICE_NAME="${SERVICE_NAME:-platform-sample}"
NAMESPACE="${NAMESPACE:-default}"
RELEASE="${RELEASE:-$SERVICE_NAME}"

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "${ROOT_DIR}"

if [[ -z "${IMAGE_REPO}" ]]; then
  echo "usage: IMAGE_REPO=your-registry/your-image [IMAGE_TAG=1.0.0] [PUSH=1] $0" >&2
  exit 2
fi

if [[ -z "${IMAGE_TAG}" ]]; then
  if command -v git >/dev/null 2>&1; then
    IMAGE_TAG="dev-$(git rev-parse --short HEAD 2>/dev/null || echo dev)"
  else
    IMAGE_TAG="dev"
  fi
fi

IMAGE="${IMAGE_REPO}:${IMAGE_TAG}"

echo "Publish image"
echo "  AppModule:    ${APP_MODULE}"
echo "  JavaVersion:  ${JAVA_VERSION}"
echo "  Image:        ${IMAGE}"
echo "  Push:         ${PUSH}"
echo "  Context:      ${DOCKER_CONTEXT:-<default>}"

command -v mvn >/dev/null 2>&1 || { echo "mvn not found in PATH" >&2; exit 2; }
command -v docker >/dev/null 2>&1 || { echo "docker not found in PATH" >&2; exit 2; }

docker_cmd=(docker)
if [[ -n "${DOCKER_CONTEXT}" ]]; then
  docker_cmd+=(--context "${DOCKER_CONTEXT}")
fi

echo ""
echo "[1/3] Build app jar..."
mvn -q -pl "${APP_MODULE}" -am -DskipTests package

JAR_FILE="$(
  ls -t "${ROOT_DIR}/${APP_MODULE}/target/${APP_MODULE}-"*.jar 2>/dev/null \
    | grep -vE '(sources|javadoc)\\.jar$' \
    | head -n 1 \
    || true
)"
if [[ -z "${JAR_FILE}" ]]; then
  echo "jar not found under ${APP_MODULE}/target" >&2
  exit 2
fi

JAR_REL="${APP_MODULE}/target/$(basename "${JAR_FILE}")"

echo "  Jar: ${JAR_REL}"

echo ""
echo "[2/3] Build image..."
"${docker_cmd[@]}" build \
  -f "${ROOT_DIR}/platform-deploy/docker/Dockerfile.jvm" \
  --build-arg "JAVA_VERSION=${JAVA_VERSION}" \
  --build-arg "JAR_FILE=${JAR_REL}" \
  -t "${IMAGE}" \
  "${ROOT_DIR}"

repo_digest=""
if [[ "${PUSH}" == "1" ]]; then
  echo ""
  echo "[3/3] Push image..."
  "${docker_cmd[@]}" push "${IMAGE}"

  repo_digest="$("${docker_cmd[@]}" image inspect --format '{{index .RepoDigests 0}}' "${IMAGE}" 2>/dev/null || true)"
  if [[ -z "${repo_digest}" ]]; then
    # Some daemons only populate RepoDigests after a pull.
    "${docker_cmd[@]}" pull "${IMAGE}" >/dev/null
    repo_digest="$("${docker_cmd[@]}" image inspect --format '{{index .RepoDigests 0}}' "${IMAGE}" 2>/dev/null || true)"
  fi
else
  echo ""
  echo "[3/3] Push skipped (set PUSH=1)."
fi

echo ""
echo "Result:"
echo "  Image(tag):    ${IMAGE}"
if [[ -n "${repo_digest}" ]]; then
  echo "  Image(digest): ${repo_digest}"
fi

echo ""
echo "Deploy examples:"
echo "  ./platform-deploy/deploy.sh docker ${SERVICE_NAME} ${IMAGE}"
echo "  ./platform-deploy/deploy.sh swarm ${SERVICE_NAME} ${IMAGE}"
echo "  NAMESPACE=${NAMESPACE} RELEASE=${RELEASE} ./platform-deploy/deploy.sh k8s ${SERVICE_NAME} ${IMAGE}"
if [[ -n "${repo_digest}" ]]; then
  echo ""
  echo "Recommended (immutable digest):"
  echo "  NAMESPACE=${NAMESPACE} RELEASE=${RELEASE} ./platform-deploy/deploy.sh k8s ${SERVICE_NAME} ${repo_digest}"
  echo "  ./platform-deploy/deploy.sh swarm ${SERVICE_NAME} ${repo_digest}"
fi

