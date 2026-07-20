# WhatIRead

Self-hosted app for tracking books, shelves, and reading with friends.

Licensed under [AGPL-3.0](./LICENSE). Training AI/ML models on this code is not allowed — see [NON-AI-LICENSE](./licenses/NON-AI-LICENSE) and [licenses/README.md](./licenses/README.md) for how the two fit together. Deployed sites also block AI crawlers via [robots.txt](./frontend/public/robots.txt) and nginx.

## Demo

https://github.com/user-attachments/assets/54d53784-0899-4ef7-9982-c9728e91b455

Full 1080p from CI builds: [product-demo.mp4](https://github.com/kvin101/whatiread/releases/latest/download/product-demo.mp4).

## Run without Docker

```bash
./mvnw spring-boot:run -Pdev -Dspring-boot.run.profiles=dev
cd frontend && npm install && npm run dev
```

API: http://localhost:8080/api/v1/status  
UI: http://localhost:5173

## Run with Docker

Edit `.env` (passwords, secrets — the placeholders aren't meant to stay), then:

```bash
docker compose up --build -d
```

Open http://localhost, or whatever you set for `HTTP_PORT`.

To pull images instead of building, set `API_IMAGE` and `WEB_IMAGE` in `.env`:

- Docker Hub: `vinayk101/whatiread-api:latest`, `vinayk101/whatiread-web:latest`
- GHCR: `ghcr.io/kvin101/whatiread-api:latest`, `ghcr.io/kvin101/whatiread-web:latest`

Per-build tags and digests: [Releases](https://github.com/kvin101/whatiread/releases).

### Observability

The default compose profile brings up Prometheus, Jaeger, Grafana, Loki, and Dozzle. To skip them, clear `COMPOSE_PROFILES` and set `OBSERVABILITY_ENABLED=false`.

| Service | Port |
|---------|------|
| Prometheus | 9090 |
| Jaeger | 16686 |
| Grafana | 3000 |
| Loki | 3100 |
| Dozzle | 8087 |

## Configuration

Docker settings are in `.env`. The ones you'll change first:

- `JWT_SECRET` — 32+ characters
- `POSTGRES_PASSWORD`
- `MEILISEARCH_MASTER_KEY`
- `REGISTRATION_ENABLED` — set `false` to lock sign-ups
- `CORS_ORIGINS` — frontend URL(s)

## Tests

```bash
./mvnw verify
cd frontend && npm run build
python3 -m pip install -r scripts/smoke/requirements.txt
python3 scripts/smoke/run.py   # needs the Docker stack up
```

Playwright E2E walks the UI in a real browser. Details in [frontend/e2e/README.md](./frontend/e2e/README.md).

```bash
cd frontend
npm install
npm run test:e2e:install   # once — downloads Chromium
npm run test:e2e
npm run test:e2e:headed    # visible window, slower pacing
npm run test:e2e:ui
```

From repo root: `node scripts/smoke/ui_audit.mjs` (add `--headed` for a visible browser).

Optional: `BASE_URL`, `SMOKE_PASSWORD`, `SMOKE_ADMIN_EMAIL`, `SMOKE_ADMIN_PASSWORD`. If smoke runs hit rate limits, set `RATE_LIMIT_ENABLED=false` in `.env`.

## Production

Use `SPRING_PROFILES_ACTIVE=prod`, strong secrets, HTTPS in front of the web container, and `MEILI_ENV=production` for Meilisearch. Compose already mounts `avatars-data`.
