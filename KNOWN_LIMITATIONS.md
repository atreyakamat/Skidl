# Known Limitations — Hotspot Skribble v1.0

This document lists known limitations, trade-offs, and areas for future improvement.

---

## Network & Connectivity

### 1. Wi-Fi Hotspot Must Be Created Manually
**Impact:** Medium  
The app cannot programmatically create a Wi-Fi hotspot on modern Android (API 26+) due to OS restrictions. Players must manually enable hotspot in system settings before hosting.

**Workaround:** Clear instructions on the Host Setup screen guide users through this step.

### 2. UDP Broadcast May Fail on Some OEM ROMs
**Impact:** Medium  
Certain manufacturers (Xiaomi MIUI, Samsung One UI) may restrict UDP broadcast traffic even on local networks.

**Workaround:** Manual IP entry and QR code scanning provide alternative discovery paths. Both are prominently available in the Join UI.

### 3. WebSocket Binds to All Network Interfaces
**Impact:** Low  
The Java-WebSocket server uses `0.0.0.0` (all interfaces) rather than binding exclusively to the hotspot interface.

**Risk:** On devices with multiple active network connections, the game server may be reachable from other networks. Since this is a party game on a private hotspot, this is an accepted trade-off.

### 4. No TLS / Encryption
**Impact:** Low (LAN context)  
All WebSocket traffic is unencrypted. Secret words are sent as plaintext to the drawing player.

**Context:** The game operates on a private hotspot where all participants are physically co-located. Adding TLS would require certificate management that is disproportionate for the use case.

### 5. Single Port, No Port Configuration
**Impact:** Low  
WebSocket uses port `50000` and UDP uses port `60000`. These are not user-configurable. If another app uses these ports, the host will fail to start.

**Future:** Add port configuration in settings screen.

---

## Game Logic

### 6. No Spectator Mode
**Impact:** Low  
All connected players must participate. There is no option to watch without being assigned as drawer or guesser.

**Future:** Add a `spectator` role that receives strokes and guesses but is never assigned as drawer.

### 7. No In-Game Chat (Outside Guessing)
**Impact:** Low  
The only text communication during a round is the guess input. There is no general chat between rounds or in the lobby.

**Future:** Add a chat panel using a new `ChatMessage` type.

### 8. Drawer Disconnect Does Not Auto-End Round
**Impact:** Medium  
If the drawer disconnects mid-round, a `PlayerLeftMessage` is broadcast but the round timer continues. Other players see no new strokes but must wait for the timer to expire.

**Future:** Detect drawer disconnect → immediately end round → reveal word → advance to next round.

### 9. No Player Kick / Ban
**Impact:** Low  
The host cannot remove disruptive players from the lobby or game.

**Future:** Add host-only `KickMessage` type + UI button.

### 10. Word Pool Size
**Impact:** Low  
The bundled `words.json` contains a fixed pool. Once all words are used in a session, the set resets (may repeat).

**Future:** Allow custom word lists or download supplementary packs.

---

## UI / UX

### 11. Drawing Canvas — Limited Tool Set
**Impact:** Low  
Canvas supports freehand only. No shapes, fill bucket, eraser tool, or undo beyond stroke removal.

**Future:** Add eraser (special color), undo button (remove last stroke), line thickness selector.

### 12. No Landscape Mode Optimization
**Impact:** Low  
The app works in landscape but layouts are designed primarily for portrait. Canvas may feel cramped on shorter devices in landscape.

### 13. No Accessibility Audit
**Impact:** Medium  
The app has not been audited against WCAG or Android accessibility guidelines. Content descriptions, focus order, and screen reader support need review.

**Future:** Add `contentDescription` to all icons and interactive elements. Test with TalkBack.

### 14. No Localization
**Impact:** Low  
All strings are English-only and hardcoded or in `strings.xml` without alternative locales.

**Future:** Extract all user-facing text to resources, add locale variants.

---

## Platform

### 15. Android Only
**Impact:** Medium  
There is no iOS, desktop, or web client in v1.0. The `web-client/` folder contains a prototype but is not production-ready.

### 16. minSdk 26 (Android 8.0)
**Impact:** Low  
Devices running Android 7.1 or earlier cannot install the app. Android 8.0+ covers ~95% of active devices (as of 2024).

### 17. No Data Persistence Between Sessions
**Impact:** Low  
Game state is entirely in-memory. If the host app is killed, all game progress is lost. There is no save/resume feature.

**Future:** Use DataStore (already included as dependency) to checkpoint game state for crash recovery.

---

## Build & Distribution

### 18. No Google Play Listing
**Impact:** Info  
v1.0 targets sideloading via APK. Google Play distribution requires additional metadata, screenshots, content rating, and privacy policy.

### 19. Release Signing
**Impact:** Info  
The `keystore/` directory contains setup instructions but no actual keystore. A release keystore must be generated before signing release builds.

---

*Last updated: v1.0 release*
