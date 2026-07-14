WhatIRead
=========

Minimal FOSS virtual bookshelf — self-hosted on your own hardware. Track reading, organize shelves, connect with friends.

**License:** [AGPL-3.0](./LICENSE)

## Implemented features

- Personal reading tracker (To Read / Reading / Read / DNF) with ratings, notes, and progress
- Custom shelves (private, friends-only, public) with sharing and explore feed
- Friends, friend requests, and direct + group messaging (STOMP/WebSocket)
- Book and user recommendations with accept/dismiss flows
- Comments on shelves and library entries
- Book search (Google Books) and **autocomplete** (Meilisearch book + user suggest)
- Book preview popover (Open Library)
- Usernames with bloom-filter availability checks
- Profile avatars (upload, serve, persistent volume in Docker)
- Instance setup wizard and admin user management
- Tiered API rate limiting and observability stack (optional Docker profile)

**Not shipped:** Goodreads CSV import, data export, and reading goals (removed from codebase).

## Quick start (local dev)

Requirements: Java 21, Maven (or `./mvnw`).

```bash
./mvnw spring-boot:run -Pdev -Dspring-boot.run.profiles=dev
```

- API: http://localhost:8080/api/v1/status
- Health: http://localhost:8080/actuator/health
- OpenAPI: http://localhost:8080/swagger-ui.html
- H2 console (dev only): http://localhost:8080/h2-console

Frontend (separate terminal):

```bash
cd frontend && npm install && npm run dev
```

See [frontend/README.md](./frontend/README.md).

## Quick start (Docker / self-host)

```bash
cp .env.example .env
# Edit .env — set POSTGRES_PASSWORD, JWT_SECRET (min 32 chars), MEILISEARCH_MASTER_KEY

docker compose up --build
```

Meilisearch is included in `docker-compose.yml` for book/user suggest. Production stacks (`docker-prod-compose.yml`, `docker-selfhost-compose.yml`) also include Meilisearch and a persistent `avatars-data` volume.

### Rebuild Docker images

```bash
docker compose up --build -d
docker compose build --no-cache   # force clean rebuild
```

## Configuration

| Variable | Default | Description |
|----------|---------|-------------|
| `DATABASE_URL` | `jdbc:postgresql://localhost:5432/whatiread` | JDBC URL |
| `DATABASE_USERNAME` | `whatiread` | DB user |
| `DATABASE_PASSWORD` | `whatiread` (local only) | DB password — **required** in `prod` |
| `JWT_SECRET` | *(required)* | HMAC secret, min 32 characters |
| `REGISTRATION_ENABLED` | `true` (local), `false` (Docker prod) | Allow new user registration |
| `CORS_ORIGINS` | `http://localhost:5173` | Comma-separated SPA origins |
| `GOOGLE_BOOKS_API_KEY` | *(empty)* | Optional metadata fallback |
| `MEILISEARCH_ENABLED` | `true` | Enable suggest indexes |
| `MEILISEARCH_HOST` | `http://meilisearch:7700` | Meilisearch URL (compose) |
| `MEILISEARCH_MASTER_KEY` | *(required in prod)* | Meilisearch master key |
| `WHATIREAD_AVATARS_DIRECTORY` | `/data/avatars` | Avatar file storage path |
| `RATE_LIMIT_ENABLED` | `true` | Enable tiered per-IP rate limiting |

### Trace sampling

The API exports **all spans** (`management.tracing.sampling.probability` defaults to `1.0`). Sampling happens in the **OTel collector** via tail-based policies: always keep errors, HTTP 4xx/5xx, and slow traces; higher rates for auth/messaging; drop actuator paths.

| Layer | Behavior | Config |
|-------|----------|--------|
| API (local + prod) | Export 100% of spans | `application.yaml` / `application-prod.yaml` |
| OTel collector | Tail sampling before Jaeger | `docker/otel/otel-collector-config.yml` + `.env` |

Tune collector rates with `OTEL_TAIL_SAMPLING_*` in `.env`.

### Rate limiting

Per-IP tiered limits use [Resilience4j](https://resilience4j.readme.io/docs/ratelimiter).

| Tier | Paths | Default (dev) | Prod |
|------|-------|---------------|------|
| `strict-auth` | `/api/v1/auth/**`, `/api/v1/setup/**` | 15/min | 10/min |
| `search` | `GET /api/v1/books/search`, suggest endpoints | 45/min | 30/min |
| `write` | POST/PUT/PATCH/DELETE on `/api/v1/**` | 90/min | 60/min |
| `read` | GET/HEAD on `/api/v1/**` | 250/min | 200/min |
| `default` | other `/api/v1/**` | 120/min | 100/min |

Excluded: `/actuator/**`, `/ws/**`, static assets.

### Performance & caching

Backend optimizations include:

- Batch shelf book loading (`listByIds` + `@EntityGraph`) instead of full-library fetches
- Batched messaging queries (mentions, conversation summaries)
- JWT auth principal cache (2-min TTL, evicted on logout/password change)
- Tiered Caffeine caches: friend IDs, shelf book counts, books-by-id, public shelves
- Explore feed batch membership lookups

## Production checklist

1. **Secrets** — Strong `POSTGRES_PASSWORD`, `JWT_SECRET` (≥32 chars), `MEILISEARCH_MASTER_KEY`, `GRAFANA_ADMIN_PASSWORD`
2. **Registration** — Keep `REGISTRATION_ENABLED=false` unless you want public sign-ups
3. **TLS** — Terminate HTTPS at a reverse proxy
4. **CORS** — Set `CORS_ORIGINS` to your SPA origin(s)
5. **Avatars** — Ensure `avatars-data` volume is mounted (all compose files)
6. **Meilisearch** — Required for autocomplete in production

## Development

```bash
./mvnw verify          # Tests + JaCoCo (H2)
./mvnw spring-boot:run -Pdev -Dspring-boot.run.profiles=dev
cd frontend && npm run build
```

### Project layout

```
src/main/java/com/whatiread/
├── identity/        Users, auth, avatars, usernames
├── library/         Personal book collection
├── shelf/           Shelves, sharing, explore
├── catalog/         Books, search, suggest, previews
├── messaging/       Conversations, WebSocket
├── social/          Friends, blocks
├── recommendation/  Book/shelf recommendations
├── comment/         Threaded comments
├── instance/        Setup, admin settings
├── config/          Security, cache, rate limits
└── shared/          API paths, cursor utils, events
```

## Contributing

Issues and pull requests welcome. By contributing, you agree to license your work under AGPL-3.0.

## Third-party notices

See [NOTICE](./NOTICE).
