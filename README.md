# Commander MTG

A lightweight, cross-platform MTG Commander game client built with Kotlin and Compose Multiplatform, inspired by Cockatrice.

## Features (Planned)

- **Commander-focused**: Designed specifically for EDH/Commander format
- **P2P Networking**: Host or join games directly, no central server needed
- **Deck Import**: Load decks from Cockatrice format
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

## Current Status

✅ Project structure and Gradle setup
✅ Core domain models (Card, Deck, Player, GameState, Zones)
✅ MVVM architecture with ViewModels and StateFlow
✅ Cockatrice deck format parser
✅ Complete UI flow (menu, lobby, game screen)
✅ Deck loading with file chooser
✅ **Scryfall API integration** - Full card data (mana cost, type, text)
✅ **Starting hand draw** - Automatic 7-card opening hands
✅ **Draw cards** - Draw button with working functionality
✅ **Hand view** - Dialog showing your cards with Play button
✅ **Play cards** - Move cards from hand to battlefield
✅ Game zones with dynamic card counts
✅ Life tracking with +/- buttons
⏳ P2P networking (Ktor WebSockets)
⏳ Card image rendering
⏳ Drag-and-drop for card movement
⏳ Tap/untap cards on battlefield
⏳ Full multiplayer support

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

1. Implement Ktor WebSocket server/client for P2P
2. Integrate Scryfall API for card data
3. Add card rendering with images
4. Implement drag-and-drop for card movement
5. Add deck loading UI
6. Implement basic game actions
