from __future__ import annotations

import json
import subprocess
from typing import TYPE_CHECKING

import httpx

if TYPE_CHECKING:
    from context import SmokeContext
    from runner import Runner


def run_platform(ctx: SmokeContext, r: Runner) -> None:
    r.section("Platform & status")
    r.check_status("status", 200, ctx.client.get("/status").status)
    r.check_status("ws-sockjs-info", 200, ctx.client.get("/ws/info", absolute_url=f"{ctx.config.base_url}/ws/info").status)

    corr = f"corr-smoke-{ctx.ts}"
    resp = ctx.client.request("GET", "/status", headers={"X-Correlation-Id": corr})
    echoed = resp.headers.get("x-correlation-id", "")
    r.check_bool("correlation-id-echoed", corr, echoed)

    hdrs = ctx.client.get("/status")
    for name, expected in [("x-content-type-options", "nosniff"), ("x-frame-options", "deny")]:
        raw = hdrs.headers.get(name, "").lower()
        values = [v.strip() for v in raw.split(",") if v.strip()]
        r.check_true(f"security-header-{name}", expected in values, raw or "missing")

    for name, path in [("actuator-liveness", "/actuator/health/liveness"), ("actuator-readiness", "/actuator/health/readiness")]:
        out = _docker_exec(ctx, f"wget -q -O - http://127.0.0.1:8080{path}")
        r.check_true(name, '"status":"UP"' in out or '"status": "UP"' in out)


def run_setup(ctx: SmokeContext, r: Runner) -> None:
    r.section("Setup")
    resp = ctx.client.get("/setup/required")
    r.check_status("setup-required", 200, resp.status)
    r.check_bool("setup-already-done", False, resp.field("setupRequired", default=True))
    dup = ctx.client.post(
        "/setup/admin",
        json_body={
            "email": f"dup-{ctx.ts}@example.com",
            "username": f"dup{ctx.ts}",
            "password": ctx.config.password,
            "firstName": "Dup",
        },
    )
    r.check_status("setup-admin-rejects-duplicate", 409, dup.status)


def _docker_exec(ctx: SmokeContext, cmd: str) -> str:
    try:
        proc = subprocess.run(
            ["docker", "compose", "exec", "-T", "api", "sh", "-c", cmd],
            cwd=ctx.config.project_root,
            capture_output=True,
            text=True,
            timeout=30,
        )
        return proc.stdout
    except (subprocess.SubprocessError, FileNotFoundError):
        return ""


def run_infra(ctx: SmokeContext, r: Runner) -> None:
    r.section("Observability smoke test")
    urls = {
        "prometheus-ready": "http://127.0.0.1:9090/-/ready",
        "jaeger-ui": "http://127.0.0.1:16686/",
        "loki-ready": "http://127.0.0.1:3100/ready",
        "grafana-health": "http://127.0.0.1:3000/api/health",
        "dozzle-ui": "http://127.0.0.1:8087/",
        "meilisearch-health": "http://127.0.0.1:7700/health",
    }
    with httpx.Client(timeout=10.0) as http:
        for name, url in urls.items():
            try:
                code = http.get(url).status_code
            except httpx.HTTPError:
                code = 0
            r.check_status(name, 200, code)

        try:
            targets = http.get("http://127.0.0.1:9090/api/v1/targets").json()
            up = [t for t in targets.get("data", {}).get("activeTargets", []) if t.get("health") == "up"]
            r.check_true("prometheus-scrapes-api", len(up) > 0)
        except (httpx.HTTPError, json.JSONDecodeError, AttributeError):
            r.check_true("prometheus-scrapes-api", False)

        try:
            prom = http.get("http://127.0.0.1:9090/api/v1/query", params={"query": "up"}).json()
            r.check_true("prometheus-query", prom.get("status") == "success")
        except (httpx.HTTPError, json.JSONDecodeError, AttributeError):
            r.check_true("prometheus-query", False)

        try:
            jaeger = http.get("http://127.0.0.1:16686/api/services").json()
            r.check_true("jaeger-service-registered", "whatiread" in jaeger.get("data", []))
        except (httpx.HTTPError, json.JSONDecodeError, AttributeError):
            r.check_true("jaeger-service-registered", False)

    r.section("Docker container status")
    try:
        proc = subprocess.run(
            ["docker", "compose", "ps", "--format", "{{.Service}} {{.Status}}"],
            cwd=ctx.config.project_root,
            capture_output=True,
            text=True,
            timeout=30,
        )
        for line in proc.stdout.splitlines():
            svc = line.split()[0] if line else ""
            ok = "(healthy)" in line or ("Up" in line and "unhealthy" not in line)
            r.check_true(f"container-{svc}", ok, line)
    except (subprocess.SubprocessError, FileNotFoundError):
        r.check_true("container-list", False, "docker compose unavailable")

    r.section("API logs (unexpected errors)")
    try:
        proc = subprocess.run(
            ["docker", "compose", "logs", "api", "--since", "90s"],
            cwd=ctx.config.project_root,
            capture_output=True,
            text=True,
            timeout=30,
        )
        bad = [
            ln for ln in proc.stdout.splitlines()
            if " ERROR " in ln
            and "index_not_found" not in ln
            and "NoResourceFoundException" not in ln
        ]
        r.check_true("no unexpected errors in api logs", len(bad) == 0)
        for ln in bad[:5]:
            print(ln)
    except (subprocess.SubprocessError, FileNotFoundError):
        r.skip("api-logs", "docker compose unavailable")
