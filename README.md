# Commander MTG

A lightweight, cross-platform MTG Commander game client built with Kotlin and Compose Multiplatform.

## Features (Planned)

- **Commander-focused**: Designed specifically for EDH/Commander format
- **P2P Networking**: Host or join games directly, no central server needed
- **Deck Import**: Load decks from text format
- **Cross-platform**: Runs on Windows, macOS, and Linux

## Project Structure

```
commander-mtg/
├── shared/              # Shared game logic and models
│   ├── models/         # Card, Deck, GameState, Player, Zone
│   ├── network/        # P2P networking protocol (TODO)
│   └── game/           # Game logic, deck parser
├── desktop/            # Compose Desktop UI
│   ├── ui/             # UI components (game screen, zones, cards)
│   ├── viewmodel/      # ViewModels with StateFlow (MVVM architecture)
│   ├── client/         # Network client (TODO)
│   └── server/         # Network server/host (TODO)
└── resources/          # Card images cache (TODO)
```

## Architecture

This project follows the **MVVM (Model-View-ViewModel)** pattern:

- **Models** (`shared/models/`): Domain objects like Card, Deck, Player, GameState
- **ViewModels** (`desktop/viewmodel/`): Manage UI state with Kotlin StateFlow
  - `GameViewModel`: Manages game state, player actions, card movements
  - `MenuViewModel`: Handles menu navigation, deck loading, lobby management
- **Views** (`desktop/ui/`): Composable UI components that observe ViewModel state

Benefits:
- Clean separation of concerns
- Testable business logic
- Reactive state management with StateFlow
- Easy to integrate P2P networking (ViewModels handle network events)

## Building and Running

### Prerequisites
- JDK 11 or higher
- Gradle (wrapper included)

### Run the application
```bash
cd commander-mtg
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

## Current Status (v2.10.6)

### ✅ Implemented Features
- Project structure and Gradle setup
- Core domain models (Card, Deck, Player, GameState, Zones)
- MVVM architecture with ViewModels and StateFlow
- Text-based deck format parser
- Complete UI flow (menu, lobby, game screen)
- Deck loading with file chooser
- **Scryfall API integration** - Full card data (mana cost, type, text)
- **Starting hand draw** - Automatic 7-card opening hands
- **Draw cards** - Draw button with working functionality
- **Hand view** - Dialog showing your cards with Play button
- **Play cards** - Move cards from hand to battlefield
- **Hotseat Mode** - Full 2-4 player support on same computer
- **Battlefield Grid** - Drag-and-drop card arrangement
- **Tap/Untap** - Double-click to tap/untap cards
- **Zone Viewers** - Click to view graveyard, exile, library, command zone
- Game zones with dynamic card counts
- Life tracking with +/- buttons
- Commander zone support

### ⏳ In Progress / Planned
- **Turn/Phase System** - Phase indicator and turn advancement
- **Commander Damage Tracking** - Damage matrix UI
- **Game Log** - Event history and action feed
- **P2P Networking** - Ktor WebSockets for remote multiplayer
- **Card Images** - Image loading and caching
- **Additional Game Actions** - Scry, mill, tutor, tokens, etc.

### Completion Status
- **Hotseat Mode:** ~75% complete (missing turn system, commander damage UI)
- **Network Mode:** ~5% complete (protocol defined, not implemented)

## Tech Stack

- **Kotlin**: Primary language
- **Compose Multiplatform**: Cross-platform UI framework
- **Ktor**: Networking (server + client)
- **kotlinx.serialization**: JSON serialization for network protocol
- **Scryfall API**: Card data and images (planned)

## Game Zones

The UI includes all Commander zones:
- **Command Zone**: Your commander
- **Library**: Draw deck
- **Hand**: Cards in hand
- **Battlefield**: Permanents in play
- **Graveyard**: Discarded/destroyed cards
- **Exile**: Exiled cards
- **Stack**: Spells/abilities being resolved (not visible yet)

## Next Steps

See [TODO.md](TODO.md) for detailed development roadmap.

### Immediate Priorities (v2.11.0)
1. **Turn/Phase System** - Critical for playable games
2. **Commander Damage UI** - Essential for Commander format
3. **Game Log** - Important for multiplayer awareness
4. **Commander Tax** - Track commander casting costs

### Medium Term (v2.12.0)
5. Card images with caching
6. Additional game actions (scry, mill, tokens)
7. Improved battlefield visualization
8. UI enhancements and keyboard shortcuts

### Long Term (v3.0.0)
9. P2P networking with Ktor WebSockets
10. Lobby system for remote multiplayer
11. Network game state synchronization
12. Spectator mode

