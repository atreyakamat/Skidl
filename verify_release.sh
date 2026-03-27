#!/usr/bin/env bash
# ═══════════════════════════════════════════════════════════════════
# Hotspot Skribble - Pre-Release Verification Script
# ═══════════════════════════════════════════════════════════════════
# This script performs all checks from the RELEASE_CHECKLIST.md
# Run this before creating any release builds.
#
# Usage: ./verify_release.sh
# ═══════════════════════════════════════════════════════════════════

set -euo pipefail

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Counters
PASSED=0
FAILED=0
WARNINGS=0

# Print header
echo -e "${BLUE}════════════════════════════════════════════════════════${NC}"
echo -e "${BLUE}   Hotspot Skribble - Pre-Release Verification${NC}"
echo -e "${BLUE}════════════════════════════════════════════════════════${NC}"
echo ""

# Helper functions
check_pass() {
    echo -e "${GREEN}✓${NC} $1"
    ((PASSED++))
}

check_fail() {
    echo -e "${RED}✗${NC} $1"
    ((FAILED++))
}

check_warn() {
    echo -e "${YELLOW}⚠${NC} $1"
    ((WARNINGS++))
}

section() {
    echo ""
    echo -e "${BLUE}▶ $1${NC}"
    echo "─────────────────────────────────────────────────────────"
}

# ═══════════════════════════════════════════════════════════════════
# 1. PRE-BUILD CHECKS
# ═══════════════════════════════════════════════════════════════════
section "Pre-Build Checks"

# Check git status
if [[ -n $(git status -s) ]]; then
    check_warn "Working directory has uncommitted changes"
    git status -s
else
    check_pass "Working directory is clean"
fi

# Check current branch
CURRENT_BRANCH=$(git rev-parse --abbrev-ref HEAD)
echo "   Current branch: $CURRENT_BRANCH"

# Check version in build.gradle.kts
if grep -q 'versionCode = 1' app/build.gradle.kts && grep -q 'versionName = "1.0.0"' app/build.gradle.kts; then
    check_pass "Version set to 1.0.0 (versionCode 1)"
else
    check_warn "Version may not be set correctly"
fi

# ═══════════════════════════════════════════════════════════════════
# 2. ENVIRONMENT CHECKS
# ═══════════════════════════════════════════════════════════════════
section "Environment Setup"

# Check ANDROID_HOME
if [[ -n "${ANDROID_HOME:-}" ]] && [[ -d "$ANDROID_HOME" ]]; then
    check_pass "ANDROID_HOME is set: $ANDROID_HOME"
else
    check_fail "ANDROID_HOME is not set or doesn't exist"
fi

# Check Java version
JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
if [[ "$JAVA_VERSION" -ge 17 ]]; then
    check_pass "Java version $JAVA_VERSION (requires 17+)"
else
    check_fail "Java version $JAVA_VERSION is too old (requires 17+)"
fi

# Check for gradlew
if [[ -x ./gradlew ]]; then
    check_pass "Gradle wrapper is executable"
else
    check_warn "Gradle wrapper may not be executable (run: chmod +x gradlew)"
fi

# ═══════════════════════════════════════════════════════════════════
# 3. PROJECT STRUCTURE CHECKS
# ═══════════════════════════════════════════════════════════════════
section "Project Structure"

# Check critical files
CRITICAL_FILES=(
    "app/src/main/AndroidManifest.xml"
    "app/src/main/assets/words.json"
    "app/src/main/res/values/strings.xml"
    "app/proguard-rules.pro"
    "app/build.gradle.kts"
    "build.gradle.kts"
    "settings.gradle.kts"
)

for file in "${CRITICAL_FILES[@]}"; do
    if [[ -f "$file" ]]; then
        check_pass "Found: $file"
    else
        check_fail "Missing: $file"
    fi
done

# Count Kotlin files
KT_COUNT=$(find app/src/main/java -name "*.kt" 2>/dev/null | wc -l)
if [[ $KT_COUNT -gt 0 ]]; then
    check_pass "Found $KT_COUNT Kotlin source files"
else
    check_fail "No Kotlin source files found"
fi

# Check test files
TEST_COUNT=$(find app/src/test -name "*.kt" 2>/dev/null | wc -l)
if [[ $TEST_COUNT -gt 0 ]]; then
    check_pass "Found $TEST_COUNT test files"
else
    check_warn "No test files found"
fi

# ═══════════════════════════════════════════════════════════════════
# 4. SIGNING SETUP CHECKS
# ═══════════════════════════════════════════════════════════════════
section "Release Signing Setup"

if [[ -f "keystore.properties" ]]; then
    check_pass "keystore.properties exists"

    # Verify it has required properties
    if grep -q "storeFile" keystore.properties && \
       grep -q "storePassword" keystore.properties && \
       grep -q "keyAlias" keystore.properties && \
       grep -q "keyPassword" keystore.properties; then
        check_pass "keystore.properties has all required fields"
    else
        check_fail "keystore.properties is missing required fields"
    fi

    # Check if keystore file exists
    KEYSTORE_FILE=$(grep "storeFile=" keystore.properties | cut -d'=' -f2)
    if [[ -f "$KEYSTORE_FILE" ]]; then
        check_pass "Keystore file exists: $KEYSTORE_FILE"
    else
        check_fail "Keystore file not found: $KEYSTORE_FILE"
    fi
else
    check_warn "keystore.properties not found (required for release builds)"
fi

# ═══════════════════════════════════════════════════════════════════
# 5. COMPILATION CHECK
# ═══════════════════════════════════════════════════════════════════
section "Compilation Check"

echo "   Compiling Kotlin code..."
if ./gradlew compileDebugKotlin --no-daemon --quiet 2>&1 | tee /tmp/compile_output.txt; then
    check_pass "Kotlin compilation successful"
else
    check_fail "Kotlin compilation failed"
    echo "   Check /tmp/compile_output.txt for details"
fi

# ═══════════════════════════════════════════════════════════════════
# 6. LINT CHECKS
# ═══════════════════════════════════════════════════════════════════
section "Lint Analysis"

echo "   Running lint checks..."
if ./gradlew lintDebug --no-daemon --quiet 2>&1 | tee /tmp/lint_output.txt; then
    check_pass "Lint checks passed"

    # Check for lint report
    if [[ -f "app/build/reports/lint-results-debug.html" ]]; then
        echo "   Lint report: app/build/reports/lint-results-debug.html"
    fi
else
    check_warn "Lint checks found issues"
    echo "   Check /tmp/lint_output.txt for details"
fi

# ═══════════════════════════════════════════════════════════════════
# 7. UNIT TESTS
# ═══════════════════════════════════════════════════════════════════
section "Unit Tests"

echo "   Running unit tests..."
if ./gradlew testDebugUnitTest --no-daemon --quiet 2>&1 | tee /tmp/test_output.txt; then
    check_pass "All unit tests passed"

    # Check test report
    if [[ -f "app/build/reports/tests/testDebugUnitTest/index.html" ]]; then
        echo "   Test report: app/build/reports/tests/testDebugUnitTest/index.html"
    fi
else
    check_fail "Some unit tests failed"
    echo "   Check /tmp/test_output.txt for details"
fi

# ═══════════════════════════════════════════════════════════════════
# 8. PROGUARD RULES CHECK
# ═══════════════════════════════════════════════════════════════════
section "ProGuard Configuration"

if [[ -f "app/proguard-rules.pro" ]]; then
    check_pass "ProGuard rules file exists"

    # Check for critical keep rules
    if grep -q "kotlinx.serialization" app/proguard-rules.pro; then
        check_pass "kotlinx.serialization rules present"
    else
        check_warn "kotlinx.serialization rules may be missing"
    fi

    if grep -q "WebSocket" app/proguard-rules.pro || grep -q "OkHttp" app/proguard-rules.pro; then
        check_pass "Networking library rules present"
    else
        check_warn "Networking library rules may be missing"
    fi
else
    check_fail "ProGuard rules file not found"
fi

# ═══════════════════════════════════════════════════════════════════
# 9. DOCUMENTATION CHECK
# ═══════════════════════════════════════════════════════════════════
section "Documentation"

DOCS=(
    "README.md"
    "ARCHITECTURE.md"
    "NETWORK_PROTOCOL.md"
    "QA_REPORT.md"
    "KNOWN_LIMITATIONS.md"
    "RELEASE_CHECKLIST.md"
    "INSTALL.md"
    "SIDELOAD_GUIDE.md"
)

for doc in "${DOCS[@]}"; do
    if [[ -f "$doc" ]]; then
        check_pass "$doc present"
    else
        check_warn "$doc missing"
    fi
done

# ═══════════════════════════════════════════════════════════════════
# SUMMARY
# ═══════════════════════════════════════════════════════════════════
echo ""
echo -e "${BLUE}════════════════════════════════════════════════════════${NC}"
echo -e "${BLUE}   Verification Summary${NC}"
echo -e "${BLUE}════════════════════════════════════════════════════════${NC}"
echo ""
echo -e "${GREEN}   Passed:   $PASSED${NC}"
echo -e "${YELLOW}   Warnings: $WARNINGS${NC}"
echo -e "${RED}   Failed:   $FAILED${NC}"
echo ""

if [[ $FAILED -eq 0 ]]; then
    echo -e "${GREEN}✓ Project is ready for release build!${NC}"
    echo ""
    echo "Next steps:"
    echo "  1. Build debug APK:   ./make_apk.sh"
    echo "  2. Build release APK: ./make_apk.sh release"
    echo "  3. Test the APK on a device"
    echo "  4. Follow RELEASE_CHECKLIST.md for final verification"
    exit 0
else
    echo -e "${RED}✗ Project has $FAILED critical issues${NC}"
    echo ""
    echo "Please fix the issues above before creating a release."
    exit 1
fi
