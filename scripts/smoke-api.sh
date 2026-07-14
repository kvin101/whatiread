#!/usr/bin/env bash
set -euo pipefail

BASE="${BASE_URL:-http://localhost}"
API="$BASE/api/v1"
PASS="${SMOKE_PASSWORD:-TestPass123!}"
TS=$(date +%s)
EMAIL="smoke-$TS@example.com"
USERNAME="smoke$TS"
FAIL=0
PASS_COUNT=0

check() {
  local name="$1" expected="$2" actual="$3"
  if [[ "$actual" == "$expected" ]]; then
    echo "PASS: $name (HTTP $actual)"
    PASS_COUNT=$((PASS_COUNT + 1))
  else
    echo "FAIL: $name (expected HTTP $expected, got $actual)"
    FAIL=$((FAIL + 1))
  fi
}

body_status() {
  curl -s -o /tmp/smoke_body -w "%{http_code}" "$@"
}

# 1. Status
check "status" "200" "$(body_status "$API/status")"

# 2. Register
REG_CODE=$(curl -s -o /tmp/smoke_reg.json -w "%{http_code}" -X POST "$API/auth/register" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$EMAIL\",\"username\":\"$USERNAME\",\"password\":\"$PASS\",\"firstName\":\"Smoke\",\"lastName\":\"Test\"}")
check "register" "201" "$REG_CODE"
ACCESS=$(python3 -c "import json; print(json.load(open('/tmp/smoke_reg.json'))['accessToken'])")
REFRESH=$(python3 -c "import json; print(json.load(open('/tmp/smoke_reg.json'))['refreshToken'])")
USER_ID=$(python3 -c "import json; print(json.load(open('/tmp/smoke_reg.json'))['user']['id'])")

AUTH=(-H "Authorization: Bearer $ACCESS")

# 3. Login
check "login" "200" "$(body_status -X POST "$API/auth/login" -H "Content-Type: application/json" \
  -d "{\"email\":\"$EMAIL\",\"password\":\"$PASS\"}")"

# 4. Me
check "me" "200" "$(body_status "${AUTH[@]}" "$API/me")"

# 5. Username availability
check "username-available" "200" "$(body_status "${AUTH[@]}" "$API/me/username/available?username=newuser$TS")"

# 6. Profile update
check "profile-update" "200" "$(body_status -X PATCH "${AUTH[@]}" "$API/me" \
  -H "Content-Type: application/json" \
  -d '{"firstName":"Smoke","lastName":"Runner"}')"

# 7. Avatar upload (1x1 PNG)
printf '\x89PNG\r\n\x1a\n\x00\x00\x00\rIHDR\x00\x00\x00\x01\x00\x00\x00\x01\x08\x02\x00\x00\x00\x90wS\xde\x00\x00\x00\x0cIDATx\x9cc\xf8\x0f\x00\x00\x01\x01\x00\x05\x18\xd8N\x00\x00\x00\x00IEND\xaeB`\x82' > /tmp/smoke_avatar.png
AV_CODE=$(curl -s -o /tmp/smoke_avatar_resp.json -w "%{http_code}" -X POST "${AUTH[@]}" \
  -F "file=@/tmp/smoke_avatar.png;type=image/png" "$API/me/avatar")
check "avatar-upload" "200" "$AV_CODE"
AVATAR_URL=$(python3 -c "import json; print(json.load(open('/tmp/smoke_avatar_resp.json')).get('avatarUrl',''))" 2>/dev/null || echo "")
if [[ -n "$AVATAR_URL" ]]; then
  check "avatar-get" "200" "$(body_status "$BASE$AVATAR_URL")"
else
  echo "FAIL: avatar-get (no avatarUrl in response)"
  FAIL=$((FAIL + 1))
fi

# 8. Token refresh
REF_CODE=$(curl -s -o /tmp/smoke_refresh.json -w "%{http_code}" -X POST "$API/auth/refresh" \
  -H "Content-Type: application/json" \
  -d "{\"refreshToken\":\"$REFRESH\"}")
check "token-refresh" "200" "$REF_CODE"
ACCESS2=$(python3 -c "import json; print(json.load(open('/tmp/smoke_refresh.json'))['accessToken'])")
AUTH2=(-H "Authorization: Bearer $ACCESS2")

# 9. Book search
check "books-search" "200" "$(body_status "${AUTH2[@]}" "$API/books/search?q=harry")"

# 10. Book suggest
check "books-suggest" "200" "$(body_status "${AUTH2[@]}" "$API/books/suggest?q=har")"

# 11. Create book
BOOK_CODE=$(curl -s -o /tmp/smoke_book.json -w "%{http_code}" -X POST "${AUTH2[@]}" "$API/books" \
  -H "Content-Type: application/json" \
  -d '{"title":"Smoke Book","authors":["Smoke Author"],"pageCount":100}')
check "create-book" "201" "$BOOK_CODE"
BOOK_ID=$(python3 -c "import json; print(json.load(open('/tmp/smoke_book.json'))['id'])")

# 12. Library add
check "library-add" "201" "$(body_status -X POST "${AUTH2[@]}" "$API/library" \
  -H "Content-Type: application/json" \
  -d "{\"bookId\":\"$BOOK_ID\"}")"

# 13. Library list
check "library-list" "200" "$(body_status "${AUTH2[@]}" "$API/library")"

# 14. Create shelf
SHELF_CODE=$(curl -s -o /tmp/smoke_shelf.json -w "%{http_code}" -X POST "${AUTH2[@]}" "$API/shelves" \
  -H "Content-Type: application/json" \
  -d '{"name":"Smoke Shelf","visibility":"PRIVATE"}')
check "create-shelf" "201" "$SHELF_CODE"
SHELF_ID=$(python3 -c "import json; print(json.load(open('/tmp/smoke_shelf.json'))['id'])")

# 15. Add book to shelf
check "shelf-add-book" "201" "$(body_status -X POST "${AUTH2[@]}" "$API/shelves/$SHELF_ID/books" \
  -H "Content-Type: application/json" \
  -d "{\"bookId\":\"$BOOK_ID\"}")"

# 16. Shelves list
check "shelves-list" "200" "$(body_status "${AUTH2[@]}" "$API/shelves")"

# 17. Explore feed
check "explore-feed" "200" "$(body_status "${AUTH2[@]}" "$API/shelves/explore")"

# 18. Second user for friends/messaging
EMAIL2="smoke2-$TS@example.com"
USERNAME2="smoke2$TS"
curl -s -o /tmp/smoke_reg2.json -X POST "$API/auth/register" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$EMAIL2\",\"username\":\"$USERNAME2\",\"password\":\"$PASS\",\"firstName\":\"Smoke2\",\"lastName\":\"Test\"}" >/dev/null
ACCESS_B=$(python3 -c "import json; print(json.load(open('/tmp/smoke_reg2.json'))['accessToken'])")
USER2_ID=$(python3 -c "import json; print(json.load(open('/tmp/smoke_reg2.json'))['user']['id'])")
AUTH_B=(-H "Authorization: Bearer $ACCESS_B")

# 19. Friend request
REQ_CODE=$(curl -s -o /tmp/smoke_friend.json -w "%{http_code}" -X POST "${AUTH2[@]}" "$API/friends/requests" \
  -H "Content-Type: application/json" \
  -d "{\"userId\":\"$USER2_ID\"}")
check "friend-request" "201" "$REQ_CODE"
REQ_ID=$(python3 -c "import json; d=json.load(open('/tmp/smoke_friend.json')); print(d.get('id',''))" 2>/dev/null || echo "")

# 20. Accept friend request
if [[ -n "$REQ_ID" ]]; then
  check "friend-accept" "200" "$(body_status -X POST "${AUTH_B[@]}" "$API/friends/requests/$REQ_ID/accept")"
else
  echo "FAIL: friend-accept (no request id)"
  FAIL=$((FAIL + 1))
fi

# 21. Friends list
check "friends-list" "200" "$(body_status "${AUTH2[@]}" "$API/friends")"

# 22. Create DM conversation
CONV_CODE=$(curl -s -o /tmp/smoke_conv.json -w "%{http_code}" -X POST "${AUTH2[@]}" "$API/conversations/with/$USER2_ID")
check "create-conversation" "200" "$CONV_CODE"
CONV_ID=$(python3 -c "import json; print(json.load(open('/tmp/smoke_conv.json'))['id'])")

# 23. Messaging (send is WebSocket-only; verify read endpoints)
check "unread-count" "200" "$(body_status "${AUTH_B[@]}" "$API/conversations/unread-count")"
check "mark-read" "204" "$(body_status -X POST "${AUTH2[@]}" "$API/conversations/$CONV_ID/read")"

# 24. List conversations
check "conversations-list" "200" "$(body_status "${AUTH2[@]}" "$API/conversations")"

# 25. List messages
check "messages-list" "200" "$(body_status "${AUTH2[@]}" "$API/conversations/$CONV_ID/messages")"

# 26. Recommendation send
check "recommendation-send" "201" "$(body_status -X POST "${AUTH2[@]}" "$API/recommendations" \
  -H "Content-Type: application/json" \
  -d "{\"toUserId\":\"$USER2_ID\",\"bookId\":\"$BOOK_ID\",\"message\":\"Try this\"}")"

# 27. Recommendations inbox
check "recommendations-inbox" "200" "$(body_status "${AUTH_B[@]}" "$API/recommendations/inbox")"

# 28. Avatar remove
check "avatar-remove" "204" "$(body_status -X DELETE "${AUTH2[@]}" "$API/me/avatar")"

# 29. Unauthenticated returns 401
check "unauth-me" "401" "$(body_status "$API/me")"

echo ""
echo "=== Smoke test summary: $PASS_COUNT passed, $FAIL failed ==="
exit "$FAIL"
