# WhatIRead Frontend

React SPA. See the [root README](../README.md) for running the full stack.

## Stack

React 19, TypeScript, Vite, TanStack Query, STOMP/SockJS for chat, Tailwind.

## Dev

```bash
npm install
npm run dev
```

`/api` and `/ws` proxy to `http://localhost:8080`. Point elsewhere with `VITE_API_URL` in `frontend/.env`.

## Build

```bash
npm run build
```

Production image: `frontend/Dockerfile` (nginx).

## E2E

Playwright tests need the app running. See [e2e/README.md](./e2e/README.md).

```bash
npm run test:e2e:install   # once
npm run test:e2e
npm run test:e2e:headed
npm run test:e2e:ui
```
