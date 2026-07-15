from __future__ import annotations

import os
from dataclasses import dataclass


@dataclass(frozen=True)
class SmokeConfig:
    base_url: str
    password: str
    admin_email: str
    admin_password: str
    rate_limit_retry_seconds: int
    rate_limit_max_retries: int
    project_root: str

    @property
    def api_url(self) -> str:
        return f"{self.base_url.rstrip('/')}/api/v1"

    @classmethod
    def from_env(cls, project_root: str) -> SmokeConfig:
        password = os.environ.get("SMOKE_PASSWORD", "TestPass123!")
        return cls(
            base_url=os.environ.get("BASE_URL", "http://localhost"),
            password=password,
            admin_email=os.environ.get("SMOKE_ADMIN_EMAIL", ""),
            admin_password=os.environ.get("SMOKE_ADMIN_PASSWORD", password),
            rate_limit_retry_seconds=int(os.environ.get("SMOKE_RATE_LIMIT_WAIT", "65")),
            rate_limit_max_retries=int(os.environ.get("SMOKE_RATE_LIMIT_RETRIES", "3")),
            project_root=project_root,
        )
