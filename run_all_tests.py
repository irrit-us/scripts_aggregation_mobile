#!/usr/bin/env python3
"""
ScriptHost Master Test Runner
Runs all test suites and generates comprehensive report
"""

import os
import subprocess
import sys
from pathlib import Path
from datetime import datetime

PROJECT_ROOT = Path(__file__).resolve().parent


def run_command(cmd, description):
    """Run a command and return success status"""
    print(f"\n{'='*70}")
    print(f"Running: {description}")
    print(f"{'='*70}\n")

    env = dict(os.environ)
    env["PYTHONIOENCODING"] = "utf-8"

    try:
        result = subprocess.run(
            cmd,
            shell=True,
            text=True,
            cwd=PROJECT_ROOT,
            env=env
        )
        return result.returncode == 0
    except Exception as e:
        print(f"Error: {e}")
        return False


def main():
    print("╔" + "="*68 + "╗")
    print("║" + " "*20 + "SCRIPTHOST TEST SUITE" + " "*27 + "║")
    print("║" + " "*18 + "Comprehensive Testing Report" + " "*22 + "║")
    print("╚" + "="*68 + "╝")
    print()
    print(f"Date: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    print(f"Project: ScriptHost - Mobile Script Aggregation Platform")
    print()

    results = {}

    # Test 1: Project Verification
    results['verification'] = run_command(
        "bash verify.sh",
        "Project Structure Verification"
    )

    # Test 2: Code Validation
    results['validation'] = run_command(
        f"{sys.executable} validate.py",
        "Code Validation Suite"
    )

    # Test 3: JavaScript Examples
    results['examples'] = run_command(
        f"{sys.executable} test_examples.py",
        "JavaScript Example Tests"
    )

    # Test 4: Integration Tests
    results['integration'] = run_command(
        f"{sys.executable} test_integration.py",
        "Integration Test Suite"
    )

    # Print Summary
    print("\n" + "="*70)
    print("FINAL TEST SUMMARY")
    print("="*70 + "\n")

    test_names = {
        'verification': 'Project Structure Verification',
        'validation': 'Code Validation Suite',
        'examples': 'JavaScript Example Tests',
        'integration': 'Integration Test Suite',
    }

    passed = sum(1 for v in results.values() if v)
    total = len(results)

    for key, name in test_names.items():
        status = "PASSED" if results[key] else "FAILED"
        print(f"  {status}  {name}")

    print()
    print(f"Overall: {passed}/{total} test suites passed")
    print()

    if passed == total:
        print("╔" + "="*68 + "╗")
        print("║" + " "*19 + "ALL TESTS PASSED SUCCESSFULLY!" + " "*19 + "║")
        print("╚" + "="*68 + "╝")
        print()
        print("Next steps:")
        print("  1. Set up JDK 11+ and the Android SDK")
        print("  2. Build: cd android && ./gradlew assembleDebug")
        print("  3. Test: cd android && ./gradlew testDebugUnitTest")
        print("  4. Install: cd android && ./gradlew installDebug")
        print("  5. Run example scripts")
        return 0
    else:
        print("╔" + "="*68 + "╗")
        print("║" + " "*25 + "SOME TESTS FAILED" + " "*26 + "║")
        print("╚" + "="*68 + "╝")
        print()
        print("Please review the failed tests above.")
        return 1


if __name__ == "__main__":
    sys.exit(main())
