from __future__ import annotations

import json
import subprocess
from pathlib import Path
from typing import TYPE_CHECKING

if TYPE_CHECKING:
    from context import SmokeContext
    from runner import Runner


def send_ws_message(ctx: SmokeContext, r: Runner, token: str, conversation_id: str, body: str) -> bool:
    root = Path(ctx.config.project_root)
    ws_script = root / "scripts" / "smoke-ws.mjs"
    node_modules = root / "frontend" / "node_modules" / "sockjs-client" / "package.json"
    if not ws_script.exists():
        r.skip("ws-send-message", "scripts/smoke-ws.mjs missing")
        return False
    if not node_modules.exists():
        r.skip("ws-send-message", "frontend/node_modules not installed")
        return False
    result = subprocess.run(
        ["node", str(ws_script), ctx.config.base_url, token, conversation_id, body],
        cwd=root,
        capture_output=True,
        text=True,
    )
    if result.returncode == 0:
        r.check_true("ws-send-message", True)
        return True
    r.check_true("ws-send-message", False, result.stderr.strip() or "node script failed")
    return False
