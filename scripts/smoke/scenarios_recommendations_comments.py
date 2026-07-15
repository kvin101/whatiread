from __future__ import annotations

from typing import TYPE_CHECKING

if TYPE_CHECKING:
    from context import SmokeContext
    from runner import Runner


def run_recommendations(ctx: SmokeContext, r: Runner) -> None:
    r.section("Recommendations")
    u1 = ctx.users["u1"]
    u2 = ctx.users["u2"]
    s = ctx.state

    rec = ctx.client.post("/recommendations", token=u1.access_token, json_body={"toUserId": u2.user_id, "bookId": s.book_id, "message": "Try this"})
    r.check_status("recommendation-send", 201, rec.status)
    s.rec_id = rec.field("id", default="")
    r.check_status("recommendations-inbox", 200, ctx.client.get("/recommendations/inbox", token=u2.access_token).status)
    r.check_status("recommendations-sent", 200, ctx.client.get("/recommendations/sent", token=u1.access_token).status)
    r.check_status("recommendation-accept", 200, ctx.client.post(f"/recommendations/{s.rec_id}/accept", token=u2.access_token).status)

    rec2 = ctx.client.post("/recommendations", token=u1.access_token, json_body={"toUserId": u2.user_id, "bookId": s.book2_id})
    s.rec2_id = rec2.field("id", default="")
    r.check_status("recommendation-dismiss", 200, ctx.client.post(f"/recommendations/{s.rec2_id}/dismiss", token=u2.access_token).status)
    r.check_status("recommendations-suggestions", 200, ctx.client.get("/recommendations/suggestions", token=u2.access_token).status)

    batch = ctx.client.post("/recommendations/batch", token=u1.access_token, json_body={"toUserId": u2.user_id, "bookIds": [s.book_id, s.book2_id], "message": "Batch"})
    r.check_status("recommendation-batch", 201, batch.status)

    rec3 = ctx.client.post("/recommendations", token=u1.access_token, json_body={"toUserId": u2.user_id, "bookId": s.book3_id})
    s.rec3_id = rec3.field("id", default="")
    r.check_status("recommendation-delete", 204, ctx.client.delete(f"/recommendations/{s.rec3_id}", token=u1.access_token).status)

    r.check_status("recommendation-opt-out", 200, ctx.client.patch("/me", token=u2.access_token, json_body={"acceptRecommendations": False}).status)
    r.check_status("recommendation-rejected-opt-out", 403, ctx.client.post("/recommendations", token=u1.access_token, json_body={"toUserId": u2.user_id, "bookId": s.book3_id}).status)
    ctx.client.patch("/me", token=u2.access_token, json_body={"acceptRecommendations": True})


def run_comments(ctx: SmokeContext, r: Runner) -> None:
    r.section("Comments")
    u1 = ctx.users["u1"]
    s = ctx.state

    bcom = ctx.client.post("/comments", token=u1.access_token, json_body={"targetType": "BOOK", "targetId": s.book_id, "body": "Great book"})
    r.check_status("comment-create-book", 201, bcom.status)
    s.book_comment_id = bcom.field("id", default="")
    r.check_status("comments-list-book", 200, ctx.client.get("/comments", token=u1.access_token, params={"targetType": "BOOK", "targetId": s.book_id}).status)
    r.check_status("comment-update", 200, ctx.client.patch(f"/comments/{s.book_comment_id}", token=u1.access_token, json_body={"body": "Updated comment"}).status)

    scom = ctx.client.post("/comments", token=u1.access_token, json_body={"targetType": "SHELF", "targetId": s.shelf_id, "body": "Shelf note"})
    s.shelf_comment_id = scom.field("id", default="")
    r.check_status("comment-report", 202, ctx.client.post(f"/comments/{s.shelf_comment_id}/report", token=u1.access_token, json_body={"reason": "spam"}).status)

    ubcom = ctx.client.post("/comments", token=u1.access_token, json_body={"targetType": "USER_BOOK", "targetId": s.user_book_id, "body": "Reading notes"})
    ub_id = ubcom.field("id", default="")
    r.check_status("comment-delete", 204, ctx.client.delete(f"/comments/{ub_id}", token=u1.access_token).status)
