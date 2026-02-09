#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
MAX_AGE_YEARS="${MAX_AGE_YEARS:-3}"

PYTHON_BIN="${PYTHON_BIN:-python3}"

ARGS=( "--max-age-years" "${MAX_AGE_YEARS}" )

if [[ "${INCLUDE_TRANSITIVE:-}" == "1" ]]; then
  ARGS+=( "--include-transitive" )
fi
if [[ "${VENDOR:-}" == "1" ]]; then
  ARGS+=( "--vendor" )
fi

"${PYTHON_BIN}" "${ROOT_DIR}/scripts/deps-age-audit.py" "${ARGS[@]}"

