from __future__ import annotations

import time
from dataclasses import dataclass, field
from typing import TYPE_CHECKING

if TYPE_CHECKING:
    from client import ApiClient
    from config import SmokeConfig
    from runner import Runner


# 1x1 PNG
TINY_PNG = (
    b"\x89PNG\r\n\x1a\n\x00\x00\x00\rIHDR\x00\x00\x00\x01\x00\x00\x00\x01\x08\x02"
    b"\x00\x00\x00\x90wS\xde\x00\x00\x00\x0cIDATx\x9cc\xf8\x0f\x00\x00\x01\x01"
    b"\x00\x05\x18\xd8N\x00\x00\x00\x00IEND\xaeB`\x82"
)


@dataclass
class UserSession:
    access_token: str
    refresh_token: str
    user_id: str
    email: str
    username: str

    @classmethod
    def from_auth_response(cls, data: dict) -> UserSession:
        user = data.get("user") or {}
        return cls(
            access_token=data["accessToken"],
            refresh_token=data["refreshToken"],
            user_id=user["id"],
            email=user.get("email", ""),
            username=user.get("username", ""),
        )


@dataclass
class SmokeState:
    book_id: str = ""
    book2_id: str = ""
    book3_id: str = ""
    user_book_id: str = ""
    note_id: str = ""
    shelf_id: str = ""
    shelf_slug: str = ""
    public_shelf_id: str = ""
    public_shelf_slug: str = ""
    secret_shelf_id: str = ""
    share_token: str = ""
    share_link_id: str = ""
    conv_id: str = ""
    group_id: str = ""
    rec_id: str = ""
    rec2_id: str = ""
    rec3_id: str = ""
    book_comment_id: str = ""
    shelf_comment_id: str = ""
    avatar_url: str = ""


@dataclass
class SmokeContext:
    config: SmokeConfig
    client: ApiClient
    runner: Runner
    ts: int
    users: dict[str, UserSession] = field(default_factory=dict)
    state: SmokeState = field(default_factory=SmokeState)

    def register(self, key: str, email: str, username: str) -> tuple[UserSession | None, int]:
        resp = self.client.post(
            "/auth/register",
            json_body={
                "email": email,
                "username": username,
                "password": self.config.password,
                "firstName": "Smoke",
                "lastName": "Test",
            },
        )
        if resp.status == 201 and isinstance(resp.json, dict):
            session = UserSession.from_auth_response(resp.json)
            self.users[key] = session
            return session, resp.status
        return None, resp.status

    def login(self, identifier: str) -> UserSession:
        resp = self.client.post(
            "/auth/login",
            json_body={"email": identifier, "password": self.config.password},
        )
        assert resp.status == 200, f"login failed: {resp.status} {resp.text}"
        return UserSession.from_auth_response(resp.json)

    def make_friends(self, a_key: str, b_key: str) -> None:
        a = self.users[a_key]
        b = self.users[b_key]
        friends = self.client.get("/friends", token=a.access_token)
        if friends.status == 200 and isinstance(friends.json, list):
            if any(f.get("id") == b.user_id for f in friends.json):
                return

        req = self.client.post(
            "/friends/requests",
            token=a.access_token,
            json_body={"userId": b.user_id},
        )
        if req.status == 201:
            req_id = req.field("id")
            self.client.post(f"/friends/requests/{req_id}/accept", token=b.access_token)
            return
        if req.status == 409:
            incoming = self.client.get("/friends/requests/incoming", token=b.access_token)
            if incoming.status == 200 and isinstance(incoming.json, list):
                for item in incoming.json:
                    if item.get("status") == "PENDING":
                        self.client.post(
                            f"/friends/requests/{item['id']}/accept",
                            token=b.access_token,
                        )
                        return

    def sleep(self, seconds: float = 1.0) -> None:
        time.sleep(seconds)
