# Browser E2E tests (Playwright)

Full **product demo** suite for GitHub screen recordings — Indian personas, meaningful journey names, visible typing, every major button and flow.

## Personas

| Character | Role | Sample data |
|-----------|------|-------------|
| **Priya Sharma** | Primary reader | *The God of Small Things* (Arundhati Roy), shelf **Monsoon Reading List** |
| **Arjun Mehta** | Friend | *The White Tiger* (Aravind Adiga), shelf **Hyderabad Picks**, sends a book recommendation |

Fixtures live in `helpers/personas.ts`; seed data is created in `helpers/seed.ts`.

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

**29 tests** in 12 chapters — serial run with Priya & Arjun seed data:

| Chapter | What it exercises |
|---------|-------------------|
| **01 Welcome & account access** | Login & register forms, links between them |
| **02 Priya tours the app** | Every sidebar link, profile → settings |
| **03 Priya’s home dashboard** | Add book (Open Library search), reading goal, streak, activity |
| **04 Priya’s library** | Status filters, search *God of Small*, sort, progress, drawer tabs |
| **05 Book page & Arundhati Roy** | Comment on *The God of Small Things*, author profile tabs |
| **06 Priya’s shelves** | Visibility filters, create **Chennai Weekend Reads**, book club wizard, **Monsoon Reading List** detail |
| **07 Explore & Activity** | Source filters, sort, clone dialog, activity filters |
| **08 Priya & Arjun** | Friend search, message, sent requests, Arjun’s profile |
| **09 Priya messages Arjun** | New chat, send message about *The White Tiger* |
| **10 Book recommendations** | Recommend modal, accept Arjun’s recommendation |
| **11 Notifications & settings** | Inbox, update writer bio |
| **12 Sign out** | Logout → login page |

## Recording for GitHub

1. Start the stack: `docker compose up -d`
2. Run headed demo: `cd frontend && npm run test:e2e:headed`
3. Use screen capture (QuickTime, OBS) while tests run — names appear in the terminal as chapter titles
4. Upload video to GitHub README or Releases; attach `playwright-report/` from CI for detailed traces

## Helpers

| File | Purpose |
|------|---------|
| `helpers/personas.ts` | Priya Sharma, Arjun Mehta, Indian book & shelf fixtures |
| `helpers/seed.ts` | Registers users, library entries, shelf, friendship, recommendation |
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
