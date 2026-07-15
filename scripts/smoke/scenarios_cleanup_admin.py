from __future__ import annotations

from typing import TYPE_CHECKING

if TYPE_CHECKING:
    from context import SmokeContext
    from runner import Runner


def run_cleanup(ctx: SmokeContext, r: Runner) -> None:
    r.section("Library — access control & cleanup")
    u1 = ctx.users["u1"]
    u2 = ctx.users["u2"]
    s = ctx.state

    r.check_status("library-other-user-denied", 404, ctx.client.get(f"/library/{s.user_book_id}", token=u2.access_token).status)
    r.check_status("library-delete", 204, ctx.client.delete(f"/library/{s.user_book_id}", token=u1.access_token).status)
    r.check_status("library-deleted-not-found", 404, ctx.client.get(f"/library/{s.user_book_id}", token=u1.access_token).status)
    r.check_status("friend-unfriend", 204, ctx.client.delete(f"/friends/{u2.user_id}", token=u1.access_token).status)
    friends = ctx.client.get("/friends", token=u1.access_token)
    r.check_status("friends-empty-after-unfriend", 200, friends.status)
    r.check_bool("friends-list-empty", True, isinstance(friends.json, list) and len(friends.json) == 0)
    r.check_status("avatar-remove", 204, ctx.client.delete("/me/avatar", token=u1.access_token).status)


def run_admin(ctx: SmokeContext, r: Runner) -> None:
    r.section("Admin (optional)")
    cfg = ctx.config
    admin_token = ""

    if cfg.admin_email:
        resp = ctx.client.post("/auth/login", json_body={"email": cfg.admin_email, "password": cfg.admin_password})
        if resp.status == 200:
            admin_token = resp.field("accessToken", default="")
    else:
        me = ctx.client.get("/me", token=ctx.users["u1"].access_token)
        if me.field("admin") is True:
            admin_token = ctx.users["u1"].access_token

    if not admin_token:
        r.skip("admin-api-tests", "no admin credentials (set SMOKE_ADMIN_EMAIL or use admin user)")
        return

    me = ctx.client.get("/me", token=admin_token)
    admin_me_id = me.field("id", default="")

    r.check_status("admin-users-list", 200, ctx.client.get("/admin/users", token=admin_token).status)
    r.check_status("admin-users-suggest", 200, ctx.client.get("/admin/users/suggest", token=admin_token, params={"q": "smoke"}).status)
    r.check_status("admin-registration-toggle-off", 200, ctx.client.patch("/admin/instance/registration", token=admin_token, json_body={"enabled": False}).status)
    setup = ctx.client.get("/setup/required")
    r.check_bool("admin-registration-disabled", False, setup.field("registrationEnabled"))
    r.check_status("admin-registration-toggle-on", 200, ctx.client.patch("/admin/instance/registration", token=admin_token, json_body={"enabled": True}).status)

    created = ctx.client.post(
        "/admin/users",
        token=admin_token,
        json_body={
            "email": f"admin-created-{ctx.ts}@example.com",
            "username": f"admu{ctx.ts}",
            "password": cfg.password,
            "firstName": "Admin",
            "lastName": "Created",
            "role": "USER",
        },
    )
    r.check_status("admin-create-user", 201, created.status)
    created_id = created.field("id", default="")

    r.check_status("admin-disable-user", 200, ctx.client.patch(f"/admin/users/{created_id}/enabled", token=admin_token, json_body={"enabled": False}).status)
    r.check_status("admin-disabled-login", 401, ctx.client.post("/auth/login", json_body={"email": created.field("email"), "password": cfg.password}).status)
    r.check_status("admin-enable-user", 200, ctx.client.patch(f"/admin/users/{created_id}/enabled", token=admin_token, json_body={"enabled": True}).status)
    r.check_status("admin-reset-password", 200, ctx.client.patch(f"/admin/users/{created_id}/password", token=admin_token, json_body={"password": f"NewPass{ctx.ts}!"}).status)
    r.check_status("admin-delete-user", 204, ctx.client.delete(f"/admin/users/{created_id}", token=admin_token).status)
    r.check_status("admin-delete-self-denied", 403, ctx.client.delete(f"/admin/users/{admin_me_id}", token=admin_token).status)
    r.check_status("non-admin-users-denied", 403, ctx.client.get("/admin/users", token=ctx.users["u2"].access_token).status)
