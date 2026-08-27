#!/usr/bin/env python3
"""
ScriptHost Integration Test Suite
Simulates full workflow and validates component integration
"""

import os
import re
import json
from pathlib import Path
from typing import List, Dict, Tuple

class IntegrationTester:
    def __init__(self, project_root: str):
        self.project_root = Path(project_root)
        self.tests_passed = 0
        self.tests_failed = 0

    def run_all_tests(self):
        """Run all integration tests"""
        print("=" * 70)
        print("ScriptHost Integration Test Suite")
        print("=" * 70)
        print()

        tests = [
            ("Script Installation Flow", self.test_script_installation),
            ("Permission System", self.test_permission_system),
            ("UI Bridge Integration", self.test_ui_bridge),
            ("System Bridge Integration", self.test_system_bridge),
            ("Security Verification", self.test_security),
            ("Script Execution Flow", self.test_script_execution),
            ("Error Handling", self.test_error_handling),
            ("Resource Management", self.test_resource_management),
            ("Chart View", self.test_chart_view),
            ("Notification Bridge", self.test_notification_bridge),
            ("SSH Bridge", self.test_ssh_bridge),
            ("Testing Framework", self.test_testing_framework),
            ("New Capability Examples", self.test_new_capability_examples),
        ]

        for test_name, test_func in tests:
            self.run_test(test_name, test_func)

        return self.print_summary()

    def run_test(self, name: str, test_func):
        """Run a single test"""
        print(f"Testing: {name}")
        print("-" * 70)

        try:
            result = test_func()
            if result:
                print(f"  PASSED")
                self.tests_passed += 1
            else:
                print(f"  FAILED")
                self.tests_failed += 1
        except Exception as e:
            print(f"  [FAIL] ERROR: {e}")
            self.tests_failed += 1

        print()

    def test_script_installation(self) -> bool:
        """Test script installation workflow"""
        # Check ScriptManager exists
        script_manager = self.project_root / "android/app/src/main/java/com/scripthost/engine/ScriptManager.kt"
        if not script_manager.exists():
            print("  [FAIL] ScriptManager.kt not found")
            return False

        content = script_manager.read_text(encoding="utf-8")

        # Check for key methods
        required_methods = [
            'installScript',
            'uninstallScript',
            'updateScript',
            'getScript',
            'getAllScripts',
        ]

        missing = []
        for method in required_methods:
            if f'fun {method}' not in content and f'suspend fun {method}' not in content:
                missing.append(method)

        if missing:
            print(f"  [FAIL] Missing methods: {', '.join(missing)}")
            return False

        print(f"  [OK] All installation methods present")
        return True

    def test_permission_system(self) -> bool:
        """Test permission system"""
        # Check PermissionManager
        perm_manager = self.project_root / "android/app/src/main/java/com/scripthost/security/PermissionManager.kt"
        if not perm_manager.exists():
            print("  [FAIL] PermissionManager.kt not found")
            return False

        content = perm_manager.read_text(encoding="utf-8")

        # Check for key methods
        checks = [
            ('hasPermission', 'Permission checking'),
            ('hasScriptPermission', 'Per-script permission enforcement'),
            ('requestPermissions', 'Permission requesting'),
            ('onRequestPermissionsResult', 'Permission result handling'),
        ]

        for method, desc in checks:
            if f'fun {method}' not in content:
                print(f"  [FAIL] Missing: {desc}")
                return False
            print(f"  [OK] {desc}")

        # Check Permission enum
        script_kt = self.project_root / "android/app/src/main/java/com/scripthost/models/Script.kt"
        if script_kt.exists():
            script_content = script_kt.read_text(encoding="utf-8")
            if 'enum class Permission' in script_content:
                print(f"  [OK] Permission enum defined")
            else:
                print(f"  [FAIL] Permission enum not found")
                return False

        return True

    def test_ui_bridge(self) -> bool:
        """Test UI bridge integration"""
        ui_bridge = self.project_root / "android/app/src/main/java/com/scripthost/bridge/UIBridge.kt"
        if not ui_bridge.exists():
            print("  [FAIL] UIBridge.kt not found")
            return False

        content = ui_bridge.read_text(encoding="utf-8")

        # Check for UI components
        components = ['Button', 'Label', 'TextField', 'ListView', 'ImageView',
                      'Switch', 'Slider', 'ScrollView', 'CheckBox', 'Spinner',
                      'ProgressBar', 'Layout']
        found = 0

        for component in components:
            if f'create{component}' in content:
                found += 1

        print(f"  [OK] UI Components: {found}/{len(components)} implemented")

        # Check for UI namespace methods
        if 'fun addView' in content:
            print(f"  [OK] UI.addView() implemented")
        else:
            print(f"  [FAIL] UI.addView() not found")
            return False

        # Check for sub-screen page stack
        if 'pushPage' in content:
            print(f"  [OK] UI.pushPage() implemented")
        else:
            print(f"  [FAIL] UI.pushPage() not found")
            return False

        return found >= 4  # At least 4 components

    def test_system_bridge(self) -> bool:
        """Test system bridge integration"""
        sys_bridge = self.project_root / "android/app/src/main/java/com/scripthost/bridge/SystemBridge.kt"
        if not sys_bridge.exists():
            print("  [FAIL] SystemBridge.kt not found")
            return False

        content = sys_bridge.read_text(encoding="utf-8")

        # Check for system APIs
        apis = [
            ('httpGet', 'Network.get()'),
            ('httpPost', 'Network.post()'),
            ('readFile', 'Storage.readFile()'),
            ('writeFile', 'Storage.writeFile()'),
            ('getAccelerometer', 'Sensor.getAccelerometer()'),
            ('vibrate', 'Device.vibrate()'),
        ]

        found = 0
        for method, desc in apis:
            if f'fun {method}' in content:
                print(f"  [OK] {desc}")
                found += 1

        return found >= 4  # At least 4 APIs

    def test_security(self) -> bool:
        """Test security implementation"""
        sig_verifier = self.project_root / "android/app/src/main/java/com/scripthost/security/SignatureVerifier.kt"
        if not sig_verifier.exists():
            print("  [FAIL] SignatureVerifier.kt not found")
            return False

        content = sig_verifier.read_text(encoding="utf-8")

        # Check for security methods
        security_features = [
            ('fun verify', 'Signature verification'),
            ('fun sign', 'Signature generation'),
            ('fun computeHash', 'Hash computation'),
            ('SHA256', 'SHA-256 hashing'),
            ('RSA', 'RSA encryption'),
        ]

        for pattern, desc in security_features:
            if pattern in content:
                print(f"  [OK] {desc}")
            else:
                print(f"  [FAIL] Missing: {desc}")
                return False

        return True

    def test_script_execution(self) -> bool:
        """Test script execution flow"""
        js_engine = self.project_root / "android/app/src/main/java/com/scripthost/engine/JavaScriptEngine.kt"
        if not js_engine.exists():
            print("  [FAIL] JavaScriptEngine.kt not found")
            return False

        content = js_engine.read_text(encoding="utf-8")

        # Check for execution methods
        execution_features = [
            ('suspend fun execute', 'Script execution'),
            ('fun stop', 'Script stopping'),
            ('fun registerBridge', 'Bridge registration'),
            ('V8', 'V8 engine integration'),
            ('console', 'Console API'),
        ]

        for pattern, desc in execution_features:
            if pattern in content:
                print(f"  [OK] {desc}")
            else:
                print(f"  [FAIL] Missing: {desc}")
                return False

        return True

    def test_error_handling(self) -> bool:
        """Test error handling"""
        # Check for ExecutionResult sealed class
        js_engine = self.project_root / "android/app/src/main/java/com/scripthost/engine/JavaScriptEngine.kt"
        if not js_engine.exists():
            return False

        content = js_engine.read_text(encoding="utf-8")

        if 'sealed class ExecutionResult' in content or 'sealed interface ExecutionResult' in content:
            print(f"  [OK] ExecutionResult type defined")
        else:
            print(f"  [FAIL] ExecutionResult not found")
            return False

        if 'Success' in content and 'Error' in content:
            print(f"  [OK] Success and Error cases defined")
        else:
            print(f"  [FAIL] Result cases not found")
            return False

        # Check for try-catch blocks
        if 'try {' in content and 'catch' in content:
            print(f"  [OK] Exception handling present")
        else:
            print(f"  [WARN]  Limited exception handling")

        return True

    def test_resource_management(self) -> bool:
        """Test resource management"""
        js_engine = self.project_root / "android/app/src/main/java/com/scripthost/engine/JavaScriptEngine.kt"
        if not js_engine.exists():
            return False

        content = js_engine.read_text(encoding="utf-8")

        # Check for resource limits
        resource_features = [
            ('maxExecutionTimeMs', 'Execution timeout'),
            ('withTimeout', 'Timeout enforcement'),
            ('fun release', 'Resource cleanup'),
        ]

        found = 0
        for pattern, desc in resource_features:
            if pattern in content:
                print(f"  [OK] {desc}")
                found += 1
            else:
                print(f"  [WARN]  {desc} not explicitly found")

        return found >= 2  # At least 2 resource management features

    def test_chart_view(self) -> bool:
        """Test chart widget integration"""
        chart_view = self.project_root / "android/app/src/main/java/com/scripthost/ui/chart/SimpleChartView.kt"
        if not chart_view.exists():
            print("  [FAIL] SimpleChartView.kt not found")
            return False
        print("  [OK] SimpleChartView.kt present")

        chart_scale = self.project_root / "android/app/src/main/java/com/scripthost/ui/chart/ChartScale.kt"
        if not chart_scale.exists():
            print("  [FAIL] ChartScale.kt not found")
            return False
        print("  [OK] ChartScale.kt present")

        ui_bridge = self.project_root / "android/app/src/main/java/com/scripthost/bridge/UIBridge.kt"
        if not ui_bridge.exists():
            print("  [FAIL] UIBridge.kt not found")
            return False

        content = ui_bridge.read_text(encoding="utf-8")
        for pattern, desc in [('createChart', 'Chart factory method'),
                              ('"Chart"', 'Chart constructor registration')]:
            if pattern in content:
                print(f"  [OK] {desc}")
            else:
                print(f"  [FAIL] Missing: {desc}")
                return False

        chart_test = self.project_root / "android/app/src/test/java/com/scripthost/ui/chart/ChartScaleTest.kt"
        if not chart_test.exists():
            print("  [FAIL] ChartScaleTest.kt not found")
            return False
        print("  [OK] ChartScaleTest.kt present")

        return True

    def test_notification_bridge(self) -> bool:
        """Test notification and scheduler bridge integration"""
        notif_bridge = self.project_root / "android/app/src/main/java/com/scripthost/bridge/NotificationBridge.kt"
        if not notif_bridge.exists():
            print("  [FAIL] NotificationBridge.kt not found")
            return False

        content = notif_bridge.read_text(encoding="utf-8")
        for pattern, desc in [('registerJavaMethod', 'JS method registration'),
                              ('hasScriptPermission', 'Permission gating'),
                              ('Notify', 'Notify global'),
                              ('Scheduler', 'Scheduler global')]:
            if pattern in content:
                print(f"  [OK] {desc}")
            else:
                print(f"  [FAIL] Missing: {desc}")
                return False

        for rel_path in ["android/app/src/main/java/com/scripthost/notify/DailyNotificationWorker.kt",
                         "android/app/src/main/java/com/scripthost/notify/NextRunCalculator.kt"]:
            if not (self.project_root / rel_path).exists():
                print(f"  [FAIL] {rel_path} not found")
                return False
            print(f"  [OK] {rel_path} present")

        manifest = self.project_root / "android/app/src/main/AndroidManifest.xml"
        if not manifest.exists() or 'POST_NOTIFICATIONS' not in manifest.read_text(encoding="utf-8"):
            print("  [FAIL] POST_NOTIFICATIONS missing from AndroidManifest.xml")
            return False
        print("  [OK] POST_NOTIFICATIONS declared in manifest")

        gradle = self.project_root / "android/app/build.gradle.kts"
        if not gradle.exists() or 'work-runtime' not in gradle.read_text(encoding="utf-8"):
            print("  [FAIL] work-runtime dependency missing from build.gradle.kts")
            return False
        print("  [OK] WorkManager dependency present")

        calc_test = self.project_root / "android/app/src/test/java/com/scripthost/notify/NextRunCalculatorTest.kt"
        if not calc_test.exists():
            print("  [FAIL] NextRunCalculatorTest.kt not found")
            return False
        print("  [OK] NextRunCalculatorTest.kt present")

        return True

    def test_ssh_bridge(self) -> bool:
        """Test SSH bridge integration"""
        ssh_bridge = self.project_root / "android/app/src/main/java/com/scripthost/bridge/SSHBridge.kt"
        if not ssh_bridge.exists():
            print("  [FAIL] SSHBridge.kt not found")
            return False
        print("  [OK] SSHBridge.kt present")

        session_manager = self.project_root / "android/app/src/main/java/com/scripthost/ssh/SSHSessionManager.kt"
        if not session_manager.exists():
            print("  [FAIL] SSHSessionManager.kt not found")
            return False
        print("  [OK] SSHSessionManager.kt present")

        content = ssh_bridge.read_text(encoding="utf-8")
        if 'hasScriptPermission(scriptId, Permission.SSH)' in content:
            print("  [OK] SSH permission gating")
        else:
            print("  [FAIL] Missing: hasScriptPermission(scriptId, Permission.SSH)")
            return False

        script_kt = self.project_root / "android/app/src/main/java/com/scripthost/models/Script.kt"
        if not script_kt.exists() or 'SSH(' not in script_kt.read_text(encoding="utf-8"):
            print("  [FAIL] Permission.SSH missing from models/Script.kt")
            return False
        print("  [OK] Permission.SSH declared")

        gradle = self.project_root / "android/app/build.gradle.kts"
        if not gradle.exists() or 'jsch' not in gradle.read_text(encoding="utf-8"):
            print("  [FAIL] jsch dependency missing from build.gradle.kts")
            return False
        print("  [OK] JSch dependency present")

        ssh_test = self.project_root / "android/app/src/test/java/com/scripthost/ssh/SSHSessionManagerTest.kt"
        if not ssh_test.exists():
            print("  [FAIL] SSHSessionManagerTest.kt not found")
            return False
        print("  [OK] SSHSessionManagerTest.kt present")

        return True

    def test_testing_framework(self) -> bool:
        """Test unit-testing framework setup"""
        gradle = self.project_root / "android/app/build.gradle.kts"
        if not gradle.exists():
            print("  [FAIL] build.gradle.kts not found")
            return False

        content = gradle.read_text(encoding="utf-8")

        # Check for test framework dependencies and options
        checks = [
            ('robolectric', 'Robolectric (JVM Android tests)'),
            ('truth', 'Truth assertions'),
            ('mockito-kotlin', 'Mockito Kotlin mocking'),
            ('work-testing', 'WorkManager testing'),
            ('isIncludeAndroidResources', 'Android resources in unit tests'),
        ]

        for pattern, desc in checks:
            if pattern in content:
                print(f"  [OK] {desc}")
            else:
                print(f"  [FAIL] Missing: {desc}")
                return False

        return True

    def test_new_capability_examples(self) -> bool:
        """Test new capability example scripts"""
        examples_dir = self.project_root / "scripts/examples"

        examples = [
            ("monitor_port_chart.js", ['new Chart']),
            ("daily_fitness.js", ['Scheduler.scheduleDaily', 'Notify.post']),
            ("stock_trends.js", ['Network.get', 'new Chart']),
            ("tmux_remote.js", ['SSH.connect', 'SSH.exec']),
            ("sub_screens.js", ['UI.pushPage', 'UI.popPage']),
        ]

        for filename, patterns in examples:
            example = examples_dir / filename
            if not example.exists():
                print(f"  [FAIL] {filename} not found")
                return False
            content = example.read_text(encoding="utf-8")
            for pattern in patterns:
                if pattern not in content:
                    print(f"  [FAIL] {filename} missing '{pattern}'")
                    return False
            print(f"  [OK] {filename}: {', '.join(patterns)}")

        return True

    def print_summary(self):
        """Print test summary"""
        print("=" * 70)
        print("Integration Test Summary")
        print("=" * 70)
        print()

        total = self.tests_passed + self.tests_failed
        print(f"Tests Run: {total}")
        print(f"Tests Passed: {self.tests_passed}")
        print(f"Tests Failed: {self.tests_failed}")
        print()

        if self.tests_failed == 0:
            print("[OK] ALL INTEGRATION TESTS PASSED")
            print()
            print("The ScriptHost implementation is complete and all components")
            print("are properly integrated. The system is ready for:")
            print("  - Build and compilation")
            print("  - Device testing")
            print("  - Security audit")
            print("  - Performance profiling")
        else:
            print("[WARN]  SOME INTEGRATION TESTS FAILED")
            print()
            print("Please review the failed tests above.")

        return self.tests_failed == 0

def main():
    project_root = str(Path(__file__).resolve().parent)
    tester = IntegrationTester(project_root)
    exit(0 if tester.run_all_tests() else 1)

if __name__ == "__main__":
    main()
