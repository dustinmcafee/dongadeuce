# Dong-A-Deuce - Development TODO

**Current Version:** v2.19.0
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

### 4. **Grid Recalculation Performance** ⚠️ PERFORMANCE
**Current Status:** O(n²) complexity on every card position change
**Estimated Effort:** 2-3 days

**What's Needed:**
- [ ] Profile grid recalculation in DraggableBattlefieldGrid.kt
- [ ] Use incremental updates instead of full recalculation
- [ ] Add memoization for expensive calculations
- [ ] Use derivedStateOf for computed grid positions

**Why Critical:** Performance bottleneck that gets worse with more cards

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

### 7. **Advanced Library Operations**
**Current Status:** Basic operations only
**Estimated Effort:** 3-4 days

**What's Needed:**
- [ ] View top N cards
- [ ] View bottom N cards
- [ ] Move top/bottom cards to specific zones
- [ ] Shuffle top N cards
- [ ] Shuffle bottom N cards
- [ ] Reveal top card to all players
- [ ] Reveal top card to self only
- [ ] Conditional moves with filter

**Why Important:** Core MTG gameplay mechanics missing

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
**Estimated Effort:** 2 days

**What's Needed:**
- [ ] Add `powerOverride: Int?` and `toughnessOverride: Int?` to CardInstance
- [ ] Context menu option "Set P/T"
- [ ] Dialog for entering custom P/T
- [ ] Display modified P/T in UI
- [ ] Clear override option

**Why Important:** Very common in Commander gameplay

---

### 10. **Refactor GameScreen.kt** 🛠️ MAINTENANCE
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

### 21. **Refactor DraggableBattlefieldGrid** 🛠️ MAINTENANCE
**Estimated Effort:** 2-3 days

- [ ] Split 492-line file into smaller functions
- [ ] Extract drag state management
- [ ] Extract drop detection logic
- [ ] Simplify nested conditionals
- [ ] Add unit tests for complex logic

---

### 22. **Add Logging Framework** 🛠️ CODE QUALITY
**Estimated Effort:** 1 day

- [ ] Replace println() with SLF4J or similar
- [ ] Add log levels (DEBUG, INFO, WARN, ERROR)
- [ ] Structured logging
- [ ] Log file rotation

---

### 23. **Centralize Magic Numbers** 🛠️ CODE QUALITY
**Estimated Effort:** 1 day

- [ ] Move all constants to UIConstants.kt
- [ ] MAX_STACK_SIZE = 3
- [ ] GRID_COLUMNS = 4
- [ ] GRID_ROWS = 10
- [ ] Document all constants

---

### 24. **Extract Sub-Composables** 🛠️ CODE QUALITY
**Estimated Effort:** 2 days

- [ ] Break down 300+ line composables
- [ ] Extract reusable components
- [ ] Reduce nesting depth
- [ ] Improve readability

---

### 25. **Add Input Debouncing** 🛠️ PERFORMANCE
**Estimated Effort:** 1 day

- [ ] Add 300ms debounce to search inputs
- [ ] Prevent unnecessary recompositions
- [ ] Apply to LibrarySearchDialog
- [ ] Apply to all text input fields

---

### 26. **Zone Dialog Performance** ⚠️ PERFORMANCE
**Estimated Effort:** 2 days

- [ ] Add virtualization for large lists
- [ ] Lazy loading for zone contents
- [ ] Pagination for graveyards/exile with 100+ cards
- [ ] Performance testing with large zones

---

### 27. **Chat System** (Requires networking first)
**Estimated Effort:** 1-2 days

- [ ] ChatPanel composable
- [ ] Chat input field
- [ ] Message history
- [ ] Player color coding
- [ ] Chat commands: /roll, /flip
- [ ] Network integration

---

## 🔵 LOW PRIORITY - Nice to Have

### 28. **Testing Improvements**
**Estimated Effort:** 1-2 weeks

- [ ] Add Compose UI tests
- [ ] Add integration tests for full game flows
- [ ] Add performance benchmarks
- [ ] Increase test coverage to 80%+
- [ ] Add snapshot tests for UI

---

### 29. **Card Peek**
**Estimated Effort:** 1-2 days

- [ ] Look at face-down cards without revealing
- [ ] Private peek (only you see)
- [ ] Context menu "Peek at Card"
- [ ] Peek indicator in UI

---

### 30. **Related Cards**
**Estimated Effort:** 3-4 days

- [ ] Query Scryfall for related cards
- [ ] Generate tokens from card database
- [ ] Show token options for cards
- [ ] Create all related tokens at once

---

### 31. **Card Arrows**
**Estimated Effort:** 2-3 days

- [ ] Draw arrows between cards
- [ ] Draw arrows from cards to players
- [ ] Color-coded arrows
- [ ] Click to delete arrows
- [ ] Arrow persistence

---

### 32. **Animations**
**Estimated Effort:** 1-2 weeks

- [ ] Card movement animations
- [ ] Tap rotation animation (currently instant)
- [ ] Zone transition effects
- [ ] Life counter animations
- [ ] Smooth drag animations

---

### 33. **Sound Effects**
**Estimated Effort:** 3-4 days

- [ ] Card draw sound
- [ ] Card play sound
- [ ] Tap sound
- [ ] Life change sound
- [ ] Turn pass sound
- [ ] Phase change sound
- [ ] Volume controls

---

### 34. **Themes**
**Estimated Effort:** 1 week

- [ ] Dark mode (current)
- [ ] Light mode
- [ ] Custom card backs
- [ ] Custom backgrounds
- [ ] Custom zone colors
- [ ] Theme selection UI

---

### 35. **Game Save/Load**
**Estimated Effort:** 3-4 days

- [ ] Save game state to JSON file
- [ ] Load saved games
- [ ] Auto-save on exit
- [ ] Game replay system
- [ ] Save file management

---

### 36. **Combat System Helpers** (Optional)
**Estimated Effort:** 1-2 weeks

- [ ] Declare attackers UI
- [ ] Declare blockers UI
- [ ] Combat damage assignment
- [ ] First strike handling
- [ ] Combat damage tracking

**Note:** MTG combat is very complex. Consider leaving as manual.

---

### 37. **Stack Management** (Optional)
**Estimated Effort:** 2-3 weeks

- [ ] Stack visualization
- [ ] Spell/ability ordering
- [ ] Priority passing
- [ ] Response windows

**Note:** Extremely complex. Consider leaving as manual.

---

### 38. **Deck Builder**
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

### 39. **Additional Deck Formats**
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
1. Fix Recursive Stack Risk (2 hours) ⚠️
2. Package Naming Refactor (3 hours)
3. Game Log/History System (2-3 days)
4. Commander Tax Tracking (1 day)
5. Grid Recalculation Performance (2-3 days)
6. Player Counters System (2-3 days)
7. Advanced Library Operations (3-4 days)

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
**Version:** v2.19.0
