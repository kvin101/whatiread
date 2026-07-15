WhatIRead
=========

Self-hosted reading tracker. Track books, organize shelves, connect with friends.

**License:** [AGPL-3.0](./LICENSE)

## Run locally (no Docker)

```bash
./mvnw spring-boot:run -Pdev -Dspring-boot.run.profiles=dev
cd frontend && npm install && npm run dev
```

- API: http://localhost:8080/api/v1/status
- Frontend: http://localhost:5173

## Run with Docker

Edit `.env` (passwords, secrets), then:

```bash
docker compose up --build -d
```

Open http://localhost (or `HTTP_PORT` from `.env`).

Pull pre-built images instead of building — set `API_IMAGE` and `WEB_IMAGE` in `.env`.

### Observability

On by default (see `.env`). To disable, clear `COMPOSE_PROFILES` and set `OBSERVABILITY_ENABLED=false`.

Services: Prometheus `:9090`, Jaeger `:16686`, Grafana `:3000`, Loki `:3100`, Dozzle `:8087`.

## Configuration

See `.env` for Docker variables. Key API settings:

| Variable | Description |
|----------|-------------|
| `JWT_SECRET` | Min 32 characters |
| `POSTGRES_PASSWORD` | Database password |
| `MEILISEARCH_MASTER_KEY` | Meilisearch key |
| `REGISTRATION_ENABLED` | Allow new sign-ups |
| `CORS_ORIGINS` | Frontend origin(s) |

## Tests

```bash
./mvnw verify
cd frontend && npm run build
python3 -m pip install -r scripts/smoke/requirements.txt
python3 scripts/smoke/run.py   # live Docker stack E2E
```

Optional env: `BASE_URL`, `SMOKE_PASSWORD`, `SMOKE_ADMIN_EMAIL`, `SMOKE_ADMIN_PASSWORD`.

Tip: set `RATE_LIMIT_ENABLED=false` in `.env` for faster full smoke runs.

## Production notes

- Set strong secrets in `.env`
- Put HTTPS in front of the web container
- Set `MEILI_ENV=production` for Meilisearch
- Mount `avatars-data` volume (included in compose)
