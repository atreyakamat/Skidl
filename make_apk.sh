#!/usr/bin/env bash
# ──────────────────────────────────────────────
# Hotspot Skribble – Build Script
# Builds debug or release APK
# Usage:
#   ./make_apk.sh          → debug APK
#   ./make_apk.sh release  → release APK (requires keystore.properties)
# ──────────────────────────────────────────────
set -euo pipefail

BUILD_TYPE="${1:-debug}"
echo "🎨 Hotspot Skribble – Building $BUILD_TYPE APK..."

chmod +x gradlew

if [ "$BUILD_TYPE" = "release" ]; then
    if [ ! -f keystore.properties ]; then
        echo "❌ keystore.properties not found."
        echo "   Create it with: storeFile, storePassword, keyAlias, keyPassword"
        exit 1
    fi
    echo "🔑 Release signing config found"
    ./gradlew clean testDebugUnitTest assembleRelease
    APK_PATH="app/build/outputs/apk/release/"
else
    ./gradlew clean testDebugUnitTest assembleDebug
    APK_PATH="app/build/outputs/apk/debug/"
fi

echo ""
echo "✅ Build complete!"
echo "📦 APK location: $APK_PATH"
echo ""
ls -la "$APK_PATH"*.apk 2>/dev/null || echo "   (APK files listed above)"

echo "APK generated at app/build/outputs/apk/release/app-release.apk"
