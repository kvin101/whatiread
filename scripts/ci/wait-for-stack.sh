#!/usr/bin/env bash
# Wait until the Docker Compose web proxy serves the API status endpoint.
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost}"
STATUS_URL="${BASE_URL%/}/api/v1/status"
TIMEOUT_SEC="${1:-360}"

deadline=$((SECONDS + TIMEOUT_SEC))

echo "Waiting for ${STATUS_URL} (timeout ${TIMEOUT_SEC}s)..."

while (( SECONDS < deadline )); do
  if curl -sf "${STATUS_URL}" >/dev/null 2>&1; then
    echo "Stack is ready."
    exit 0
  fi
  sleep 5
done

echo "Stack did not become ready within ${TIMEOUT_SEC}s."
docker compose ps || true
docker compose logs api --tail 80 || true
docker compose logs web --tail 40 || true
exit 1
