from __future__ import annotations

from typing import TYPE_CHECKING

if TYPE_CHECKING:
    from context import SmokeContext
    from runner import Runner


def run_features(ctx: SmokeContext, r: Runner) -> None:
    """End-to-end API checks for every major product surface."""
    r.section("Feature matrix — library & catalog")
    u1 = ctx.users["u1"]
    s = ctx.state

    cursor = ctx.client.get("/library", token=u1.access_token, params={"cursor": " ", "limit": 24, "sort": "UPDATED_DESC"})
    r.check_status("library-cursor-first-page", 200, cursor.status)
    r.check_bool("library-cursor-has-items-key", True, isinstance(cursor.json, dict) and "items" in cursor.json)

    r.check_status("library-page-mode", 200, ctx.client.get("/library", token=u1.access_token, params={"page": 0, "size": 10}).status)
    r.check_status("library-sort-title", 200, ctx.client.get("/library", token=u1.access_token, params={"sort": "TITLE_ASC", "page": 0, "size": 10}).status)
    r.check_status("library-filter-reading", 200, ctx.client.get("/library", token=u1.access_token, params={"status": "READING", "page": 0, "size": 10}).status)

    if s.book_id:
        r.check_status("library-by-book", 200, ctx.client.get(f"/library/by-book/{s.book_id}", token=u1.access_token).status)
    if s.user_book_id:
        r.check_status("library-entry-get", 200, ctx.client.get(f"/library/{s.user_book_id}", token=u1.access_token).status)

    r.check_status("books-search", 200, ctx.client.get("/books/search", params={"q": "smoke"}).status)
    r.check_status("books-suggest", 200, ctx.client.get("/books/suggest", params={"q": "sm"}).status)

    r.section("Feature matrix — home data (reading & social)")
    r.check_status("me-profile", 200, ctx.client.get("/me", token=u1.access_token).status)
    r.check_status("reading-goal", 200, ctx.client.get("/me/reading-goal", token=u1.access_token, params={"year": "2026"}).status)
    r.check_status("reading-stats", 200, ctx.client.get("/me/stats", token=u1.access_token, params={"year": "2026"}).status)
    r.check_status("reading-streak", 200, ctx.client.get("/me/streak", token=u1.access_token).status)
    r.check_status("activity-feed", 200, ctx.client.get("/activity", token=u1.access_token).status)
    r.check_status("notifications", 200, ctx.client.get("/notifications", token=u1.access_token).status)

    r.section("Feature matrix — shelves & discovery")
    r.check_status("shelves-list", 200, ctx.client.get("/shelves", token=u1.access_token).status)
    r.check_status("shelves-system", 200, ctx.client.get("/shelves/system", token=u1.access_token).status)
    r.check_status("shelves-system-books", 200, ctx.client.get("/shelves/system/TO_READ/books", token=u1.access_token, params={"page": 0, "size": 5}).status)
    r.check_status("shelves-explore", 200, ctx.client.get("/shelves/explore", token=u1.access_token, params={"page": 0, "size": 5}).status)

    if s.shelf_id:
        r.check_status("shelf-detail", 200, ctx.client.get(f"/shelves/{s.shelf_id}", token=u1.access_token).status)
        r.check_status("shelf-books", 200, ctx.client.get(f"/shelves/{s.shelf_id}/books", token=u1.access_token).status)
        r.check_status("shelf-events", 200, ctx.client.get(f"/shelves/{s.shelf_id}/events", token=u1.access_token, params={"page": 0, "size": 5}).status)
        r.check_status("shelf-reading-overlap", 200, ctx.client.get(f"/shelves/{s.shelf_id}/reading-overlap", token=u1.access_token).status)

    if s.public_shelf_slug:
        r.check_status(
            "public-shelf-page",
            200,
            ctx.client.get(f"/public/users/{u1.user_id}/shelves/{s.public_shelf_slug}").status,
        )

    r.section("Feature matrix — social & recs")
    r.check_status("friends-list", 200, ctx.client.get("/friends", token=u1.access_token).status)
    r.check_status("recs-inbox", 200, ctx.client.get("/recommendations/inbox", token=u1.access_token).status)
    r.check_status("recs-sent", 200, ctx.client.get("/recommendations/sent", token=u1.access_token).status)
    r.check_status("conversations-list", 200, ctx.client.get("/conversations", token=u1.access_token).status)
    r.check_status("conversations-unread", 200, ctx.client.get("/conversations/unread-count", token=u1.access_token).status)

    if s.user_book_id:
        r.check_status(
            "comments-on-book",
            200,
            ctx.client.get("/comments", token=u1.access_token, params={"targetType": "BOOK", "targetId": s.book_id}).status,
        )

    r.section("Feature matrix — public catalog")
    r.check_status("setup-status", 200, ctx.client.get("/setup/required").status)
    r.check_status("instance-status", 200, ctx.client.get("/status").status)
