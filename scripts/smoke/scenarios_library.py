from __future__ import annotations

from typing import TYPE_CHECKING

if TYPE_CHECKING:
    from context import SmokeContext
    from runner import Runner


def run_library(ctx: SmokeContext, r: Runner) -> None:
    r.section("Library")
    u1 = ctx.users["u1"]
    s = ctx.state

    add = ctx.client.post("/library", token=u1.access_token, json_body={"bookId": s.book_id, "status": "TO_READ"})
    r.check_status("library-add", 201, add.status)
    s.user_book_id = add.field("id", default="")

    r.check_status("library-duplicate", 409, ctx.client.post("/library", token=u1.access_token, json_body={"bookId": s.book_id}).status)
    r.check_status("library-list", 200, ctx.client.get("/library", token=u1.access_token).status)
    r.check_status("library-filter-reading-empty", 200, ctx.client.get("/library", token=u1.access_token, params={"status": "READING"}).status)
    r.check_status("library-by-book", 200, ctx.client.get(f"/library/by-book/{s.book_id}", token=u1.access_token).status)
    r.check_status("library-get-entry", 200, ctx.client.get(f"/library/{s.user_book_id}", token=u1.access_token).status)

    progress = ctx.client.patch(f"/library/{s.user_book_id}", token=u1.access_token, json_body={"status": "READING", "progressPages": 68})
    r.check_status("library-update-progress", 200, progress.status)
    r.check_bool("library-progress-percent", 25, progress.field("progressPercent"))

    r.check_status("library-rating-valid", 200, ctx.client.patch(f"/library/{s.user_book_id}", token=u1.access_token, json_body={"rating": 4.5}).status)
    r.check_status("library-rating-invalid", 400, ctx.client.patch(f"/library/{s.user_book_id}", token=u1.access_token, json_body={"rating": 4.3}).status)
    r.check_status("library-clear-rating", 200, ctx.client.patch(f"/library/{s.user_book_id}", token=u1.access_token, json_body={"clearRating": True}).status)

    note = ctx.client.post(f"/library/{s.user_book_id}/notes", token=u1.access_token, json_body={"body": "Smoke test note"})
    r.check_status("library-note-create", 201, note.status)
    s.note_id = note.field("id", default="")
    r.check_status("library-notes-list", 200, ctx.client.get(f"/library/{s.user_book_id}/notes", token=u1.access_token).status)
    r.check_status("library-note-update", 200, ctx.client.patch(f"/library/{s.user_book_id}/notes/{s.note_id}", token=u1.access_token, json_body={"body": "Updated note"}).status)
    r.check_status("library-note-delete", 204, ctx.client.delete(f"/library/{s.user_book_id}/notes/{s.note_id}", token=u1.access_token).status)

    book3 = ctx.client.post("/books", token=u1.access_token, json_body={"title": f"Cursor B {ctx.ts}", "authors": ["B"], "pageCount": 120})
    s.book3_id = book3.field("id", default="")
    ctx.client.post("/library", token=u1.access_token, json_body={"bookId": s.book3_id})

    page1 = ctx.client.get("/library", token=u1.access_token, params={"cursor": " ", "limit": 1})
    r.check_status("library-cursor-page", 200, page1.status)
    next_cursor = page1.field("nextCursor", default="")
    if next_cursor:
        page2 = ctx.client.get("/library", token=u1.access_token, params={"cursor": next_cursor, "limit": 1})
        r.check_status("library-cursor-next", 200, page2.status)
    else:
        r.check_true("library-cursor-next", False, "no nextCursor")
