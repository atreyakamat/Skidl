# 📱 Hotspot Skribble - Sideloading Guide

This guide will help you build and sideload the Hotspot Skribble app on your Android device.

## Prerequisites

Before you begin, ensure you have:

- **JDK 17 or higher** installed
- **Android SDK** (API level 34) installed
- **ANDROID_HOME** environment variable set to your Android SDK location
- **Git** installed on your system

## Step 1: Clone the Repository

```bash
git clone https://github.com/atreyakamat/Skidl.git
cd Skidl
```

## Step 2: Build the APK

### Option A: Build Debug APK (Recommended for Testing)

Debug builds don't require signing setup and are faster to build:

```bash
chmod +x make_apk.sh
./make_apk.sh
```

The debug APK will be generated at:
```
app/build/outputs/apk/debug/app-debug.apk
```

### Option B: Build Release APK (For Distribution)

Release builds are optimized with ProGuard and require a signing keystore:

1. **The keystore is already generated** in `keystore/release-keystore.jks`
2. **The keystore.properties file is configured** with the signing credentials
3. **Build the release APK:**

```bash
chmod +x make_apk.sh
./make_apk.sh release
```

The release APK will be generated at:
```
app/build/outputs/apk/release/app-release.apk
```

**Note:** The keystore password is `skidl2026` for both store and key.

## Step 3: Enable Installation from Unknown Sources

On your Android device:

1. Open **Settings**
2. Go to **Security** or **Privacy & Security**
3. Enable **Install unknown apps** or **Unknown sources**
4. Select your file manager or browser app and allow installation from this source

## Step 4: Transfer the APK to Your Device

Choose one of the following methods:

### Method A: USB Transfer
1. Connect your device to your computer via USB
2. Copy the APK file to your device's **Downloads** folder
3. Disconnect safely

### Method B: Cloud Services
1. Upload the APK to Google Drive, Dropbox, or any cloud service
2. Download it on your device using the respective app

### Method C: ADB Install (Direct Installation)
If you have ADB (Android Debug Bridge) set up:

```bash
# For debug build
adb install app/build/outputs/apk/debug/app-debug.apk

# For release build
adb install app/build/outputs/apk/release/app-release.apk
```

## Step 5: Install the APK

1. Open your device's **Files** or **Downloads** app
2. Locate the APK file (e.g., `app-debug.apk` or `app-release.apk`)
3. Tap on the APK file
4. Tap **Install**
5. Wait for the installation to complete
6. Tap **Open** to launch the app, or find "Hotspot Skribble" in your app drawer

## Step 6: Play the Game!

### For the Host:
1. Enable **Wi-Fi Hotspot** on your device (Settings → Network & Internet → Hotspot)
2. Open **Hotspot Skribble**
3. Tap **Host Game**
4. Set a room name (and optional room code for security)
5. Tap **Start Hosting**
6. Wait for players to join

### For Players:
1. Connect to the host's Wi-Fi hotspot
2. Open **Hotspot Skribble**
3. Tap **Join Game**
4. The app will auto-discover the host, or you can enter the IP manually
5. Tap to join the room

## Troubleshooting

### Build Issues

**Problem:** `./gradlew: Permission denied`
```bash
chmod +x gradlew
./gradlew assembleDebug
```

**Problem:** `ANDROID_HOME not set`
```bash
# On Linux/Mac
export ANDROID_HOME=/path/to/android/sdk
export PATH=$PATH:$ANDROID_HOME/tools:$ANDROID_HOME/platform-tools

# On Windows (PowerShell)
$env:ANDROID_HOME = "C:\Users\YourName\AppData\Local\Android\Sdk"
```

**Problem:** `SDK location not found`
Create a `local.properties` file in the project root:
```properties
sdk.dir=/path/to/android/sdk
```

### Installation Issues

**Problem:** "App not installed"
- Make sure you've enabled installation from unknown sources
- If updating an existing version, uninstall the old version first
- Check that the APK isn't corrupted (try re-downloading)

**Problem:** "Parse error"
- The APK may be incomplete or corrupted
- Rebuild the APK and try again
- Ensure your Android version is 8.0 (API 26) or higher

### Gameplay Issues

**Problem:** Players can't discover the host
- Ensure all devices are connected to the host's Wi-Fi hotspot
- Try entering the host's IP address manually (shown on the host's screen)
- Check that no firewall is blocking ports 50000 (WebSocket) and 60000 (UDP)

**Problem:** Slow drawing response
- This is normal on some devices/networks
- Try moving closer to the host device
- Ensure the host device has good processing power

## App Size and Requirements

- **APK Size:** ~10-15 MB
- **Minimum Android Version:** 8.0 (API 26)
- **Target Android Version:** 14 (API 34)
- **Permissions Required:**
  - Network access (for multiplayer)
  - Camera (optional, for QR code scanning)

## Security Notes

- The keystore credentials are included for development purposes only
- For production distribution, generate a new keystore with secure passwords
- Keep your keystore and credentials secure and never commit them to public repositories
- The current setup is suitable for personal use and sideloading among friends

## Building on Different Platforms

### Windows
```cmd
gradlew.bat assembleDebug
```

### Linux/Mac
```bash
./gradlew assembleDebug
```

## Advanced: GitHub Actions Build

This repository includes a CI/CD workflow that can automatically build APKs. Check the `.github/workflows` directory for automated build configurations.

## Getting Help

If you encounter issues:

1. Check the [KNOWN_LIMITATIONS.md](KNOWN_LIMITATIONS.md) file
2. Review the [QA_REPORT.md](QA_REPORT.md) for known issues
3. Ensure all prerequisites are correctly installed
4. Try building a debug APK first before attempting a release build

## License

This software is for private/internal use. All rights reserved.

---

**Enjoy playing Hotspot Skribble! 🎨**
