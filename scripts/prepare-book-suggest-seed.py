#!/usr/bin/env python3
"""Build a cleaned title-only book suggest seed from Goodreads BBE CSV."""

from __future__ import annotations

import csv
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SOURCE_CSV = ROOT / "data" / "seed" / "books_1.Best_Books_Ever.csv"
OUT_JSON = ROOT / "data" / "seed" / "book-suggest.json"
OUT_CSV = ROOT / "data" / "seed" / "books-clean.csv"

MAX_TITLE_LEN = 180


def normalize_title(title: str) -> str:
    return re.sub(r"\s+", " ", title.strip().lower())


def clean_title(title: str) -> str:
    value = re.sub(r"\s+", " ", (title or "").strip())
    if len(value) > MAX_TITLE_LEN:
        value = value[:MAX_TITLE_LEN].rstrip()
    return value


def is_english(row: dict[str, str]) -> bool:
    language = (row.get("language") or "").strip().lower()
    return language in {"", "english", "en", "eng"}


def popularity(row: dict[str, str]) -> int:
    try:
        return int(float(row.get("numRatings") or 0))
    except ValueError:
        return 0


def main() -> int:
    if not SOURCE_CSV.exists():
        print(f"Missing source CSV: {SOURCE_CSV}", file=sys.stderr)
        return 1

    best_by_title: dict[str, tuple[str, int]] = {}
    skipped = 0
    with SOURCE_CSV.open(newline="", encoding="utf-8") as handle:
        for row in csv.DictReader(handle):
            if not is_english(row):
                skipped += 1
                continue
            title = clean_title(row.get("title", ""))
            if len(title) < 2:
                skipped += 1
                continue
            key = normalize_title(title)
            score = popularity(row)
            existing = best_by_title.get(key)
            if existing is None or score > existing[1]:
                best_by_title[key] = (title, score)

    records = [{"title": title} for title, _ in sorted(best_by_title.values(), key=lambda item: item[0].lower())]

    OUT_JSON.parent.mkdir(parents=True, exist_ok=True)
    with OUT_JSON.open("w", encoding="utf-8") as handle:
        json.dump(records, handle, ensure_ascii=False, separators=(",", ":"))

    with OUT_CSV.open("w", newline="", encoding="utf-8") as handle:
        writer = csv.DictWriter(handle, fieldnames=["title"])
        writer.writeheader()
        writer.writerows(records)

    print(f"Wrote {len(records)} title-only books")
    print(f"  JSON: {OUT_JSON}")
    print(f"  CSV:  {OUT_CSV}")
    print(f"Skipped {skipped} non-English or invalid rows")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
