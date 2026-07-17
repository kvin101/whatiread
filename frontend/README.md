# WhatIRead Frontend

React SPA for [WhatIRead](../README.md).

## Stack

- React 19 + TypeScript + Vite
- TanStack Query
- STOMP over SockJS for messaging
- Tailwind CSS

## Development

```bash
npm install
npm run dev
```

Proxies `/api` and `/ws` to `http://localhost:8080`. Set `VITE_API_URL` in `frontend/.env` if the API is elsewhere.

## Build

```bash
npm run build
```

Production image: `frontend/Dockerfile` (nginx).

## Browser E2E tests

Playwright tests open Chromium and walk every app route against a running stack. Details: [e2e/README.md](./e2e/README.md).

```bash
npm run test:e2e:install   # once
npm run test:e2e           # headless
npm run test:e2e:headed    # visible browser
npm run test:e2e:ui        # interactive mode
```
