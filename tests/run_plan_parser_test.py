#!/usr/bin/env python3
"""
Plan parser test runner.
Re-extracts parseYamlPlan from scripts/examples/daily_fitness.js, injects it
into the PARSER_SOURCE marker block of tests/plan_parser.test.js (so the test
always runs against the current parser), and executes it with node.
"""

import subprocess
import sys
import tempfile
from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parent.parent
SOURCE = PROJECT_ROOT / "scripts/examples/daily_fitness.js"
TEST = PROJECT_ROOT / "tests/plan_parser.test.js"
MARKER = "/*PARSER_SOURCE*/"


def extract_parser() -> str:
    src = SOURCE.read_text(encoding="utf-8")
    start = src.index("// --- Restricted YAML subset parser")
    brace_start = src.index("function parseYamlPlan(text) {")
    depth = 0
    i = brace_start + len("function parseYamlPlan(text) {") - 1
    while True:
        if src[i] == "{":
            depth += 1
        elif src[i] == "}":
            depth -= 1
            if depth == 0:
                break
        i += 1
    return src[start:i + 1]


def main() -> int:
    print("=" * 70)
    print("Plan Parser Unit Tests (node)")
    print("=" * 70)
    print()

    parser_src = extract_parser()
    test_src = TEST.read_text(encoding="utf-8")
    if MARKER not in test_src:
        print(f"[FAIL] marker {MARKER} missing in {TEST}")
        return 1

    with tempfile.NamedTemporaryFile(
        mode="w", suffix=".test.js", delete=False, encoding="utf-8"
    ) as tmp:
        tmp.write(test_src.replace(MARKER, parser_src))
        tmp_path = tmp.name

    try:
        result = subprocess.run(["node", tmp_path], cwd=PROJECT_ROOT)
        return result.returncode
    finally:
        Path(tmp_path).unlink(missing_ok=True)


if __name__ == "__main__":
    sys.exit(main())
