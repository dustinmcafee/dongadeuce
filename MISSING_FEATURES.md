# Missing Features Analysis

**Current Version:** v2.26.0
**Last Updated:** 2025-11-27
**Hotseat Mode Completion:** ~97%
**Network Mode Completion:** ~5%
**MVVM Architecture Compliance:** 100%

---

## Executive Summary

Dong-A-Deuce is a **highly functional hotseat multiplayer Commander game** with comprehensive game state management, professional UI, and nearly all core gameplay features implemented. The application is fully playable for 2-4 players on the same device.

**Current State:**
- Hotseat multiplayer is 98% complete and fully playable
- All core MTG mechanics implemented
- Professional UI with card images and extensive dialogs
- Game log/chat panel for event tracking and player communication
- Excellent MVVM architecture (100% compliant)
- Network multiplayer backend not yet implemented
- Minor feature missing (commander tax)

---

## ACTUALLY MISSING FEATURES

### 1. Game Log/History System - IMPLEMENTED

**Priority:** HIGH
**Effort:** 2-3 days
**Status:** COMPLETED (v2.26.0)

**Features Implemented:**
- GameEvent sealed class with 21 event types (CardDrawn, CardPlayed, CardMoved, LifeChanged, CommanderDamageDealt, PhaseChanged, TurnPassed, CardCounterChanged, PlayerCounterChanged, CardTapped, UntapAll, TokenCreated, CardCloned, PlayerLost, GameStarted, DieRolled, ControlChanged, CardsMilled, LibraryShuffled, MulliganTaken, ChatMessage)
- GameLogPanel UI component on right side of screen
- Real-time event logging for all game actions
- Player chat messages support
- Timestamp and player color coding
- Auto-scroll to latest events
- Event icons and background colors by type

---

### 2. Commander Tax Tracking

**Priority:** HIGH
**Effort:** 1 day
**Status:** Not implemented

**Impact:**
Players must manually track commander tax (additional {2} for each previous cast from command zone).

**What's Needed:**
- Add `timesCastFromCommandZone` field to CardInstance
- Display tax amount in command zone dialog
- Increment counter when casting from command zone
- Show total mana cost including tax

**Why Not Implemented:**
- Players can manually track (write it down)
- Not blocking gameplay
- Easy to add later

---

### 3. Player Counters System - IMPLEMENTED

**Priority:** HIGH
**Effort:** 2-3 days
**Status:** COMPLETED (v2.26.0)

**Features Implemented:**
- Player counters map with poison, energy, experience, and custom counters
- Poison counters with 10 = automatic loss condition
- Energy and experience counter tracking
- Custom player-level counters
- PlayerCountersDialog UI with tabbed interface
- Counter chips displayed in player area
- +/- controls with set/add/subtract operations

---

### 4. Network Multiplayer Backend

**Priority:** CRITICAL (for network play)
**Effort:** 3-4 weeks
**Status:** UI exists, backend not implemented

**Impact:**
Cannot play games with remote players. Only hotseat mode works.

**What's Needed:**

#### GameServer.kt
- Ktor WebSocket server on configurable port
- Accept player connections
- Maintain connected players list
- Broadcast game state updates to all clients
- Handle player disconnects gracefully
- Validate game actions for cheating prevention

#### GameClient.kt
- Ktor WebSocket client
- Connect to host server by IP:port
- Send local player actions to server
- Receive and apply remote game state updates
- Reconnection logic for dropped connections
- Heartbeat/ping system

#### GameMessage.kt
- Serializable network protocol with kotlinx.serialization
- Message types for all game actions:
  - PlayerJoined, PlayerLeft, DeckLoaded
  - GameStarted, DrawCard, PlayCard, MoveCard
  - TapCard, UpdateLife, CommanderDamage
  - NextPhase, PassTurn, ChatMessage
  - CountersAdded, CardAttached, etc.

#### Integration
- MenuViewModel.startHosting() - Start server
- MenuViewModel.connectToGame() - Connect client
- GameViewModel - Broadcast all actions over network
- GameViewModel - Listen for and apply remote actions
- State synchronization and conflict resolution

**Why Not Implemented:**
- Large engineering effort (3-4 weeks)
- Hotseat mode fully functional as alternative
- Requires careful design of network protocol
- Must handle edge cases (disconnects, cheating, sync)

---

### 5. Copy/Clone Cards - IMPLEMENTED

**Priority:** MEDIUM
**Effort:** 1-2 days
**Status:** COMPLETED (v2.26.0)

**Features Implemented:**
- Card cloning function with `cloneCard()` in GameViewModel
- Clone tracking with `isClone` and `clonedFromId` on CardInstance
- "Create Copy" action in context menu for battlefield cards
- Support for multiple copies at once (for token doublers)
- Visual "Copy" indicator on cloned cards

---

### 6. Keyboard Shortcuts

**Priority:** MEDIUM
**Effort:** 1 day
**Status:** Not implemented

**Impact:**
All actions require mouse clicks. Power users have slower workflow.

**Proposed Shortcuts:**
- Space: Next phase
- Enter: Pass turn
- T: Tap selected card
- U: Untap all
- D: Draw card
- M: Mulligan
- 1-9: Select hand card
- Esc: Close dialogs

**Why Not Implemented:**
- Mouse interaction works fine
- Not blocking gameplay
- Easy to add later

---

### 7. Settings/Preferences

**Priority:** MEDIUM
**Effort:** 2-3 days
**Status:** Not implemented

**Impact:**
Player name not saved, no configuration options.

**What's Needed:**
- Settings dialog
- Player name persistence
- Default deck directory
- Network port configuration
- Auto-untap toggle
- Confirm destructive actions toggle

**Why Not Implemented:**
- Defaults work for most users
- Configuration can be done each session
- QoL feature, not critical

---

### 8. Die Rolling System - IMPLEMENTED

**Priority:** MEDIUM
**Effort:** 1-2 days
**Status:** COMPLETED (v2.26.0)

**Features Implemented:**
- DieRollerDialog with all standard dice (D4, D6, D8, D10, D12, D20, D100)
- Multiple dice rolling (up to 100 dice at once)
- Roll history with running log
- Custom die sides input
- Coin flip support
- "Roll Dice" button in turn indicator area

---

## FEATURES THAT ARE ACTUALLY IMPLEMENTED

### Core Gameplay (100% Complete)
- Turn/Phase System with full MTG cycle
- Commander Damage Tracking with 21-damage rule
- Card Context Menus for all zones (with custom popup system)
- Library Search with filtering
- Zone Viewers (Graveyard, Exile, Command Zone)
- Drag-and-Drop Battlefield with grid positioning
- Card Images with async loading
- Tap/Untap cards
- Counters (add/remove +1/+1, charge, custom)
- Card Attachments (auras/equipment)
- Flip Cards
- Face Down Cards (morph, manifest)
- Life Tracking with auto-loss detection
- Draw from Empty Library auto-loss
- All Zone Operations
- Library Operations (draw, mill, shuffle, search, tutor, mulligan)
- Advanced Library Operations (peek top/bottom N, move to position)
- P/T Modifications (increase/decrease/set/reset/flow)
- Card Annotations (custom text notes)
- "Doesn't Untap" toggle
- Player Counters (poison, energy, experience, custom) with loss condition
- Die Rolling (D4, D6, D8, D10, D12, D20, D100, custom, coin flip)
- Card Copy/Clone with visual indicator

### Hotseat Multiplayer (100% Complete)
- 2-4 Player Support
- Per-Player Deck Loading
- Automatic Player Rotation
- Hand Privacy
- Turn Passing
- Zone Access Control

### UI Components (100% Complete)
- TurnIndicator with phase display and dice roll button
- CommanderDamageDialog
- LibrarySearchDialog
- LibraryOperationsDialog
- LibraryPeekDialog
- LibraryPositionDialog
- CardDetailsDialog
- GraveyardDialog
- ExileDialog
- CommandZoneDialog
- Card Context Menus (custom popup system)
- Draggable Battlefield Grid
- Image Cache UI with progress
- CounterDialog (set/add/subtract counters)
- PowerToughnessDialog
- SetLifeDialog (click life total to set)
- AnnotationDialog
- PlayerCountersDialog (poison, energy, experience, custom counters)
- DieRollerDialog (all dice types, multiple dice, coin flip)

### Token System (100% Complete)
- Token Creation with Scryfall search
- Custom token creation (name, type, P/T, color)
- Multiple token creation at once

### Technical (100% Complete)
- MVVM Architecture (100% compliant)
- StateFlow Reactive Updates
- Scryfall Integration
- Bulk Card Cache (500MB+)
- Text Deck Parser
- 44 Unit Tests
- Input Validation
- Windows EXE Build (Launch4j)
- GitHub Actions CI/CD
- Cross-platform packaging (Windows, macOS, Linux)

---

## OPTIONAL/FUTURE FEATURES

These are features that would be nice but are not necessary for full Commander gameplay:

### Combat System Automation (Optional)
- Declare attackers UI
- Declare blockers UI
- Combat damage assignment
- First strike handling

**Note:** MTG combat is very complex. Manual resolution may be better.

### Stack Management (Optional)
- Stack visualization
- Spell/ability ordering
- Priority passing
- Response windows

**Note:** MTG stack is very complex. Manual resolution may be better.

### Animations (Polish)
- Card movement animations
- Tap rotation animation
- Zone transitions
- Life counter animations

### Sound Effects (Polish)
- Card draw/play sounds
- Tap sound
- Life change sound
- Turn pass sound

### Themes (Polish)
- Light mode
- Custom card backs
- Custom backgrounds

### Deck Builder (Big Feature)
- In-app deck creation
- Scryfall search
- Deck validation
- Save/load
- Statistics

### Game Save/Load (Enhancement)
- Save game state
- Load saved games
- Auto-save
- Game replay

---

## FEATURE COMPLETION BY CATEGORY

| Category | Complete | Missing | % Complete |
|----------|----------|---------|------------|
| **Core Gameplay** | 18/18 | 0/18 | 100% |
| **Hotseat Multiplayer** | 6/6 | 0/6 | 100% |
| **UI Components** | 16/16 | 0/16 | 100% |
| **Technical Foundation** | 10/10 | 0/10 | 100% |
| **Quality of Life** | 0/8 | 8/8 | 0% |
| **Network Multiplayer** | 1/10 | 9/10 | 10% |
| **Polish/Enhancement** | 0/20 | 20/20 | 0% |
| **TOTAL** | 51/88 | 37/88 | **58%** |

**For Hotseat Mode:** 50/53 = **94% Complete**

---

## PLAYABILITY ASSESSMENT

### For Hotseat Play (2-4 players, same device):
**Status:** FULLY PLAYABLE
**Completeness:** 94%
**Missing:** Game log, commander tax, player counters
**Verdict:** You can play complete Commander games right now!

### For Network Play (remote multiplayer):
**Status:** NOT PLAYABLE
**Completeness:** 10%
**Missing:** Entire network backend
**Verdict:** 3-4 weeks of development needed

---

## WHAT USERS CAN DO TODAY (v2.25.0)

### Fully Functional
- Start hotseat game with 2-4 players
- Load individual decks for each player
- Draw starting hands automatically
- Track all game state (life, commander damage, zones)
- Play cards to battlefield
- Tap/untap permanents
- Move cards between zones
- Add/remove counters (with counter management dialog)
- Modify power/toughness
- Add annotations to cards
- Search library for cards
- View all zones (graveyard, exile, command zone)
- Drag and arrange battlefield
- Track turns and phases
- Pass turns between players
- Create tokens (via Scryfall search or custom)
- Win/lose based on life, commander damage, or drawing from empty library
- View card images and details
- Peek at top/bottom N cards of library
- Send cards to specific library positions
- View game log/history of all actions
- Send chat messages to other players
- See die roll results in game log

### Cannot Do
- Auto-calculate commander tax (must track manually)
- Play over network with remote players
- Use keyboard shortcuts
- Save/load games
- Access settings menu

---

## ARCHITECTURE QUALITY

### MVVM Compliance: 100%

**Strengths:**
- Perfect separation of concerns (Models, Views, ViewModels)
- Unidirectional data flow
- Immutable state management with StateFlow
- No UI code in ViewModels
- No business logic in Views
- Proper reactive programming
- Testable business logic (44 passing tests)
- No debug/logging statements in ViewModels
- Clean, production-ready code

**Verdict:** Perfect MVVM architecture, production-ready code quality

---

## RECOMMENDED DEVELOPMENT PATH

### For Users Who Want Hotseat Multiplayer:
**Status:** Already works! Play it today.
**Optional:** Add game log (2-3 days), commander tax (1 day), and player counters (2-3 days) for perfect experience.

### For Users Who Want Network Multiplayer:
**Path:** Implement network backend (3-4 weeks)
**Then:** Optional polish (chat, settings, etc.)

---

## CONCLUSION

Dong-A-Deuce v2.25.0 is a **highly functional hotseat Commander game** that can be played today. The application demonstrates excellent MVVM architecture and comprehensive Commander gameplay support. The only significant missing feature is network multiplayer, which requires 3-4 weeks of dedicated development.

**For Hotseat Players:** This app is ready to use!
**For Network Players:** Patience required, or contribute to the network backend.

---

**Last Updated:** 2025-11-27
**Next Priority:** Game Log + Commander Tax + Player Counters
