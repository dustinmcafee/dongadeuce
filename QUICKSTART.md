# Quick Start Guide

## Running the Application

```bash
./gradlew desktop:run
```

This will launch the Dong-A-Deuce application.

## Loading a Deck

Three example deck files are included in the project root:
- `edgar_markov_deck.txt` - Edgar Markov Vampire tribal
- `first_sliver_deck.txt` - The First Sliver

To load a deck:
1. Launch the application
2. Click "Load Deck"
3. Navigate to one of the `.txt` files
4. The commander name will appear in the UI

## Starting a Game

### Host a Game (Local)
1. Load your deck
2. Click "Host Game"
3. Click "Start Game" (for now, single player testing)

### Game Controls

**Life Tracking:**
- Click `-` to decrease life by 1
- Click `+` to increase life by 1

**Game Zones:**
- All zones are visible with card counts
- Command Zone: Your commander
- Library: Your deck
- Hand: Cards in hand
- Battlefield: Permanents in play
- Graveyard: Cards in graveyard
- Exile: Exiled cards

## What Works Now

✅ Menu system with deck loading
✅ Host/Join lobby screens (UI only, networking TODO)
✅ Game screen with all zones
✅ Life tracking
✅ MVVM architecture with reactive state
✅ Text-based deck file parsing

## What's Coming Next

- P2P networking with Ktor WebSockets
- Card data from Scryfall API
- Visual card rendering
- Drag-and-drop card movement
- Draw, play, tap/untap actions
- Multiplayer gameplay

## Project Structure

- `shared/` - Game logic, models, networking
- `desktop/` - Compose UI and ViewModels
- `resources/` - Card images cache (when implemented)

## Building Distributions

```bash
# macOS .dmg
./gradlew desktop:packageDmg

# Windows .msi
./gradlew desktop:packageMsi

# Linux .deb
./gradlew desktop:packageDeb
```

## Development

The project uses MVVM architecture:
- **Models**: Domain objects in `shared/models/`
- **ViewModels**: State management in `desktop/viewmodel/`
- **Views**: Composable UI in `desktop/ui/`

State flows from ViewModel → UI using Kotlin StateFlow.
