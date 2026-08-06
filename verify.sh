#!/bin/bash

# ScriptHost Project Verification Script
# Verifies that all components are in place

echo "========================================="
echo "ScriptHost Project Verification"
echo "========================================="
echo ""

# Color codes
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Counters
PASSED=0
FAILED=0

# Check function
check_file() {
    if [ -f "$1" ]; then
        echo -e "${GREEN}✓${NC} $2"
        ((PASSED++))
    else
        echo -e "${RED}✗${NC} $2 (missing: $1)"
        ((FAILED++))
    fi
}

check_dir() {
    if [ -d "$1" ]; then
        echo -e "${GREEN}✓${NC} $2"
        ((PASSED++))
    else
        echo -e "${RED}✗${NC} $2 (missing: $1)"
        ((FAILED++))
    fi
}

echo "Checking Core Files..."
check_file "README.md" "Project README"
check_file "LICENSE" "License file"
check_file "CONTRIBUTING.md" "Contributing guidelines"
check_file "CHANGELOG.md" "Changelog"
check_file "BUILD.md" "Build instructions"
check_file "PROJECT_SUMMARY.md" "Project summary"
echo ""

echo "Checking Android Project..."
check_dir "android" "Android directory"
check_file "android/build.gradle.kts" "Root build.gradle"
check_file "android/settings.gradle.kts" "Settings gradle"
check_file "android/app/build.gradle.kts" "App build.gradle"
check_file "android/app/proguard-rules.pro" "ProGuard rules"
check_file "android/app/src/main/AndroidManifest.xml" "Android manifest"
echo ""

echo "Checking Build Tooling..."
check_file "android/gradlew" "Gradle wrapper (Unix)"
check_file "android/gradlew.bat" "Gradle wrapper (Windows)"
check_file "android/gradle/wrapper/gradle-wrapper.jar" "Gradle wrapper jar"
check_file "android/gradle/wrapper/gradle-wrapper.properties" "Gradle wrapper properties"
echo ""

echo "Checking Source Code..."
check_file "android/app/src/main/java/com/scripthost/ScriptHostApplication.kt" "Application class"
check_file "android/app/src/main/java/com/scripthost/models/Script.kt" "Data models"
check_file "android/app/src/main/java/com/scripthost/engine/JavaScriptEngine.kt" "JavaScript engine"
check_file "android/app/src/main/java/com/scripthost/engine/ScriptManager.kt" "Script manager"
check_file "android/app/src/main/java/com/scripthost/bridge/UIBridge.kt" "UI bridge"
check_file "android/app/src/main/java/com/scripthost/bridge/SystemBridge.kt" "System bridge"
check_file "android/app/src/main/java/com/scripthost/security/PermissionManager.kt" "Permission manager"
check_file "android/app/src/main/java/com/scripthost/security/SignatureVerifier.kt" "Signature verifier"
check_file "android/app/src/main/java/com/scripthost/ui/MainActivity.kt" "Main activity"
check_file "android/app/src/main/java/com/scripthost/ui/ScriptEditorActivity.kt" "Script editor"
check_file "android/app/src/main/java/com/scripthost/ui/ScriptRuntimeActivity.kt" "Script runtime"
echo ""

echo "Checking Resources..."
check_file "android/app/src/main/res/values/strings.xml" "Strings"
check_file "android/app/src/main/res/values/themes.xml" "Themes"
check_file "android/app/src/main/res/values/dimens.xml" "Dimensions"
check_file "android/app/src/main/res/xml/backup_rules.xml" "Backup rules"
check_file "android/app/src/main/res/xml/data_extraction_rules.xml" "Data extraction rules"
echo ""

echo "Checking Documentation..."
check_file "docs/API.md" "API documentation"
check_file "docs/SECURITY.md" "Security documentation"
check_file "docs/EXAMPLES.md" "Examples documentation"
echo ""

echo "Checking Example Scripts..."
check_file "scripts/examples/hello_world.js" "Hello World example"
check_file "scripts/examples/counter.js" "Counter example"
check_file "scripts/examples/todo_list.js" "Todo List example"
check_file "scripts/examples/network_request.js" "Network Request example"
check_file "scripts/examples/sensors.js" "Sensors example"
check_file "scripts/examples/storage.js" "Storage example"
echo ""

echo "Checking Tests..."
check_file "tests/README.md" "Test documentation"
check_file "android/app/src/test/java/com/scripthost/engine/ScriptManagerTest.kt" "Script manager tests"
check_file "android/app/src/test/java/com/scripthost/security/SignatureVerifierTest.kt" "Signature verifier tests"
echo ""

echo "========================================="
echo "Verification Complete"
echo "========================================="
echo -e "${GREEN}Passed: $PASSED${NC}"
if [ $FAILED -gt 0 ]; then
    echo -e "${RED}Failed: $FAILED${NC}"
    exit 1
else
    echo -e "${GREEN}All checks passed!${NC}"
    echo ""
    echo "Project is ready for:"
    echo "  1. Build: cd android && ./gradlew assembleDebug"
    echo "  2. Test: cd android && ./gradlew test"
    echo "  3. Install: cd android && ./gradlew installDebug"
    echo ""
    echo "Next steps:"
    echo "  - Review documentation in docs/"
    echo "  - Try example scripts in scripts/examples/"
    echo "  - Read BUILD.md for build instructions"
    echo "  - Read CONTRIBUTING.md to contribute"
    exit 0
fi
