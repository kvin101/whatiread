from __future__ import annotations

from typing import TYPE_CHECKING

from context import TINY_PNG

if TYPE_CHECKING:
    from context import SmokeContext
    from runner import Runner


def run_users(ctx: SmokeContext, r: Runner) -> None:
    r.section("Users & profile")
    u1 = ctx.users["u1"]
    r.check_status("me", 200, ctx.client.get("/me", token=u1.access_token).status)
    r.check_status("profile-update", 200, ctx.client.patch("/me", token=u1.access_token, json_body={"firstName": "Smoke", "lastName": "Runner", "writer": True, "writerBio": "Reads a lot"}).status)
    r.check_status("me-username-available-own", 200, ctx.client.get("/me/username/available", token=u1.access_token, params={"username": u1.username}).status)
    r.check_status("me-username-available-fresh", 200, ctx.client.get("/me/username/available", token=u1.access_token, params={"username": f"newuser{ctx.ts}"}).status)

    avatar = ctx.client.request(
        "POST",
        "/me/avatar",
        token=u1.access_token,
        files={"file": ("avatar.png", TINY_PNG, "image/png")},
    )
    r.check_status("avatar-upload", 200, avatar.status)
    ctx.state.avatar_url = avatar.field("avatarUrl", default="")
    if ctx.state.avatar_url:
        r.check_status("avatar-get", 200, ctx.client.get(ctx.state.avatar_url, absolute_url=f"{ctx.config.base_url}{ctx.state.avatar_url}").status)
    else:
        r.check_true("avatar-get", False, "no avatarUrl")

    r.check_status("profile-by-id", 200, ctx.client.get(f"/users/{u1.user_id}/profile", token=u1.access_token).status)
    r.check_status("profile-by-username", 200, ctx.client.get(f"/users/{u1.username}/profile", token=u1.access_token).status)
    r.check_status("user-shelves", 200, ctx.client.get(f"/users/{u1.user_id}/shelves", token=u1.access_token).status)
    r.check_status("users-suggest-no-auth", 401, ctx.client.get("/users/suggest", params={"q": "smoke"}).status)
    r.check_status("users-suggest-missing-q", 400, ctx.client.get("/users/suggest", token=u1.access_token).status)
    r.check_status("users-suggest-invite", 200, ctx.client.get("/users/suggest", token=u1.access_token, params={"q": "smoke", "scope": "invite"}).status)


def run_books(ctx: SmokeContext, r: Runner) -> None:
    r.section("Books")
    u1 = ctx.users["u1"]
    r.check_status("books-search-public", 200, ctx.client.get("/books/search", params={"q": "harry"}).status)
    r.check_status("books-suggest-public", 200, ctx.client.get("/books/suggest", params={"q": "har"}).status)
    r.check_status("create-book-unauth", 401, ctx.client.post("/books", json_body={"title": "No Auth", "authors": ["X"], "pageCount": 1}).status)

    book = ctx.client.post("/books", token=u1.access_token, json_body={"title": f"Smoke Book {ctx.ts}", "authors": ["Smoke Author"], "pageCount": 271, "description": "E2E test"})
    r.check_status("create-book", 201, book.status)
    ctx.state.book_id = book.field("id", default="")

    r.check_status("get-book", 200, ctx.client.get(f"/books/{ctx.state.book_id}", token=u1.access_token).status)
    r.check_status("book-not-found", 404, ctx.client.get("/books/00000000-0000-0000-0000-000000000001", token=u1.access_token).status)
    r.check_status("external-preview-not-found", 404, ctx.client.get("/books/external-preview", token=u1.access_token, params={"externalId": "unsupported-id", "source": "GOOGLE"}).status)

    idem_key = f"idem-{ctx.ts}"
    book2 = ctx.client.post(
        "/books",
        token=u1.access_token,
        json_body={"title": "Idempotent Title", "authors": ["Author"], "pageCount": 100},
        headers={"Idempotency-Key": idem_key},
    )
    r.check_status("idempotency-first", 201, book2.status)
    ctx.state.book2_id = book2.field("id", default="")

    replay = ctx.client.post(
        "/books",
        token=u1.access_token,
        json_body={"title": "Different Title", "authors": ["Other"], "pageCount": 50},
        headers={"Idempotency-Key": idem_key},
    )
    r.check_bool("idempotency-replay-same-id", ctx.state.book2_id, replay.field("id", default=""))
