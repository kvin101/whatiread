#!/usr/bin/env bash
# Wait until the API container liveness probe succeeds inside the Compose network.
set -euo pipefail

COMPOSE_FILES="${COMPOSE_FILES:--f docker-compose.yml -f docker-compose.ci.yml}"
TIMEOUT_SEC="${1:-420}"

deadline=$((SECONDS + TIMEOUT_SEC))

echo "Waiting for API liveness (timeout ${TIMEOUT_SEC}s)..."

while (( SECONDS < deadline )); do
  if docker compose ${COMPOSE_FILES} ps api --status running --quiet 2>/dev/null | grep -q . \
    && docker compose ${COMPOSE_FILES} exec -T api \
      wget -q -O /dev/null http://127.0.0.1:8080/actuator/health/liveness 2>/dev/null; then
    echo "API is ready."
    exit 0
  fi

  if docker compose ${COMPOSE_FILES} ps api --status exited --quiet 2>/dev/null | grep -q .; then
    echo "API container exited before becoming healthy."
    docker compose ${COMPOSE_FILES} ps api || true
    docker compose ${COMPOSE_FILES} logs api --tail 200 || true
    exit 1
  fi

  sleep 5
done

echo "API did not become healthy within ${TIMEOUT_SEC}s."
docker compose ${COMPOSE_FILES} ps api || true
docker compose ${COMPOSE_FILES} logs api --tail 200 || true
exit 1
