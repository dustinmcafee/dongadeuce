# Dong-A-Deuce - Development TODO

**Current Version:** v2.10.6
**Hotseat Mode:** ~90% complete (fully playable!)
**Network Mode:** ~5% complete (UI only)

---

## ✅ ALREADY IMPLEMENTED (v2.10.6)

### Core Gameplay ✓
- **Turn/Phase System** - Full MTG phase cycle with TurnIndicator UI
- **Commander Damage Tracking** - Complete UI with lethal indicators
- **Card Context Menus** - Right-click menus for all zones
- **Library Search** - Full search/filter dialog
- **Zone Viewers** - Graveyard, Exile, Command Zone dialogs
- **Drag-and-Drop Battlefield** - Grid-based card positioning
- **Card Images** - Async loading with local caching
- **Tap/Untap** - Double-click and context menu
- **Counters** - Add/remove +1/+1, charge, and custom counters
- **Card Attachments** - Aura/Equipment attachment system
- **Flip Cards** - Card flipping support

### Hotseat Mode ✓
- **2-4 Player Support** - Full multiplayer on one device
- **Per-Player Deck Loading** - Each player loads their own deck
- **Automatic Player Rotation** - UI rotates to show active player at bottom
- **Hand Privacy** - Only active player sees their cards
- **Turn Passing** - Automatic player advancement
- **Zone Access Control** - Only active player can interact

### Game State Management ✓
- **Life Tracking** - Increment/decrement with automatic loss detection
- **Commander Damage** - Per-commander tracking with 21-damage rule
- **Draw from Empty Library** - Automatic loss detection
- **Zone Management** - All 7 MTG zones supported
- **Card Movement** - Move cards between any zones
- **Library Operations** - Draw, mill, shuffle, search, tutor
- **Mulligan** - Full mulligan support

### UI Components ✓
- **TurnIndicator** - Shows phase, turn number, active player
- **CommanderDamageDialog** - Damage matrix for all commanders
- **LibrarySearchDialog** - Search and manipulate library
- **CardDetailsDialog** - Full card information view
- **Zone Viewers** - Graveyard, Exile, Command Zone
- **Card Context Menus** - Right-click actions for all zones
- **Draggable Battlefield** - Grid-based card arrangement
- **Image Cache UI** - Bulk download with progress bar

### Technical ✓
- **MVVM Architecture** - Clean separation of concerns
- **StateFlow** - Reactive UI updates
- **Scryfall Integration** - Card data and images
- **Bulk Card Cache** - 500MB+ offline card database
- **Deck Parsing** - Text format with commander detection
- **Unit Tests** - 44 tests covering core mechanics
- **Input Validation** - Comprehensive validation throughout

---

## 🔴 CRITICAL - What's Actually Missing

### 1. Game Log/History System (Priority: HIGH)

**Current Status:** ❌ Not implemented
**Estimated Effort:** 2-3 days
**Blocking:** Game review, dispute resolution

**What's Needed:**
- [ ] Create `GameEvent` sealed class
  - [ ] CardDrawn, CardPlayed, CardMoved
  - [ ] LifeChanged, CommanderDamageDealt
  - [ ] TurnAdvanced, PhaseChanged
  - [ ] CardTapped, CountersAdded
- [ ] Create `GameLog` composable
  - [ ] Scrollable event list
  - [ ] Timestamp display
  - [ ] Player color coding
  - [ ] Auto-scroll to bottom
- [ ] Add logging to all GameViewModel actions
- [ ] Integrate into GameScreen layout

**Why Important:** Players need to review what happened during complex turns

---

### 2. Commander Tax Tracking (Priority: MEDIUM)

**Current Status:** ❌ Not implemented
**Estimated Effort:** 1 day

**What's Needed:**
- [ ] Add `timescastFromCommandZone` to CardInstance
- [ ] Calculate tax amount: `timescast * 2`
- [ ] Display tax in command zone dialog
- [ ] Display tax when viewing commander
- [ ] Increment count when casting from command zone
- [ ] Show total mana cost including tax in UI

**Note:** Currently players must manually track commander tax

---

### 3. Network Multiplayer Backend (Priority: CRITICAL for network play)

**Current Status:** ❌ UI only, no backend
**Estimated Effort:** 3-4 weeks
**Blocks:** Network multiplayer entirely

**What's Needed:**

#### GameServer.kt
- [ ] Ktor WebSocket server
- [ ] Accept player connections
- [ ] Maintain connected players list
- [ ] Broadcast game state updates
- [ ] Handle player disconnects
- [ ] Validate game actions

#### GameClient.kt
- [ ] Ktor WebSocket client
- [ ] Connect to host server
- [ ] Send local actions to server
- [ ] Receive and apply state updates
- [ ] Reconnection logic

#### GameMessage.kt (Network Protocol)
- [ ] Serializable message types for all game actions
- [ ] PlayerJoined/PlayerLeft
- [ ] GameStarted
- [ ] DrawCard, PlayCard, MoveCard
- [ ] TapCard, UpdateLife
- [ ] CommanderDamage
- [ ] NextPhase, PassTurn
- [ ] ChatMessage

#### Integration
- [ ] Connect MenuViewModel to server/client
- [ ] Connect GameViewModel to network layer
- [ ] Broadcast all game actions over network
- [ ] Apply remote actions to local game state
- [ ] Handle synchronization edge cases

**Note:** This is the ONLY blocker for network multiplayer. Hotseat mode is fully functional.

---

## 🟡 HIGH PRIORITY - Quality of Life

### 4. Chat System (Requires networking first)

- [ ] Create ChatPanel composable
- [ ] Chat input field
- [ ] Message history
- [ ] Player color coding
- [ ] Send messages over network
- [ ] Chat commands: /roll, /flip

**Estimated Effort:** 1-2 days
**Dependency:** Network multiplayer

---

### 5. Token Creation

- [ ] Create Token model (extends CardInstance?)
- [ ] Token creation UI
- [ ] Common tokens (treasures, food, clues, etc.)
- [ ] Custom token creation
- [ ] Token context menu actions

**Estimated Effort:** 2-3 days

---

### 6. Copy/Clone Cards

- [ ] Implement card cloning
- [ ] Copy tokens (for token doublers)
- [ ] Copy permanents
- [ ] Track copied vs original cards

**Estimated Effort:** 1-2 days

---

## 🔵 MEDIUM PRIORITY - Enhancements

### 7. Combat System Helpers (Optional)

- [ ] Declare attackers UI
- [ ] Declare blockers UI
- [ ] Combat damage assignment
- [ ] First strike handling
- [ ] Combat damage tracking

**Note:** MTG combat is complex. Consider leaving as manual for now.
**Estimated Effort:** 1-2 weeks

---

### 8. Stack Management (Optional)

- [ ] Stack visualization
- [ ] Spell/ability ordering
- [ ] Priority passing
- [ ] Response windows

**Note:** Very complex. Consider leaving as manual.
**Estimated Effort:** 2-3 weeks

---

### 9. Keyboard Shortcuts

- [ ] Space: Next phase
- [ ] Enter: Pass turn
- [ ] T: Tap selected card
- [ ] U: Untap all
- [ ] D: Draw card
- [ ] M: Mulligan

**Estimated Effort:** 1 day

---

### 10. Settings/Preferences

- [ ] Settings dialog
- [ ] Player name persistence
- [ ] Default deck directory
- [ ] Network port configuration
- [ ] Auto-untap on turn start (toggle)
- [ ] Confirm destructive actions (toggle)

**Estimated Effort:** 2-3 days

---

## 🎨 POLISH - Future Enhancements

### 11. Animations

- [ ] Card movement animations
- [ ] Tap rotation animation (currently instant)
- [ ] Zone transition effects
- [ ] Life counter animations

**Estimated Effort:** 1-2 weeks

---

### 12. Sound Effects

- [ ] Card draw sound
- [ ] Card play sound
- [ ] Tap sound
- [ ] Life change sound
- [ ] Turn pass sound

**Estimated Effort:** 3-4 days

---

### 13. Themes

- [ ] Dark mode (current)
- [ ] Light mode
- [ ] Custom card backs
- [ ] Custom backgrounds

**Estimated Effort:** 1 week

---

### 14. Deck Builder (Big Feature)

- [ ] In-app deck creation
- [ ] Scryfall card search
- [ ] Add/remove cards
- [ ] Commander selection
- [ ] Deck validation
- [ ] Save/load decks
- [ ] Import/export formats
- [ ] Deck statistics (mana curve, etc.)

**Estimated Effort:** 2-3 weeks

---

### 15. Game Save/Load

- [ ] Save game state to file
- [ ] Load saved games
- [ ] Auto-save on exit
- [ ] Game replay system

**Estimated Effort:** 3-4 days

---

### 16. Additional Deck Formats

- [ ] Support multiple deck formats
  - [ ] Text format (current)
  - [ ] .dec format
  - [ ] .cod format (standard format)
  - [ ] JSON format
- [ ] Deck editor UI (basic)
  - [ ] Add/remove cards
  - [ ] Search card database
  - [ ] Set commander
- [ ] Deck validation UI
  - [ ] Check commander legality
  - [ ] Check 100-card requirement
  - [ ] Check color identity
  - [ ] Check banned/restricted cards

**Estimated Effort:** 1-2 weeks

---

## 📊 COMPLETION STATUS

### For Hotseat Play (Local Multiplayer):
**Status:** ~90% Complete ✅
**Fully Playable:** YES
**Missing:** Game log, commander tax tracking

### For Network Play (Remote Multiplayer):
**Status:** ~5% Complete ❌
**Fully Playable:** NO
**Blocking:** Network backend (3-4 weeks of work)

---

## 🎯 RECOMMENDED NEXT STEPS

### Option A: Polish Hotseat Mode (1 week)
1. Implement Game Log/History (2-3 days)
2. Add Commander Tax tracking (1 day)
3. Add keyboard shortcuts (1 day)
4. Add settings panel (2-3 days)

**Result:** Feature-complete hotseat multiplayer with excellent UX

---

### Option B: Enable Network Multiplayer (3-4 weeks)
1. Implement GameServer + GameClient + GameMessage (2-3 weeks)
2. Integrate networking into MenuViewModel and GameViewModel (1 week)
3. Test 2-4 player network games thoroughly (3-4 days)

**Result:** Functional network multiplayer

---

### Option C: Both (Recommended)
1. **Week 1:** Game Log + Commander Tax + Keyboard Shortcuts
2. **Weeks 2-4:** Network multiplayer implementation
3. **Week 5:** Network testing and bug fixes

**Result:** Complete Dong-A-Deuce experience

---

## 📝 NOTES

### What Makes This App Already Great:
- **Hotseat mode is fully playable** - You can play complete Commander games right now
- **Comprehensive game state management** - All MTG rules are tracked
- **Professional UI** - Clean, Material3 design with card images
- **Excellent architecture** - MVVM with reactive state management
- **Well-tested** - 44 unit tests covering core mechanics

### What Would Make It Perfect:
- **Game log** - So players can review complex turns
- **Network multiplayer** - So players can play remotely
- **Commander tax** - Minor convenience feature
- **Keyboard shortcuts** - Speed up common actions
- **Settings persistence** - Remember player names, etc.

---

## 🚀 CURRENT DEVELOPMENT PRIORITY

**Based on this audit, the actual next priorities should be:**

1. ✅ **DONE:** Drag-and-drop grid snapping (v2.10.5)
2. ✅ **DONE:** Zone card text cutoff (v2.10.6)
3. **NEXT:** Game Log/History System (2-3 days)
4. **THEN:** Commander Tax Tracking (1 day)
5. **THEN:** Decide: Polish hotseat OR start network multiplayer

---

**Last Updated:** 2025-10-28
**Version:** v2.10.6
