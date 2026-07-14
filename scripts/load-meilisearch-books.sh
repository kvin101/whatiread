#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
HOST="${MEILISEARCH_HOST:-http://localhost:7700}"
API_KEY="${MEILISEARCH_API_KEY:-}"
INDEX="${MEILISEARCH_INDEX:-book-suggest}"
SEED="${ROOT}/data/seed/book-suggest.json"

if [[ ! -f "${SEED}" ]]; then
  echo "Seed file missing. Run: python3 scripts/prepare-book-suggest-seed.py" >&2
  exit 1
fi

auth_header=()
if [[ -n "${API_KEY}" ]]; then
  auth_header=(-H "Authorization: Bearer ${API_KEY}")
fi

echo "Ensuring Meilisearch index '${INDEX}' at ${HOST}..."
curl -sf "${auth_header[@]}" -X POST "${HOST}/indexes" \
  -H 'Content-Type: application/json' \
  --data-binary "{\"uid\":\"${INDEX}\",\"primaryKey\":\"id\"}" >/dev/null || true

curl -sf "${auth_header[@]}" -X PATCH "${HOST}/indexes/${INDEX}/settings" \
  -H 'Content-Type: application/json' \
  --data-binary '{"searchableAttributes":["title"],"rankingRules":["words","exactness","typo","proximity","attribute","sort"],"typoTolerance":{"enabled":true,"minWordSizeForTypos":{"oneTypo":3,"twoTypos":4}}}' >/dev/null

python3 - "${SEED}" "${HOST}" "${INDEX}" "${API_KEY}" <<'PY'
import json
import sys
import urllib.request

seed_path, host, index, api_key = sys.argv[1:5]
with open(seed_path, encoding="utf-8") as handle:
    records = json.load(handle)

for i, item in enumerate(records, start=1):
    item["id"] = str(i)

batch_size = 1000
headers = {"Content-Type": "application/json"}
if api_key:
    headers["Authorization"] = f"Bearer {api_key}"

for start in range(0, len(records), batch_size):
    chunk = records[start : start + batch_size]
    batch = [{"id": str(start + offset + 1), "title": row["title"]} for offset, row in enumerate(chunk)]
    payload = json.dumps(batch).encode("utf-8")
    request = urllib.request.Request(
        f"{host}/indexes/{index}/documents",
        data=payload,
        headers=headers,
        method="POST",
    )
    with urllib.request.urlopen(request) as response:
        task = json.load(response)
    print(f"Queued batch {start // batch_size + 1}: task {task.get('taskUid')}")

print(f"Loaded {len(records)} documents into {index}")
PY
