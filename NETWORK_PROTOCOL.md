# Network Protocol — Hotspot Skribble v1.0

## Transport

| Layer | Protocol | Port | Direction |
|-------|----------|------|-----------|
| Discovery | UDP Broadcast | 60000 | Host → All (every 700ms) |
| Game | WebSocket (TCP) | 50000 | Bidirectional (JSON + newline) |

All messages are **newline-delimited JSON** (`\n` terminated).  
Max message size: **16,384 bytes** (16KB). Oversized messages are silently dropped.

## JSON Configuration

```kotlin
Json {
    encodeDefaults = true
    ignoreUnknownKeys = true
    prettyPrint = false
    explicitNulls = false
    classDiscriminator = "#class"
}
```

## Message Types

Every message has a `type` string field used for routing.

### Discovery

#### `host_ad` — Host Advertisement (UDP)
Broadcast by host every 700ms on UDP port 60000.

```json
{
  "type": "host_ad",
  "roomName": "Party Room",
  "ip": "192.168.43.1",
  "port": 50000,
  "roomId": "uuid-here",
  "players": 4
}
```

### Connection

#### `join` — Client Join Request
```json
{
  "type": "join",
  "playerId": "client-uuid",
  "name": "Alice",
  "roomCode": "optional-code"
}
```

#### `player_joined` — New Player Notification
Broadcast by host to all clients when a new player joins.
```json
{
  "type": "player_joined",
  "player": { "playerId": "...", "name": "Alice", "isHost": false, "isReady": false, "score": 0 }
}
```

#### `player_left` — Player Disconnect Notification
```json
{
  "type": "player_left",
  "playerId": "client-uuid",
  "name": "Alice"
}
```

#### `lobby_update` — Full Player List Sync
Broadcast after any join/leave/ready change.
```json
{
  "type": "lobby_update",
  "players": [
    { "playerId": "...", "name": "Alice", "isHost": true, "isReady": true, "score": 130 }
  ]
}
```

#### `ready` — Ready Toggle
```json
{
  "type": "ready",
  "playerId": "client-uuid",
  "ready": true
}
```

#### `heartbeat` — Keep-Alive
Sent by host every 10 seconds.
```json
{
  "type": "heartbeat",
  "time": 1690000000000
}
```

### Round Management

#### `round_start` — New Round Begins
```json
{
  "type": "round_start",
  "drawerId": "host-uuid",
  "roundId": "round-uuid",
  "timeLimit": 90
}
```

#### `secret_word_assigned` — Secret Word (to drawer only)
Sent via `sendTo()` to the drawer only.
```json
{
  "type": "secret_word_assigned",
  "drawerId": "host-uuid",
  "hash": "apple"
}
```
> **Note:** The `hash` field contains the plain-text word (sent only to the drawer via targeted WebSocket message, not broadcast).

#### `timer_update` — Timer Tick
Broadcast every second during a round.
```json
{
  "type": "timer_update",
  "secondsRemaining": 45
}
```

#### `round_end` — Round Over
```json
{
  "type": "round_end",
  "word": "apple",
  "scores": [{ "playerId": "...", "name": "Bob", "score": 230 }]
}
```

#### `game_end` — All Rounds Complete
```json
{
  "type": "game_end",
  "scores": [{ "playerId": "...", "name": "Alice", "score": 500 }]
}
```

### Drawing

#### `stroke_start` — Begin New Stroke
```json
{
  "type": "stroke_start",
  "strokeId": "stroke-uuid",
  "playerId": "host-uuid",
  "color": "#FF0000",
  "thickness": 5.0,
  "x": 120.5,
  "y": 220.3,
  "timestamp": 1690000000000
}
```

#### `stroke_point` — Add Point to Stroke
```json
{
  "type": "stroke_point",
  "strokeId": "stroke-uuid",
  "x": 125.0,
  "y": 230.0,
  "timestamp": 1690000000050
}
```

#### `stroke_points` — Batch Points
```json
{
  "type": "stroke_points",
  "strokeId": "stroke-uuid",
  "pts": [[125.0, 230.0, 1690000000050], [130.0, 235.0, 1690000000100]]
}
```

#### `stroke_end` — Finish Stroke
```json
{
  "type": "stroke_end",
  "strokeId": "stroke-uuid"
}
```

#### `stroke_remove` — Undo Stroke
```json
{
  "type": "stroke_remove",
  "strokeId": "stroke-uuid"
}
```

#### `canvas_clear` — Clear All Strokes
```json
{
  "type": "canvas_clear"
}
```

### Guessing

#### `guess` — Submit Guess
```json
{
  "type": "guess",
  "playerId": "client-uuid",
  "text": "apple"
}
```

#### `correct_guess` — Correct Guess Notification
```json
{
  "type": "correct_guess",
  "playerId": "client-uuid",
  "word": "apple",
  "points": 130
}
```

## Message Flow Diagrams

### Join Flow
```
Client                          Host
  │──── join ──────────────────►│
  │                              │── update GameController
  │◄── player_joined ───────────│
  │◄── lobby_update ────────────│
```

### Round Flow
```
Host                            All Clients
  │── round_start ─────────────►│
  │── secret_word_assigned ────►│ (drawer only)
  │                              │
  │◄── stroke_start ────────────│ (drawer)
  │── stroke_start ────────────►│ (broadcast to guessers)
  │                              │
  │◄── guess ───────────────────│ (guesser)
  │── guess ───────────────────►│ (broadcast for chat)
  │── correct_guess ───────────►│ (if correct)
  │                              │
  │── timer_update ────────────►│ (every 1s)
  │── round_end ───────────────►│ (timer=0)
```

## Rate Limiting

- **Guesses:** Max 3 per player per 2-second window (server-side enforcement)
- **Messages:** Max 16KB per message (both host and client)

## Reconnection

- Client attempts up to **3 reconnects** with **2-second delay** between attempts
- On permanent failure, client returns to Join screen with error message
- Host detects disconnects via WebSocket close events and broadcasts `player_left`
