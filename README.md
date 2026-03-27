# 🎨 Hotspot Skribble

> **Draw. Guess. Laugh.** — A LAN multiplayer drawing & guessing party game for Android.

No internet required. One device creates a Wi-Fi hotspot, others connect and play. That's it.

---

## 🚀 Quick Start

### Play in 60 Seconds
1. **Host** enables their phone's **Wi-Fi Hotspot**
2. **Players** connect their phones to that hotspot's Wi-Fi
3. Host opens the app → taps **Host Game** → sets room name → **Start Hosting**
4. Players open the app → tap **Join Game** → pick the discovered host (or enter IP manually)
5. Everyone readies up → Host taps **Start Round** → Draw and guess!

### Build the APK

**📋 For detailed build instructions, see [BUILD_INSTRUCTIONS.md](BUILD_INSTRUCTIONS.md)**

Quick build:
```bash
# Debug build (no signing required)
./make_apk.sh

# Release build (keystore already configured!)
./make_apk.sh release
```

**Prerequisites:** JDK 17+, Android SDK 34, `ANDROID_HOME` environment variable set.

**✅ Release signing is already configured** - keystore and credentials are ready!

---

## 🎮 How It Works

| Role | What You Do |
|------|-------------|
| **Drawer** | Sees the secret word, draws on canvas with colors & brush sizes |
| **Guessers** | See the drawing in real-time, type guesses |
| **Scoring** | 100 base pts + time bonus (seconds remaining). Faster = more points |

### Game Flow
```
Welcome → Host Setup / Join → Lobby → Game (draw/guess) → Scores → Next Round / Game Over
```

- **Rounds:** Configurable (default 3). Each round picks a new drawer round-robin.
- **Timer:** 90 seconds per round (configurable).
- **Words:** Loaded from `assets/words.json` — easy to customize.

---

## 🏗 Architecture

```
┌─────────────┐     WebSocket (port 50000)     ┌─────────────┐
│   HOST      │◄──────────────────────────────►│   CLIENT    │
│  Device     │     UDP Broadcast (port 60000)  │  Device(s)  │
└─────────────┘                                 └─────────────┘
```

- **Host-authoritative model**: Host runs an embedded WebSocket server + UDP broadcaster
- **Clients**: Discover host via UDP, connect via WebSocket
- **All communication** is JSON over WebSocket (newline-delimited)
- **Zero internet dependency** — everything runs on the local hotspot network

See [ARCHITECTURE.md](ARCHITECTURE.md) for the full technical deep-dive.

---

## 📁 Project Structure

```
app/src/main/java/com/skidl/
├── model/          # Data classes: Player, Stroke, GameState, Messages
├── game/           # GameController, ScoreManager, WordBank
├── network/        # Host/Client networking, Discovery, Serializer
├── ui/
│   ├── screens/    # 6 Compose screens (Welcome → Scores)
│   ├── components/ # DrawingCanvas, QrCode
│   └── theme/      # Color/Theme/Type (goofy party palette)
└── util/           # Constants
```

---

## 🧪 Testing

```bash
# Run all unit tests
./gradlew testDebugUnitTest
```

Tests cover:
- **ScoreManager** — 10 test cases (scoring, accumulation, reset, edge cases)
- **SkidlMessageSerializer** — 22 test cases (all 20 message types + error handling)
- **GameController** — 20 test cases (lobby, rounds, strokes, guesses, timer)
- **Model data classes** — 8 test cases (defaults, copying, edge values)

See [QA_REPORT.md](QA_REPORT.md) for the full testing report.

---

## 🔌 Network Protocol

20 message types over JSON/WebSocket. See [NETWORK_PROTOCOL.md](NETWORK_PROTOCOL.md) for the complete specification.

### Sample Messages
```json
{"type":"host_ad","roomName":"Party Room","ip":"192.168.43.1","port":50000,"roomId":"r123","players":4}
{"type":"join","playerId":"p123","name":"Atreya"}
{"type":"stroke_start","strokeId":"s1","playerId":"p123","color":"#000000","thickness":4,"x":120,"y":220,"timestamp":1690000000}
{"type":"correct_guess","playerId":"p234","word":"apple","points":120}
{"type":"round_end","word":"apple","scores":[{"playerId":"p234","name":"Bob","score":120}]}
```

---

## 🌐 Web Client

Located in `web-client/`. For desktop testing:
1. Serve via `python -m http.server` (from `web-client/` directory)
2. Open browser connected to same hotspot Wi-Fi
3. Enter `ws://<HOST_IP>:50000`, set name, click **Connect**
4. Draw & guess directly from the browser

---

## 📋 Configuration

| Setting | Default | Location |
|---------|---------|----------|
| WebSocket Port | 50000 | `Constants.kt` |
| UDP Discovery Port | 60000 | `Constants.kt` |
| Broadcast Interval | 700ms | `Constants.kt` |
| Heartbeat Interval | 10s | `Constants.kt` |
| Round Time | 90s | `GameSettings` |
| Total Rounds | 3 | `SkidlViewModel` |
| Max Message Size | 16KB | Host/Client managers |
| Reconnect Attempts | 3 | `ClientNetworkingManager` |

---

## 🔒 Privacy

Hotspot Skribble communicates only over the local hotspot network. Player names are stored in memory for the session duration and cleared when the host ends. No data is transmitted to external services.

---

## 📄 Documentation Index

| Document | Description |
|----------|-------------|
| [BUILD_INSTRUCTIONS.md](BUILD_INSTRUCTIONS.md) | **START HERE** - Complete build guide with prerequisites and troubleshooting |
| [PROJECT_STATUS.md](PROJECT_STATUS.md) | Project completion status and what's ready |
| [SIDELOAD_GUIDE.md](SIDELOAD_GUIDE.md) | Step-by-step sideloading instructions |
| [ARCHITECTURE.md](ARCHITECTURE.md) | System architecture & design decisions |
| [NETWORK_PROTOCOL.md](NETWORK_PROTOCOL.md) | Complete message protocol specification |
| [QA_REPORT.md](QA_REPORT.md) | Testing report & edge case analysis |
| [KNOWN_LIMITATIONS.md](KNOWN_LIMITATIONS.md) | Known issues & workarounds |
| [RELEASE_CHECKLIST.md](RELEASE_CHECKLIST.md) | Pre-release verification checklist |
| [DEPLOYMENT_GUIDE.md](DEPLOYMENT_GUIDE.md) | Comprehensive deployment options |
| [INSTALL.md](INSTALL.md) | Developer setup & build instructions |

---

## License

Private / Internal Use — All rights reserved.
