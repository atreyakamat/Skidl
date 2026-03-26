#!/usr/bin/env sh
set -e
SCRIPT_DIR=$(CDPATH= cd -- "$(dirname "$0")" && pwd)
GRADLE_VERSION=8.5
DIST_NAME="gradle-${GRADLE_VERSION}"
DIST_DIR="$SCRIPT_DIR/gradle/${DIST_NAME}"
GRADLE_BIN="$DIST_DIR/bin/gradle"

download_gradle() {
  local url="https://services.gradle.org/distributions/${DIST_NAME}-bin.zip"
  local zip_path="$SCRIPT_DIR/gradle/${DIST_NAME}.zip"
  mkdir -p "$SCRIPT_DIR/gradle"
  if command -v curl >/dev/null 2>&1; then
    curl -L "$url" -o "$zip_path"
  elif command -v wget >/dev/null 2>&1; then
    wget "$url" -O "$zip_path"
  else
    echo "Either curl or wget is required to download Gradle." >&2
    exit 1
  fi
  unzip -q "$zip_path" -d "$SCRIPT_DIR/gradle"
  rm -f "$zip_path"
}

if [ ! -x "$GRADLE_BIN" ]; then
  download_gradle
fi

exec "$GRADLE_BIN" "$@"
