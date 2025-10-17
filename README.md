# Skidl

HotspotSkribble (Skidl) is a local-only drawing and guessing party game designed to run entirely over an Android hotspot. One device hosts the lobby while others join via the in-app discovery flow or by entering the host IP manually. A lightweight browser client is provided for quick testing over WebSockets.

## Features
- Host and client modes in a single Android application written in Kotlin with Jetpack Compose.
- UDP broadcast discovery on port 60000 with manual IP fallback and optional room code.
- Embedded WebSocket server for the host (Java-WebSocket) and OkHttp-based client connections on port 50000.
- Realtime drawing canvas with color picker, stroke width, undo hook, and synchronized strokes.
- Guess chat with automatic scoring, scoreboard, and round management.
- Heartbeat and reconnect-ready networking layer with JSON message schema.
- Companion web client for desktop testing via manual WebSocket connection.
- GitHub Actions pipeline building a signed release APK artifact.

## Getting Started
1. **Requirements**
   - Android Studio Giraffe+ with JDK 17
   - Android device or emulator running Android 8.0 (API 26) or later
   - Local network / hotspot with multicast enabled (for discovery)

2. **Build (Debug)**
   ```bash
   ./gradlew assembleDebug
   ```

3. **Release Signing**
   - Create a keystore (example):
     ```bash
     keytool -genkeypair -v \
       -keystore keystore/release-keystore.jks \
       -keyalg RSA -keysize 2048 -validity 10000 \
       -alias hotspot_skribble_key
     ```
   - Create `keystore.properties` (not committed):
     ```properties
     storeFile=keystore/release-keystore.jks
     storePassword=change-me
     keyAlias=hotspot_skribble_key
     keyPassword=change-me
     ```
   - Run
     ```bash
     ./make_apk.sh
     ```
     The signed APK will be located at `app/build/outputs/apk/release/app-release.apk`.

4. **GitHub Actions**
   - Add repository secrets: `KEYSTORE_BASE64`, `STORE_PASSWORD`, `KEY_ALIAS`, and `KEY_PASSWORD`.
   - The workflow `.github/workflows/build-apk.yml` produces a signed APK artifact on every push to `main` or manual dispatch.

## Networking Overview
- **Discovery**: Host broadcasts JSON advertisements every 700ms on UDP port `60000`.
- **WebSocket**: Server listens on TCP `50000`. Clients use OkHttp, host uses `org.java-websocket`.
- **Messages**: Newline-delimited JSON objects. Core message types are described in `com.skidl.model.Messages.kt`.

### Sample Messages
```json
{"type":"host_ad","roomName":"Skidl Room","ip":"192.168.43.1","port":50000,"roomId":"r123"}
{"type":"join","playerId":"p123","name":"Atreya"}
{"type":"stroke_start","strokeId":"s1","playerId":"p123","color":"#000000","thickness":4,"x":120,"y":220,"timestamp":1690000000}
{"type":"correct_guess","playerId":"p234","word":"apple","points":120}
```

## Web Client
Located in `web-client/`.
1. Serve via any static file server (e.g. `python -m http.server`).
2. Open in a browser connected to the same hotspot.
3. Enter `ws://<HOST_IP>:50000`, pick a name, and click **Connect**.
4. Draw directly on the canvas or submit guesses.

## Testing
- **Unit tests**: `./gradlew test`
- **Instrumentation**: `./gradlew connectedAndroidTest`

Included tests:
- Message serialization/deserialization coverage.
- Drawing persistence logic ensures stroke events are retained in the game controller.

## Privacy Notice
Skidl communicates only over the local hotspot network. Player display names are stored in memory for the duration of the session and cleared when the host ends the game. No personal or gameplay data is transmitted to external services.

## Known Limitations
- Embedded WebSocket server binds to all interfaces; ensure firewall rules permit local traffic.
- UDP broadcast may be restricted on some OEM devices; manual IP entry and QR sharing are provided as fallback.
- Round timer and reconnect behaviour are basic and may need refinement for large lobbies.
- Hotspot must be activated manually in device settings prior to hosting.

## Project Structure
```
app/                    Android application module
  src/main/java/com/skidl
    ui/                 Compose screens and state management
    network/            Discovery + WebSocket managers
    game/               Game engine, scoring, word bank
    model/              Shared data classes and serialization
web-client/             Browser reference client
.github/workflows/      CI pipeline for signed APK builds
ci/                     Reserved for additional automation scripts
```

## QA Checklist (Summary)
- Host discovery across two Android devices on hotspot.
- Realtime drawing latency under ~200ms within hotspot network.
- Scoreboard updates after correct guess confirmation.
- Manual IP join validated when discovery disabled.
- Signed release APK installs on Android 8.0+.

Enjoy sketching with Skidl!
