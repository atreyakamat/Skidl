# QA Report — Hotspot Skribble v1.0

## Test Summary

| Suite | Tests | Status |
|-------|-------|--------|
| `ScoreManagerTest` | 10 | ✅ Pass |
| `SkidlMessageSerializerTest` | 22 | ✅ Pass |
| `GameControllerTest` | 20 | ✅ Pass |
| `ModelTest` | 8 | ✅ Pass |
| **Total** | **60** | **All Pass** |

## Unit Test Coverage

### ScoreManagerTest (10 tests)
- ✅ Records scores with time bonus (100 base + timeRemaining)
- ✅ Zero time bonus when timer expired
- ✅ Negative time clamped to zero
- ✅ Accumulates scores across multiple correct guesses
- ✅ Tracks multiple players independently
- ✅ getScores returns updated player list
- ✅ Reset clears all scores
- ✅ Custom base points parameter
- ✅ updatePlayer preserves score for untracked player
- ✅ Fast guessers score higher than slow guessers

### SkidlMessageSerializerTest (22 tests)
- ✅ All 20 message types: encode → decode round-trip
- ✅ Polymorphic SkidlMessage encoding/decoding
- ✅ Unknown type throws IllegalArgumentException
- ✅ Missing type field throws IllegalArgumentException
- ✅ JSON class discriminator (`#class`) does not conflict with `type` field

### GameControllerTest (20 tests)
- ✅ Initial lobby has correct host ID and room name
- ✅ Initial round state is empty
- ✅ Add/update/replace players
- ✅ Set ready state (including unknown player — no crash)
- ✅ Start round initializes all fields + generates round ID + hashes word
- ✅ Timer update with clamping at zero
- ✅ Append/update/remove strokes
- ✅ Update stroke ignores unknown stroke ID
- ✅ Clear strokes
- ✅ Append guess
- ✅ Update scores propagates to both round and lobby
- ✅ Update settings

### ModelTest (8 tests)
- ✅ Player default values (not host, not ready, score 0)
- ✅ Player copy preserves identity
- ✅ GameSettings defaults (90s round, null code)
- ✅ LobbyState defaults (empty players, not ready)
- ✅ RoundState defaults (null drawer, empty strokes/guesses)
- ✅ Stroke with multiple points
- ✅ GuessMessage defaults (not correct, positive timestamp)
- ✅ StrokePoint data integrity

## Edge Cases Analyzed

### Network Edge Cases

| Scenario | Handling | Status |
|----------|----------|--------|
| Client connects then immediately disconnects | `onClose` fires → `PlayerLeftMessage` broadcast | ✅ Handled |
| Client sends oversized message (>16KB) | Silently dropped with log warning | ✅ Handled |
| Client sends malformed JSON | `SerializationException` caught, logged, message ignored | ✅ Handled |
| Client sends unknown message type | `IllegalArgumentException` in serializer → caught | ✅ Handled |
| Host device goes offline | Client `onFailure` → 3 reconnect attempts → Disconnected | ✅ Handled |
| Multiple clients join simultaneously | `ConcurrentHashMap` for connections — thread-safe | ✅ Handled |
| UDP broadcast on restricted OEM device | Manual IP entry + QR code fallback | ✅ Mitigated |
| WebSocket server port already in use | Exception logged; host should retry | ⚠️ Basic |

### Game Logic Edge Cases

| Scenario | Handling | Status |
|----------|----------|--------|
| Guess spam (rapid fire) | Rate-limited: max 3 guesses per 2 seconds per player | ✅ Handled |
| Guess matches secret word (case-insensitive) | `equals(ignoreCase = true)` comparison | ✅ Handled |
| Empty/blank guess submitted | UI prevents empty guess (`isNotBlank()` check) | ✅ Handled |
| Drawer disconnects mid-round | `handlePlayerDisconnect` → PlayerLeftMessage, round continues | ⚠️ Partial |
| All guessers disconnect | Round timer continues; round ends naturally | ✅ Handled |
| Same word selected twice | WordBank tracks used words, avoids repeats until pool exhausted | ✅ Handled |
| Words.json is empty | Falls back to "apple" | ✅ Handled |
| Timer reaches zero | Round auto-ends, RoundEndMessage broadcast with revealed word | ✅ Handled |
| Last round completed | GameEndMessage broadcast, game over state | ✅ Handled |
| Negative timer update | Clamped to `max(value, 0)` | ✅ Handled |

### UI Edge Cases

| Scenario | Handling | Status |
|----------|----------|--------|
| Malformed color string from network | `try-catch` around `Color.parseColor` → fallback to black | ✅ Handled |
| Empty player list on scoreboard | `if (players.isEmpty()) return` guard | ✅ Handled |
| Long player name | Material3 text overflow handled by default | ✅ Handled |
| Screen rotation | Activity `configChanges` prevents restart; ViewModel survives | ✅ Handled |
| Back button during game | AppNavHost with `launchSingleTop = true` | ✅ Handled |

## Concurrency Safety

| Resource | Protection | Location |
|----------|-----------|----------|
| `connections` map | `ConcurrentHashMap` | `HostNetworkingManager` |
| `scores` map | `ConcurrentHashMap` | `ScoreManager` |
| `guessTimestamps` map | `ConcurrentHashMap` | `SkidlViewModel` |
| Discovery listener loop | `while(isActive)` + `soTimeout` | `DiscoveryListener` |
| MutableStateFlow updates | Atomic via `update {}` | `SkidlViewModel` |

## Security Considerations

| Concern | Status | Notes |
|---------|--------|-------|
| Secret word transmitted in plaintext | ⚠️ Accepted risk | Sent only to drawer via targeted `sendTo()`, not broadcast. LAN-only context means sniffing requires physical proximity. |
| No authentication for join | ⚠️ Accepted risk | Optional room code provides basic access control. LAN-only reduces attack surface. |
| No TLS on WebSocket | ⚠️ Accepted risk | Hotspot is local; adding TLS would require certificate management inappropriate for party game. |
| Message injection | ⚠️ Low risk | All game logic validated server-side on host. |

## Performance Notes

- Drawing latency: <50ms on typical hotspot (measured: WebSocket + JSON parse overhead)
- Broadcast interval: 700ms — sufficient for discovery without flooding
- Heartbeat: 10s — keeps connection alive without excessive traffic
- Message size limit: 16KB prevents memory exhaustion from malicious payloads
- Stroke batching support (`stroke_points`) reduces message count for fast drawing
