# Release Checklist — Hotspot Skribble v1.0

Use this checklist before distributing any build.

---

## Pre-Build

- [ ] All code changes committed to version control
- [ ] `git status` shows clean working tree
- [ ] Branch is `main` (or release branch)
- [ ] Version name & code updated in `app/build.gradle.kts`
  - `versionCode` incremented
  - `versionName` set to `"1.0.0"` (or appropriate)

## Static Analysis

- [ ] Zero Kotlin compiler errors: `./gradlew compileDebugKotlin`
- [ ] Lint passes with no errors: `./gradlew lintDebug`
- [ ] ProGuard rules reviewed in `app/proguard-rules.pro`
  - kotlinx.serialization classes preserved
  - Model classes preserved
  - Java-WebSocket, OkHttp, ZXing not stripped

## Unit Tests

- [ ] All 60 tests pass: `./gradlew testDebugUnitTest`
  - [ ] ScoreManagerTest — 10 tests
  - [ ] SkidlMessageSerializerTest — 22 tests
  - [ ] GameControllerTest — 20 tests
  - [ ] ModelTest — 8 tests

## Debug Build Verification

- [ ] Debug APK builds successfully: `./gradlew assembleDebug`
- [ ] Install on device: `adb install app/build/outputs/apk/debug/app-debug.apk`
- [ ] App launches without crash
- [ ] Welcome screen renders with goofy theme (yellow/coral/blue)
- [ ] Launcher icon shows pencil + stars design

## Functional Testing (Manual — 2 Devices)

### Hosting
- [ ] "Host Game" → Host Setup screen loads
- [ ] Room name and optional room code entry works
- [ ] Start hosting → Lobby screen appears
- [ ] Host appears in player list as host

### Joining
- [ ] Second device connects to first device's hotspot
- [ ] "Join Game" → Join screen loads
- [ ] Auto-discovery finds host within 5 seconds
- [ ] OR manual IP entry connects successfully
- [ ] OR QR code scan connects (if camera available)
- [ ] Joiner appears in host's lobby

### Game Play
- [ ] Host starts game → Round begins
- [ ] Drawer sees secret word + canvas + color palette
- [ ] Guessers see canvas + guess input (no secret word)
- [ ] Drawing strokes appear on guesser screens in real-time (<100ms)
- [ ] Correct guess triggers CorrectGuessMessage + score update
- [ ] Incorrect guess appears in guess list
- [ ] Timer counts down correctly
- [ ] Timer reaching zero ends the round

### Multi-Round
- [ ] After round ends, next round starts with different drawer
- [ ] Different word is selected each round
- [ ] Scores accumulate across rounds
- [ ] After final round → Scores screen shows final standings

### Edge Cases
- [ ] Rapid guess spam → Rate-limited (max 3 per 2s)
- [ ] Client disconnect → PlayerLeftMessage appears
- [ ] Client reconnect → Rejoins (up to 3 attempts)
- [ ] Long player name → No UI overflow/crash

## Release Build

- [ ] Generate signing keystore (if not exists):
  ```bash
  keytool -genkey -v -keystore keystore/release.jks \
    -keyalg RSA -keysize 2048 -validity 10000 \
    -alias hotspot-skribble
  ```
- [ ] Configure signing in `app/build.gradle.kts` or `keystore.properties`
- [ ] Build release APK: `./gradlew assembleRelease`
- [ ] Verify APK is signed: `apksigner verify app/build/outputs/apk/release/app-release.apk`
- [ ] Install release APK on fresh device (no debug version)
- [ ] Repeat functional testing on release build
- [ ] ProGuard: verify app functions correctly with minification enabled

## Release Artifacts

- [ ] APK file: `app/build/outputs/apk/release/app-release.apk`
- [ ] APK size is reasonable (<15MB)
- [ ] Test report: `app/build/reports/tests/testDebugUnitTest/index.html`

## Documentation

- [ ] README.md is up to date
- [ ] INSTALL.md build instructions verified
- [ ] ARCHITECTURE.md reflects current codebase
- [ ] NETWORK_PROTOCOL.md matches implemented message types
- [ ] QA_REPORT.md reflects current test results
- [ ] KNOWN_LIMITATIONS.md reviewed for accuracy

## Final Verification

- [ ] 30-minute continuous play session with no crashes
- [ ] Memory usage stable (no leaks visible in Android Profiler)
- [ ] Battery usage reasonable during game session
- [ ] App survives configuration change (rotation)
- [ ] App survives process death and restart

---

### Sign-Off

| Role | Name | Date | Approved |
|------|------|------|----------|
| Dev Lead | | | ☐ |
| QA | | | ☐ |
| Product | | | ☐ |

---

*Checklist version: 1.0*
