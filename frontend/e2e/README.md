# Browser E2E tests (Playwright)

Full **product demo** suite for GitHub screen recordings — meaningful test names, visible typing, every major button and flow.

## Prerequisites

1. App is up and reachable (default: http://localhost)
2. **API must include author linking on book create** (redeploy after backend changes)
3. Node 20+ in `frontend/`

```bash
docker compose up --build -d
cd frontend
npm install
npm run test:e2e:install        # downloads Chromium (once)
```

Tip: set `RATE_LIMIT_ENABLED=false` in `.env` for long local runs.

## Run tests

**Headless (CI / quick check):**

```bash
cd frontend
npm run test:e2e
```

**Headed (record for GitHub)** — full screen, visible typing, deliberate clicks, 3s pauses:

```bash
cd frontend
npm run test:e2e:headed
```

Videos are saved to `frontend/test-results/` on failure; in headed mode (`E2E_VISIBLE=1`) every run records video.

**From repo root:**

```bash
node scripts/smoke/ui_audit.mjs --headed
```

**HTML report (share on CI):**

```bash
cd frontend
npx playwright show-report
```

## Environment

| Variable | Default | Description |
|----------|---------|-------------|
| `BASE_URL` | `http://localhost` | Web app origin |
| `E2E_VISIBLE` | `1` when using `test:e2e:headed` | Visible typing, click pauses, slowMo |
| `E2E_STEP_PAUSE_MS` | `3000` (visible) / `400` (headless) | Pause after each major step |
| `E2E_ACTION_PAUSE_MS` | `800` (visible) / `0` (headless) | Pause before & after each click |
| `E2E_TYPING_DELAY_MS` | `100` (visible) / `0` (headless) | Per-character typing speed |
| `E2E_SLOW_MO_MS` | `400` (visible) / `0` (headless) | Global Playwright action delay |
| `E2E_WINDOW_WIDTH` / `E2E_WINDOW_HEIGHT` | auto-detected | Headed browser size |

## Product demo coverage (`e2e/product-demo.spec.ts`)

**29 tests** in 12 chapters — run serially with shared seed data:

| Chapter | What it exercises |
|---------|-------------------|
| **01 Authentication** | Login & register forms, links between them |
| **02 Navigation** | Every sidebar link, profile → settings |
| **03 Home** | Add book modal, reading goal, streak, activity link |
| **04 Library** | All status filters, search, sort, book drawer (details/notes/comments/rating/progress) |
| **05 Books & authors** | Post comment with timestamp, author profile tabs |
| **06 Shelves** | Visibility filters, system shelves, create shelf, book club wizard, detail tabs (Books/Updates/Sharing), edit/add books/members, discussion |
| **07 Explore & Activity** | Source filters, sort, clone dialog, activity filters |
| **08 Friends** | Search, message, sent requests, blocked list, friend profile |
| **09 Messages** | New chat, send message |
| **10 Recommendations** | Recommend modal, accept friend recommendation |
| **11 Notifications & Settings** | Inbox, save profile bio |
| **12 Sign out** | Logout → login page |

## Recording for GitHub

1. Start the stack: `docker compose up -d`
2. Run headed demo: `cd frontend && npm run test:e2e:headed`
3. Use screen capture (QuickTime, OBS) while tests run — names appear in the terminal as chapter titles
4. Upload video to GitHub README or Releases; attach `playwright-report/` from CI for detailed traces

## Helpers

| File | Purpose |
|------|---------|
| `helpers/seed.ts` | Creates user, book, shelf, friend, incoming recommendation |
| `helpers/navigation.ts` | Sidebar tour |
| `helpers/pages.ts` | Book drawer, filter chips, modals |
| `helpers/interactions.ts` | `visibleClick`, `visibleType` for recordings |
| `helpers/fixtures.ts` | `visitAuthed`, `visitLoggedOut` |

## Full test stack

```bash
./mvnw verify                              # Java unit + integration
python3 scripts/smoke/run.py               # API smoke
cd frontend && npm run test:e2e:headed     # Browser product demo
```
