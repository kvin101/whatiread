from __future__ import annotations

import time
from dataclasses import dataclass
from typing import Any

import httpx

from config import SmokeConfig


@dataclass
class ApiResponse:
    status: int
    json: Any | None
    text: str
    headers: httpx.Headers

    def field(self, *path: str, default: Any = None) -> Any:
        if not isinstance(self.json, dict):
            return default
        cur: Any = self.json
        for key in path:
            if not isinstance(cur, dict) or key not in cur:
                return default
            cur = cur[key]
        return cur


class ApiClient:
    def __init__(self, config: SmokeConfig) -> None:
        self.config = config
        self._client = httpx.Client(timeout=30.0, follow_redirects=True)

    def close(self) -> None:
        self._client.close()

    def request(
        self,
        method: str,
        path: str,
        *,
        token: str | None = None,
        json_body: Any = None,
        data: dict | None = None,
        content: str | bytes | None = None,
        files: dict | None = None,
        params: dict | None = None,
        headers: dict[str, str] | None = None,
        absolute_url: str | None = None,
    ) -> ApiResponse:
        url = absolute_url or self._url(path)
        req_headers = dict(headers or {})
        if token:
            req_headers["Authorization"] = f"Bearer {token}"
        if json_body is not None and "Content-Type" not in req_headers:
            req_headers["Content-Type"] = "application/json"

        for attempt in range(1, self.config.rate_limit_max_retries + 1):
            try:
                resp = self._client.request(
                    method,
                    url,
                    json=json_body,
                    data=data,
                    content=content,
                    files=files,
                    params=params,
                    headers=req_headers,
                )
            except httpx.HTTPError:
                return ApiResponse(0, None, "", httpx.Headers())

            if resp.status_code != 429 or attempt == self.config.rate_limit_max_retries:
                return self._wrap(resp)

            print(
                f"RATE-LIMIT: waiting {self.config.rate_limit_retry_seconds}s "
                f"before retry ({attempt}/{self.config.rate_limit_max_retries})...",
                flush=True,
            )
            time.sleep(self.config.rate_limit_retry_seconds)

        return ApiResponse(0, None, "", httpx.Headers())

    def _url(self, path: str) -> str:
        if path.startswith("http://") or path.startswith("https://"):
            return path
        if path.startswith("/api/"):
            return f"{self.config.base_url.rstrip('/')}{path}"
        base = self.config.api_url.rstrip("/")
        if not path.startswith("/"):
            path = f"/{path}"
        return f"{base}{path}"

    @staticmethod
    def _wrap(resp: httpx.Response) -> ApiResponse:
        text = resp.text
        parsed: Any | None
        try:
            parsed = resp.json() if text else None
        except ValueError:
            parsed = None
        return ApiResponse(resp.status_code, parsed, text, resp.headers)

    def get(self, path: str, *, token: str | None = None, params: dict | None = None, absolute_url: str | None = None) -> ApiResponse:
        return self.request("GET", path, token=token, params=params, absolute_url=absolute_url)

    def post(self, path: str, *, token: str | None = None, json_body: Any = None, absolute_url: str | None = None, **kwargs: Any) -> ApiResponse:
        return self.request("POST", path, token=token, json_body=json_body, absolute_url=absolute_url, **kwargs)

    def patch(self, path: str, *, token: str | None = None, json_body: Any = None) -> ApiResponse:
        return self.request("PATCH", path, token=token, json_body=json_body)

    def put(self, path: str, *, token: str | None = None, json_body: Any = None) -> ApiResponse:
        return self.request("PUT", path, token=token, json_body=json_body)

    def delete(self, path: str, *, token: str | None = None) -> ApiResponse:
        return self.request("DELETE", path, token=token)
