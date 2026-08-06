#!/usr/bin/env python3
"""
ScriptHost Code Validation Suite
Validates Kotlin code structure, syntax patterns, and implementation completeness
"""

import os
import re
import json
from pathlib import Path
from typing import List, Dict, Tuple

class CodeValidator:
    def __init__(self, project_root: str):
        self.project_root = Path(project_root)
        self.errors = []
        self.warnings = []
        self.passed = 0
        self.failed = 0

    def validate_all(self):
        """Run all validation checks"""
        print("=" * 70)
        print("ScriptHost Code Validation Suite")
        print("=" * 70)
        print()

        self.validate_kotlin_files()
        self.validate_javascript_examples()
        self.validate_documentation()
        self.validate_configuration()
        self.validate_architecture()

        return self.print_summary()

    def validate_kotlin_files(self):
        """Validate Kotlin source files"""
        print(" Validating Kotlin Source Files...")

        kotlin_dir = self.project_root / "android/app/src/main/java/com/scripthost"
        if not kotlin_dir.exists():
            self.add_error("Kotlin source directory not found")
            return

        kotlin_files = list(kotlin_dir.rglob("*.kt"))

        for kt_file in kotlin_files:
            self.validate_kotlin_file(kt_file)

        print(f"  [OK] Validated {len(kotlin_files)} Kotlin files")
        print()

    def validate_kotlin_file(self, file_path: Path):
        """Validate individual Kotlin file"""
        content = file_path.read_text()

        # Check package declaration
        if not re.search(r'^package\s+com\.scripthost', content, re.MULTILINE):
            self.add_warning(f"{file_path.name}: Missing or incorrect package declaration")

        # Check for class/interface/object declaration
        if not re.search(r'(class|interface|object|enum class)\s+\w+', content):
            self.add_warning(f"{file_path.name}: No class/interface/object found")

        # Check for proper imports
        if 'import' in content:
            self.passed += 1
        else:
            self.add_warning(f"{file_path.name}: No imports found")

        # Check for documentation
        if '/**' in content or '/*' in content:
            self.passed += 1
        else:
            self.add_warning(f"{file_path.name}: Missing documentation comments")

    def validate_javascript_examples(self):
        """Validate JavaScript example scripts"""
        print(" Validating JavaScript Examples...")

        examples_dir = self.project_root / "scripts/examples"
        if not examples_dir.exists():
            self.add_error("Examples directory not found")
            return

        js_files = list(examples_dir.glob("*.js"))

        for js_file in js_files:
            self.validate_javascript_file(js_file)

        print(f"  [OK] Validated {len(js_files)} JavaScript examples")
        print()

    def validate_javascript_file(self, file_path: Path):
        """Validate individual JavaScript file"""
        content = file_path.read_text()

        # Check for API usage
        api_patterns = [
            (r'new\s+(Button|Label|TextField|ListView|ImageView|Switch|Slider|ScrollView|CheckBox|Spinner|ProgressBar|Layout)', 'UI Components'),
            (r'UI\.addView', 'UI.addView()'),
            (r'console\.log', 'console.log()'),
            (r'function\s*\(', 'Functions'),
        ]

        found_apis = []
        for pattern, name in api_patterns:
            if re.search(pattern, content):
                found_apis.append(name)

        if found_apis:
            self.passed += 1
        else:
            self.add_warning(f"{file_path.name}: No API usage found")

    def validate_documentation(self):
        """Validate documentation files"""
        print(" Validating Documentation...")

        required_docs = [
            "README.md",
            "docs/API.md",
            "docs/SECURITY.md",
            "docs/EXAMPLES.md",
            "docs/BUILD.md",
            "docs/CHANGELOG.md",
            "docs/CONTRIBUTING.md",
            "docs/FINAL_SUMMARY.md",
            "docs/PROJECT_SUMMARY.md",
            "docs/QUICKSTART.md",
            "docs/STATUS.md",
            "docs/TEST_REPORT.md",
        ]

        for doc in required_docs:
            doc_path = self.project_root / doc
            if doc_path.exists():
                content = doc_path.read_text()
                if len(content) > 100:  # At least 100 characters
                    self.passed += 1
                else:
                    self.add_warning(f"{doc}: Documentation too short")
            else:
                self.add_error(f"{doc}: Documentation file missing")

        print(f"  [OK] Validated {len(required_docs)} documentation files")
        print()

    def validate_configuration(self):
        """Validate configuration files"""
        print(" Validating Configuration Files...")

        config_files = [
            ("android/build.gradle.kts", "Root build.gradle"),
            ("android/app/build.gradle.kts", "App build.gradle"),
            ("android/settings.gradle.kts", "Settings gradle"),
            ("android/app/src/main/AndroidManifest.xml", "Android Manifest"),
            ("android/app/proguard-rules.pro", "ProGuard rules"),
        ]

        for file_path, name in config_files:
            full_path = self.project_root / file_path
            if full_path.exists():
                self.passed += 1
            else:
                self.add_error(f"{name}: Configuration file missing")

        print(f"  [OK] Validated {len(config_files)} configuration files")
        print()

    def validate_architecture(self):
        """Validate architectural components"""
        print(" Validating Architecture...")

        components = {
            "Engine": ["JavaScriptEngine.kt", "ScriptManager.kt"],
            "Bridge": ["UIBridge.kt", "SystemBridge.kt"],
            "Security": ["PermissionManager.kt", "SignatureVerifier.kt"],
            "UI": ["MainActivity.kt", "ScriptEditorActivity.kt", "ScriptRuntimeActivity.kt"],
            "Models": ["Script.kt"],
        }

        for component, files in components.items():
            found = 0
            for file_name in files:
                file_path = self.project_root / f"android/app/src/main/java/com/scripthost"
                if list(file_path.rglob(file_name)):
                    found += 1

            if found == len(files):
                self.passed += 1
                print(f"  [OK] {component}: All files present ({found}/{len(files)})")
            else:
                self.add_warning(f"{component}: Missing files ({found}/{len(files)})")

        print()

    def add_error(self, message: str):
        """Add an error"""
        self.errors.append(message)
        self.failed += 1

    def add_warning(self, message: str):
        """Add a warning"""
        self.warnings.append(message)

    def print_summary(self):
        """Print validation summary"""
        print("=" * 70)
        print("Validation Summary")
        print("=" * 70)
        print()

        print(f"[OK] Passed: {self.passed}")
        print(f"[WARN]  Warnings: {len(self.warnings)}")
        print(f"[FAIL] Errors: {len(self.errors)}")
        print()

        if self.warnings:
            print("Warnings:")
            for warning in self.warnings[:10]:  # Show first 10
                print(f"  [WARN]  {warning}")
            if len(self.warnings) > 10:
                print(f"  ... and {len(self.warnings) - 10} more")
            print()

        if self.errors:
            print("Errors:")
            for error in self.errors:
                print(f"  [FAIL] {error}")
            print()

        if self.errors:
            print("[FAIL] VALIDATION FAILED")
            return False
        elif self.warnings:
            print("[WARN]  VALIDATION PASSED WITH WARNINGS")
            return True
        else:
            print("[OK] VALIDATION PASSED")
            return True

def main():
    project_root = str(Path(__file__).resolve().parent)
    validator = CodeValidator(project_root)
    success = validator.validate_all()

    exit(0 if success else 1)

if __name__ == "__main__":
    main()
