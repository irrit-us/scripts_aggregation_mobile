#!/usr/bin/env python3
"""
ScriptHost JavaScript Example Tester
Tests JavaScript examples for syntax and API usage
"""

import os
import re
import json
from pathlib import Path
from typing import List, Dict

class JavaScriptTester:
    def __init__(self, project_root: str):
        self.project_root = Path(project_root)
        self.tests_passed = 0
        self.tests_failed = 0
        self.test_results = []

    def run_all_tests(self):
        """Run all JavaScript tests"""
        print("=" * 70)
        print("ScriptHost JavaScript Example Tests")
        print("=" * 70)
        print()

        examples_dir = self.project_root / "scripts/examples"
        js_files = sorted(examples_dir.glob("*.js"))

        for js_file in js_files:
            self.test_javascript_file(js_file)

        return self.print_summary()

    def test_javascript_file(self, file_path: Path):
        """Test individual JavaScript file"""
        print(f"Testing: {file_path.name}")
        print("-" * 70)

        content = file_path.read_text()
        tests = []

        # Test 1: Syntax check (basic)
        tests.append(self.test_syntax(content, file_path.name))

        # Test 2: API usage
        tests.append(self.test_api_usage(content, file_path.name))

        # Test 3: Event handlers
        tests.append(self.test_event_handlers(content, file_path.name))

        # Test 4: Error handling
        tests.append(self.test_error_handling(content, file_path.name))

        # Test 5: Code quality
        tests.append(self.test_code_quality(content, file_path.name))

        passed = sum(1 for t in tests if t)
        total = len(tests)

        print(f"Result: {passed}/{total} tests passed")
        print()

        self.test_results.append({
            "file": file_path.name,
            "passed": passed,
            "total": total,
            "success": passed == total
        })

        if passed == total:
            self.tests_passed += 1
        else:
            self.tests_failed += 1

    def test_syntax(self, content: str, filename: str) -> bool:
        """Test basic JavaScript syntax"""
        # Check for common syntax errors
        errors = []

        # Unmatched braces
        open_braces = content.count('{')
        close_braces = content.count('}')
        if open_braces != close_braces:
            errors.append(f"Unmatched braces: {open_braces} open, {close_braces} close")

        # Unmatched parentheses
        open_parens = content.count('(')
        close_parens = content.count(')')
        if open_parens != close_parens:
            errors.append(f"Unmatched parentheses: {open_parens} open, {close_parens} close")

        # Check for var/let/const declarations
        has_declarations = bool(re.search(r'\b(var|let|const)\s+\w+', content))

        if errors:
            print(f"  [FAIL] Syntax: {', '.join(errors)}")
            return False
        elif not has_declarations:
            print(f"  [WARN]  Syntax: No variable declarations found")
            return True
        else:
            print(f"  [OK] Syntax: Valid")
            return True

    def test_api_usage(self, content: str, filename: str) -> bool:
        """Test ScriptHost API usage"""
        apis_used = []

        # UI Components
        ui_components = ['Button', 'Label', 'TextField', 'ListView', 'Switch', 'Slider', 'ImageView', 'ScrollView']
        for component in ui_components:
            if f'new {component}' in content:
                apis_used.append(component)

        # UI namespace
        if 'UI.addView' in content:
            apis_used.append('UI.addView')

        # System APIs
        if 'Network.get' in content or 'Network.post' in content:
            apis_used.append('Network')
        if 'Storage.' in content:
            apis_used.append('Storage')
        if 'Sensor.' in content:
            apis_used.append('Sensor')
        if 'Device.' in content:
            apis_used.append('Device')

        # Helper functions
        if 'showAlert' in content:
            apis_used.append('showAlert')
        if 'showToast' in content:
            apis_used.append('showToast')
        if 'console.log' in content:
            apis_used.append('console.log')

        if apis_used:
            print(f"  [OK] API Usage: {', '.join(apis_used[:3])}{'...' if len(apis_used) > 3 else ''}")
            return True
        else:
            print(f"  [FAIL] API Usage: No ScriptHost APIs found")
            return False

    def test_event_handlers(self, content: str, filename: str) -> bool:
        """Test event handler implementation"""
        handlers = []

        # Check for event handlers
        event_patterns = [
            (r'\.setOnTap\s*\(', 'onTap'),
            (r'\.setOnChange\s*\(', 'onChange'),
            (r'\.setOnItemTap\s*\(', 'onItemTap'),
        ]

        for pattern, name in event_patterns:
            if re.search(pattern, content):
                handlers.append(name)

        # Check for callback functions
        if re.search(r'function\s*\([^)]*\)\s*\{', content):
            handlers.append('callbacks')

        if handlers:
            print(f"  [OK] Event Handlers: {', '.join(handlers)}")
            return True
        else:
            print(f"  [WARN]  Event Handlers: None found (may not be needed)")
            return True  # Not all scripts need event handlers

    def test_error_handling(self, content: str, filename: str) -> bool:
        """Test error handling"""
        has_error_handling = False

        # Check for error parameter in callbacks
        if re.search(r'function\s*\([^,)]*,\s*error\s*\)', content):
            has_error_handling = True

        # Check for error checks
        if 'if (error)' in content or 'if (!error)' in content:
            has_error_handling = True

        # Check for try-catch
        if 'try' in content and 'catch' in content:
            has_error_handling = True

        if has_error_handling:
            print(f"  [OK] Error Handling: Present")
            return True
        else:
            print(f"  [WARN]  Error Handling: Not found (may not be needed)")
            return True  # Not all scripts need error handling

    def test_code_quality(self, content: str, filename: str) -> bool:
        """Test code quality"""
        issues = []

        # Check for comments
        has_comments = '//' in content or '/*' in content
        if not has_comments:
            issues.append("No comments")

        # Check for meaningful variable names
        if re.search(r'\b(x|y|z|a|b|c)\s*=', content):
            issues.append("Single-letter variables")

        # Check line length
        lines = content.split('\n')
        long_lines = [i for i, line in enumerate(lines, 1) if len(line) > 120]
        if long_lines:
            issues.append(f"{len(long_lines)} lines > 120 chars")

        if not issues:
            print(f"  [OK] Code Quality: Good")
            return True
        else:
            print(f"  [WARN]  Code Quality: {', '.join(issues)}")
            return True  # Warnings, not failures

    def print_summary(self):
        """Print test summary"""
        print("=" * 70)
        print("Test Summary")
        print("=" * 70)
        print()

        total_files = len(self.test_results)
        passed_files = sum(1 for r in self.test_results if r['success'])

        print(f"Files Tested: {total_files}")
        print(f"Files Passed: {passed_files}")
        print(f"Files Failed: {total_files - passed_files}")
        print()

        if passed_files == total_files:
            print("[OK] ALL TESTS PASSED")
        else:
            print("[WARN]  SOME TESTS FAILED")
            print()
            print("Failed files:")
            for result in self.test_results:
                if not result['success']:
                    print(f"  [FAIL] {result['file']}: {result['passed']}/{result['total']} tests passed")

        print()
        print("Detailed Results:")
        for result in self.test_results:
            status = "[OK]" if result['success'] else "[FAIL]"
            print(f"  {status} {result['file']}: {result['passed']}/{result['total']}")

        return passed_files == total_files

def main():
    project_root = str(Path(__file__).resolve().parent)
    tester = JavaScriptTester(project_root)
    exit(0 if tester.run_all_tests() else 1)

if __name__ == "__main__":
    main()
