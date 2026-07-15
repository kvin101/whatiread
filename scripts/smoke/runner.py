from __future__ import annotations

import sys
from dataclasses import dataclass, field


@dataclass
class Runner:
    passed: int = 0
    failed: int = 0
    skipped: int = 0
    failures: list[str] = field(default_factory=list)

    def section(self, title: str) -> None:
        print(f"\n=== {title} ===")

    def check_status(self, name: str, expected: int, actual: int) -> None:
        if actual == expected:
            print(f"PASS: {name} (HTTP {actual})")
            self.passed += 1
        else:
            msg = f"FAIL: {name} (expected HTTP {expected}, got {actual})"
            print(msg)
            self.failed += 1
            self.failures.append(msg)

    def check_bool(self, name: str, expected: bool | str, actual: bool | str) -> None:
        exp = str(expected).lower()
        act = str(actual).lower()
        if exp == act:
            print(f"PASS: {name}")
            self.passed += 1
        else:
            msg = f"FAIL: {name} (expected {expected}, got {actual})"
            print(msg)
            self.failed += 1
            self.failures.append(msg)

    def check_true(self, name: str, condition: bool, detail: str = "") -> None:
        if condition:
            print(f"PASS: {name}")
            self.passed += 1
        else:
            msg = f"FAIL: {name}" + (f" ({detail})" if detail else "")
            print(msg)
            self.failed += 1
            self.failures.append(msg)

    def skip(self, name: str, reason: str) -> None:
        print(f"SKIP: {name} — {reason}")
        self.skipped += 1

    def summary(self) -> int:
        print(f"\n=== Summary: {self.passed} passed, {self.failed} failed, {self.skipped} skipped ===")
        return 1 if self.failed else 0

    def exit(self) -> None:
        sys.exit(self.summary())
