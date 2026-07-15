#!/usr/bin/env python3
"""End-to-end smoke test against a running WhatIRead Docker stack."""

from __future__ import annotations

import sys
import time
from pathlib import Path

SMOKE_ROOT = Path(__file__).resolve().parent
PROJECT_ROOT = SMOKE_ROOT.parent.parent
sys.path.insert(0, str(SMOKE_ROOT))

from client import ApiClient  # noqa: E402
from config import SmokeConfig  # noqa: E402
from context import SmokeContext  # noqa: E402
from runner import Runner  # noqa: E402
from scenarios_auth import run_auth  # noqa: E402
from scenarios_cleanup_admin import run_admin, run_cleanup  # noqa: E402
from scenarios_friends import run_friends  # noqa: E402
from scenarios_library import run_library  # noqa: E402
from scenarios_messaging import run_messaging  # noqa: E402
from scenarios_platform import run_infra, run_platform, run_setup  # noqa: E402
from scenarios_recommendations_comments import run_comments, run_recommendations  # noqa: E402
from scenarios_shelves import run_shelves, run_shelves_extended  # noqa: E402
from scenarios_users_books import run_books, run_users  # noqa: E402

SCENARIOS = [
    run_platform,
    run_setup,
    run_auth,
    run_users,
    run_books,
    run_library,
    run_shelves,
    run_friends,
    run_shelves_extended,
    run_messaging,
    run_recommendations,
    run_comments,
    run_cleanup,
    run_admin,
    run_infra,
]


def main() -> int:
    print(f"=== WhatIRead smoke test ({SmokeConfig.from_env(str(PROJECT_ROOT)).api_url}) ===")
    config = SmokeConfig.from_env(str(PROJECT_ROOT))
    runner = Runner()
    client = ApiClient(config)
    ctx = SmokeContext(config=config, client=client, runner=runner, ts=int(time.time()))

    try:
        for scenario in SCENARIOS:
            scenario(ctx, runner)
    finally:
        client.close()

    return runner.summary()


if __name__ == "__main__":
    sys.exit(main())
