# Architecture — Hotspot Skribble

## System Overview

Hotspot Skribble is a **host-authoritative LAN multiplayer** game. One Android device acts as both game server and player; other devices connect as clients. All communication happens over the local Wi-Fi hotspot network with zero internet dependency.

```
┌────────────────────────────────────────────────────────────────┐
│                        HOST DEVICE                              │
│                                                                  │
│  ┌─────────────┐  ┌──────────────┐  ┌────────────────────┐    │
│  │ Discovery    │  │  WebSocket   │  │   Game Engine      │    │
│  │ Broadcaster  │  │  Server      │  │   (GameController  │    │
│  │ (UDP 60000)  │  │  (TCP 50000) │  │    ScoreManager    │    │
│  └──────┬───────┘  └──────┬───────┘  │    WordBank)       │    │
│         │                  │          └─────────┬──────────┘    │
│         │                  │                    │                │
│         │          ┌───────▼────────┐           │                │
│         │          │  SkidlViewModel │◄──────────┘                │
│         │          │  (UI State)     │                            │
│         │          └───────┬─────────┘                            │
│         │                  │                                      │
│         │          ┌───────▼─────────┐                            │
│         │          │  Compose UI      │                            │
│         │          │  (6 screens)     │                            │
│         │          └──────────────────┘                            │
└─────────┼──────────────────────────────────────────────────────────┘
          │  UDP broadcast
          ▼
┌────────────────────────────────────────────────────┐
│                    CLIENT DEVICE                     │
│                                                      │
│  ┌─────────────┐  ┌──────────────┐                  │
│  │ Discovery    │  │  WebSocket   │                  │
│  │ Listener     │  │  Client      │                  │
│  │ (UDP 60000)  │  │  (OkHttp)    │                  │
│  └──────────────┘  └──────┬───────┘                  │
│                           │                          │
│                   ┌───────▼─────────┐                │
│                   │  SkidlViewModel  │                │
│                   │  (UI State)      │                │
│                   └───────┬──────────┘                │
│                           │                          │
│                   ┌───────▼──────────┐                │
│                   │  Compose UI       │                │
│                   └──────────────────┘                │
└──────────────────────────────────────────────────────┘
```

## Layer Breakdown

### 1. Model Layer (`model/`)
Pure data classes with kotlinx.serialization annotations.

| Class | Role |
|-------|------|
| `SkidlMessage` | Sealed interface — 20 message subtypes |
| `Player` | Player identity + state (name, score, ready, host) |
| `Stroke` / `StrokePoint` | Drawing data model |
| `GameState` | `LobbyState`, `RoundState`, `GuessMessage`, `GameSettings` |

### 2. Network Layer (`network/`)

| Component | Role | Technology |
|-----------|------|------------|
| `HostNetworkingManager` | Embedded WebSocket server | Java-WebSocket 1.5.4 |
| `ClientNetworkingManager` | WebSocket client with reconnect | OkHttp 4.11.0 |
| `DiscoveryBroadcaster` | UDP broadcast host advertisements | `DatagramSocket` |
| `DiscoveryListener` | UDP receive host discoveries | `DatagramSocket` |
| `SkidlMessageSerializer` | Polymorphic JSON deserializer | kotlinx.serialization |

### 3. Game Engine (`game/`)

| Component | Role |
|-----------|------|
| `GameController` | Manages lobby state + round state via `MutableStateFlow` |
| `ScoreManager` | Tracks scores with time-based bonus (thread-safe `ConcurrentHashMap`) |
| `WordBank` | Loads words from `assets/words.json`, provides non-repeating random words |

### 4. UI Layer (`ui/`)

Built entirely with **Jetpack Compose** and **Material 3**.

| Screen | Route | Purpose |
|--------|-------|---------|
| `WelcomeScreen` | `welcome` | Landing page — Host or Join |
| `HostSetupScreen` | `host` | Room name, code, display name config |
| `JoinScreen` | `join` | Discovery list + manual IP entry |
| `LobbyScreen` | `lobby` | Player list, ready toggles, QR share |
| `GameScreen` | `game` | Drawing canvas + guess input (split by role) |
| `ScoresScreen` | `scores` | Round/game scores with medals |

Navigation is driven by `SkidlUiState.screen` enum, mapped via `AppNavHost`.

## Design Decisions

### Host-Authoritative Model
All game state mutations go through the host. Clients send actions (strokes, guesses); host validates, updates state, and broadcasts to all clients. This prevents cheating and ensures consistency.

### Single ViewModel
`SkidlViewModel` handles both host and client logic. The `isHosting` flag determines which code paths execute. This keeps the architecture simple for a single-APK game.

### Sealed Interface for Messages
Using Kotlin's sealed interface with `@SerialName` annotations allows type-safe polymorphic serialization. The custom `SkidlMessageSerializer` dispatches on the `type` field for deserialization.

### ConcurrentHashMap for Thread Safety
Network callbacks arrive on IO threads while UI updates happen on Main. All shared mutable maps use `ConcurrentHashMap` to prevent `ConcurrentModificationException`.

### Custom JSON Discriminator
The Json configuration uses `classDiscriminator = "#class"` to avoid conflict with our `type` property that serves as the manual discriminator for the `SkidlMessageSerializer`.

### Coroutine Scopes
- `applicationScope` — Long-lived networking operations (survive ViewModel rotation)
- `viewModelScope` — UI-bound operations (timer, connection monitoring)

## Data Flow

### Drawing Flow (Drawer → All Clients)
```
Drawer Touch → StrokeStartMessage → Host.broadcast() → Client.mirror() → Canvas render
             → StrokePointMessage → Host.updateStroke() + broadcast() → Client.mirror()
             → StrokeEndMessage → Host.broadcast() → Client (no-op currently)
```

### Guess Flow
```
Guesser types → GuessNetworkMessage → Host.handleGuess()
  → Rate limit check (3/2s)
  → Correct? → ScoreManager.recordCorrectGuess() → CorrectGuessMessage broadcast
  → Wrong? → GuessNetworkMessage broadcast (shows in chat)
```

### Round Lifecycle
```
Host.hostStartRound()
  → Pick drawer (round-robin)
  → WordBank.nextWord()
  → GameController.startRound()
  → Broadcast RoundStartMessage
  → SendTo drawer: SecretWordAssignedMessage
  → Start timer coroutine (1s ticks → TimerUpdateMessage)
  → Timer expires → RoundEndMessage (reveal word + scores)
  → hostNextRound() or endGame()
```
