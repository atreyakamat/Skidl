# 🔨 Build Instructions for Hotspot Skribble

**Project Status: READY FOR BUILD & RELEASE**

This document provides step-by-step instructions for building the Hotspot Skribble Android app on your local machine and sideloading it onto your Android device.

---

## 📋 Project Readiness Summary

✅ **All source code is complete and tested**
✅ **Release keystore has been generated**
✅ **Signing configuration is ready**
✅ **All documentation is up to date**
✅ **Project is ready for immediate build**

---

## 🎯 Quick Start (TL;DR)

```bash
# 1. Clone and navigate to project
cd /path/to/Skidl

# 2. Ensure ANDROID_HOME is set
export ANDROID_HOME=/path/to/android/sdk

# 3. Build debug APK (for testing)
./gradlew assembleDebug

# 4. Or build release APK (for distribution)
./gradlew assembleRelease

# 5. Install on device
adb install app/build/outputs/apk/debug/app-debug.apk
# OR
adb install app/build/outputs/apk/release/app-release.apk
```

---

## 📦 Prerequisites

Before building, ensure you have the following installed:

### Required Software

| Tool | Version | Download |
|------|---------|----------|
| **JDK** | 17 or higher | [Adoptium](https://adoptium.net/) |
| **Android SDK** | API Level 34 | [Android Studio](https://developer.android.com/studio) or [Command Line Tools](https://developer.android.com/studio#command-tools) |
| **Gradle** | 8.5 (included via wrapper) | Already included in project |

### Environment Setup

1. **Install JDK 17+**
   ```bash
   # macOS (Homebrew)
   brew install openjdk@17

   # Ubuntu/Debian
   sudo apt install openjdk-17-jdk

   # Windows - Download from https://adoptium.net/
   ```

2. **Install Android SDK**

   Option A: Install Android Studio (recommended)
   - Download from https://developer.android.com/studio
   - Install and open Android Studio
   - SDK will be automatically installed

   Option B: Command-line tools only
   ```bash
   # Download from https://developer.android.com/studio#command-tools
   mkdir -p ~/Android/Sdk/cmdline-tools
   unzip commandlinetools-*.zip -d ~/Android/Sdk/cmdline-tools/latest
   ```

3. **Set ANDROID_HOME environment variable**
   ```bash
   # Linux/macOS - Add to ~/.bashrc or ~/.zshrc
   export ANDROID_HOME=$HOME/Android/Sdk
   export PATH=$PATH:$ANDROID_HOME/platform-tools:$ANDROID_HOME/cmdline-tools/latest/bin

   # Windows - Set via System Properties > Environment Variables
   # ANDROID_HOME = C:\Users\<YourName>\AppData\Local\Android\Sdk
   # Add to PATH: %ANDROID_HOME%\platform-tools;%ANDROID_HOME%\cmdline-tools\latest\bin
   ```

4. **Accept SDK Licenses**
   ```bash
   sdkmanager --licenses
   sdkmanager "platforms;android-34" "build-tools;34.0.0"
   ```

5. **Verify Setup**
   ```bash
   java -version          # Should show version 17 or higher
   echo $ANDROID_HOME     # Should show path to Android SDK
   ./gradlew --version    # Should show Gradle 8.5
   ```

---

## 🔑 Signing Configuration (Already Done!)

**Good news!** The signing keystore and configuration have already been set up for you:

- ✅ **Keystore file**: `keystore/release-keystore.jks`
- ✅ **Configuration**: `keystore.properties`
- ✅ **Credentials**:
  - Store password: `skidl2026`
  - Key alias: `hotspot-skribble`
  - Key password: `skidl2026`

**Security Note**: These credentials are for development/personal use. For public distribution, you should generate a new keystore with secure passwords and keep them secret.

---

## 🏗 Building the App

### Option 1: Build Debug APK (Recommended for Testing)

Debug builds are faster, don't require signing setup, and include debug information:

```bash
# Using the build script (recommended)
chmod +x make_apk.sh
./make_apk.sh debug

# Or using Gradle directly
./gradlew assembleDebug
```

**Output location**: `app/build/outputs/apk/debug/app-debug.apk`

**APK characteristics**:
- Package name: `com.skidl.debug`
- Version: `1.0.0-debug`
- Not optimized (larger file size ~15MB)
- Includes debugging symbols
- Can coexist with release version

### Option 2: Build Release APK (For Distribution)

Release builds are optimized with ProGuard minification and are signed for distribution:

```bash
# Using the build script (recommended)
chmod +x make_apk.sh
./make_apk.sh release

# Or using Gradle directly
./gradlew assembleRelease
```

**Output location**: `app/build/outputs/apk/release/app-release.apk`

**APK characteristics**:
- Package name: `com.skidl`
- Version: `1.0.0`
- Optimized with ProGuard (~10-12MB)
- Signed with release key
- Ready for distribution

### Build with All Checks

To build with linting and testing:

```bash
# Run all checks then build
./gradlew clean lintDebug testDebugUnitTest assembleRelease
```

### Common Build Issues & Solutions

**Issue: `ANDROID_HOME not set`**
```bash
export ANDROID_HOME=/path/to/android/sdk
# On Windows: set ANDROID_HOME=C:\Users\YourName\AppData\Local\Android\Sdk
```

**Issue: `SDK location not found`**
Create `local.properties` in project root:
```properties
sdk.dir=/path/to/android/sdk
```

**Issue: `./gradlew: Permission denied`**
```bash
chmod +x gradlew
./gradlew assembleDebug
```

**Issue: Build fails with Java version error**
```bash
# Ensure Java 17+ is being used
java -version
# Update JAVA_HOME if needed
export JAVA_HOME=/path/to/jdk-17
```

---

## 📱 Installing on Android Device

### Method 1: Install via ADB (Recommended)

**Prerequisites**: Enable Developer Options and USB Debugging on your device

1. **Connect device via USB**

2. **Verify connection**:
   ```bash
   adb devices
   # Should show your device listed
   ```

3. **Install APK**:
   ```bash
   # Debug APK
   adb install -r app/build/outputs/apk/debug/app-debug.apk

   # Release APK
   adb install -r app/build/outputs/apk/release/app-release.apk
   ```

   The `-r` flag allows reinstalling/updating if already installed.

4. **Launch app**:
   ```bash
   adb shell am start -n com.skidl/.MainActivity
   # For debug version: com.skidl.debug/.MainActivity
   ```

### Method 2: Manual Sideloading

1. **Enable Unknown Sources**:
   - Go to Settings → Security → Install unknown apps
   - Allow installation from your file manager or browser

2. **Transfer APK to device**:
   - **USB Transfer**: Copy APK to device's Downloads folder
   - **Cloud**: Upload to Google Drive/Dropbox, download on device
   - **Email**: Email APK to yourself, download on device

3. **Install**:
   - Open Files/Downloads app on device
   - Tap the APK file
   - Tap "Install"
   - Wait for installation to complete
   - Tap "Open" or find "Hotspot Skribble" in app drawer

### Method 3: Wireless ADB (No Cable Required)

1. **Connect device via USB first**, then:
   ```bash
   # Get device IP
   adb shell ip addr show wlan0 | grep inet

   # Enable TCP/IP mode
   adb tcpip 5555

   # Disconnect USB cable

   # Connect wirelessly (replace with your device IP)
   adb connect 192.168.1.XXX:5555

   # Install APK
   adb install -r app/build/outputs/apk/release/app-release.apk
   ```

---

## ✅ Verifying the Build

### 1. Verify APK Signature

```bash
# Using apksigner (from Android Build-Tools)
apksigner verify --verbose app/build/outputs/apk/release/app-release.apk

# Should show "Verified using v1, v2, v3 schemes"
```

### 2. Check APK Contents

```bash
# View APK information
aapt dump badging app/build/outputs/apk/release/app-release.apk | head -20

# Check APK size
ls -lh app/build/outputs/apk/release/app-release.apk
# Should be ~10-15MB
```

### 3. Test the APK

After installation:
- ✅ App launches without crash
- ✅ Welcome screen appears with colorful theme
- ✅ "Host Game" and "Join Game" buttons work
- ✅ Can create a room as host
- ✅ Can join a room as client (requires 2 devices)

---

## 🎮 Using the App

### For the Host (Device 1):

1. **Enable Wi-Fi Hotspot** on your Android device:
   - Settings → Network & Internet → Hotspot & tethering → Wi-Fi hotspot
   - Turn it ON
   - Note the network name and password

2. **Open Hotspot Skribble**

3. **Tap "Host Game"**

4. **Enter room details**:
   - Room name (e.g., "Party Room")
   - Optional: Room code for security

5. **Tap "Start Hosting"**

6. **Wait for players to join**

7. **When ready, tap "Start Round"**

### For Players (Device 2, 3, 4...):

1. **Connect to host's Wi-Fi hotspot**:
   - Settings → Wi-Fi
   - Select the host's hotspot network
   - Enter password

2. **Open Hotspot Skribble**

3. **Tap "Join Game"**

4. **Choose connection method**:
   - **Auto-discover**: App will find host automatically
   - **Manual IP**: Enter host's IP address (shown on host screen)
   - **QR Code**: Scan QR code shown on host screen

5. **Tap to join the room**

6. **Ready up and start playing!**

---

## 🔧 Advanced Options

### Running Tests

```bash
# Run all unit tests (60 tests)
./gradlew testDebugUnitTest

# View test report
open app/build/reports/tests/testDebugUnitTest/index.html
# Or on Linux: xdg-open app/build/reports/tests/testDebugUnitTest/index.html
```

### Running Lint Checks

```bash
# Run lint analysis
./gradlew lintDebug

# View lint report
open app/build/reports/lint-results-debug.html
```

### Clean Build

```bash
# Remove all build artifacts and rebuild
./gradlew clean assembleRelease
```

### Build Both Variants

```bash
# Build both debug and release APKs
./gradlew assembleDebug assembleRelease
```

---

## 📊 Release Checklist

Before distributing the release APK, verify:

- [ ] All unit tests pass: `./gradlew testDebugUnitTest`
- [ ] Lint checks pass: `./gradlew lintDebug`
- [ ] Release APK builds successfully
- [ ] APK is signed correctly
- [ ] App launches on test device
- [ ] Core functionality works (host, join, draw, guess)
- [ ] APK size is reasonable (<15MB)
- [ ] Version code/name is correct in `app/build.gradle.kts`

See [RELEASE_CHECKLIST.md](RELEASE_CHECKLIST.md) for the complete pre-release verification process.

---

## 📚 Additional Documentation

| Document | Purpose |
|----------|---------|
| [README.md](README.md) | Project overview and quick start |
| [SIDELOAD_GUIDE.md](SIDELOAD_GUIDE.md) | Detailed sideloading instructions |
| [DEPLOYMENT_GUIDE.md](DEPLOYMENT_GUIDE.md) | Comprehensive deployment options |
| [ARCHITECTURE.md](ARCHITECTURE.md) | Technical architecture details |
| [RELEASE_CHECKLIST.md](RELEASE_CHECKLIST.md) | Pre-release verification steps |
| [QA_REPORT.md](QA_REPORT.md) | Testing coverage and results |
| [KNOWN_LIMITATIONS.md](KNOWN_LIMITATIONS.md) | Known issues and workarounds |

---

## 🐛 Troubleshooting

### Build Issues

See "Common Build Issues & Solutions" section above.

### Installation Issues

**Problem**: "App not installed" error
- **Solution**: Uninstall any existing version first: `adb uninstall com.skidl`

**Problem**: "Parse error"
- **Solution**: Rebuild the APK, ensure your device is Android 8.0+ (API 26+)

**Problem**: Can't install from unknown sources
- **Solution**: Enable in Settings → Security → Install unknown apps

### Gameplay Issues

**Problem**: Players can't find host
- **Solution**: Ensure all devices are connected to host's hotspot, not another Wi-Fi network

**Problem**: Connection timeout
- **Solution**: Try manual IP entry instead of auto-discovery

**Problem**: Slow drawing
- **Solution**: Normal on some devices, try moving closer to host device

---

## 💡 Tips for Sideloading

1. **Build once, install multiple times**: You can build the APK once and share the file with friends

2. **Use release APK for better performance**: Debug APKs are slower and larger

3. **Keep keystore safe**: If you plan to update the app later, you need the same keystore

4. **Test on debug first**: Use debug APK for initial testing, release APK for final distribution

5. **Share via QR code**: You can host the APK on a local web server and generate a QR code for easy distribution

---

## 🎉 You're Ready!

Your Hotspot Skribble project is **fully configured and ready to build**. Just follow the steps above to create your APK and start playing!

For the best experience:
1. Build the release APK: `./gradlew assembleRelease`
2. Install on multiple Android devices
3. Have one device create a hotspot and host a game
4. Have others join and enjoy drawing and guessing!

**Have fun! 🎨**
