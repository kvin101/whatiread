WhatIRead
=========

Minimal FOSS virtual bookshelf — self-hosted on your own hardware. Track reading, organize shelves, connect with friends.

**License:** [AGPL-3.0](./LICENSE)

## Features (planned)

- Personal reading tracker (To Read / Reading / Read / DNF)
- Custom shelves and tags
- Visual 2D bookshelf UI
- Goodreads CSV import / data export
- No telemetry, no ads, no algorithmic feed

See [documents/features.md](./documents/features.md), [priorities.md](./documents/priorities.md), and [plan.md](./documents/plan.md).

## Quick start (local dev)

Requirements: Java 21, Maven (or `./mvnw`).

```bash
./mvnw spring-boot:run -Pdev -Dspring-boot.run.profiles=dev
```

- API: http://localhost:8080/api/v1/status
- Health: http://localhost:8080/actuator/health
- OpenAPI: http://localhost:8080/swagger-ui.html
- H2 console (dev only): http://localhost:8080/h2-console

## Quick start (Docker / self-host)

```bash
cp .env.example .env
# Edit .env — set POSTGRES_PASSWORD and JWT_SECRET (min 32 chars)

docker compose up --build
```

### Rebuild Docker images

From the repo root (after editing `.env` if needed):

```bash
# Rebuild API + web and restart the stack (db image is pulled, not built)
docker compose up --build -d

# Force a clean rebuild (no layer cache)
docker compose build --no-cache
docker compose up -d

# Rebuild one service only
docker compose build api
docker compose build web
```

Build images without starting containers:

```bash
docker compose build
```

Rebuild with plain `docker build` (same contexts as compose):

```bash
docker build -t whatiread-api:latest -f Dockerfile .
docker build -t whatiread-web:latest -f frontend/Dockerfile ./frontend
```

Optional: set `IMAGE_TAG` in `.env` (e.g. `IMAGE_TAG=2026-06-14`); compose tags images as `whatiread-api:${IMAGE_TAG}` and `whatiread-web:${IMAGE_TAG}`.

## Configuration

| Variable | Default | Description |
|----------|---------|-------------|
| `DATABASE_URL` | `jdbc:postgresql://localhost:5432/whatiread` | JDBC URL |
| `DATABASE_USERNAME` | `whatiread` | DB user |
| `DATABASE_PASSWORD` | `whatiread` (local only) | DB password — **required** in `prod` (no default) |
| `JWT_SECRET` | *(required)* | HMAC secret, min 32 characters |
| `REGISTRATION_ENABLED` | `true` (local), `false` (Docker prod) | Allow new user registration |
| `CORS_ORIGINS` | `http://localhost:5173` | Comma-separated SPA origins |
| `GOOGLE_BOOKS_API_KEY` | *(empty)* | Optional metadata fallback |
| `RATE_LIMIT_ENABLED` | `true` | Enable tiered per-IP rate limiting (Resilience4j) |
| `OTEL_TAIL_SAMPLING_NORMAL_PERCENT` | `5` | Collector fallback sample rate for non-critical traces (%) |
| `OTEL_TAIL_SAMPLING_CRITICAL_PERCENT` | `50` | Collector sample rate for auth/messaging/import/export (%) |
| `OTEL_TAIL_SAMPLING_SLOW_MS` | `2000` | Collector keeps traces slower than this (ms) |
| `OTEL_TAIL_SAMPLING_DECISION_WAIT` | `10s` | Collector tail-sampling decision window |

### Trace sampling

The API exports **all spans** (`management.tracing.sampling.probability` defaults to `1.0` in every profile). Sampling happens in the **OTel collector** via tail-based policies: always keep errors, HTTP 4xx/5xx, and slow traces; higher rates for auth/messaging/import/export; drop actuator paths; probabilistic fallback for everything else.

| Layer | Behavior | Config |
|-------|----------|--------|
| API (local + prod) | Export 100% of spans | `application.yaml` / `application-prod.yaml` |
| OTel collector | Tail sampling before Jaeger | `docker/otel/otel-collector-config.yml` + `.env` |

Tune collector rates with `OTEL_TAIL_SAMPLING_*` in `.env`. Do **not** set `OTEL_TRACES_SAMPLER` / `OTEL_TRACES_SAMPLER_ARG` on the API.

Jaeger UI (Docker stack): http://localhost:16686

### Rate limiting

Per-IP tiered limits use [Resilience4j](https://resilience4j.readme.io/docs/ratelimiter) (`RateLimitFilter` + `resilience4j.ratelimiter.configs.*` in `application.yaml`). Toggle with `RATE_LIMIT_ENABLED` / `whatiread.rate-limit.enabled`.

| Tier | Paths | Default (dev) | Prod |
|------|-------|---------------|------|
| `strict-auth` | `/api/v1/auth/**`, `/api/v1/setup/**` | 15/min | 10/min |
| `strict-import` | `/api/v1/import/**` | 8/min | 5/min |
| `search` | `GET /api/v1/books/search` | 45/min | 30/min |
| `write` | POST/PUT/PATCH/DELETE on `/api/v1/**` | 90/min | 60/min |
| `read` | GET/HEAD on `/api/v1/**` | 250/min | 200/min |
| `default` | other `/api/v1/**` | 120/min | 100/min |

Excluded: `/actuator/**`, `/ws/**`, static assets. Metrics: `resilience4j.ratelimiter.*` on `/actuator/prometheus`.

For a typical self-hosted instance (~10–50 concurrent users), the defaults above are sufficient. Busier deploys: raise `read`/`write` in `application-prod.yaml` or add profile-specific overrides; lower `strict-auth` if you see credential-stuffing. Limits are per API instance (in-memory); multi-instance would need a shared store (not included).

## Production checklist

Before exposing a self-hosted instance to the internet:

1. **Secrets** — Set strong `POSTGRES_PASSWORD`, `JWT_SECRET` (≥32 chars), and `GRAFANA_ADMIN_PASSWORD` in `.env`.
2. **Registration** — Keep `REGISTRATION_ENABLED=false` unless you want public sign-ups.
3. **TLS** — Terminate HTTPS at a reverse proxy (Caddy, Traefik, nginx + certbot). HSTS is sent when the API sees HTTPS (`X-Forwarded-Proto`).
4. **Network** — Only publish the `web` port (`HTTP_PORT`). Observability stacks bind to `127.0.0.1` (Grafana `:3000`, Prometheus `:9090`, etc.).
5. **Grafana** — Anonymous admin is **off** by default. Use `GRAFANA_ANONYMOUS=true` only on trusted local networks.
6. **CORS** — Set `CORS_ORIGINS` to your real SPA origin(s).
7. **Auth tokens** — Refresh tokens live in `localStorage` (XSS risk). CSP headers are set on API and nginx; HttpOnly cookies would be a future hardening step.
8. **Metrics** — `/actuator/prometheus` is restricted to private-network callers; Prometheus scrapes `api:8080` inside the Docker network.
9. **Tracing** — API exports all spans; tune collector tail sampling with `OTEL_TAIL_SAMPLING_*` in `.env`.

## Development

```bash
./mvnw verify          # Run tests
./mvnw spring-boot:run -Pdev -Dspring-boot.run.profiles=dev
```

### Project layout

```
src/main/java/com/whatiread/
├── config/          Security, CORS, OpenAPI, properties
├── domain/          JPA entities (Phase 1+)
├── repository/      Spring Data repositories
├── service/         Business logic
├── web/             REST controllers
├── integration/     OpenFeign clients
├── importexport/    CSV import/export
└── exception/       RFC 7807 error handling
```

## Contributing

Issues and pull requests welcome. By contributing, you agree to license your work under AGPL-3.0.

## Third-party notices

See [NOTICE](./NOTICE).
