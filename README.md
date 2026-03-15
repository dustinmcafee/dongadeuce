# Dong-A-Deuce

<p align="center">
  <img src="resources/dongadeuce_icon.svg" alt="Dong-A-Deuce Icon" width="200"/>
</p>

A lightweight, cross-platform MTG Commander game client built with Kotlin and Compose Multiplatform.

[![LinkedIn](https://img.shields.io/badge/LinkedIn-Dustin%20McAfee-blue?style=flat&logo=linkedin)](https://www.linkedin.com/in/dustinmcafee/)

**Support this project:**

[![GitHub Sponsors](https://img.shields.io/badge/Sponsor-❤-red?style=flat&logo=github-sponsors)](https://github.com/sponsors/dustinmcafee)
[![PayPal](https://img.shields.io/badge/PayPal-Donate-blue?style=flat&logo=paypal)](https://paypal.me/dustinmcafee)
[![Buy Me A Coffee](https://img.shields.io/badge/Buy%20Me%20A%20Coffee-☕-yellow?style=flat&logo=buy-me-a-coffee)](https://buymeacoffee.com/dustinmcafee)
[![Bitcoin](https://img.shields.io/badge/Bitcoin-₿-orange?style=flat&logo=bitcoin)](#crypto-donations)
[![Ethereum](https://img.shields.io/badge/Ethereum-Ξ-blue?style=flat&logo=ethereum)](#crypto-donations)
[![Solana](https://img.shields.io/badge/Solana-◎-purple?style=flat&logo=solana)](#crypto-donations)
[![Monero](https://img.shields.io/badge/Monero-XMR-grey?style=flat&logo=monero)](#crypto-donations)

<details>
<summary id="crypto-donations">💰 Crypto Donations</summary>

**Bitcoin (BTC)**
```
3QVD3H1ryqyxhuf8hNTTuBXSbczNuAKaM8
```

**Ethereum (ETH)**
```
0xaFE28A1Dd57660610Ef46C05EfAA363356e98DC7
```

**Solana (SOL)**
```
6uWx4wuHERBpNxyWjeQKrMLBVte91aBzkHaJb8rhw4rn
```

**Monero (XMR)**
```
8C5aCs7Api3WE67GMw54AhQKnJsCg6CVffCuPxUcaKoiMrnaicyvDch8M2CXTm1DJqhpHKxtLvum9Thw4yHn8zeu7sj8qmC
```

</details>

## Features

- **Commander-focused**: Designed specifically for EDH/Commander format
- **Hotseat Multiplayer**: 2-6 players on the same device
- **Network Multiplayer**: Host or join games over LAN, or connect to a dedicated server (2-6 players)
- **Dedicated Server**: Run on Android (foreground service) or JVM, with self-signed TLS encryption (TOFU)
- **Multi-format Deck Import**: Load decks from Cockatrice (.cod), .dec, .dek, .txt, .mwDeck formats
- **Clipboard Paste**: Paste deck lists directly from clipboard on desktop and Android
- **Commander Selection**: Interactive dialog when importing decks without explicit commander
- **Cross-platform**: Runs on Windows, macOS, Linux, and Android
- **Offline Card Cache**: 500MB+ Scryfall bulk data for instant deck loading
- **Settings Persistence**: Player name and network settings saved between sessions
- **Keyboard Shortcuts**: 120+ shortcuts for fast gameplay

## Download & Install

Download the latest release from [GitHub Releases](https://github.com/dustinmcafee/dongadeuce/releases/latest).

### Windows
1. Download `Commander.MTG-X.X.X.msi`
2. Run the installer
3. Launch "Commander MTG" from the Start Menu

*Or download `dongadeuce-windows-X.X.X.jar` and run with `java -jar dongadeuce-windows-X.X.X.jar`*

### macOS
1. Download `Commander.MTG-X.X.X.dmg`
2. Open the DMG and drag to Applications
3. Launch from Applications folder

*First launch: Right-click → Open to bypass Gatekeeper*

### Linux
**Debian/Ubuntu:**
```bash
sudo dpkg -i dong-a-deuce_X.X.X_amd64.deb
dong-a-deuce
```

**Other distributions:**
```bash
java -jar dongadeuce-windows-X.X.X.jar
```

### Android
Android APK builds are available from GitHub Releases. Full touch support with drag-and-drop battlefield, gesture-based card interactions, and feature parity with the desktop version.

### First Run
1. Click "Download Cache" to get card data (~500MB, one-time)
2. Load a deck file (examples included in repo)
3. Start a hotseat or network game

## Project Structure

```
dongadeuce/
├── shared/              # Shared game logic and models (Kotlin Multiplatform)
│   ├── models/         # Card, Deck, GameState, Player, Zone
│   ├── network/        # GameEngine, GameServer, GameClient, protocol
│   ├── server/         # GameRoom, LobbyManager, ServerConfig (shared by JVM + Android)
│   ├── tls/            # TLS config, cert generation, trusted servers store
│   ├── settings/       # User settings persistence
│   └── game/           # Game logic, deck parser
├── desktop/            # Compose Desktop UI
│   ├── ui/             # UI components (game screen, zones, cards)
│   ├── viewmodel/      # ViewModels with StateFlow (MVVM architecture)
│   └── utils/          # Image cache, utilities
├── android/            # Android app + dedicated server foreground service
├── server/             # Dedicated game server (standalone JVM app)
├── mcp-server/         # MCP server for AI integration
└── resources/          # Icons and assets
```

## Architecture

This project follows the **MVVM (Model-View-ViewModel)** pattern with a **dual-mode networking architecture**:

### Dual-Mode Networking

The game supports two network modes that share the same core components:

- **LAN / P2P Mode**: One player runs an embedded Ktor server; all players (including host) connect via GameClient
- **Dedicated Server Mode**: A standalone server manages multiple game rooms; players connect by game code
- **Android Dedicated Server**: Runs as a foreground service with persistent notification, survives app close and device reboots via WorkManager
- **Self-Signed TLS (TOFU)**: Server auto-generates RSA 2048 cert on first start; clients verify via SSH-style fingerprint prompt, with auto-renewal

Shared components (zero duplication):
- **GameEngine**: All game logic — validation, execution, state management
- **GameClient**: All players use this to connect (even the P2P host connects to localhost)
- **GameMessage protocol**: Identical for both modes
- **GameRoom / LobbyManager**: Shared by JVM and Android dedicated servers

### MVVM Layers

- **Models** (`shared/models/`): Domain objects like Card, Deck, Player, GameState
- **ViewModels** (`shared/viewmodel/`): Manage UI state with Kotlin StateFlow
  - `GameViewModel`: Manages game state, player actions, card movements
  - `MenuViewModel`: Handles menu navigation, deck loading, lobby management
- **Views** (`desktop/ui/`, `android/ui/`): Composable UI components that observe ViewModel state

Benefits:
- Clean separation of concerns
- Testable business logic
- Reactive state management with StateFlow
- Single code path for host and client networking
- Easy to integrate P2P networking (ViewModels handle network events)

## Building and Running

### Prerequisites
- JDK 11 or higher
- Gradle (wrapper included)

### Run the application
```bash
cd dongadeuce
./gradlew desktop:run
```

### Build distributions
```bash
# macOS .dmg
./gradlew desktop:packageDmg

# Windows .msi
./gradlew desktop:packageMsi

# Linux .deb
./gradlew desktop:packageDeb
```

## Current Status (v6.1.0-beta)

**Desktop:** 99% Complete - Fully Playable! ✅
**Android:** 99% Complete - Fully Playable! ✅
**Network Mode:** 100% Complete - Fully Playable! ✅

### ✅ Fully Implemented

**Core Gameplay:**
- **Turn/Phase System** - Full MTG phase cycle with visual indicator
- **Commander Damage Tracking** - Complete UI with 21-damage lethal detection
- **Card Context Menus** - Right-click menus for all zones with comprehensive actions
- **Library Search** - Full search/filter dialog with card manipulation
- **Zone Viewers** - Interactive dialogs for graveyard, exile, command zone
- **Drag-and-Drop Battlefield** - Grid-based card positioning system
- **Card Images** - Async loading with 500MB+ offline cache
- **Tap/Untap** - Double-click and context menu support
- **Counters** - Add/remove +1/+1, charge, and custom counters
- **Card Attachments** - Aura/Equipment attachment system
- **Flip Cards** - Full flip card support
- **Face Down Cards** - Morph/manifest support
- **Life Tracking** - Automatic loss detection
- **Draw from Empty Library** - Automatic loss detection
- **All Zone Operations** - Move cards between any zones
- **Library Operations** - Draw, mill, shuffle, search, tutor, mulligan, peek top/bottom N
- **P/T Modifications** - Increase/decrease/set/reset/flow power and toughness
- **Card Annotations** - Custom text notes on cards
- **Token Creation** - Create tokens via Scryfall search or custom
- **Card Copy/Clone** - Clone cards with visual indicator
- **Player Counters** - Poison, energy, experience, and custom counters
- **Die Rolling** - D4, D6, D8, D10, D12, D20, D100, custom dice, coin flip
- **Game Log** - Real-time event logging with 21 event types
- **Chat Messages** - In-game chat between players
- **Give Control** - Transfer control of permanents between players

**Hotseat Multiplayer:**
- **2-6 Player Support** - Full local multiplayer
- **Per-Player Deck Loading** - Each player loads their own deck
- **Automatic Player Rotation** - UI rotates to show active player
- **Hand Privacy** - Only active player sees their cards
- **Turn Passing** - Automatic player advancement

**Network Multiplayer:**
- **Host/Join Games** - WebSocket-based networking over local network or internet
- **Dedicated Server** - Run on Android (foreground service) or PC (standalone JVM jar)
- **Self-Signed TLS** - Auto-generated certs with SSH-style trust-on-first-use (TOFU)
- **Certificate Auto-Renewal** - 10-year validity, auto-regenerates 30 days before expiry
- **Lobby System** - Player ready status, host can kick players
- **Create Game via REST** - Clients create game rooms on dedicated servers via API
- **Real-time Sync** - Full game state synchronization
- **35+ Network Actions** - All game actions supported over network
- **Disconnect Handling** - Game pauses on player disconnect
- **Action Validation** - Server-side cheating prevention
- **Unique Player Names** - Auto-rename duplicate names

**Settings & Persistence:**
- **Settings Dialog** - Gear icon in main menu for configuration
- **Player Name** - Persisted between sessions
- **Server Address** - Last used address saved
- **Server Port** - Custom port configuration
- **Default Deck Directory** - File picker remembers last location
- **Cross-platform Storage** - Windows: %APPDATA%, Linux/macOS: ~/.commandermtg

**Technical:**
- MVVM architecture with StateFlow (100% compliant)
- Ktor WebSocket server (Netty for TLS, CIO for plain) and client (OkHttp for TLS, CIO for plain)
- Self-signed TLS with Bouncy Castle cert generation (Android) and Ktor TLS certificates (JVM)
- Scryfall API integration
- Bulk card cache with progress UI
- Multi-format deck parser (Cockatrice, .dec, .dek, .txt, .mwDeck)
- Sideboard support with deck loading
- 590+ passing tests (480 JVM shared, 29 JVM server, 86 Android instrumented including 8 TLS)
- CI/CD with GitHub Actions (APK signing, test automation)
- Comprehensive input validation
- Cross-platform packaging (Windows, macOS, Linux, Android)

**Keyboard Shortcuts (120+ implemented):**
- **Game Phases**: F5-F10 for phases, Ctrl+Space/Tab for next phase, Ctrl+Enter for pass turn
- **Card Actions**: T to tap, Ctrl+U untap all, Del to graveyard, Ctrl+X to exile
- **Power/Toughness**: Ctrl/Alt +/- for P/T, Ctrl+Alt+=/- for both
- **Counters**: Ctrl+./,  Alt+./, for colored counters A-F
- **Library**: Ctrl+D draw, Ctrl+M mulligan, Ctrl+S shuffle
- **View Zones**: F3 library, F4 graveyard, Ctrl+W peek top
- **Selection**: Ctrl+A select all, Ctrl+Shift+X select row
- **Arrows**: Alt+A draw arrow, Ctrl+R remove arrows
- **Mana Counters**: W/U/B/R/G/X for mana pool tracking
- **Stack Until Found**: Ctrl+Shift+Y to reveal cards until match

### ❌ Not Yet Implemented

**Missing Features:**
- **Game Save/Load** - Games meant to be played in one session

### Completion Status
- **Desktop:** ~99% complete (fully playable)
- **Android:** ~99% complete (fully playable)
- **Network Mode:** 100% complete (P2P + Dedicated Server + TLS)

## Tech Stack

- **Kotlin**: Primary language (Kotlin Multiplatform)
- **Compose Multiplatform**: Cross-platform UI framework (Desktop + Android)
- **Ktor**: Networking — Netty server (TLS), CIO server (plain), OkHttp client (TLS), CIO client (plain)
- **Bouncy Castle**: Self-signed certificate generation on Android (bcprov + bcpkix)
- **kotlinx.serialization**: JSON serialization for network protocol
- **Scryfall API**: Card data and images with offline bulk cache
- **WorkManager**: Android server auto-restart after device reboot

## Game Zones

The UI includes all Commander zones:
- **Command Zone**: Your commander
- **Library**: Draw deck
- **Hand**: Cards in hand
- **Battlefield**: Permanents in play
- **Graveyard**: Discarded/destroyed cards
- **Exile**: Exiled cards
- **Stack**: Spells/abilities being resolved (not visible yet)

## Running a Dedicated Server

### On Android
1. Open DongADeuce on your Android device
2. Tap "Host Dedicated Server" from the main menu
3. Configure port, max games, max players
4. Check "Enable TLS encryption" for internet-facing servers
5. Tap "Start Server"
6. Share the displayed IP address and fingerprint with players

The server runs as a foreground service — it stays alive when you close the app and auto-restarts after device reboots.

### On PC (JVM)
```bash
cd server
../gradlew shadowJar
java -jar build/libs/dongadeuce-server-*-all.jar
```

Environment variables:
- `PORT` — Server port (default: 9090)
- `MAX_GAMES` — Max concurrent game rooms (default: 100)
- `MAX_PLAYERS` — Max players per game (default: 6)
- `TLS_ENABLED` — Set to `true` to enable TLS
- `TLS_KEYSTORE_PATH` — Path to keystore file (default: `./server.jks`, auto-generated)

### Connecting as a Client
1. In the Join screen, select "Dedicated Server" mode
2. Enter the server address and port
3. Check "Encrypt connection (TLS)" if the server has TLS enabled
4. Click "Create New Game" to create a room, or enter an existing game code
5. Click "Connect"
6. On first TLS connection, verify the fingerprint matches what the server displays

## Next Steps

See [TODO.md](TODO.md) for development roadmap and feature status.

### Future Enhancements (Optional Polish)
- **Animations** - Card movement and tap animations
- **Sound Effects** - Audio feedback for actions
- **Themes** - Light mode, custom card backs
- **Deck Builder** - In-app deck creation and editing
- **Spectator Mode** - Watch games in progress

**Result:** Feature-complete Commander experience

