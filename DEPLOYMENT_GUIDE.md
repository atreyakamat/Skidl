# Deployment Guide — Hotspot Skribble v1.0

> **Audience:** Deployment team, DevOps, release engineers  
> **Last updated:** February 2026

---

## Table of Contents

1. [Prerequisites](#1-prerequisites)
2. [Environment Setup](#2-environment-setup)
3. [Build the APK](#3-build-the-apk)
4. [Sign the Release APK](#4-sign-the-release-apk)
5. [Test the Build](#5-test-the-build)
6. [Deploy via Sideloading](#6-deploy-via-sideloading)
7. [Deploy via Google Play](#7-deploy-via-google-play)
8. [Deploy Web Client](#8-deploy-web-client)
9. [CI/CD Pipeline](#9-cicd-pipeline)
10. [Rollback Procedure](#10-rollback-procedure)
11. [Monitoring & Crash Reporting](#11-monitoring--crash-reporting)
12. [Troubleshooting](#12-troubleshooting)

---

## 1. Prerequisites

| Tool | Version | Purpose |
|------|---------|---------|
| JDK | 17+ | Kotlin compilation |
| Android SDK | 34 | Compile target |
| Android Build-Tools | 34.0.0 | APK packaging |
| Gradle | 8.5 | Build system (bundled via `gradlew`) |
| `adb` | Latest | Device install & testing |
| `keytool` | (JDK bundled) | Keystore generation |
| `apksigner` | (Build-Tools bundled) | APK signing verification |
| Git | 2.x+ | Source control |

### Minimum Device Requirements

| Spec | Requirement |
|------|-------------|
| Android | 8.0+ (API 26) |
| RAM | 2 GB+ |
| Storage | 50 MB free |
| Wi-Fi | Must support hotspot creation (host) or hotspot connection (clients) |

---

## 2. Environment Setup

### 2.1 Install JDK 17

```bash
# macOS (Homebrew)
brew install openjdk@17

# Ubuntu/Debian
sudo apt install openjdk-17-jdk

# Windows — download from https://adoptium.net/
# Add JAVA_HOME to environment variables
```

### 2.2 Install Android SDK

```bash
# Option A: Install Android Studio (recommended for first setup)
# Download from https://developer.android.com/studio

# Option B: Command-line tools only
# Download from https://developer.android.com/studio#command-tools
mkdir -p ~/Android/Sdk/cmdline-tools
unzip commandlinetools-*.zip -d ~/Android/Sdk/cmdline-tools/latest
```

### 2.3 Set Environment Variables

```bash
# Linux/macOS — add to ~/.bashrc or ~/.zshrc
export ANDROID_HOME=$HOME/Android/Sdk
export PATH=$PATH:$ANDROID_HOME/platform-tools:$ANDROID_HOME/cmdline-tools/latest/bin

# Windows — System Properties > Environment Variables
# ANDROID_HOME = C:\Users\<user>\AppData\Local\Android\Sdk
# PATH += %ANDROID_HOME%\platform-tools;%ANDROID_HOME%\cmdline-tools\latest\bin
```

### 2.4 Accept SDK Licenses

```bash
sdkmanager --licenses
sdkmanager "platforms;android-34" "build-tools;34.0.0"
```

### 2.5 Clone Repository

```bash
git clone https://github.com/atreyakamat/Skidl.git
cd Skidl
```

---

## 3. Build the APK

### 3.1 Debug Build (for testing)

```bash
# Linux/macOS
./gradlew assembleDebug

# Windows
gradlew.bat assembleDebug
```

**Output:** `app/build/outputs/apk/debug/app-debug.apk`

### 3.2 Release Build (for distribution)

```bash
# Linux/macOS
./gradlew assembleRelease

# Windows
gradlew.bat assembleRelease
```

**Output:** `app/build/outputs/apk/release/app-release.apk`

> **Note:** Release build requires a signing configuration. See Section 4.

### 3.3 Build with Tests

```bash
# Run all unit tests, then build
./gradlew testDebugUnitTest assembleDebug

# Run lint + tests + build
./gradlew lintDebug testDebugUnitTest assembleDebug
```

### 3.4 Using the Build Script

```bash
chmod +x make_apk.sh

# Debug build
./make_apk.sh debug

# Release build
./make_apk.sh release
```

### 3.5 Clean Build (if caching issues)

```bash
./gradlew clean assembleRelease
```

---

## 4. Sign the Release APK

### 4.1 Generate Keystore (first time only)

```bash
keytool -genkey -v \
  -keystore keystore/release.jks \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -alias hotspot-skribble \
  -storepass <STORE_PASSWORD> \
  -keypass <KEY_PASSWORD> \
  -dname "CN=Hotspot Skribble, OU=Mobile, O=Skidl, L=City, ST=State, C=US"
```

> **CRITICAL:** Back up `keystore/release.jks` and passwords securely. Losing the keystore means you cannot update the app on Google Play.

### 4.2 Configure Signing

Create `keystore.properties` in the project root (DO NOT commit this file):

```properties
storeFile=keystore/release.jks
storePassword=<STORE_PASSWORD>
keyAlias=hotspot-skribble
keyPassword=<KEY_PASSWORD>
```

Add to `.gitignore`:
```
keystore.properties
keystore/release.jks
```

### 4.3 Verify Signature

```bash
# Using apksigner (from Android Build-Tools)
apksigner verify --verbose app/build/outputs/apk/release/app-release.apk

# Using jarsigner
jarsigner -verify -verbose -certs app/build/outputs/apk/release/app-release.apk
```

---

## 5. Test the Build

### 5.1 Automated Tests

```bash
# Unit tests (60 tests across 4 suites)
./gradlew testDebugUnitTest

# View report
open app/build/reports/tests/testDebugUnitTest/index.html
```

| Suite | Tests | What it covers |
|-------|-------|---------------|
| ScoreManagerTest | 10 | Scoring logic, accumulation, reset |
| SkidlMessageSerializerTest | 22 | All 20 message types + error cases |
| GameControllerTest | 20 | Lobby, rounds, strokes, guesses |
| ModelTest | 8 | Data class defaults and edge cases |

### 5.2 Install on Device

```bash
# Debug APK (over USB)
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Release APK
adb install -r app/build/outputs/apk/release/app-release.apk

# Install to specific device (when multiple connected)
adb -s <DEVICE_SERIAL> install -r app/build/outputs/apk/debug/app-debug.apk
```

### 5.3 Smoke Test Checklist

| # | Test | Expected |
|---|------|----------|
| 1 | App launches | Welcome screen with goofy theme (yellow/coral) |
| 2 | Host → setup → start hosting | Lobby screen, host in player list |
| 3 | 2nd device joins via discovery | Joiner appears in both lobbies |
| 4 | Start round | Drawer sees word + canvas, guesser sees canvas + input |
| 5 | Draw strokes | Strokes appear on guesser device <100ms |
| 6 | Correct guess | Score updates, toast/highlight |
| 7 | Timer expires | Round ends, word revealed |
| 8 | Final round ends | Game Over screen with medals |
| 9 | Back to Lobby | Players still connected |
| 10 | 30min continuous play | No crashes, stable memory |

### 5.4 ProGuard Verification (Release Only)

After installing the release APK, verify:
- App launches without crash
- All screens render correctly
- WebSocket connections work
- JSON messages are sent/received
- QR code generates properly

If ProGuard strips too aggressively, check `app/proguard-rules.pro` and add missing keep rules.

---

## 6. Deploy via Sideloading

### 6.1 Direct APK Distribution

1. Build the signed release APK (Section 3.2 + 4)
2. Distribute `app-release.apk` via:
   - File share (Google Drive, Dropbox, etc.)
   - Direct transfer (USB, AirDrop equivalent)
   - Local web server (see Section 8)
   - QR code linking to download URL

### 6.2 Install from APK

Users must enable "Install from unknown sources":
- **Android 8-11:** Settings → Apps → Special access → Install unknown apps → Allow for browser/file manager
- **Android 12+:** Prompted automatically on install attempt

### 6.3 APK Hosting on Internal Server

```bash
# Quick Python HTTP server for LAN distribution
cd app/build/outputs/apk/release/
python3 -m http.server 8080

# Users navigate to http://<YOUR_IP>:8080/app-release.apk
```

---

## 7. Deploy via Google Play

### 7.1 Prerequisites

- Google Play Developer account ($25 one-time fee)
- Signed release APK or AAB (App Bundle)
- App icon, screenshots, feature graphic
- Privacy policy URL
- Content rating questionnaire completed

### 7.2 Build App Bundle (preferred for Play Store)

```bash
./gradlew bundleRelease
```

**Output:** `app/build/outputs/bundle/release/app-release.aab`

### 7.3 Upload to Play Console

1. Go to [play.google.com/console](https://play.google.com/console)
2. Create new app → Fill in store listing
3. Upload AAB to Production (or Internal Testing first)
4. Complete content rating, pricing, and distribution
5. Submit for review

### 7.4 Recommended Release Track Order

| Track | Purpose | Audience |
|-------|---------|----------|
| Internal Testing | Team validation | Up to 100 testers |
| Closed Testing | Beta feedback | Invited testers |
| Open Testing | Public beta | Anyone can opt in |
| Production | Full release | Everyone |

---

## 8. Deploy Web Client

The web client lives in `/web/` and allows browser-based players to join games hosted by an Android device.

### 8.1 Static File Hosting

The web client is purely static (HTML + CSS + JS). No build step required.

```bash
# Serve locally
cd web/
python3 -m http.server 3000
# Open http://localhost:3000
```

### 8.2 Deploy to Netlify

```bash
# Install Netlify CLI
npm install -g netlify-cli

# Deploy
cd web/
netlify deploy --prod --dir=.
```

### 8.3 Deploy to GitHub Pages

1. Push `web/` contents to `gh-pages` branch, or
2. Configure GitHub Pages in repo Settings → Pages → Source: `main` branch, `/web` folder

### 8.4 Deploy to Vercel

```bash
cd web/
npx vercel --prod
```

### 8.5 Deploy to any Static Host

Upload the contents of `/web/` to any static file hosting:
- AWS S3 + CloudFront
- Firebase Hosting
- Cloudflare Pages
- Nginx / Apache

No server-side processing required.

---

## 9. CI/CD Pipeline

### 9.1 GitHub Actions (preconfigured)

The repository includes `.github/workflows/build.yml` which runs:

| Job | Trigger | Steps |
|-----|---------|-------|
| `lint-and-test` | All pushes + PRs | Lint → Unit tests |
| `build-debug` | After lint passes | Debug APK → Upload artifact |
| `build-release` | `main` branch only | Release APK → Upload artifact |

### 9.2 Download CI Artifacts

1. Go to Actions tab in GitHub
2. Select the latest successful workflow run
3. Download `debug-apk` or `release-apk` artifact

### 9.3 Triggering a Release Build

```bash
git checkout main
git tag v1.0.0
git push origin v1.0.0
# CI will build release APK automatically
```

---

## 10. Rollback Procedure

### 10.1 Sideloading Rollback

1. Locate the previous APK version from CI artifacts or local backup
2. Uninstall current version: `adb uninstall com.skidl`
3. Install previous version: `adb install previous-release.apk`

### 10.2 Google Play Rollback

1. Go to Play Console → Release Management → App Releases
2. Click "Release to Production"
3. Select a previous AAB from the artifact library
4. Submit for review (expedited for rollbacks)

### 10.3 Web Client Rollback

```bash
# Revert to previous commit
git log --oneline web/
git checkout <PREVIOUS_COMMIT_HASH> -- web/
# Redeploy
```

---

## 11. Monitoring & Crash Reporting

### 11.1 Recommended (Future)

- **Firebase Crashlytics** — crash reporting + ANR tracking
- **Google Analytics for Firebase** — user engagement
- **Android Vitals** (Play Console) — performance metrics

### 11.2 Manual Monitoring

```bash
# Live logcat from connected device
adb logcat -s SkidlViewModel:V HostNetworkingManager:V ClientNetworkingManager:V

# Filter for crashes
adb logcat *:E | grep -i "skidl\|fatal\|crash"

# Dump heap for memory analysis
adb shell am dumpheap com.skidl /data/local/tmp/heap.hprof
adb pull /data/local/tmp/heap.hprof
```

---

## 12. Troubleshooting

### Build Issues

| Problem | Solution |
|---------|----------|
| `ANDROID_HOME not set` | Set environment variable (Section 2.3) |
| `SDK license not accepted` | Run `sdkmanager --licenses` |
| `Could not find com.android.tools.build:gradle` | Check internet, Gradle proxy settings |
| `Execution failed: Java 17 required` | Ensure `JAVA_HOME` points to JDK 17 |
| `Build fails after clean` | Delete `.gradle/`, `build/`, and rebuild |
| `ProGuard errors` | Check `app/proguard-rules.pro`, add missing keep rules |

### Device Issues

| Problem | Solution |
|---------|----------|
| `INSTALL_FAILED_VERSION_DOWNGRADE` | Uninstall first: `adb uninstall com.skidl` |
| `INSTALL_FAILED_UNKNOWN_SOURCES` | Enable unknown sources in device Settings |
| APK won't install on Android 7 | App requires minSdk 26 (Android 8.0+) |
| Hotspot not working | Ensure hotspot is enabled in system settings *before* hosting |
| Clients can't discover host | Use manual IP entry or QR code; some OEMs block UDP broadcast |
| WebSocket timeout | Check both devices are on the same hotspot network |

### Network Ports

| Port | Protocol | Purpose |
|------|----------|---------|
| 50000 | TCP (WebSocket) | Game server |
| 60000 | UDP (Broadcast) | Host discovery |

Ensure these ports are not blocked by device firewall or VPN.

---

## Quick Reference — Deployment Commands

```bash
# Full build pipeline (test + lint + build)
./gradlew clean lintDebug testDebugUnitTest assembleRelease

# Install debug APK on connected device
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Install release APK on connected device
adb install -r app/build/outputs/apk/release/app-release.apk

# Check APK contents & size
aapt dump badging app/build/outputs/apk/release/app-release.apk

# Verify signing
apksigner verify --print-certs app/build/outputs/apk/release/app-release.apk

# View device logs live
adb logcat -s SkidlViewModel:V

# Serve web client on LAN
cd web/ && python3 -m http.server 3000
```

---

*Document version: 1.0 — Hotspot Skribble v1.0.0*
