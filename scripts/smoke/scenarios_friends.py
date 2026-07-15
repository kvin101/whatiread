from __future__ import annotations

from typing import TYPE_CHECKING

if TYPE_CHECKING:
    from context import SmokeContext
    from runner import Runner


def run_friends(ctx: SmokeContext, r: Runner) -> None:
    r.section("Friends")
    u1 = ctx.users["u1"]
    _, _ = ctx.register("u2", f"smoke2-{ctx.ts}@example.com", f"smoke2{ctx.ts}")
    _, _ = ctx.register("u3", f"smoke3-{ctx.ts}@example.com", f"smoke3{ctx.ts}")
    u2 = ctx.users["u2"]
    u3 = ctx.users["u3"]

    req = ctx.client.post("/friends/requests", token=u1.access_token, json_body={"userId": u2.user_id})
    r.check_status("friend-request-by-id", 201, req.status)
    req_id = req.field("id", default="")
    r.check_status("friend-requests-outgoing", 200, ctx.client.get("/friends/requests/outgoing", token=u1.access_token).status)
    r.check_status("friend-requests-incoming", 200, ctx.client.get("/friends/requests/incoming", token=u2.access_token).status)
    r.check_status("friend-accept", 200, ctx.client.post(f"/friends/requests/{req_id}/accept", token=u2.access_token).status)
    r.check_status("friends-list", 200, ctx.client.get("/friends", token=u1.access_token).status)

    req2 = ctx.client.post("/friends/requests", token=u1.access_token, json_body={"email": u3.email})
    r.check_status("friend-request-by-email", 201, req2.status)
    req2_id = req2.field("id", default="")
    r.check_status("friend-decline", 200, ctx.client.post(f"/friends/requests/{req2_id}/decline", token=u3.access_token).status)

    req3 = ctx.client.post("/friends/requests", token=u1.access_token, json_body={"userId": u3.user_id})
    r.check_status("friend-request-cancel", 201, req3.status)
    req3_id = req3.field("id", default="")
    r.check_status("friend-cancel-pending", 204, ctx.client.delete(f"/friends/requests/{req3_id}", token=u1.access_token).status)

    ctx.make_friends("u1", "u3")
    r.check_status("friend-block", 204, ctx.client.post(f"/friends/{u3.user_id}/block", token=u1.access_token).status)
    r.check_status("friends-blocked-list", 200, ctx.client.get("/friends/blocked", token=u1.access_token).status)
    r.check_status("friend-request-blocked", 409, ctx.client.post("/friends/requests", token=u3.access_token, json_body={"userId": u1.user_id}).status)
    r.check_status("friend-unblock", 204, ctx.client.delete(f"/friends/{u3.user_id}/block", token=u1.access_token).status)
    ctx.make_friends("u1", "u3")
