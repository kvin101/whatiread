from __future__ import annotations

from typing import TYPE_CHECKING

from ws import send_ws_message

if TYPE_CHECKING:
    from context import SmokeContext
    from runner import Runner


def run_messaging(ctx: SmokeContext, r: Runner) -> None:
    r.section("Messaging")
    u1 = ctx.users["u1"]
    u2 = ctx.users["u2"]
    s = ctx.state

    conv = ctx.client.post(f"/conversations/with/{u2.user_id}", token=u1.access_token)
    r.check_status("create-conversation", 200, conv.status)
    s.conv_id = conv.field("id", default="")
    r.check_status("conversations-list", 200, ctx.client.get("/conversations", token=u1.access_token).status)
    r.check_status("messages-list-empty", 200, ctx.client.get(f"/conversations/{s.conv_id}/messages", token=u1.access_token).status)

    if send_ws_message(ctx, r, u1.access_token, s.conv_id, f"Smoke E2E message {ctx.ts}"):
        ctx.sleep(1)
        msgs = ctx.client.get(f"/conversations/{s.conv_id}/messages", token=u2.access_token)
        r.check_status("messages-list-after-send", 200, msgs.status)
        count = len(msgs.field("items", default=[])) if isinstance(msgs.json, dict) else 0
        r.check_bool("messages-received", 1, count)
        r.check_status("unread-count", 200, ctx.client.get("/conversations/unread-count", token=u2.access_token).status)
        r.check_status("mark-read", 204, ctx.client.post(f"/conversations/{s.conv_id}/read", token=u2.access_token).status)
    else:
        r.check_status("unread-count", 200, ctx.client.get("/conversations/unread-count", token=u2.access_token).status)
        r.check_status("mark-read", 204, ctx.client.post(f"/conversations/{s.conv_id}/read", token=u1.access_token).status)

    group = ctx.client.post("/conversations/groups", token=u1.access_token, json_body={"name": "Book Club", "memberUserIds": [u2.user_id]})
    r.check_status("create-group", 201, group.status)
    s.group_id = group.field("id", default="")
    r.check_status("groups-list", 200, ctx.client.get("/conversations/groups", token=u1.access_token).status)
    if s.group_id:
        r.check_status("group-rename", 200, ctx.client.put(f"/conversations/{s.group_id}", token=u1.access_token, json_body={"name": "Renamed Club"}).status)
        r.check_status("group-leave", 204, ctx.client.post(f"/conversations/{s.group_id}/leave", token=u2.access_token).status)
    else:
        r.check_true("group-rename", False, "no group id")
        r.check_true("group-leave", False, "no group id")
