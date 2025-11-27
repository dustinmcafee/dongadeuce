# Dong-A-Deuce - Development TODO

**Current Version:** v2.26.0
**Hotseat Mode:** ~97% complete (fully playable!)
**Network Mode:** ~5% complete (UI only)
**Last Updated:** 2025-11-27

---

## PRIORITY DEVELOPMENT ROADMAP

This roadmap is organized by priority. Complete Phase 1 before moving to Phase 2, etc.

---

## Phase 1: Complete Hotseat Mode (1-2 weeks)

These features will make hotseat mode 100% complete.

### 1. Player Counters System - COMPLETED
**Priority:** CRITICAL
**Effort:** 2-3 days
**Why:** Poison is a win condition in MTG
**Status:** DONE

- [x] Add `counters: Map<String, Int>` to Player model
- [x] Implement poison counters (10 = automatic loss)
- [x] Add energy counters support
- [x] Add experience counters support
- [x] Add custom counter types
- [x] Create PlayerCountersDialog UI
- [x] Add +/- controls in player area
- [x] Integrate with checkGameEnd() for poison victory

### 2. Commander Tax Tracking
**Priority:** HIGH
**Effort:** 1 day
**Why:** Core Commander mechanic

- [ ] Add `timesCastFromCommandZone: Int` to CardInstance
- [ ] Calculate tax: `timesCast * 2` generic mana
- [ ] Display tax amount in CommandZoneDialog
- [ ] Increment counter when card moves from command zone to battlefield/stack
- [ ] Show total mana cost including tax
- [ ] Reset counter when commander changes zones (not back to command zone)

### 3. Game Log/History System
**Priority:** HIGH
**Effort:** 2-3 days
**Why:** Dispute resolution, game tracking

- [ ] Create `GameEvent` sealed class:
  - `CardDrawn(playerId, cardName, timestamp)`
  - `CardPlayed(playerId, cardName, zone, timestamp)`
  - `CardMoved(playerId, cardName, fromZone, toZone, timestamp)`
  - `LifeChanged(playerId, oldLife, newLife, timestamp)`
  - `CommanderDamageDealt(sourceId, targetId, amount, timestamp)`
  - `PhaseChanged(phase, timestamp)`
  - `TurnPassed(fromPlayer, toPlayer, timestamp)`
  - `CounterChanged(cardId, counterType, oldAmount, newAmount, timestamp)`
- [ ] Add `gameLog: List<GameEvent>` to GameState
- [ ] Create GameLogDialog with scrollable event list
- [ ] Color-code events by player
- [ ] Add timestamps to all events
- [ ] Auto-scroll to latest events
- [ ] Add "View Game Log" button to game screen

### 4. Die Rolling System - COMPLETED
**Priority:** MEDIUM
**Effort:** 1-2 days
**Why:** Common in Commander gameplay
**Status:** DONE

- [x] Create DieRollerDialog
- [x] Support standard dice: D4, D6, D8, D10, D12, D20, D100
- [x] Add multiple dice rolling
- [x] Display roll results prominently
- [x] Roll history tracking
- [x] Add "Roll Dice" button in turn indicator area
- [x] Coin flip support

---

## Phase 2: Quality of Life (1 week)

### 5. Keyboard Shortcuts
**Priority:** MEDIUM
**Effort:** 1 day

- [ ] Space: Next phase
- [ ] Enter: Pass turn
- [ ] T: Tap selected card(s)
- [ ] U: Untap all
- [ ] D: Draw card
- [ ] M: Mulligan
- [ ] Escape: Close dialogs
- [ ] 1-9: Select hand card by position
- [ ] Add keyboard shortcut hints to UI

### 6. Copy/Clone Cards - COMPLETED
**Priority:** MEDIUM
**Effort:** 1-2 days
**Status:** DONE

- [x] Add `cloneCard(cardId): CardInstance` to GameViewModel
- [x] Track `isClone: Boolean` on CardInstance
- [x] Track `clonedFromId: String?` for clone relationship
- [x] Add "Create Copy" to context menu
- [x] Support multiple copies at once (for token doublers)
- [x] Visual "Copy" indicator on cloned cards

### 7. Settings/Preferences
**Priority:** MEDIUM
**Effort:** 2-3 days

- [ ] Create SettingsDialog UI
- [ ] Player name persistence (save to file)
- [ ] Default deck directory setting
- [ ] Network port configuration (for future)
- [ ] Auto-untap on turn start toggle
- [ ] Confirm destructive actions toggle
- [ ] Card image quality setting
- [ ] Save settings to `~/.dongadeuce/settings.json`

### 8. Hand Management Improvements
**Priority:** LOW
**Effort:** 1-2 days

- [ ] Sort hand by: name, CMC, color, type
- [ ] Reveal hand to specific player(s)
- [ ] Reveal random card from hand
- [ ] Discard random card
- [ ] Add sorting buttons to hand area

---

## Phase 3: Code Quality & Maintenance (1 week)

### 9. Refactor GameScreen.kt
**Priority:** HIGH (Maintenance)
**Effort:** 1-2 days
**Current:** 2,388 lines - too large

- [ ] Extract HotseatGameLayout.kt (hotseat-specific layout)
- [ ] Extract NetworkGameLayout.kt (network-specific layout)
- [ ] Extract PlayerArea.kt (player panel component)
- [ ] Extract DialogManager.kt (dialog state management)
- [ ] Remove code duplication in player layouts
- [ ] Reduce file to <500 lines

### 10. Refactor DraggableBattlefieldGrid.kt
**Priority:** MEDIUM (Maintenance)
**Effort:** 1-2 days
**Current:** 517 lines

- [ ] Extract DragState management to separate file
- [ ] Extract drop detection logic
- [ ] Simplify nested conditionals
- [ ] Add unit tests for grid calculations
- [ ] Document the grid coordinate system

### 11. Add Logging Framework
**Priority:** LOW (Code Quality)
**Effort:** 1 day

- [ ] Add SLF4J or similar logging library
- [ ] Replace println() with proper log levels
- [ ] Add DEBUG, INFO, WARN, ERROR levels
- [ ] Configure log file rotation
- [ ] Add structured logging for debugging

### 12. Centralize Magic Numbers
**Priority:** LOW (Code Quality)
**Effort:** 0.5 days

- [ ] Move all constants to UIConstants.kt or GameConstants.kt
- [ ] MAX_STACK_SIZE = 3
- [ ] GRID_COLUMNS = 4
- [ ] GRID_ROWS = 10
- [ ] All dialog dimensions
- [ ] Document all constants

---

## Phase 4: Network Multiplayer (3-4 weeks)

Only start this phase if network play is a priority.

### 13. Network Protocol Design
**Priority:** CRITICAL (for network)
**Effort:** 2-3 days

- [ ] Design GameMessage sealed class hierarchy:
  - `PlayerJoined(playerId, playerName)`
  - `PlayerLeft(playerId)`
  - `DeckLoaded(playerId)`
  - `GameStarted(gameState)`
  - `ActionPerformed(action: GameAction)`
  - `StateSync(gameState)`
  - `ChatMessage(playerId, message)`
  - `Heartbeat(timestamp)`
- [ ] Design state synchronization strategy
- [ ] Plan conflict resolution approach
- [ ] Document protocol in PROTOCOL.md

### 14. GameServer Implementation
**Priority:** CRITICAL (for network)
**Effort:** 1 week

- [ ] Create GameServer.kt with Ktor WebSocket server
- [ ] Accept player connections on configurable port
- [ ] Maintain connected players list
- [ ] Broadcast game state updates
- [ ] Handle player disconnects gracefully
- [ ] Validate incoming actions
- [ ] Implement heartbeat system
- [ ] Add timeout handling

### 15. GameClient Implementation
**Priority:** CRITICAL (for network)
**Effort:** 1 week

- [ ] Create GameClient.kt with Ktor WebSocket client
- [ ] Connect to host by IP:port
- [ ] Send local actions to server
- [ ] Receive and apply game state updates
- [ ] Implement reconnection logic
- [ ] Handle connection errors gracefully
- [ ] Add connection status UI

### 16. ViewModel Integration
**Priority:** CRITICAL (for network)
**Effort:** 3-4 days

- [ ] Update MenuViewModel with startHosting() and connectToGame()
- [ ] Update GameViewModel to broadcast actions
- [ ] Update GameViewModel to apply remote actions
- [ ] Add network state to UI state
- [ ] Handle network errors in UI
- [ ] Test 2-player, 3-player, 4-player scenarios

### 17. Chat System
**Priority:** MEDIUM (for network)
**Effort:** 1-2 days

- [ ] Create ChatPanel composable
- [ ] Chat input field
- [ ] Message history with scrolling
- [ ] Player name color coding
- [ ] Chat commands: /roll, /flip
- [ ] Network integration

---

## Phase 5: Polish & Extras (Ongoing)

These are nice-to-have features that can be added anytime.

### 18. Animations
**Effort:** 1-2 weeks

- [ ] Card movement animations (zone transitions)
- [ ] Tap rotation animation (smooth 90-degree turn)
- [ ] Life counter animations (bounce on change)
- [ ] Smooth drag animations
- [ ] Phase transition effects

### 19. Sound Effects
**Effort:** 3-4 days

- [ ] Card draw sound
- [ ] Card play sound
- [ ] Tap sound
- [ ] Life change sound
- [ ] Turn pass sound
- [ ] Volume controls
- [ ] Mute toggle

### 20. Themes
**Effort:** 1 week

- [ ] Light mode support
- [ ] Custom card backs
- [ ] Custom backgrounds
- [ ] Theme selection UI
- [ ] Save theme preference

### 21. Game Save/Load
**Effort:** 3-4 days

- [ ] Save game state to JSON file
- [ ] Load saved games
- [ ] Auto-save on exit
- [ ] Game replay system
- [ ] Save file management UI

### 22. Testing Improvements
**Effort:** 1-2 weeks

- [ ] Add Compose UI tests
- [ ] Add integration tests for full game flows
- [ ] Add performance benchmarks
- [ ] Increase test coverage to 80%+
- [ ] Add snapshot tests for UI

### 23. Deck Builder
**Effort:** 2-3 weeks

- [ ] In-app deck creation
- [ ] Scryfall card search
- [ ] Add/remove cards
- [ ] Commander selection
- [ ] Deck validation (Commander rules)
- [ ] Save/load decks
- [ ] Import/export formats
- [ ] Deck statistics (mana curve, color distribution)

---

## COMPLETED FEATURES

### Core Gameplay
- [x] Turn/Phase System with TurnIndicator UI
- [x] Commander Damage Tracking with lethal indicators
- [x] Card Context Menus for all zones (custom popup system)
- [x] Library Search with filtering
- [x] Zone Viewers (Graveyard, Exile, Command Zone)
- [x] Drag-and-Drop Battlefield with grid positioning
- [x] Card Images with async loading and caching
- [x] Tap/Untap (double-click and context menu)
- [x] Counters (add/remove +1/+1, charge, custom)
- [x] Counter Management Dialog (set/add/subtract)
- [x] Card Attachments (aura/equipment system)
- [x] Flip Cards
- [x] Face Down Cards
- [x] Multi-card Selection (shift+click)
- [x] Batch Operations on selected cards
- [x] Token Creation with Scryfall search
- [x] Drag to Zone buttons
- [x] Battlefield Scrolling
- [x] Give Control to other players
- [x] P/T Modification System
- [x] Card Annotations
- [x] "Doesn't Untap" toggle
- [x] Advanced Library Operations (peek, position, shuffle top/bottom)
- [x] Clickable Life Total (set exact value)
- [x] Player Counters (poison, energy, experience, custom) with loss condition
- [x] Die Rolling System (D4, D6, D8, D10, D12, D20, D100, custom, coin flip)
- [x] Card Copy/Clone with visual indicator

### Hotseat Mode
- [x] 2-4 Player Support
- [x] Per-Player Deck Loading
- [x] Automatic Player Rotation
- [x] Hand Privacy
- [x] Turn Passing
- [x] Zone Access Control

### Game State Management
- [x] Life Tracking with auto-loss detection
- [x] Commander Damage with 21-damage rule
- [x] Draw from Empty Library auto-loss
- [x] Zone Management (all 7 MTG zones)
- [x] Card Movement between zones
- [x] Library Operations (draw, mill, shuffle, search, tutor)
- [x] Mulligan support

### Build System
- [x] Windows EXE Build (Launch4j)
- [x] Custom Icon (donkey-dragon hybrid)
- [x] GitHub Actions CI/CD
- [x] Cross-platform JAR
- [x] macOS DMG packaging
- [x] Linux DEB packaging

### Technical
- [x] MVVM Architecture (100% compliant)
- [x] StateFlow reactive updates
- [x] Scryfall Integration
- [x] Bulk Card Cache (500MB+)
- [x] Deck Parsing
- [x] Unit Tests (44 tests)
- [x] Input Validation
- [x] O(n) Grid Performance Optimization
- [x] Package naming consistency (com.dustinmcafee.dongadeuce)
- [x] Stack overflow fix in battlefield positioning

### Bug Fixes (v2.19.0 - v2.25.0)
- [x] Fixed recursive stack overflow crash
- [x] Fixed battlefield card stacking
- [x] Fixed library peek dialog not updating
- [x] Fixed counter display on flipped cards
- [x] Replaced ContextMenuArea with custom popup system

---

## WHAT TO WORK ON NEXT

**Recommended order:**

1. **Player Counters System** (2-3 days) - Poison is a win condition!
2. **Commander Tax Tracking** (1 day) - Core Commander mechanic
3. **Game Log/History** (2-3 days) - Helps dispute resolution
4. **Die Rolling** (1-2 days) - Common in Commander

After Phase 1, decide whether to:
- Polish hotseat mode (QoL features, code cleanup)
- Start network multiplayer (3-4 weeks major effort)

---

## QUICK REFERENCE

### Effort Estimates
- 0.5 days = 4 hours
- 1 day = 8 hours
- 1 week = 40 hours

### Priority Levels
- **CRITICAL:** Blocking major functionality
- **HIGH:** Important for user experience
- **MEDIUM:** Nice to have
- **LOW:** Can wait

---

**Last Updated:** 2025-11-27
**Version:** v2.25.0
