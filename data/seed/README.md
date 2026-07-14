# Book suggest seed

Static autocomplete data for Meilisearch (not stored in Postgres).

## Files

| File | Purpose |
|------|---------|
| `books-clean.csv` | **Canonical seed** — `title` only (English, deduplicated) |
| `book-suggest.json` | Same data as JSON for Meilisearch import |
| `books_1.Best_Books_Ever.csv` | Raw source dump (optional, local only) |

## Regenerate

```bash
python3 scripts/prepare-book-suggest-seed.py
cp data/seed/book-suggest.json src/main/resources/book-suggest/book-suggest.json
docker volume rm whatiread_meilisearch_data
docker compose up --build -d
```
