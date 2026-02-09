#!/usr/bin/env bash
set -euo pipefail

SCRIPT="${1:-}"
BASE_URL="${2:-http://localhost:8080}"
VUS="${VUS:-10}"
DURATION="${DURATION:-15s}"
K6_IMAGE="${PLATFORM_K6_IMAGE:-grafana/k6:0.49.0}"

if [[ -z "${SCRIPT}" ]]; then
  echo "usage: $0 <script-name> [base-url]" >&2
  exit 2
fi

SCRIPT_FILE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/k6/${SCRIPT}.js"
if [[ ! -f "${SCRIPT_FILE}" ]]; then
  echo "k6 script not found: ${SCRIPT_FILE}" >&2
  exit 2
fi

echo "Running k6:"
echo "  Image:    ${K6_IMAGE}"
echo "  Script:   ${SCRIPT}"
echo "  BaseUrl:  ${BASE_URL}"
echo "  VUs:      ${VUS}"
echo "  Duration: ${DURATION}"

docker run --rm \
  -v "$(dirname "${SCRIPT_FILE}"):/scripts:ro" \
  -e "BASE_URL=${BASE_URL}" \
  -e "VUS=${VUS}" \
  -e "DURATION=${DURATION}" \
  "${K6_IMAGE}" run "/scripts/${SCRIPT}.js"

