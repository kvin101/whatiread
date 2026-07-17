from __future__ import annotations

from typing import TYPE_CHECKING

if TYPE_CHECKING:
    from context import SmokeContext
    from runner import Runner


def run_shelves(ctx: SmokeContext, r: Runner) -> None:
    r.section("Shelves")
    u1 = ctx.users["u1"]
    s = ctx.state

    shelf = ctx.client.post("/shelves", token=u1.access_token, json_body={"name": "Smoke Shelf", "visibility": "PRIVATE", "description": "E2E"})
    r.check_status("create-shelf", 201, shelf.status)
    s.shelf_id = shelf.field("id", default="")
    s.shelf_slug = shelf.field("slug", default="")

    pub = ctx.client.post("/shelves", token=u1.access_token, json_body={"name": "Public Picks", "visibility": "PUBLIC"})
    r.check_status("create-public-shelf", 201, pub.status)
    s.public_shelf_id = pub.field("id", default="")
    s.public_shelf_slug = pub.field("slug", default="")

    r.check_status("shelf-add-book", 201, ctx.client.post(f"/shelves/{s.shelf_id}/books", token=u1.access_token, json_body={"userBookId": s.user_book_id}).status)
    r.check_status("shelf-books-list", 200, ctx.client.get(f"/shelves/{s.shelf_id}/books", token=u1.access_token).status)
    r.check_status("shelves-list", 200, ctx.client.get("/shelves", token=u1.access_token).status)
    r.check_status("shelf-get", 200, ctx.client.get(f"/shelves/{s.shelf_id}", token=u1.access_token).status)
    r.check_status("shelf-update", 200, ctx.client.patch(f"/shelves/{s.shelf_id}", token=u1.access_token, json_body={"name": "Renamed Shelf", "description": "Updated"}).status)
    r.check_status("shelf-events", 200, ctx.client.get(f"/shelves/{s.shelf_id}/events", token=u1.access_token).status)
    r.check_status("system-shelves", 200, ctx.client.get("/shelves/system", token=u1.access_token).status)
    r.check_status("system-shelf-books", 200, ctx.client.get("/shelves/system/TO_READ/books", token=u1.access_token).status)
    r.check_status("public-user-shelves", 200, ctx.client.get(f"/public/users/{u1.user_id}/shelves").status)
    if s.public_shelf_slug:
        r.check_status("public-shelf-by-slug", 200, ctx.client.get(f"/public/users/{u1.user_id}/shelves/{s.public_shelf_slug}").status)
        r.check_status("public-shelf-books", 200, ctx.client.get(f"/public/users/{u1.user_id}/shelves/{s.public_shelf_slug}/books").status)


def run_shelves_extended(ctx: SmokeContext, r: Runner) -> None:
    r.section("Shelves — members, clone, explore, share")
    u1 = ctx.users["u1"]
    u2 = ctx.users["u2"]
    u3 = ctx.users["u3"]
    s = ctx.state

    r.check_status("shelf-add-member", 201, ctx.client.post(f"/shelves/{s.shelf_id}/members", token=u1.access_token, json_body={"userId": u2.user_id, "role": "EDITOR"}).status)
    r.check_status("shelf-members-list", 200, ctx.client.get(f"/shelves/{s.shelf_id}/members", token=u1.access_token).status)
    r.check_status("shelf-member-role-update", 200, ctx.client.patch(f"/shelves/{s.shelf_id}/members/{u2.user_id}", token=u1.access_token, json_body={"role": "VIEWER"}).status)

    clone = ctx.client.post(f"/shelves/{s.shelf_id}/clone", token=u2.access_token, json_body={"name": "Cloned shelf"})
    r.check_status("shelf-clone", 201, clone.status)
    s.clone_shelf_id = clone.field("id", default="")

    explore = ctx.client.get("/shelves/explore", token=u2.access_token)
    r.check_status("explore-feed", 200, explore.status)
    has_public = False
    if isinstance(explore.json, dict):
        content = explore.json.get("content", [])
        has_public = any(item.get("id") == s.public_shelf_id for item in content)
    r.check_bool("explore-includes-public", True, has_public)

    share_shelf = ctx.client.post("/shelves", token=u1.access_token, json_body={"name": "Share Link Shelf", "visibility": "PRIVATE"})
    s.share_shelf_id = share_shelf.field("id", default="")
    ctx.client.post(f"/shelves/{s.share_shelf_id}/books", token=u1.access_token, json_body={"userBookId": s.user_book_id})

    link = ctx.client.post(f"/shelves/{s.share_shelf_id}/share-links", token=u1.access_token, json_body={})
    r.check_status("share-link-create", 201, link.status)
    s.share_token = link.field("token", default="")
    s.share_link_id = link.field("id", default="")

    r.check_status("share-link-public-view", 200, ctx.client.get(f"/public/shelves/share/{s.share_token}").status)
    r.check_status("share-link-clone", 201, ctx.client.post(f"/shelves/share/{s.share_token}/clone", token=u3.access_token, json_body={"name": "From share link"}).status)
    r.check_status("share-link-non-manager-denied", 403, ctx.client.post(f"/shelves/{s.share_shelf_id}/share-links", token=u3.access_token, json_body={}).status)
    r.check_status("share-link-revoke", 204, ctx.client.delete(f"/shelves/{s.share_shelf_id}/share-links/{s.share_link_id}", token=u1.access_token).status)
    r.check_status("share-link-revoked-denied", 403, ctx.client.get(f"/public/shelves/share/{s.share_token}").status)

    r.check_status("shelf-remove-member", 204, ctx.client.delete(f"/shelves/{s.shelf_id}/members/{u2.user_id}", token=u1.access_token).status)
    r.check_status("shelf-remove-book", 204, ctx.client.delete(f"/shelves/{s.shelf_id}/books/{s.user_book_id}", token=u1.access_token).status)
