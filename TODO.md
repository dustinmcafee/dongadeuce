# Dong-A-Deuce - Development TODO

**Current Version:** v2.21.3
**Hotseat Mode:** ~90% complete (fully playable!)
**Network Mode:** ~5% complete (UI only)
**Last Code Review:** 2025-11-05

---

## ✅ ALREADY IMPLEMENTED (v2.18.0)

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
- **Multi-card Selection** - Shift+click to select multiple cards
- **Batch Operations** - Actions apply to all selected cards
- **Token Creation** - Dialog with Scryfall search and custom tokens
- **Drag to Zones** - Drag cards from battlefield to zone buttons
- **Battlefield Scrolling** - Vertical scrolling for cards in lower rows
- **Give Control** - Transfer permanents to other players

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

### Build System ✓ (v2.18.0)
- **Windows EXE Build** - Launch4j integration for cross-platform builds
- **Custom Icon** - Donkey-dragon hybrid icon integrated
- **GitHub Actions** - CI/CD workflows for automated builds
- **Cross-platform JAR** - Includes Windows, Linux, macOS dependencies

### Technical ✓
- **MVVM Architecture** - Clean separation of concerns
- **StateFlow** - Reactive UI updates
- **Scryfall Integration** - Card data and images
- **Bulk Card Cache** - 500MB+ offline card database
- **Deck Parsing** - Text format with commander detection
- **Unit Tests** - 44 tests covering core mechanics
- **Input Validation** - Comprehensive validation throughout

---

## 🔴 CRITICAL PRIORITY - Must Fix/Implement

### 1. ~~**Fix Recursive Stack Risk**~~ ✅ COMPLETED (v2.19.0)
**Status:** Fixed in v2.19.0
**Completed:** 2025-11-05

**What Was Done:**
- [x] Converted recursive function to iterative approach
- [x] Added max position check (40 positions limit)
- [x] Added graceful fallback when battlefield full
- [x] Prevents stack overflow crashes with 120+ cards

---

### 2. ~~**Package Naming Inconsistency**~~ ✅ COMPLETED (v2.19.0)
**Status:** Fixed in v2.19.0
**Completed:** 2025-11-05

**What Was Done:**
- [x] Refactored all 32 source files from com.commandermtg to com.dustinmcafee.dongadeuce
- [x] Updated all package declarations and imports
- [x] Cleaned build artifacts
- [x] Verified all modules work correctly

---

### 3. **Game Log/History System**
**Current Status:** ❌ Not implemented
**Estimated Effort:** 2-3 days

**What's Needed:**
- [ ] Create `GameEvent` sealed class (CardDrawn, CardPlayed, CardMoved, LifeChanged, etc.)
- [ ] Create `GameLog` composable (scrollable event list, timestamps, player colors)
- [ ] Add logging to all GameViewModel actions
- [ ] Integrate into GameScreen layout

**Why Critical:** Essential for reviewing complex turns, dispute resolution

---

### 4. ~~**Grid Recalculation Performance**~~ ✅ COMPLETED (v2.20.0)
**Status:** Fixed in v2.20.0
**Completed:** 2025-11-05

**What Was Done:**
- [x] Optimized from O(n²) to O(n) complexity
- [x] Replaced `remember` with `derivedStateOf` for better recomposition control
- [x] Added position count map for O(1) lookups instead of O(n) counts
- [x] Eliminated expensive string concatenation for grid keys
- [x] Optimized card grouping with reverse map
- [x] Battlefield now smooth with 100+ cards

---

### 5. **Network Multiplayer Backend** (if network play is priority)
**Current Status:** ❌ UI only, no backend
**Estimated Effort:** 3-4 weeks

**What's Needed:**
- [ ] GameServer.kt - Ktor WebSocket server
- [ ] GameClient.kt - Ktor WebSocket client
- [ ] GameMessage.kt - Serializable network protocol
- [ ] MenuViewModel integration
- [ ] GameViewModel integration
- [ ] State synchronization
- [ ] Reconnection logic

**Why Critical:** ONLY blocker for network multiplayer (if that's the goal)

---

## 🟠 HIGH PRIORITY - Important Features & Fixes

### 6. **Commander Tax Tracking**
**Current Status:** ❌ Not implemented
**Estimated Effort:** 1 day

**What's Needed:**
- [ ] Add `timescastFromCommandZone: Int` to CardInstance
- [ ] Calculate tax: `timescast * 2`
- [ ] Display in command zone dialog
- [ ] Increment when casting from command zone
- [ ] Show total mana cost including tax

**Why Important:** Commander-specific essential feature

---

### 7. ~~**Advanced Library Operations**~~ ✅ COMPLETED (v2.21.0)
**Status:** Completed in v2.21.0
**Completed:** 2025-11-05

**What Was Done:**
- [x] View top N cards (LibraryPeekDialog)
- [x] View bottom N cards (LibraryPeekDialog)
- [x] Move top/bottom cards to specific zones (batch operations)
- [x] Shuffle top N cards
- [x] Shuffle bottom N cards
- [x] Reveal top card to all players
- [x] Move individual cards to bottom of library
- [x] New LibraryOperationsDialog UI
- [x] Position indicators (#1, #2, #3) in peek view
- [x] Batch operations (move all peeked cards at once)

---

### 8. **Player Counters System**
**Current Status:** ❌ Not implemented
**Estimated Effort:** 2-3 days

**What's Needed:**
- [ ] Add counters map to Player model
- [ ] Poison counters (10 = loss)
- [ ] Energy counters
- [ ] Experience counters
- [ ] Custom player-level counters
- [ ] UI display in player area
- [ ] +/- controls

**Why Important:** Core MTG mechanics (poison is win condition)

---

### 9. **P/T Modification System**
**Current Status:** ❌ Not implemented
**Estimated Effort:** 2-3 days

**What's Needed:**
- [ ] Add `powerModifier: Int` and `toughnessModifier: Int` to CardInstance
- [ ] Context menu options:
  - [ ] Increase Power (+1)
  - [ ] Decrease Power (-1)
  - [ ] Increase Toughness (+1)
  - [ ] Decrease Toughness (-1)
  - [ ] Increase Both (+1/+1)
  - [ ] Decrease Both (-1/-1)
  - [ ] Flow P (increase power, decrease toughness)
  - [ ] Flow T (decrease power, increase toughness)
  - [ ] Set P/T (dialog for custom values)
  - [ ] Reset P/T (to card's printed values)
- [ ] Display modified P/T in UI (e.g., "3/4" in green if modified)
- [ ] Show original P/T in tooltip

**Why Important:** Very common in Commander gameplay

---

### 10. **Card State Management**
**Current Status:** Partial (tap/flip implemented)
**Estimated Effort:** 1-2 days

**What's Needed:**
- [ ] Add `doesntUntap: Boolean` to CardInstance (toggle "Doesn't Untap")
- [ ] Add `annotation: String?` to CardInstance (custom text notes)
- [ ] Context menu "Set Annotation"
- [ ] Context menu "Toggle Doesn't Untap"
- [ ] Display annotation badge on card
- [ ] Display "doesn't untap" indicator
- [ ] Auto-skip untap for marked cards

**Why Important:** Essential for tracking complex board states

---

### 11. **Counter System Overhaul**
**Current Status:** Basic counters only (+1/+1, charge)
**Estimated Effort:** 2-3 days

**What's Needed:**
- [ ] Support 6 configurable counter types (A-F)
- [ ] Assign colors to counter types (red, yellow, green, cyan, purple, magenta)
- [ ] Context menu "Set Counters" (enter specific number)
- [ ] Display counter type and color on card
- [ ] Show counter breakdown (e.g., "3x +1/+1, 2x charge")
- [ ] Settings for counter type names/colors

**Why Important:** Many cards use custom counters

---

### 12. **Library Position Operations**
**Current Status:** Can only move to top
**Estimated Effort:** 1 day

**What's Needed:**
- [ ] Add moveCardToBottomOfLibrary() support to context menu
- [ ] Add "To X Cards from Top" option (enter position 1-N)
- [ ] Add positionInLibrary field to track order
- [ ] Update LibrarySearchDialog with bottom option
- [ ] Add "In random order" option for multiple cards

**Why Important:** Common MTG effects (scry to bottom, tuck effects)

---

### 13. **Refactor GameScreen.kt** 🛠️ MAINTENANCE
**Current Status:** 2,090 lines - too large
**Estimated Effort:** 1-2 days

**What's Needed:**
- [ ] Extract to GameScreen.kt (main layout only)
- [ ] Extract to HotseatComponents.kt
- [ ] Extract to NetworkComponents.kt
- [ ] Extract to DialogComponents.kt
- [ ] Remove code duplication in player layouts

**Why Important:** Unmaintainable, hard to review, prone to bugs

---

### 11. **State Sync Pattern Duplication** 🛠️ MAINTENANCE
**Current Status:** Player reference sync repeated 8+ times
**Estimated Effort:** 1 day

**What's Needed:**
- [ ] Extract pattern to helper extension function
- [ ] Create state update wrapper
- [ ] Refactor all manual sync points
- [ ] Add unit tests

**Why Important:** Maintenance burden, error-prone

---

### 12. **Image Loading Performance** ⚠️ PERFORMANCE
**Current Status:** No size limits, unbounded cache growth
**Estimated Effort:** 2 days

**What's Needed:**
- [ ] Add image size limits
- [ ] Use thumbnails where appropriate
- [ ] Add image compression
- [ ] Implement cache size limits
- [ ] Add periodic cleanup

**Why Important:** Memory/performance issue with large card collections

---

## 🟡 MEDIUM PRIORITY - Quality of Life

### 13. **Keyboard Shortcuts**
**Estimated Effort:** 1 day

- [ ] Space: Next phase
- [ ] Enter: Pass turn
- [ ] T: Tap selected card
- [ ] U: Untap all
- [ ] D: Draw card
- [ ] M: Mulligan
- [ ] Esc: Close dialogs

---

### 14. **Settings/Preferences**
**Estimated Effort:** 2-3 days

- [ ] Settings dialog
- [ ] Player name persistence
- [ ] Default deck directory
- [ ] Network port configuration
- [ ] Auto-untap on turn start (toggle)
- [ ] Confirm destructive actions (toggle)
- [ ] Card image quality setting

---

### 15. **Token Creation Improvements**
**Estimated Effort:** 2 days

- [ ] Token color selection (W/U/B/R/G/Multicolor/Colorless)
- [ ] P/T customization in dialog
- [ ] "Destroy on zone change" toggle
- [ ] Predefined token lists from deck
- [ ] Remember last token created
- [ ] Token database integration

---

### 16. **Card Annotations**
**Estimated Effort:** 2 days

- [ ] Add `annotation: String?` to CardInstance
- [ ] Context menu "Add Note"
- [ ] Dialog for entering note text
- [ ] Display annotation on card (small badge)
- [ ] Edit/clear annotation

---

### 17. **Visual Attachment System**
**Estimated Effort:** 2-3 days

- [ ] Draw lines from attached cards to attachedTo cards
- [ ] Color-code attachment lines
- [ ] Click line to unattach
- [ ] Automatic movement when attached-to card moves
- [ ] Validate attachment relationships

---

### 18. **Die Rolling System**
**Estimated Effort:** 1-2 days

- [ ] Roll die dialog (D4, D6, D8, D10, D12, D20, D100)
- [ ] Multiple dice support
- [ ] Roll history in game log
- [ ] Quick roll buttons
- [ ] Custom die sides

---

### 19. **Hand Management**
**Estimated Effort:** 2 days

- [ ] Sort hand (by name, CMC, color, type)
- [ ] Reveal hand to specific players
- [ ] Reveal random card from hand
- [ ] Discard random card

---

### 20. **Copy/Clone Cards**
**Estimated Effort:** 1-2 days

- [ ] cloneCard() function
- [ ] Track original vs copy relationship
- [ ] Copy tokens (for doublers)
- [ ] Copy permanents
- [ ] Context menu "Create Copy"

---

### 21. **Play Face Down**
**Estimated Effort:** 1 day

- [ ] Add `isFaceDown: Boolean` to CardInstance
- [ ] Context menu "Play Face Down" (from hand)
- [ ] Context menu "Peek at Face" (private view for owner)
- [ ] Display face-down cards as card back image
- [ ] Flip face-down cards with "Turn Over"
- [ ] Hide face-down card names from opponents

**Why Important:** Core mechanic for morph, manifest, disguise

---

### 22. **Attachment System Enhancements**
**Estimated Effort:** 2-3 days

- [ ] Visual attachment lines (draw from attached to parent)
- [ ] Context menu "Attach to Card" (select target)
- [ ] Context menu "Unattach"
- [ ] Auto-move attached cards with parent
- [ ] Color-code attachment lines by type
- [ ] Click line to break attachment

**Why Important:** Visual aid to help track Auras/Equipment

---

## 🟢 LOW PRIORITY - Advanced Features

### 23. **Reveal System**
**Estimated Effort:** 2-3 days
**Requires:** Network multiplayer or hotseat reveal state

- [ ] Context menu "Reveal to All Players"
- [ ] Context menu "Reveal to [Player Name]"
- [ ] RevealedZone viewer (read-only card display)
- [ ] Track revealed state per player
- [ ] Auto-hide revealed cards after action
- [ ] Reveal from hand, library, top of library

**Why Important:** Common MTG effect, but requires multiplayer state sync

---

### 24. **Related Cards & Tokens**
**Estimated Effort:** 3-4 days

- [ ] Query Scryfall for related cards (tokens, transforms, meld)
- [ ] Context menu "View Related Cards" submenu
- [ ] Context menu "Create Token: [Name]"
- [ ] Context menu "Create All Tokens"
- [ ] Display P/T and count in token menu
- [ ] Support double-faced card transforms
- [ ] Support meld relationships

**Why Important:** Convenience feature, reduces manual token creation

---

### 25. **Targeting Arrows**
**Estimated Effort:** 2-3 days

- [ ] Context menu "Draw Arrow"
- [ ] Click source card, then target
- [ ] Draw colored arrow overlay
- [ ] Multiple arrows per card
- [ ] Arrow to player (for direct damage)
- [ ] Click arrow to remove
- [ ] Auto-clear arrows on phase change (optional)

**Why Important:** Visual aid for complex board states

---

### 26. **Selection Enhancements**
**Estimated Effort:** 1 day

- [ ] Context menu "Select All" (in current zone)
- [ ] Context menu "Select Row" (battlefield only)
- [ ] Context menu "Select Column" (zone viewers)
- [ ] Visual selection indicators
- [ ] Batch operations on selection
- [ ] Keyboard shortcuts for selection

---

### 27. **Refactor DraggableBattlefieldGrid** 🛠️ MAINTENANCE
**Estimated Effort:** 2-3 days

- [ ] Split 492-line file into smaller functions
- [ ] Extract drag state management
- [ ] Extract drop detection logic
- [ ] Simplify nested conditionals
- [ ] Add unit tests for complex logic

---

### 28. **Add Logging Framework** 🛠️ CODE QUALITY
**Estimated Effort:** 1 day

- [ ] Replace println() with SLF4J or similar
- [ ] Add log levels (DEBUG, INFO, WARN, ERROR)
- [ ] Structured logging
- [ ] Log file rotation

---

### 29. **Centralize Magic Numbers** 🛠️ CODE QUALITY
**Estimated Effort:** 1 day

- [ ] Move all constants to UIConstants.kt
- [ ] MAX_STACK_SIZE = 3
- [ ] GRID_COLUMNS = 4
- [ ] GRID_ROWS = 10
- [ ] Document all constants

---

### 30. **Extract Sub-Composables** 🛠️ CODE QUALITY
**Estimated Effort:** 2 days

- [ ] Break down 300+ line composables
- [ ] Extract reusable components
- [ ] Reduce nesting depth
- [ ] Improve readability

---

### 31. **Add Input Debouncing** 🛠️ PERFORMANCE
**Estimated Effort:** 1 day

- [ ] Add 300ms debounce to search inputs
- [ ] Prevent unnecessary recompositions
- [ ] Apply to LibrarySearchDialog
- [ ] Apply to all text input fields

---

### 32. **Zone Dialog Performance** ⚠️ PERFORMANCE
**Estimated Effort:** 2 days

- [ ] Add virtualization for large lists
- [ ] Lazy loading for zone contents
- [ ] Pagination for graveyards/exile with 100+ cards
- [ ] Performance testing with large zones

---

### 33. **Chat System** (Requires networking first)
**Estimated Effort:** 1-2 days

- [ ] ChatPanel composable
- [ ] Chat input field
- [ ] Message history
- [ ] Player color coding
- [ ] Chat commands: /roll, /flip
- [ ] Network integration

---

## 🔵 LOW PRIORITY - Nice to Have

### 34. **Testing Improvements**
**Estimated Effort:** 1-2 weeks

- [ ] Add Compose UI tests
- [ ] Add integration tests for full game flows
- [ ] Add performance benchmarks
- [ ] Increase test coverage to 80%+
- [ ] Add snapshot tests for UI

---

### 35. **Card Peek** (Duplicate - covered in #21)
**Estimated Effort:** 1-2 days

- [ ] Look at face-down cards without revealing
- [ ] Private peek (only you see)
- [ ] Context menu "Peek at Card"
- [ ] Peek indicator in UI

---

### 36. **Related Cards** (Duplicate - covered in #24)
**Estimated Effort:** 3-4 days

- [ ] Query Scryfall for related cards
- [ ] Generate tokens from card database
- [ ] Show token options for cards
- [ ] Create all related tokens at once

---

### 37. **Card Arrows** (Duplicate - covered in #25)
**Estimated Effort:** 2-3 days

- [ ] Draw arrows between cards
- [ ] Draw arrows from cards to players
- [ ] Color-coded arrows
- [ ] Click to delete arrows
- [ ] Arrow persistence

---

### 38. **Animations**
**Estimated Effort:** 1-2 weeks

- [ ] Card movement animations
- [ ] Tap rotation animation (currently instant)
- [ ] Zone transition effects
- [ ] Life counter animations
- [ ] Smooth drag animations

---

### 39. **Sound Effects**
**Estimated Effort:** 3-4 days

- [ ] Card draw sound
- [ ] Card play sound
- [ ] Tap sound
- [ ] Life change sound
- [ ] Turn pass sound
- [ ] Phase change sound
- [ ] Volume controls

---

### 40. **Themes**
**Estimated Effort:** 1 week

- [ ] Dark mode (current)
- [ ] Light mode
- [ ] Custom card backs
- [ ] Custom backgrounds
- [ ] Custom zone colors
- [ ] Theme selection UI

---

### 41. **Game Save/Load**
**Estimated Effort:** 3-4 days

- [ ] Save game state to JSON file
- [ ] Load saved games
- [ ] Auto-save on exit
- [ ] Game replay system
- [ ] Save file management

---

### 42. **Combat System Helpers** (Optional)
**Estimated Effort:** 1-2 weeks

- [ ] Declare attackers UI
- [ ] Declare blockers UI
- [ ] Combat damage assignment
- [ ] First strike handling
- [ ] Combat damage tracking

**Note:** MTG combat is very complex. Consider leaving as manual.

---

### 43. **Stack Management** (Optional)
**Estimated Effort:** 2-3 weeks

- [ ] Stack visualization
- [ ] Spell/ability ordering
- [ ] Priority passing
- [ ] Response windows

**Note:** Extremely complex. Consider leaving as manual.

---

### 44. **Deck Builder**
**Estimated Effort:** 2-3 weeks

- [ ] In-app deck creation
- [ ] Scryfall card search
- [ ] Add/remove cards
- [ ] Commander selection
- [ ] Deck validation
- [ ] Save/load decks
- [ ] Import/export formats
- [ ] Deck statistics (mana curve, etc.)

---

### 45. **Additional Deck Formats**
**Estimated Effort:** 1-2 weeks

- [ ] .dec format
- [ ] .cod format (COmmander Deck format)
- [ ] JSON format
- [ ] Deck validation UI
  - [ ] Commander legality
  - [ ] 100-card requirement
  - [ ] Color identity
  - [ ] Banned/restricted cards

---

## 📊 DEVELOPMENT ROADMAP

### 🎯 Phase 1: Critical Fixes & Core Features (2-3 weeks)
1. ✅ Fix Recursive Stack Risk (2 hours) - COMPLETED v2.19.0
2. ✅ Package Naming Refactor (3 hours) - COMPLETED v2.19.0
3. Game Log/History System (2-3 days)
4. Commander Tax Tracking (1 day)
5. ✅ Grid Recalculation Performance (2-3 days) - COMPLETED v2.20.0
6. Player Counters System (2-3 days)
7. ✅ Advanced Library Operations (3-4 days) - COMPLETED v2.21.0

**Result:** Stable, feature-complete hotseat mode

---

### 🎯 Phase 2: Code Quality & Maintenance (1-2 weeks)
8. Refactor GameScreen.kt (1-2 days)
9. State Sync Pattern Duplication (1 day)
10. Image Loading Performance (2 days)
11. Refactor DraggableBattlefieldGrid (2-3 days)
12. Add Logging Framework (1 day)
13. Centralize Magic Numbers (1 day)

**Result:** Maintainable, performant codebase

---

### 🎯 Phase 3: Quality of Life Features (1-2 weeks)
14. P/T Modification (2 days)
15. Keyboard Shortcuts (1 day)
16. Settings/Preferences (2-3 days)
17. Card Annotations (2 days)
18. Visual Attachments (2-3 days)
19. Die Rolling (1-2 days)
20. Hand Management (2 days)

**Result:** Polished user experience

---

### 🎯 Phase 4: Network Multiplayer (3-4 weeks)
21. Network Backend Implementation (3-4 weeks)
22. Chat System (1-2 days)
23. Testing & Bug Fixes (3-4 days)

**Result:** Full network multiplayer support

---

### 🎯 Phase 5: Polish & Extras (ongoing)
24. Animations
25. Sound Effects
26. Themes
27. Testing Improvements
28. Additional features as needed

**Result:** Complete, polished Dong-A-Deuce experience

---

## 📝 NOTES

### What Makes This App Great:
- Hotseat mode is fully playable (90% complete)
- Comprehensive game state management
- Professional Material3 UI
- Excellent MVVM architecture
- Well-tested core mechanics (44 unit tests)
- Cross-platform with Windows EXE builds

### Current Blockers:
- **Hotseat Polish:** Game log, commander tax, player counters
- **Network Play:** Entire backend (3-4 weeks)
- **Code Quality:** Large files, performance issues, technical debt

### Recommended Next Actions:
1. **Immediate:** Fix recursive stack risk bug (2 hours)
2. **This Week:** Game log + commander tax + package refactor
3. **Next 2 Weeks:** Performance fixes + code refactoring
4. **Then Decide:** Polish hotseat OR start network multiplayer

---

**Last Updated:** 2025-11-05
**Version:** v2.21.0
