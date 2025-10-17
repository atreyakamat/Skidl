#!/usr/bin/env bash
set -euo pipefail

if [ ! -f keystore.properties ]; then
  echo "keystore.properties not found. Create it with storeFile, storePassword, keyAlias, and keyPassword." >&2
  exit 1
fi

chmod +x gradlew
./gradlew clean assembleRelease

echo "APK generated at app/build/outputs/apk/release/app-release.apk"
