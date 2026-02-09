#!/usr/bin/env bash
set -euo pipefail

url="${1:?usage: verify-http.sh <url> [timeoutSeconds] [intervalSeconds]}"
timeout="${2:-60}"
interval="${3:-2}"

deadline=$(( $(date +%s) + timeout ))
while [ "$(date +%s)" -lt "$deadline" ]; do
  if curl -fsS "$url" >/dev/null 2>&1; then
    echo "OK: $url"
    exit 0
  fi
  sleep "$interval"
done

echo "Health check failed after ${timeout}s: $url" >&2
exit 1

