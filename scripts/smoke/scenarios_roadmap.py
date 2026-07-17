from __future__ import annotations

from typing import TYPE_CHECKING

if TYPE_CHECKING:
    from context import SmokeContext
    from runner import Runner


def run_roadmap(ctx: SmokeContext, r: Runner) -> None:
    """Reading goals, streaks, activity, notifications, library sort, reading overlap."""
    r.section("Product roadmap")
    u1 = ctx.users["u1"]
    u2 = ctx.users.get("u2")
    s = ctx.state

    goal = ctx.client.put(
        "/me/reading-goal",
        token=u1.access_token,
        json_body={"year": 2026, "targetBooks": 12, "targetPages": 5000},
    )
    r.check_status("reading-goal-set", 200, goal.status)
    r.check_bool("reading-goal-target", 12, goal.field("targetBooks"))

    r.check_status("reading-goal-get", 200, ctx.client.get("/me/reading-goal", token=u1.access_token, params={"year": "2026"}).status)
    r.check_status("reading-stats", 200, ctx.client.get("/me/stats", token=u1.access_token, params={"year": "2026"}).status)
    r.check_status("reading-streak", 200, ctx.client.get("/me/streak", token=u1.access_token).status)

    book = ctx.client.post(
        "/books",
        token=u1.access_token,
        json_body={"title": f"Roadmap Read {ctx.ts}", "authors": ["Road Author"], "pageCount": 180},
    )
    r.check_status("roadmap-book-create", 201, book.status)
    roadmap_book_id = book.field("id", default="")

    added = ctx.client.post(
        "/library",
        token=u1.access_token,
        json_body={"bookId": roadmap_book_id, "status": "READING", "progressPages": 25},
    )
    r.check_status("roadmap-library-add-reading", 201, added.status)

    streak = ctx.client.get("/me/streak", token=u1.access_token)
    r.check_status("reading-streak-after-add", 200, streak.status)
    r.check_bool("reading-streak-active", True, (streak.field("currentStreak") or 0) >= 1)

    r.check_status("library-sort-title", 200, ctx.client.get("/library", token=u1.access_token, params={"sort": "TITLE_ASC", "page": 0, "size": 10}).status)

    cursor = ctx.client.get("/library", token=u1.access_token, params={"cursor": " ", "limit": 24})
    r.check_status("library-cursor-with-book", 200, cursor.status)
    if added.status == 201:
        items = cursor.field("items", default=[]) if isinstance(cursor.json, dict) else []
        r.check_bool("library-cursor-lists-added-book", True, isinstance(items, list) and len(items) >= 1)

    r.check_status("activity-feed", 200, ctx.client.get("/activity", token=u1.access_token).status)
    r.check_status("notifications-list", 200, ctx.client.get("/notifications", token=u1.access_token).status)

    if s.shelf_id:
        overlap = ctx.client.get(f"/shelves/{s.shelf_id}/reading-overlap", token=u1.access_token)
        r.check_status("shelf-reading-overlap", 200, overlap.status)

    clone_token = u2.access_token if u2 else u1.access_token
    if s.clone_shelf_id and u2:
        got = ctx.client.get(f"/shelves/{s.clone_shelf_id}", token=clone_token)
        r.check_status("shelf-clone-get", 200, got.status)
        r.check_bool("clone-credit-present", True, got.field("clonedFromShelfId") == s.shelf_id)
    elif u2:
        clone = ctx.client.post(f"/shelves/{s.shelf_id}/clone", token=clone_token, json_body={"name": f"Roadmap clone {ctx.ts}"})
        r.check_status("shelf-clone-for-credit", 201, clone.status)
        r.check_bool("clone-credit-present", True, clone.field("clonedFromShelfId") == s.shelf_id)
    else:
        r.skip("shelf-clone-for-credit", "u2 not registered")
