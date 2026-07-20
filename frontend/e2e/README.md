# Browser E2E (Playwright)

End-to-end tests for the full app. The `product-demo.spec.ts` file is a serial walkthrough used for screen recordings and CI demo videos.

## Setup

Stack running (default http://localhost), Node 20+:

```bash
docker compose up --build -d
cd frontend
npm install
npm run test:e2e:install   # downloads Chromium, once
```

Backend must link authors when creating books — redeploy if you changed that recently.

For long local runs, `RATE_LIMIT_ENABLED=false` in `.env` helps.

## Run

```bash
cd frontend
npm run test:e2e           # headless
npm run test:e2e:headed    # visible browser, slower pacing
npm run test:e2e:ui        # Playwright UI mode
```

From repo root: `node scripts/smoke/ui_audit.mjs --headed`

After a run: `npx playwright show-report`

## Test data

Two seeded users drive the demo:

- **Priya Sharma** — main reader; *The God of Small Things*, shelf "Monsoon Reading List"
- **Arjun Mehta** — friend; *The White Tiger*, sends a recommendation

Fixtures: `helpers/personas.ts`. Registration, library, shelves, friendship: `helpers/seed.ts`.

## What `product-demo.spec.ts` covers

29 tests in order — login/register, sidebar tour, add book, library filters, book page and author profile, shelves, explore/clone, friends and chat, recommendations, settings, sign out. Tests share state (serial), so run the file, not individual cases.

## Env vars

| Variable | Default | Notes |
|----------|---------|-------|
| `BASE_URL` | `http://localhost` | App origin |
| `E2E_VISIBLE` | `1` with `test:e2e:headed` | Typing pauses, slowMo |
| `E2E_STEP_PAUSE_MS` | 3000 visible / 400 headless | After major steps |
| `E2E_ACTION_PAUSE_MS` | 800 visible / 0 headless | Around clicks |
| `E2E_TYPING_DELAY_MS` | 100 visible / 0 headless | Per character |
| `E2E_SLOW_MO_MS` | 400 visible / 0 headless | Global delay |
| `E2E_WINDOW_WIDTH` / `HEIGHT` | auto | Headed window size |

## Helpers

| File | What it does |
|------|----------------|
| `helpers/personas.ts` | User and book fixtures |
| `helpers/seed.ts` | Creates users and sample data via API |
| `helpers/navigation.ts` | Sidebar routes |
| `helpers/pages.ts` | Drawer, filters, modals |
| `helpers/interactions.ts` | `visibleClick`, `visibleType` for recordings |
| `helpers/fixtures.ts` | `visitAuthed`, `visitLoggedOut` |

## Full check before a release

```bash
./mvnw verify
python3 scripts/smoke/run.py
cd frontend && npm run test:e2e:headed
```
