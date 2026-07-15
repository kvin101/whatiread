from __future__ import annotations

from typing import TYPE_CHECKING

if TYPE_CHECKING:
    from context import SmokeContext
    from runner import Runner


def run_auth(ctx: SmokeContext, r: Runner) -> None:
    r.section("Auth — happy path & edge cases")
    u1, reg_status = ctx.register("u1", f"smoke-{ctx.ts}@example.com", f"smoke{ctx.ts}")
    r.check_status("register", 201, reg_status)
    assert u1 is not None

    r.check_status("login-email", 200, ctx.client.post("/auth/login", json_body={"email": u1.email, "password": ctx.config.password}).status)
    r.check_status("login-username", 200, ctx.client.post("/auth/login", json_body={"email": u1.username, "password": ctx.config.password}).status)
    r.check_status("login-bad-password", 401, ctx.client.post("/auth/login", json_body={"email": u1.email, "password": "wrong-password"}).status)
    r.check_status("login-malformed-json", 400, ctx.client.request("POST", "/auth/login", headers={"Content-Type": "application/json"}, content="{bad json}").status)
    r.check_status("register-duplicate-email", 409, ctx.client.post("/auth/register", json_body={"email": u1.email, "username": f"other{ctx.ts}", "password": ctx.config.password, "firstName": "X", "lastName": "Y"}).status)

    fresh = ctx.client.get("/auth/username/available", params={"username": f"fresh{ctx.ts}"})
    r.check_status("auth-username-available-fresh", 200, fresh.status)
    r.check_bool("auth-username-available-true", True, fresh.field("available"))

    taken = ctx.client.get("/auth/username/available", params={"username": u1.username})
    r.check_status("auth-username-taken", 200, taken.status)
    r.check_bool("auth-username-taken-false", False, taken.field("available"))

    r.check_status("unauth-me", 401, ctx.client.get("/me").status)
    r.check_status("invalid-bearer-me", 401, ctx.client.get("/me", token="invalid.token.here").status)

    r.section("JWT refresh & logout")
    refresh1 = ctx.client.post("/auth/refresh", json_body={"refreshToken": u1.refresh_token})
    r.check_status("token-refresh", 200, refresh1.status)
    access2 = refresh1.field("accessToken", default="")
    refresh2 = refresh1.field("refreshToken", default="")

    r.check_status("refresh-rejects-old-token", 401, ctx.client.post("/auth/refresh", json_body={"refreshToken": u1.refresh_token}).status)
    r.check_bool("refresh-rotates-access", True, u1.access_token != access2)
    r.check_status("refresh-invalid-token", 401, ctx.client.post("/auth/refresh", json_body={"refreshToken": "not-a-real-token"}).status)

    refresh3 = ctx.client.post("/auth/refresh", json_body={"refreshToken": refresh2})
    r.check_status("refresh-second-use", 200, refresh3.status)
    u1.access_token = refresh3.field("accessToken", default=u1.access_token)
    u1.refresh_token = refresh3.field("refreshToken", default=u1.refresh_token)

    logout_user, _ = ctx.register("logout", f"logout-{ctx.ts}@example.com", f"logout{ctx.ts}")
    assert logout_user is not None
    r.check_status("logout", 204, ctx.client.post("/auth/logout", json_body={"refreshToken": logout_user.refresh_token}).status)
    r.check_status("refresh-after-logout", 401, ctx.client.post("/auth/refresh", json_body={"refreshToken": logout_user.refresh_token}).status)
