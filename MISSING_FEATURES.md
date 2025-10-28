# Missing Features for Commander MTG

**Last Updated:** 2025-10-28 (v2.10.6)

This document tracks missing features needed for a complete Commander multiplayer experience. See [TODO.md](TODO.md) for detailed implementation tasks.

---

## 🔴 CRITICAL Features (Blocks Playability)

### Turn/Phase System
**Status:** ❌ Not Implemented (data structure exists, no UI)

**Description:** Visual indicator and controls for MTG phases and turn advancement.

**Needed:**
- Phase toolbar showing all MTG phases (Untap, Upkeep, Draw, Main, Combat, End)
- Current phase highlighting
- Active player indicator
- "Next Phase" button
- "Pass Turn" button
- Automatic untap on new turn

**Impact:** Games cannot progress properly without turn management.

---

### Commander Damage Tracking UI
**Status:** ⚠️ Partially Implemented (data model exists, no UI)

**Description:** Track combat damage from each commander to each opponent. 21+ damage from single commander = loss.

**Needed:**
- Commander damage dialog/overlay
- Matrix showing damage from each commander
- +/- buttons for damage adjustment
- Visual indicator at 21+ damage
- Button to access damage tracker from player area

**Impact:** Commander format rules cannot be enforced without this.

---

### Game Log/History
**Status:** ❌ Not Implemented

**Description:** Scrollable log of all game actions and events.

**Needed:**
- Real-time event feed
- Player actions ("Alice drew 2 cards")
- Game state changes ("Turn 5 - Bob's Main Phase")
- Chat messages
- Timestamp display
- Player color coding
- Auto-scroll to latest

**Impact:** Players lose track of what happened, especially in multiplayer.

---

## 🟡 HIGH Priority Features

### Commander Tax
**Status:** ❌ Not Implemented

**Description:** Track additional cost for casting commander from command zone.

**Needed:**
- Counter for times cast from command zone
- Display tax amount ({2} per previous cast)
- Increment on cast
- Optional reset on commander death

---

### Card Images
**Status:** ⚠️ Partially Implemented (URLs from Scryfall, not rendered)

**Description:** Display card artwork instead of text-only.

**Needed:**
- AsyncImage loading from Scryfall
- Local image cache
- Placeholder while loading
- Fallback for failed loads
- Image display in all zones

---

### Additional Game Actions
**Status:** ❌ Not Implemented

**Missing Actions:**
- Shuffle library
- Scry (look at top N, reorder)
- Mill (library to graveyard)
- Create tokens
- Mulligan
- Search library (tutor)
- Reveal cards
- Transform/flip cards
- Copy permanents

---

### Multi-Player Dynamic Layout
**Status:** ⚠️ Fixed 2-player layout only

**Description:** UI layout that scales for 2-4 players.

**Needed:**
- Different layouts for 2, 3, and 4 players
- Players arranged around virtual table
- Shared battlefield in center
- Compact opponent areas

**Current:** Only supports vertical 2-player layout.

---

## 🔵 CRITICAL for Network Play

### P2P Networking
**Status:** ❌ Not Implemented

**Description:** Ktor WebSocket-based client-server architecture for remote multiplayer.

**Needed:**
- GameServer (host)
- GameClient (joiners)
- GameAction protocol
- State synchronization
- Connection management
- Reconnection handling

---

### Lobby System
**Status:** ⚠️ UI exists, not functional

**Description:** Pre-game lobby for player connections and deck loading.

**Needed:**
- Show all connected players
- Ready/unready status
- Deck validation
- Kick player (host only)
- Start game when all ready
- Pass player list to game

---

## 🟢 MEDIUM Priority Features

### Improved Battlefield
**Needed:**
- Card hover preview (large card view)
- Multi-card selection
- P/T modification display
- Card attachments (auras/equipment) visualization
- Attack arrows

### UI Enhancements
**Needed:**
- Keyboard shortcuts (Space = pass, T = tap, etc.)
- Card zoom on hover
- Settings dialog
- Improved context menus
- Theme customization

### Deck Management
**Needed:**
- Deck validation (commander legality, 100 cards, color identity)
- Multiple deck formats (.dec, .cod, JSON)
- Basic deck editor
- Recent decks list

---

## 🟣 LOW Priority / Future

### Game Management
- Save/load game state
- Export game log
- Concede button
- Restart game
- Game statistics

### Advanced Features
- Game replay system
- Chat system
- Spectator mode
- Partner commander support
- Planechase support

### Special Mechanics
- Monarch/Initiative tracking
- Energy counters
- Poison counters
- Experience counters
- Day/Night indicator
- Dungeon cards

---

## Feature Comparison vs Full Implementation

| Category | Commander MTG | Complete Implementation |
|----------|--------------|-------------------------|
| Basic Zones | ✅ 100% | ✅ 100% |
| Zone Viewers | ✅ 100% | ✅ 100% |
| Life Tracking | ✅ 100% | ✅ 100% |
| Drag & Drop | ✅ 100% | ✅ 100% |
| Tap/Untap | ✅ 100% | ✅ 100% |
| Hotseat UI | ✅ 75% | ✅ 100% |
| Turn System | ❌ 0% | ✅ 100% |
| Commander Damage | ⚠️ 20% | ✅ 100% |
| Game Log | ❌ 0% | ✅ 100% |
| Card Images | ⚠️ 10% | ✅ 100% |
| Game Actions | ⚠️ 30% | ✅ 100% |
| Networking | ❌ 0% | ✅ 100% |
| **Overall** | **~25%** | **100%** |

---

## Prioritization Rationale

### Why Turn System is #1 Priority
Without turn management, players cannot properly sequence their actions. This is fundamental to Magic gameplay.

### Why Commander Damage is #2 Priority
Commander damage is a core win condition in the Commander format. Games are incomplete without it.

### Why Game Log is #3 Priority
In multiplayer games, players need to track what happened. The log provides game state awareness and prevents confusion.

### Why Networking Can Wait
Hotseat mode (local multiplayer on same computer) is valuable and should be fully functional before adding network complexity. This allows testing game logic without network complications.

---

## Estimated Completion Timeline

### Phase 1: Playable Hotseat (2 weeks)
- Turn/Phase System
- Commander Damage UI
- Game Log
- Commander Tax

**Result:** Fully playable local multiplayer

### Phase 2: Enhanced Gameplay (2-3 weeks)
- Card Images
- Additional Game Actions
- Improved Battlefield
- UI Enhancements

**Result:** Polished hotseat experience

### Phase 3: Network Multiplayer (3-4 weeks)
- P2P Networking
- Lobby System
- State Synchronization
- Network Testing

**Result:** Remote multiplayer capability

### Phase 4: Polish (1-2 weeks)
- Deck Management
- Save/Load
- Chat
- Performance

**Result:** Production-ready application

**Total Estimate:** 8-11 weeks for feature-complete Commander multiplayer game

---

## Dependencies Between Features

```
Turn System ──> Game Log (logs turn events)
              └─> Untap Automation

Commander Damage ──> Game Log (logs damage events)

Card Images ──> Image Cache
             └─> AsyncImage Loading

Networking ──> Lobby System
            └─> Game State Sync
            └─> All Game Actions (must be networkable)

Game Log ──> All Features (logs all events)
```

---

**See [TODO.md](TODO.md) for implementation details and task breakdown.**
