# Developer Setup & Build Instructions

## Prerequisites

| Tool | Version | Notes |
|------|---------|-------|
| JDK | 17+ | `temurin` recommended |
| Android SDK | 34 | Compile & target SDK |
| Android Build Tools | 34.0.0 | |
| Gradle | 8.5 | Bundled via `gradlew` wrapper |
| Kotlin | 1.9.22 | Managed by Gradle plugin |

## Environment Setup

### 1. Install JDK 17
```bash
# macOS (Homebrew)
brew install openjdk@17

# Ubuntu/Debian
sudo apt install openjdk-17-jdk

# Windows (Scoop)
scoop install temurin17-jdk
```

### 2. Install Android SDK
Option A: Install via **Android Studio** (recommended)
- Download from https://developer.android.com/studio
- Open SDK Manager → install SDK 34, Build Tools 34.0.0

Option B: Command-line tools only
```bash
# Download command-line tools from https://developer.android.com/studio#cmdline-tools
sdkmanager "platforms;android-34" "build-tools;34.0.0"
```

### 3. Set ANDROID_HOME
```bash
# Linux/macOS (add to ~/.bashrc or ~/.zshrc)
export ANDROID_HOME=$HOME/Android/Sdk

# Windows (PowerShell)
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
# Or create local.properties in project root:
# sdk.dir=C:\\Users\\YourName\\AppData\\Local\\Android\\Sdk
```

### 4. Accept Licenses
```bash
$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager --licenses
```

## Building

### Debug APK (no signing)
```bash
./gradlew assembleDebug
# Output: app/build/outputs/apk/debug/app-debug.apk
```

### Release APK (signed)

1. Generate a keystore:
```bash
keytool -genkeypair -v \
  -keystore keystore/release-keystore.jks \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias hotspot_skribble_key
```

2. Create `keystore.properties` (root of project, **not committed to git**):
```properties
storeFile=keystore/release-keystore.jks
storePassword=your-store-password
keyAlias=hotspot_skribble_key
keyPassword=your-key-password
```

3. Build:
```bash
./make_apk.sh release
# Output: app/build/outputs/apk/release/app-release.apk
```

### Using the build script
```bash
./make_apk.sh          # Debug build with tests
./make_apk.sh release  # Release build with tests + signing
```

## Running Tests

```bash
# Unit tests only
./gradlew testDebugUnitTest

# All tests with reports
./gradlew test

# Instrumented tests (requires connected device/emulator)
./gradlew connectedAndroidTest
```

Test reports are generated at: `app/build/reports/tests/`

## IDE Setup (Android Studio)

1. Open the project root folder in Android Studio
2. Let Gradle sync complete
3. Select `app` module configuration
4. Run on device/emulator (API 26+)

### Recommended plugins
- Kotlin
- Compose Multiplatform (for preview support)

## Troubleshooting

| Issue | Solution |
|-------|----------|
| `SDK location not found` | Set `ANDROID_HOME` env var or create `local.properties` |
| `License not accepted` | Run `sdkmanager --licenses` |
| Build OOM | Increase in `gradle.properties`: `org.gradle.jvmargs=-Xmx4g` |
| Compose preview not working | Ensure Android Studio Giraffe+ with Compose plugin |
