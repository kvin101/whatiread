WhatIRead
=========

Self-hosted reading tracker. Track books, organize shelves, connect with friends.

**License:** [AGPL-3.0](./LICENSE) + [NON-AI-LICENSE](./licenses/NON-AI-LICENSE) ([process](./licenses/README.md))

- **Copyleft (AGPL-3.0):** Run, modify, or offer this software over a network → share corresponding source under AGPL-3.0.
- **No AI/ML training:** [NON-AI-LICENSE](./licenses/NON-AI-LICENSE) (from [non-ai-licenses](https://github.com/non-ai-licenses/non-ai-licenses)) — copy the license **into your repo**; do not link from GitHub at runtime.
- **Crawlers:** [robots.txt](./frontend/public/robots.txt) from [ai.robots.txt](https://github.com/ai-robots-txt/ai.robots.txt) + nginx hard block.

## Run locally (no Docker)

```bash
./mvnw spring-boot:run -Pdev -Dspring-boot.run.profiles=dev
cd frontend && npm install && npm run dev
```

- API: http://localhost:8080/api/v1/status
- Frontend: http://localhost:5173

## Run with Docker

Edit `.env` (example placeholders — set real passwords and secrets), then:

```bash
docker compose up --build -d
```

Open http://localhost (or `HTTP_PORT` from `.env`).

Pull pre-built images instead of building — set `API_IMAGE` and `WEB_IMAGE` in `.env`.

| Registry | API | Web |
|----------|-----|-----|
| Docker Hub | `vinayk101/whatiread-api:latest` | `vinayk101/whatiread-web:latest` |
| GitHub Packages | `ghcr.io/kvin101/whatiread-api:latest` | `ghcr.io/kvin101/whatiread-web:latest` |

Releases and image digests for each build: [GitHub Releases](https://github.com/kvin101/whatiread/releases).

### Observability

On by default (see `.env`). To disable, clear `COMPOSE_PROFILES` and set `OBSERVABILITY_ENABLED=false`.

Services: Prometheus `:9090`, Jaeger `:16686`, Grafana `:3000`, Loki `:3100`, Dozzle `:8087`.

## Configuration

See `.env.example` for Docker variables. Key API settings:

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
python3 scripts/smoke/run.py   # API smoke (live Docker stack)
```

### Browser E2E (Playwright)

Opens a real Chromium window, seeds a user, and visits every UI route. See [frontend/e2e/README.md](./frontend/e2e/README.md).

```bash
cd frontend
npm install
npm run test:e2e:install   # once — downloads Chromium
npm run test:e2e           # headless
npm run test:e2e:headed    # visible browser, 16″ viewport, slow pacing
npm run test:e2e:ui        # interactive debugger
```

From repo root: `node scripts/smoke/ui_audit.mjs` (or `--headed`).

Optional env: `BASE_URL`, `SMOKE_PASSWORD`, `SMOKE_ADMIN_EMAIL`, `SMOKE_ADMIN_PASSWORD`.

Tip: set `RATE_LIMIT_ENABLED=false` in `.env` for faster full smoke runs.

## Production notes

- Set `SPRING_PROFILES_ACTIVE=prod` in `.env` for production deployments
- Set strong secrets in `.env`
- Put HTTPS in front of the web container
- Set `MEILI_ENV=production` for Meilisearch
- Mount `avatars-data` volume (included in compose)
