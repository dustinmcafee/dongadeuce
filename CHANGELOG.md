# Changelog

All notable changes to Commander MTG will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [1.0.1] - 2025-10-27

### Added
- Scryfall API integration for fetching card data (mana cost, type, oracle text, images)
- Deck loading now populates game state with actual CardInstance objects
- Automatic starting hand draw (7 cards for each player)
- Draw card button in player area
- Hand view dialog showing all cards in hand with card details
- Play card from hand to battlefield functionality
- Loading progress indicator during deck/card data fetching
- Rate limiting for Scryfall API requests (100ms between calls)

### Changed
- MenuViewModel now uses coroutines to fetch card data asynchronously
- Cards now contain full data from Scryfall (not just names)
- Game initialization automatically loads deck and draws starting hands

### Technical Details
- Added Ktor content negotiation and JSON serialization dependencies
- Created ScryfallApi client with proper error handling
- Enhanced GameViewModel with `drawStartingHand` and `getCards` methods
- HandDialog composable for viewing and playing cards from hand

## [1.0.0] - 2025-10-27

### Added
- Initial project structure with Gradle + Compose Multiplatform
- MVVM architecture with ViewModels and StateFlow
- Core domain models (Card, Deck, Player, GameState, Zone, CardInstance)
- Cockatrice deck format parser
- Main menu with deck loading via file chooser
- Host lobby screen (UI only, networking not implemented)
- Join lobby screen (UI only, networking not implemented)
- Game screen with all MTG zones (Library, Hand, Battlefield, Graveyard, Exile, Command Zone)
- Life tracking with +/- buttons
- Dynamic card counts per zone
- Cross-platform build support (Windows, macOS, Linux)
- Three example Commander decks included
- README and QUICKSTART documentation

### Technical Details
- Kotlin 1.9.21
- Compose Multiplatform 1.5.11
- Ktor 2.3.7 (dependencies added, not yet used)
- kotlinx.serialization for JSON
- GameViewModel manages game state
- MenuViewModel handles navigation and deck loading

### Known Limitations
- No networking implementation
- Cards are name-only (no data from Scryfall)
- No visual card rendering
- No drag-and-drop functionality
- No actual game actions (can't draw, play, or move cards)
- Deck loading parses files but doesn't populate game state
- No commander damage tracking UI
- No turn/phase system

---

## Version History

- **1.0.0** - Initial scaffold with MVVM architecture
- **1.0.1** - Scryfall integration and basic gameplay

---

## TODO LIST - Path to 2+ Player Multiplayer

### 🔴 CRITICAL - Blocking Multiplayer (v1.1.0)

#### Networking Foundation
- [ ] Implement `GameServer.kt` with Ktor WebSockets
  - [ ] Accept player connections
  - [ ] Maintain connected players list
  - [ ] Broadcast game state updates
  - [ ] Handle player disconnects
- [ ] Implement `GameClient.kt` with Ktor WebSockets
  - [ ] Connect to host server
  - [ ] Send local actions to server
  - [ ] Receive and apply game state updates
- [ ] Create `GameMessage.kt` network protocol
  - [ ] PlayerJoined/PlayerLeft messages
  - [ ] GameStarted message
  - [ ] All game action messages (draw, play, move, tap, etc.)
  - [ ] Chat messages
- [ ] Integrate networking into MenuViewModel
  - [ ] Start server in `startHosting()`
  - [ ] Connect client in `connectToGame()`
  - [ ] Sync lobby player list over network
- [ ] Integrate networking into GameViewModel
  - [ ] Broadcast local actions to network
  - [ ] Listen for and apply network actions
  - [ ] Handle state synchronization

#### Multi-Player Game Initialization
- [ ] Pass connected players from lobby to game
  - [ ] Update MainScreen.kt to pass player list to GameScreen
  - [ ] Update GameScreen to accept player list parameter
- [ ] Initialize game with all players
  - [ ] Remove hardcoded single opponent
  - [ ] Use actual player list from lobby
  - [ ] Assign proper player IDs
- [ ] Load deck for each player
  - [ ] Each client loads their deck locally
  - [ ] Broadcast deck loaded status to all players
  - [ ] Wait for all players before starting
  - [ ] Create `loadDeckForPlayer(playerId, deck)` function

#### Multi-Player UI Layout
- [ ] Create dynamic player layout (2-4 players)
  - [ ] TwoPlayerLayout composable
  - [ ] ThreePlayerLayout composable
  - [ ] FourPlayerLayout composable
  - [ ] Layout selection based on player count
- [ ] Display all opponents (not just first)
  - [ ] Loop through all opponents in UI
  - [ ] Show zones for each opponent
  - [ ] Show life totals for each opponent

### 🟡 HIGH PRIORITY - Core Gameplay (v1.2.0)

#### Battlefield Visualization
- [ ] Create `BattlefieldCard.kt` component
  - [ ] Display card name/image
  - [ ] Show tapped state (rotation)
  - [ ] Show counters
  - [ ] Show attached cards (auras/equipment)
  - [ ] Color-code by controller
- [ ] Implement `BattlefieldArea` with actual cards
  - [ ] Get all battlefield cards from GameState
  - [ ] Display in grid layout
  - [ ] Support click to select
  - [ ] Support right-click for context menu
- [ ] Add battlefield card interactions
  - [ ] Click to tap/untap
  - [ ] Drag to reorder/group
  - [ ] Right-click context menu

#### Zone Viewers
- [ ] Create `ZoneViewer.kt` dialogs
  - [ ] GraveyardDialog
  - [ ] ExileDialog
  - [ ] LibrarySearchDialog
- [ ] Make zones clickable
  - [ ] Add onClick handlers to ZoneCard
  - [ ] Show appropriate dialog when clicked
- [ ] Implement zone dialog actions
  - [ ] Return card to hand from graveyard
  - [ ] Return card to battlefield from graveyard
  - [ ] Exile from graveyard
  - [ ] Search library and put card in hand

#### Turn System UI
- [ ] Create `TurnIndicator.kt` component
  - [ ] Show active player name
  - [ ] Show current phase
  - [ ] Show turn number
  - [ ] Highlight active player
- [ ] Add turn control buttons
  - [ ] "Next Phase" button
  - [ ] "Pass Turn" button
  - [ ] Phase list display
- [ ] Implement turn/phase advancement
  - [ ] `advancePhase()` in GameViewModel
  - [ ] `passTurn()` in GameViewModel
  - [ ] Update active player
  - [ ] Sync turn changes over network

#### Card Images
- [ ] Create `CardImage.kt` component
  - [ ] Async image loading from Scryfall URI
  - [ ] Loading placeholder
  - [ ] Error fallback
  - [ ] Image caching
- [ ] Implement `ImageCache.kt`
  - [ ] Download images to local cache
  - [ ] Check cache before downloading
  - [ ] Cache management (size limits)
- [ ] Add card images throughout UI
  - [ ] Hand dialog
  - [ ] Battlefield cards
  - [ ] Zone viewers
  - [ ] Card preview on hover

### 🟢 MEDIUM PRIORITY - Enhanced Features (v1.3.0)

#### Commander Damage
- [ ] Create `CommanderDamageDialog.kt`
  - [ ] Matrix of all commanders vs all players
  - [ ] Click to add/subtract damage
  - [ ] Highlight 21+ damage
  - [ ] Button to open from player area
- [ ] Add commander damage tracking
  - [ ] Update Player model (already has field)
  - [ ] `takeCommanderDamage()` function
  - [ ] UI indicator when damaged by commander
  - [ ] Loss condition check (21+ damage)

#### Card Context Menu
- [ ] Create `CardContextMenu.kt`
  - [ ] Move to zone actions
  - [ ] Tap/untap
  - [ ] Add/remove counters
  - [ ] Attach to card
  - [ ] View details
- [ ] Add right-click handlers
  - [ ] Battlefield cards
  - [ ] Hand cards
  - [ ] Cards in zone viewers
- [ ] Implement all card actions
  - [ ] Move between zones
  - [ ] Counter management
  - [ ] Attach/detach

#### Missing GameViewModel Functions
- [ ] `shuffleLibrary(playerId)`
- [ ] `searchLibrary(playerId, predicate)`
- [ ] `millCards(playerId, count)`
- [ ] `mulligan(playerId)`
- [ ] `addCounter(cardId, type, amount)`
- [ ] `removeCounter(cardId, type, amount)`
- [ ] `flipCard(cardId)`
- [ ] `attachCard(sourceId, targetId)`
- [ ] `detachCard(cardId)`
- [ ] `untapAll(playerId)`
- [ ] `getBattlefieldCards()`
- [ ] `getPlayerBattlefieldCards(playerId)`

#### Enhanced Hand UI
- [ ] Multiple action buttons per card
  - [ ] Play to battlefield
  - [ ] Discard to graveyard
  - [ ] Exile from hand
  - [ ] Put on top of library
  - [ ] Put on bottom of library
- [ ] Card reordering in hand
- [ ] Card selection/multi-select

### 🔵 LOW PRIORITY - Quality of Life (v1.4.0)

#### Game Log
- [ ] Create game log component
  - [ ] Scrollable action history
  - [ ] "Alice drew 3 cards"
  - [ ] "Bob played Sol Ring"
  - [ ] Color-code by player
- [ ] Log all game actions
  - [ ] Draw cards
  - [ ] Play spells
  - [ ] Move cards
  - [ ] Life changes
  - [ ] Turn/phase changes

#### Chat System
- [ ] Create chat UI component
  - [ ] Text input field
  - [ ] Message history
  - [ ] Player name colors
- [ ] Implement chat messages
  - [ ] Send over network
  - [ ] Receive and display
  - [ ] Chat commands (/roll, /flip)

#### Keyboard Shortcuts
- [ ] Space: Pass priority
- [ ] Enter: Pass turn
- [ ] T: Tap selected card
- [ ] U: Untap all
- [ ] D: Draw card
- [ ] M: Mulligan

#### Card Hover/Preview
- [ ] Hover to see card details
- [ ] Zoom on hover
- [ ] Oracle text display
- [ ] Larger image preview

#### Search/Filter
- [ ] Search battlefield
- [ ] Filter by card type
- [ ] Filter by color
- [ ] Advanced search

#### Persistence
- [ ] Save game state
- [ ] Load game
- [ ] Export game log
- [ ] Game replay

### 🎨 POLISH (v2.0.0)

- [ ] Animations
  - [ ] Card movement animations
  - [ ] Tap/untap rotation animation
  - [ ] Zone transitions
  - [ ] Life counter animations
- [ ] Sound effects
  - [ ] Card draw sound
  - [ ] Tap sound
  - [ ] Life change sound
  - [ ] Turn change sound
- [ ] Themes/Skins
  - [ ] Dark mode (already default)
  - [ ] Light mode
  - [ ] Custom backgrounds
  - [ ] Custom card backs
- [ ] Settings
  - [ ] Sound volume
  - [ ] Animation speed
  - [ ] Default deck directory
  - [ ] Network port configuration
  - [ ] Player name persistence
- [ ] Deck Builder
  - [ ] Create/edit decks in-app
  - [ ] Search Scryfall
  - [ ] Validate Commander rules
  - [ ] Save to Cockatrice format

---

## Current Progress

**Overall Completion:** ~40% for full 2+ player Cockatrice-like experience

**By Category:**
- ✅ Core Architecture: 100%
- ✅ Single Player Deck Loading: 100%
- ✅ Scryfall Integration: 100%
- 🟡 Multi-Player Support: 10%
- ❌ Networking: 0%
- ❌ Battlefield Visualization: 0%
- 🟡 Zone Interactions: 20%
- ❌ Turn System: 0%
- ❌ Card Images: 0%
- ❌ Commander Damage UI: 0%
- 🟡 Game Actions: 30%

**Estimated Time to MVP (v1.2.0):**
- With networking: 2-3 weeks
- Without networking (local hotseat): 1 week
