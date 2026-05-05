#!/usr/bin/env python3
"""
ScriptHost Master Test Runner
Runs all test suites and generates comprehensive report
"""

import subprocess
import sys
from pathlib import Path
from datetime import datetime

def run_command(cmd, description):
    """Run a command and return success status"""
    print(f"\n{'='*70}")
    print(f"Running: {description}")
    print(f"{'='*70}\n")

    try:
        result = subprocess.run(
            cmd,
            shell=True,
            capture_output=False,
            text=True,
            cwd="/home/johnsilver/focus/AggApp"
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
        "python3 verify.sh 2>/dev/null || python3 /home/johnsilver/focus/AggApp/verify.sh",
        "Project Structure Verification"
    )

    # Test 2: Code Validation
    results['validation'] = run_command(
        "python3 /home/johnsilver/focus/AggApp/validate.py",
        "Code Validation Suite"
    )

    # Test 3: JavaScript Examples
    results['examples'] = run_command(
        "python3 /home/johnsilver/focus/AggApp/test_examples.py",
        "JavaScript Example Tests"
    )

    # Test 4: Integration Tests
    results['integration'] = run_command(
        "python3 /home/johnsilver/focus/AggApp/test_integration.py",
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
        status = "✅ PASSED" if results[key] else "❌ FAILED"
        print(f"  {status}  {name}")

    print()
    print(f"Overall: {passed}/{total} test suites passed")
    print()

    if passed == total:
        print("╔" + "="*68 + "╗")
        print("║" + " "*15 + "🎉 ALL TESTS PASSED SUCCESSFULLY! 🎉" + " "*16 + "║")
        print("╚" + "="*68 + "╝")
        print()
        print("ScriptHost is fully validated and ready for:")
        print("  ✓ Build and compilation")
        print("  ✓ Device testing")
        print("  ✓ Security audit")
        print("  ✓ Performance profiling")
        print("  ✓ Beta testing")
        print("  ✓ Production deployment")
        print()
        print("Next Steps:")
        print("  1. Set up Android Studio with SDK")
        print("  2. Build: cd android && ./gradlew assembleDebug")
        print("  3. Test on device: ./gradlew installDebug")
        print("  4. Run example scripts")
        print("  5. Conduct security audit")
        return 0
    else:
        print("╔" + "="*68 + "╗")
        print("║" + " "*20 + "⚠️  SOME TESTS FAILED ⚠️" + " "*23 + "║")
        print("╚" + "="*68 + "╝")
        print()
        print("Please review the failed tests above.")
        return 1

if __name__ == "__main__":
    sys.exit(main())
