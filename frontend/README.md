# WhatIRead Frontend

React SPA for [WhatIRead](../README.md) — reading tracker, shelves, social features, and messaging.

## Stack

- React 19 + TypeScript + Vite
- TanStack Query for server state
- STOMP over SockJS for realtime messaging
- Tailwind CSS

## Development

```bash
cd frontend
cp .env.example .env   # optional; dev proxy uses Vite defaults
npm install
npm run dev
```

The dev server proxies `/api` and `/ws` to the Spring Boot API (default `http://localhost:8080`). Override with `VITE_API_URL` in `.env` when the API runs elsewhere.

## Project structure

| Path | Purpose |
|------|---------|
| `src/api/` | REST client, paths, types |
| `src/auth/` | `AuthContext`, token storage, session refresh |
| `src/chat/` | `ChatProvider` (messages route only) |
| `src/components/` | UI, books, shelves, messages |
| `src/hooks/` | `useBookSuggest`, `useUsernameAvailability`, etc. |
| `src/lib/` | Constants, query cache helpers, messaging utils |
| `src/pages/` | Route-level screens (lazy-loaded) |

Query keys live in `src/lib/constants.ts` (`QUERY_KEYS`).

## Auth & realtime

- Access + refresh tokens in `localStorage` (see `src/auth/storage.ts`)
- `api/client.ts` refreshes on 401/403, proactive refresh every ~12 min while tab visible
- Cross-tab token sync via `storage` events
- WebSocket reconnects with fresh tokens on refresh (`ChatProvider`)

## Build

```bash
npm run build   # tsc + vite → dist/
```

Production images use `frontend/Dockerfile` (nginx serves static assets and proxies API).

## Performance notes

- Route-level code splitting (`React.lazy` in `App.tsx`)
- `ChatProvider` only mounts on `/messages`
- Narrow React Query cache updates in `lib/queryCache.ts`
- Messages list uses `staleTime: 30s` (no `refetchOnMount: 'always'`)
