#!/usr/bin/env bash
set -euo pipefail

mode="${1:?usage: deploy.sh <docker|compose|swarm|k8s|baremetal> <serviceName> [image]}"
service="${2:?usage: deploy.sh <docker|compose|swarm|k8s|baremetal> <serviceName> [image]}"
image="${3:-}"

root_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
deploy_dir="${root_dir}/platform-deploy"

docker_context="${DOCKER_CONTEXT:-}"
docker_cmd=(docker)
if [ -n "${docker_context}" ]; then
  docker_cmd+=(--context "${docker_context}")
fi

host_port="${HOST_PORT:-8080}"
container_port="${CONTAINER_PORT:-8080}"
health_url="${HEALTH_URL:-http://localhost:${host_port}/actuator/health/readiness}"
health_timeout="${HEALTH_TIMEOUT_SECONDS:-60}"

case "$mode" in
  docker)
    if [ -z "$image" ]; then
      echo "image required for docker mode" >&2
      exit 1
    fi
    "${docker_cmd[@]}" rm -f "$service" >/dev/null 2>&1 || true
    "${docker_cmd[@]}" run -d --name "$service" -p "${host_port}:${container_port}" "$image" >/dev/null
    "${deploy_dir}/verify-http.sh" "$health_url" "$health_timeout"
    ;;
  compose)
    if [ -z "$image" ]; then
      echo "image required for compose mode" >&2
      exit 1
    fi
    PLATFORM_IMAGE="$image" PLATFORM_PORT="$host_port" "${docker_cmd[@]}" compose \
      -f "${deploy_dir}/compose/docker-compose.yml" \
      --project-name "$service" up -d
    "${deploy_dir}/verify-http.sh" "$health_url" "$health_timeout"
    ;;
  swarm)
    if [ -z "$image" ]; then
      echo "image required for swarm mode" >&2
      exit 1
    fi
    stack_file="${STACK_FILE:-${deploy_dir}/swarm/stack.yml}"
    stack_name="${STACK_NAME:-$service}"
    PLATFORM_IMAGE="$image" PLATFORM_PORT="$host_port" "${docker_cmd[@]}" stack deploy -c "$stack_file" "$stack_name" >/dev/null
    "${deploy_dir}/verify-http.sh" "$health_url" "$health_timeout"
    ;;
  k8s)
    if [ -z "$image" ]; then
      echo "image required for k8s mode" >&2
      exit 1
    fi
    ns="${NAMESPACE:-default}"
    release="${RELEASE:-$service}"
    chart="${CHART_PATH:-${deploy_dir}/helm/platform-service}"
    repo="$image"
    tag="latest"
    if [[ "$image" =~ ^(.+):([^/]+)$ ]]; then
      repo="${BASH_REMATCH[1]}"
      tag="${BASH_REMATCH[2]}"
    fi
    helm upgrade --install "$release" "$chart" -n "$ns" --create-namespace \
      --set "image.repository=${repo}" \
      --set "image.tag=${tag}" \
      --set "service.port=${container_port}"
    kubectl rollout status "deployment/${release}" -n "$ns" --timeout "${health_timeout}s"
    ;;
  baremetal)
    echo "systemd template: ${deploy_dir}/systemd/platform-service.service"
    ;;
  *)
    echo "unknown mode: $mode" >&2
    exit 1
    ;;
esac
