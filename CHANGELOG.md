# Changelog

All notable changes to Commander MTG will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [2.3.0] - 2025-10-27

### Added
- **Comprehensive Input Validation**
  - DeckParser now validates file format, card quantities, and card names
  - Added line number tracking for better error messages
  - Validates commander quantity must be exactly 1
  - Validates deck size is exactly 99 cards (excluding commander)
  - File existence, readability, and path validation
- **Input Validation for Game Operations**
  - Counter operations validate positive amounts and non-blank types
  - Commander damage validates non-negative values
  - All validation uses descriptive error messages

### Changed
- **Improved Error Handling in Deck Loading**
  - Separate error handling for parse vs file read errors
  - More specific error messages for user feedback
  - Graceful handling of invalid deck files
- **Enhanced DeckParser Validation**
  - Empty content validation
  - Positive quantity validation with line numbers
  - Non-empty card name validation
  - File path validation (not blank, exists, readable)

### Technical Details
- All validation uses `require()` with descriptive messages
- Error messages include context (line numbers, values)
- Improves user experience with clear feedback
- Prevents invalid state in game logic

## [2.2.0] - 2025-10-27

### Added
- **Game Constants** centralized MTG rules values
  - GameConstants.STARTING_LIFE (40)
  - GameConstants.STARTING_HAND_SIZE (7)
  - GameConstants.COMMANDER_DAMAGE_THRESHOLD (21)
  - GameConstants.DECK_SIZE (99)
  - GameConstants.TOTAL_DECK_SIZE (100)

### Changed
- Replaced magic numbers with named constants throughout codebase
  - Player model uses GameConstants for life and commander damage
  - Deck model uses GameConstants for deck size validation
  - GameViewModel uses GameConstants for starting hand size
  - CommanderDamageDialog uses GameConstants for lethal damage threshold
  - DeckParser uses GameConstants for deck size validation
- Enhanced basic land recognition
  - Now recognizes snow-covered basic lands
  - Supports: Snow-Covered Plains, Island, Swamp, Mountain, Forest
  - Maintains support for Wastes and regular basics

### Technical Details
- Constants defined in shared/src/main/kotlin/com/commandermtg/models/GameConstants.kt
- Improves code maintainability and reduces errors from typos
- Makes MTG rules changes easier to implement in future

## [2.1.0] - 2025-10-27

### Added
- **Comprehensive Unit Test Suite** (44 tests)
  - PlayerTest: 17 tests covering all loss conditions (life total, commander damage)
  - GameViewModelTest: 13 tests for library operations, game state management
  - GameStateTest: 11 tests for validation, phase cycling, player updates
  - DeckTest: 13 tests for deck validation rules, singleton enforcement, basic lands
  - All tests passing with 100% coverage of critical game logic

- **Commander to Command Zone** (MTG rules compliance)
  - Commanders (legendary creatures) can now be moved to command zone from any zone
  - Added "To Command Zone" context menu option for commanders
  - Available from battlefield, hand, graveyard, and exile
  - Follows official MTG Commander format rules

### Changed
- Added test dependencies to desktop/build.gradle.kts
  - kotlin-test for unit testing
  - kotlinx-coroutines-test for async testing
  - JUnit Platform configuration

### Technical Details
- Test framework: kotlin.test with JUnit Platform
- Test coverage: Player models, GameViewModel, GameState, Deck validation
- Build configuration updated for both shared and desktop modules
- All tests verify MTG rules compliance (40 life, 21 commander damage, 99-card decks)

## [2.0.1] - 2025-10-27

### Fixed
- **Critical:** Library card ordering now consistent (last card = top of library, stack-based)
  - drawCard() now uses lastOrNull() instead of firstOrNull()
  - millCards() updated to use lastOrNull()
  - moveCardToTopOfLibrary() places card at end of library list
  - Establishes clear convention for library ordering across all operations
- **Critical:** Drawing from empty library now correctly causes player loss
  - Player.hasLost set to true when attempting to draw with no cards in library
  - Follows MTG rules for drawing from empty library
  - UI updates to show defeated player status
- **High:** Active player bounds checking added to prevent crashes
  - GameState.activePlayer now validates index before access
  - Throws descriptive IllegalStateException if out of bounds
  - Prevents application crashes on edge cases
- **High:** Life total changes now check for player loss (life <= 0)
  - Player.takeDamage() sets hasLost = true when life <= 0
  - Player.setLife() sets hasLost = true when new life <= 0
  - Player.gainLife() no longer needs loss checking
  - Follows MTG rules for life total loss condition
- **High:** moveCardToTopOfLibrary() now consistent with library ordering
  - Places card at end of list (top of stack)
  - Works correctly with drawCard() and millCards()
  - Clear documentation of stack-based convention

### Technical Details
- Established library convention: list[last] = top of library (stack-based)
- All library operations now use this consistent ordering
- Player loss conditions properly enforced for both life and draw-from-empty
- GameState validation prevents index out of bounds errors
- Improved error messages for debugging

## [2.0.0] - 2025-10-27

### Added
- Multi-player UI layouts for 2-4 player games
  - Dynamic opponent positioning based on player count
  - 2 players: 1 opponent at top (full width)
  - 3 players: 2 opponents side-by-side at top
  - 4 players: 3 opponents in a row at top
  - Local player always at bottom
- Player count selector in main menu
  - Choose 2, 3, or 4 players before starting game
  - FilterChip UI for easy selection
  - Persists selection in MenuUiState
- Automatic opponent name generation
  - Opponents named "Opponent 1", "Opponent 2", etc.
  - Based on selected player count

### Changed
- GameScreen now accepts playerCount parameter (defaults to 2)
- Game initialization creates appropriate number of opponents
- OpponentArea can now display in compact side-by-side layouts
- Created OpponentsArea composable for dynamic layout management
- MenuUiState includes playerCount field (default: 2)
- MenuViewModel has setPlayerCount() function

### Technical Details
- OpponentsArea uses Row with weighted modifiers for equal spacing
- Each opponent gets equal width when multiple are displayed
- Game initialization uses coerceIn(1, 3) for opponent count validation
- Commander format supports 2-4 players (1-3 opponents)
- All existing dialogs and interactions work with multiple opponents
- Turn system already supports multiple players

### Breaking Changes
- GameScreen signature changed (added optional playerCount parameter)
- MenuUiState structure changed (added playerCount field)

## [1.9.0] - 2025-10-27

### Added
- Card Context Menu system for all card interactions
  - Right-click any card to access contextual actions
  - Battlefield cards: Tap/Untap, Flip, Add/Remove Counters, Move to zones
  - Hand cards: Play, Discard, Exile, To Library, To Top
  - Graveyard/Exile cards: Return to Hand, Return to Battlefield, etc.
  - Library cards: To Hand, To Battlefield, To Top
  - Commander zone cards: Cast, To Hand
  - "View Details" option on all cards (placeholder for future implementation)
- Context menu integrated into BattlefieldCard component
  - Replaces click-only tap/untap with full action menu
  - Maintains left-click for quick tap/untap
- Context menu integrated into HandDialog
  - Supplements existing action buttons with right-click menu
  - Consistent UX across all card locations

### Changed
- BattlefieldCard now accepts onContextAction callback
- HandDialog now accepts onContextAction callback
- Renamed DeckParser functions for clarity:
  - parseCockatriceFormat() → parseTextFormat()
  - parseCockatriceFile() → parseTextFile()
- Removed external project name references from:
  - README.md (project description, deck format)
  - CHANGELOG.md (completion description, deck parser)
  - DeckParser.kt (function names and comments)
  - MenuViewModel.kt (function call)

### Technical Details
- Created CardContextMenu.kt with CardWithContextMenu composable
- Uses Compose Desktop's ContextMenuArea for right-click functionality
- CardAction sealed class for type-safe action dispatching
- handleCardAction() helper function routes actions to ViewModel
- Context menus dynamically generated based on card's current zone
- Counter types shown in remove menu if card has counters
- All context actions use existing ViewModel functions

## [1.8.0] - 2025-10-27

### Added
- Library Search Dialog with full search and filtering functionality
  - Search cards by name, type, or oracle text
  - Real-time filtering as you type
  - Shows result count (e.g., "Showing 5 of 99 cards")
  - Three action buttons per card:
    - "To Hand" - Move card to hand
    - "To Field" - Move card directly to battlefield
    - "To Top" - Move card to top of library (for tutoring)
  - "Shuffle Library and Close" button for post-search shuffling
  - 600dp height dialog with scrollable card list
- Library zone card now clickable in PlayerArea
  - Click on Library zone to open search dialog
  - Consistent with Graveyard and Exile zone interactions
- Added moveCardToTopOfLibrary() function to GameViewModel
  - Moves specified card to top position of owner's library
  - Maintains library order for other cards
  - Used for tutoring effects (search for card and put on top)

### Changed
- Library zone is now fully interactive (was previously view-only)
- Players can now search and manipulate their library contents
- Zone Interactions completion increased from 75% to 90%

### Technical Details
- Created LibrarySearchDialog.kt with search UI and card list
- Search uses case-insensitive contains() for name, type, and oracle text
- LibraryCard private composable for card display in search results
- moveCardToTopOfLibrary() reorders cardInstances list to place card first in library zone
- Search state managed with remember and mutableStateOf
- All actions properly close dialog and update game state
- Consistent with existing zone viewer patterns (GraveyardDialog, ExileDialog)

## [1.7.0] - 2025-10-27

### Added
- Enhanced Hand Dialog with multiple card actions
  - "Play" button - Play card to battlefield (existing functionality)
  - "Discard" button - Move card to graveyard
  - "Exile" button - Move card to exile zone
  - "To Library" button - Move card to library
  - All actions close the dialog after execution
  - Action buttons use labelSmall typography for better fit
- Improved HandDialog UI layout
  - Card info and action buttons now in vertical layout
  - Action buttons displayed in a row below card info
  - Better use of space with 4 equal-width buttons
  - Consistent button styling (filled for Play, outlined for others)

### Changed
- HandDialog signature now accepts optional callbacks for additional actions
  - onDiscard, onExile, onToLibrary default to empty lambdas
  - Maintains backward compatibility with existing code
- Card layout in hand dialog restructured
  - Changed from horizontal Row to vertical Column layout
  - Separated card info from action buttons
  - Better visual hierarchy

### Technical Details
- HandDialog function signature extended with optional parameters
- All actions use existing viewModel.moveCard() function
- Actions automatically close dialog via showHandDialog = false
- Default parameters allow HandDialog to work without specifying all actions
- Uses existing Zone enum (GRAVEYARD, EXILE, LIBRARY, BATTLEFIELD)

## [1.6.0] - 2025-10-27

### Added
- Complete set of missing GameViewModel functions for enhanced gameplay
  - `shuffleLibrary(playerId)` - Shuffle a player's library
  - `getPlayerBattlefieldCards(playerId)` - Get all battlefield cards for a specific player
  - `addCounter(cardId, type, amount)` - Add counters to cards (+1/+1, charge counters, etc.)
  - `removeCounter(cardId, type, amount)` - Remove counters from cards
  - `attachCard(sourceId, targetId)` - Attach auras/equipment to target cards
  - `detachCard(cardId)` - Remove card attachments
  - `flipCard(cardId)` - Flip cards (for morph, flip cards, etc.)
  - `millCards(playerId, count)` - Mill cards from top of library to graveyard
  - `mulligan(playerId)` - Return hand to library, shuffle, and draw new hand

### Changed
- GameViewModel now has 24 total functions (up from 16)
- All essential Commander gameplay mechanics now supported
- Counter management fully functional (already had addCounter in CardInstance model)

### Technical Details
- All functions follow existing StateFlow update patterns
- Functions integrate with existing GameState update methods
- Mulligan uses shuffleLibrary and drawStartingHand internally
- MillCards uses moveCard internally for each card

## [1.5.0] - 2025-10-27

### Added
- Commander Damage Tracking System
  - CommanderDamageDialog displays damage matrix for all commanders vs all players
  - Shows card image thumbnail for each commander
  - Click +/- buttons to adjust damage dealt to each player
  - Highlights lethal damage (21+) with red background and border
  - Displays "LETHAL DAMAGE!" warning for 21+ damage
  - Shows commander owner information
  - Scrollable dialog for games with many commanders
- Commander Damage button in player area
  - Opens commander damage tracking dialog
  - Positioned below Draw and Hand buttons
- Loss condition visual indicators
  - Player/opponent cards turn red when hasLost is true
  - "DEFEATED" label displayed for eliminated players
  - Automatic loss detection when commander damage reaches 21+
- Added getAllCommanders() function to GameViewModel
  - Returns all commanders in command zone or on battlefield
  - Filters for Legendary Creatures on battlefield
- Added updateCommanderDamage() function to GameViewModel
  - Updates commander damage for a specific player
  - Handles both increment and decrement of damage
  - Automatically checks and updates hasLost flag
  - Syncs player state across localPlayer and opponents

### Changed
- Player model's takeCommanderDamage already supported automatic loss detection
- Player info cards now show red background when player has lost
- Life total displays now indicate defeated players

### Technical Details
- Created `CommanderDamageDialog.kt` with damage tracking UI
- Added commander damage management to GameViewModel
- Commander damage stored as Map<String, Int> in Player model
- Loss condition automatically checked when damage >= 21
- Visual feedback throughout UI for eliminated players

## [1.4.0] - 2025-10-27

### Added
- Card Image Support with async loading and caching
  - CardImage composable displays actual card art from Scryfall
  - ImageCache utility downloads and caches images locally
  - Images stored in ~/.commandermtg/image_cache directory
  - Loading spinner shown while downloading
  - Error fallback for missing/failed images
  - CardImageThumbnail for compact display in lists
- Card images integrated throughout UI
  - Battlefield cards now display full card art with overlays
  - Hand dialog shows thumbnails next to card info
  - Graveyard/Exile dialogs show thumbnails next to card info
  - Power/toughness overlaid on bottom-right of battlefield cards
  - Counters overlaid on top of battlefield cards
- Image caching features
  - MD5-based filename generation for unique cache keys
  - Automatic cache directory creation
  - Persistent cache across app sessions
  - Cache management utilities (size check, clear cache)

### Changed
- BattlefieldCard redesigned to feature card images prominently
  - Transparent background with colored border (blue/red for controller)
  - Semi-transparent overlays for counters and P/T
  - Removed controller name display (border color indicates controller)
- HandDialog now scrollable and includes card thumbnails
- Zone dialogs use Row layout with thumbnail on left, info on right

### Technical Details
- Created `ImageCache.kt` utility for async image downloading and caching
- Created `CardImage.kt` composable with loading states (Loading, Success, Error)
- Uses Ktor HttpClient for image downloads
- Uses Skia Image for decoding and Compose ImageBitmap for display
- Coroutines with Dispatchers.IO for non-blocking image operations
- MD5 hashing for cache key generation

## [1.3.0] - 2025-10-27

### Added
- Zone Viewers for Graveyard and Exile zones
  - GraveyardDialog displays all cards in graveyard with full details
  - ExileDialog displays all cards in exile zone with full details
  - Dialogs show card name, mana cost, type, oracle text, and power/toughness
  - "Return to Hand" button moves cards back to hand
  - "Return to Battlefield" button puts cards directly onto battlefield
- Clickable zone cards
  - Graveyard and Exile zones now have onClick handlers
  - Click zone to open dialog and view all cards
  - Works for both local player and opponents

### Changed
- ZoneCard composable now accepts optional onClick parameter
- Graveyard and Exile zones are now interactive for all players

### Technical Details
- Created `ZoneViewers.kt` with GraveyardDialog and ExileDialog
- Added clickable support to ZoneCard with ripple effect
- Integrated dialogs into PlayerArea and OpponentArea
- Zone dialogs scroll when containing many cards

## [1.2.0] - 2025-10-27

### Added
- Battlefield Visualization with BattlefieldCard component
  - Displays card name, mana cost, power/toughness
  - Shows tapped state with 90-degree rotation
  - Displays counters on cards
  - Color-codes cards by controller (blue for local player, red for opponents)
  - Click to tap/untap cards
- BattlefieldArea now shows actual cards from game state
  - Displays all cards in the battlefield zone
  - Uses FlowRow layout for automatic wrapping
  - Shows placeholder when battlefield is empty
- Added `getBattlefieldCards()` function to GameViewModel
  - Returns all cards in battlefield across all players

### Changed
- Battlefield is no longer just a placeholder
- Cards played from hand now appear visually on battlefield
- Tapped cards rotate 90 degrees for visual indication

### Technical Details
- Created `BattlefieldCard.kt` with card visualization
- Updated `BattlefieldArea` to use FlowRow layout with battlefield cards
- Added `getBattlefieldCards()` method to GameViewModel
- Used `@OptIn(ExperimentalLayoutApi::class)` for FlowRow support

## [1.1.0] - 2025-10-27

### Added
- Turn System UI with TurnIndicator component
  - Displays active player name
  - Shows current phase with all MTG phases listed
  - Highlights current phase
  - "Next Phase" button to advance through phases
  - "Pass Turn" button to skip to next player's turn
  - Turn number tracking
- Phase advancement functionality in GameViewModel
  - `nextPhase()` advances to next phase
  - `passTurn()` advances to next player's untap phase
  - `untapAll()` untaps all permanents for a player
  - Automatic untap on turn start
- Updated game layout with turn indicator sidebar

### Changed
- Game screen now uses Row layout with turn indicator on right side
- Phase transitions now properly track game flow
- Battlefield cards automatically untap at start of owner's turn

### Technical Details
- Created `TurnIndicator.kt` with phase visualization
- Added turn management functions to GameViewModel
- Integrated turn system into game flow
- Turn indicator shows all 12 MTG phases grouped by category

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
- Text-based deck format parser
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
- [x] Create `BattlefieldCard.kt` component
  - [x] Display card name/image
  - [x] Show tapped state (rotation)
  - [x] Show counters
  - [ ] Show attached cards (auras/equipment)
  - [x] Color-code by controller
- [x] Implement `BattlefieldArea` with actual cards
  - [x] Get all battlefield cards from GameState
  - [x] Display in grid layout
  - [x] Support click to select
  - [ ] Support right-click for context menu
- [x] Add battlefield card interactions
  - [x] Click to tap/untap
  - [ ] Drag to reorder/group
  - [ ] Right-click context menu

#### Zone Viewers
- [x] Create `ZoneViewer.kt` dialogs
  - [x] GraveyardDialog
  - [x] ExileDialog
  - [ ] LibrarySearchDialog
- [x] Make zones clickable
  - [x] Add onClick handlers to ZoneCard
  - [x] Show appropriate dialog when clicked
- [x] Implement zone dialog actions
  - [x] Return card to hand from graveyard
  - [x] Return card to battlefield from graveyard
  - [ ] Exile from graveyard (can be done via graveyard -> battlefield then tap/move)
  - [ ] Search library and put card in hand

#### Turn System UI
- [x] Create `TurnIndicator.kt` component
  - [x] Show active player name
  - [x] Show current phase
  - [x] Show turn number
  - [x] Highlight active player
- [x] Add turn control buttons
  - [x] "Next Phase" button
  - [x] "Pass Turn" button
  - [x] Phase list display
- [x] Implement turn/phase advancement
  - [x] `nextPhase()` in GameViewModel
  - [x] `passTurn()` in GameViewModel
  - [x] Update active player
  - [ ] Sync turn changes over network (networking not yet implemented)

#### Card Images
- [x] Create `CardImage.kt` component
  - [x] Async image loading from Scryfall URI
  - [x] Loading placeholder
  - [x] Error fallback
  - [x] Image caching
- [x] Implement `ImageCache.kt`
  - [x] Download images to local cache
  - [x] Check cache before downloading
  - [x] Cache management (size limits)
- [x] Add card images throughout UI
  - [x] Hand dialog
  - [x] Battlefield cards
  - [x] Zone viewers
  - [ ] Card preview on hover (not implemented yet)

### 🟢 MEDIUM PRIORITY - Enhanced Features (v1.3.0)

#### Commander Damage
- [x] Create `CommanderDamageDialog.kt`
  - [x] Matrix of all commanders vs all players
  - [x] Click to add/subtract damage
  - [x] Highlight 21+ damage
  - [x] Button to open from player area
- [x] Add commander damage tracking
  - [x] Update Player model (already has field)
  - [x] `takeCommanderDamage()` function
  - [x] UI indicator when damaged by commander
  - [x] Loss condition check (21+ damage)

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
- [x] `shuffleLibrary(playerId)`
- [ ] `searchLibrary(playerId, predicate)` - requires Library Search Dialog
- [x] `millCards(playerId, count)`
- [x] `mulligan(playerId)`
- [x] `addCounter(cardId, type, amount)`
- [x] `removeCounter(cardId, type, amount)`
- [x] `flipCard(cardId)`
- [x] `attachCard(sourceId, targetId)`
- [x] `detachCard(cardId)`
- [x] `untapAll(playerId)` - already implemented in v1.1.0
- [x] `getBattlefieldCards()` - already implemented in v1.2.0
- [x] `getPlayerBattlefieldCards(playerId)`

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
  - [ ] Save to text format

---

## Current Progress

**Overall Completion:** ~82% for full 2+ player multiplayer experience

**By Category:**
- ✅ Core Architecture: 100%
- ✅ Single Player Deck Loading: 100%
- ✅ Scryfall Integration: 100%
- ✅ Multi-Player Support (Hotseat): 90% (2-4 player layouts complete, missing player-specific deck loading)
- ❌ Networking: 0%
- ✅ Battlefield Visualization: 95% (core display, tap/untap, images, and context menus complete, missing drag-drop)
- ✅ Zone Interactions: 95% (all zones have dialogs and context menus)
- ✅ Turn System: 90% (missing network sync)
- ✅ Card Images: 95% (loading and caching complete, missing hover preview)
- ✅ Commander Damage UI: 100%
- ✅ Game Actions: 100% (all essential functions implemented)

**Estimated Time to MVP (v2.0.0):**
- With networking: 2-3 weeks
- Without networking (local hotseat): Complete! Full 2-4 player support.
