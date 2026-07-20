#!/usr/bin/env bash
# Wait until the Docker Compose web proxy serves the API status endpoint.
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost}"
STATUS_URL="${BASE_URL%/}/api/v1/status"
TIMEOUT_SEC="${1:-180}"
POLL_SEC="${POLL_SEC:-3}"

start=$SECONDS
deadline=$((start + TIMEOUT_SEC))

echo "Waiting for ${STATUS_URL} (timeout ${TIMEOUT_SEC}s)..."

while (( SECONDS < deadline )); do
  if curl -sf "${STATUS_URL}" >/dev/null 2>&1; then
    echo "Stack is ready in $((SECONDS - start))s."
    exit 0
  fi

  if docker compose ps api 2>/dev/null | grep -qE 'Restarting|Exited \(1\)|Exited (1)'; then
    echo "API container crashed or is restarting — failing fast."
    docker compose ps || true
    docker compose logs api --tail 120 || true
    exit 1
  fi

  sleep "$POLL_SEC"
done

echo "Stack did not become ready within ${TIMEOUT_SEC}s."
docker compose ps || true
docker compose logs api --tail 120 || true
docker compose logs web --tail 40 || true
exit 1
