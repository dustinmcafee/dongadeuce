# Commander MTG - Development TODO

**Goal:** Full-featured Commander multiplayer experience
**Current Status:** Hotseat mode 75% complete, Network mode 5% complete
**Target:** Complete hotseat + network multiplayer with all essential Commander features

---

## 🔴 CRITICAL - Phase 1: Playable Hotseat Mode (1-2 weeks)

### Turn/Phase System (Priority: CRITICAL)
- [ ] Create `GamePhase` enum with all MTG phases
- [ ] Add `currentPhase` to `GameState`
- [ ] Create `TurnIndicator` composable component
  - [ ] Phase buttons (all phases visible)
  - [ ] Current phase highlighting
  - [ ] Turn counter display
  - [ ] Active player name
  - [ ] "Next Phase" button
  - [ ] "Pass Turn" button
- [ ] Implement `GameViewModel.advancePhase()`
- [ ] Implement `GameViewModel.passTurn()`
- [ ] Implement `GameViewModel.untapAllPermanents(playerId)`
- [ ] Add untap phase automation
- [ ] Integrate turn indicator into GameScreen layout
- [ ] Network broadcast for phase changes (when networking added)

**Files:**
- `shared/src/main/kotlin/com/commandermtg/models/GamePhase.kt` (NEW)
- `desktop/src/main/kotlin/com/commandermtg/ui/TurnIndicator.kt` (NEW)
- `desktop/src/main/kotlin/com/commandermtg/viewmodel/GameViewModel.kt` (MODIFY)
- `desktop/src/main/kotlin/com/commandermtg/ui/GameScreen.kt` (MODIFY)

---

### Commander Damage Tracking UI (Priority: CRITICAL)
- [ ] Create `CommanderDamageDialog` composable
  - [ ] Show all commanders in game
  - [ ] Damage from each commander to target player
  - [ ] +/- buttons for damage adjustment
  - [ ] Lethal indicator (21+ damage)
  - [ ] Visual styling per commander owner
- [ ] Add "Commander Damage" button to player areas
- [ ] Implement `GameViewModel.updateCommanderDamage()`
- [ ] Implement `GameViewModel.checkCommanderDamageLethal()`
- [ ] Add commander damage display in player info
- [ ] Show lethal warning when 21+ damage taken
- [ ] Network broadcast for commander damage (when networking added)

**Files:**
- `desktop/src/main/kotlin/com/commandermtg/ui/CommanderDamageDialog.kt` (NEW)
- `desktop/src/main/kotlin/com/commandermtg/viewmodel/GameViewModel.kt` (MODIFY)
- `desktop/src/main/kotlin/com/commandermtg/ui/GameScreen.kt` (MODIFY)

---

### Game Log/History (Priority: HIGH)
- [ ] Create `GameEvent` sealed class hierarchy
  - [ ] CardDrawn
  - [ ] CardPlayed
  - [ ] CardMoved
  - [ ] LifeChanged
  - [ ] CommanderDamageDealt
  - [ ] TurnAdvanced
  - [ ] PhaseChanged
  - [ ] ChatMessage
  - [ ] CardTapped/Untapped
  - [ ] CountersAdded/Removed
- [ ] Create `GameLog` composable component
  - [ ] Scrollable event list
  - [ ] Timestamp display
  - [ ] Player name with color
  - [ ] Event message formatting
  - [ ] Auto-scroll to bottom
- [ ] Add event logging to all game actions in ViewModel
- [ ] Integrate game log into GameScreen layout
- [ ] Add log filtering options (optional)
- [ ] Network broadcast for events (when networking added)

**Files:**
- `shared/src/main/kotlin/com/commandermtg/models/GameEvent.kt` (NEW)
- `desktop/src/main/kotlin/com/commandermtg/ui/GameLog.kt` (NEW)
- `desktop/src/main/kotlin/com/commandermtg/viewmodel/GameViewModel.kt` (MODIFY - add logging)
- `desktop/src/main/kotlin/com/commandermtg/ui/GameScreen.kt` (MODIFY)

---

### Commander Tax Tracking (Priority: HIGH)
- [ ] Add `commanderCastCount` to `CardInstance`
- [ ] Implement `CardInstance.getCommanderTax()`
- [ ] Implement `CardInstance.getTotalCost()`
- [ ] Show commander tax in UI when viewing commander
- [ ] Increment cast count when commander cast from command zone
- [ ] Implement `GameViewModel.castCommander()`
- [ ] Display tax amount in command zone card
- [ ] Reset tax on commander death (optional rule)

**Files:**
- `shared/src/main/kotlin/com/commandermtg/models/CardInstance.kt` (MODIFY)
- `desktop/src/main/kotlin/com/commandermtg/viewmodel/GameViewModel.kt` (MODIFY)
- `desktop/src/main/kotlin/com/commandermtg/ui/GameScreen.kt` (MODIFY)

---

## 🟡 HIGH PRIORITY - Phase 2: Enhanced Gameplay (2-3 weeks)

### Additional Game Actions
- [ ] Implement `shuffleLibrary(playerId)`
- [ ] Implement `scry(playerId, count)` - look at top N, reorder
- [ ] Implement `mill(playerId, count)` - library to graveyard
- [ ] Implement `tutor(playerId, cardName)` - search library
- [ ] Implement `createToken(playerId, tokenData)`
- [ ] Implement `mulligan(playerId)` - shuffle hand, draw N-1
- [ ] Implement `revealCard(cardId, toPlayers)` - show card to opponents
- [ ] Implement `flipCard(cardId)` - transform/flip
- [ ] Implement `cloneCard(cardId)` - copy permanent
- [ ] Implement `bounceCard(cardId)` - battlefield to hand
- [ ] Implement `blinkCard(cardId)` - exile and return

**Files:**
- `desktop/src/main/kotlin/com/commandermtg/viewmodel/GameViewModel.kt` (MODIFY)

---

### Card Images
- [ ] Create `CardImage` composable with AsyncImage
- [ ] Implement image loading from Scryfall
- [ ] Create image cache system
  - [ ] Check local cache first
  - [ ] Download if not cached
  - [ ] LRU eviction policy
- [ ] Add placeholder image while loading
- [ ] Add fallback for failed loads
- [ ] Replace text-only cards with images in:
  - [ ] Hand display
  - [ ] Battlefield cards
  - [ ] Zone viewers
  - [ ] Card preview hover

**Files:**
- `desktop/src/main/kotlin/com/commandermtg/ui/CardImage.kt` (NEW)
- `shared/src/main/kotlin/com/commandermtg/game/ImageCache.kt` (NEW)
- `desktop/src/main/kotlin/com/commandermtg/ui/BattlefieldCard.kt` (MODIFY)
- `desktop/src/main/kotlin/com/commandermtg/ui/GameScreen.kt` (MODIFY)

---

### Improved Battlefield
- [ ] Show card images instead of just text
- [ ] Add card hover preview (large card view)
- [ ] Improve counter display
- [ ] Add P/T modification display
- [ ] Show card attachments (auras/equipment)
  - [ ] Visual connection lines
  - [ ] Grouped display
- [ ] Add attack arrow system (optional)
- [ ] Multi-card selection (Shift/Ctrl+click)
- [ ] Batch operations on selected cards

**Files:**
- `desktop/src/main/kotlin/com/commandermtg/ui/DraggableBattlefieldGrid.kt` (MODIFY)
- `desktop/src/main/kotlin/com/commandermtg/ui/BattlefieldCard.kt` (MODIFY)
- `desktop/src/main/kotlin/com/commandermtg/ui/CardAttachment.kt` (NEW)

---

### UI Enhancements
- [ ] Add keyboard shortcuts
  - [ ] Space: Pass priority
  - [ ] Enter: Pass turn
  - [ ] T: Tap selected
  - [ ] U: Untap selected
  - [ ] D: Draw card
  - [ ] M: Mulligan
- [ ] Card zoom on hover
- [ ] Settings dialog
  - [ ] Display settings
  - [ ] Keybindings
  - [ ] Network settings
  - [ ] Card image quality
- [ ] Improved card context menu
  - [ ] Move to specific zone options
  - [ ] Counter management submenu
  - [ ] Token creation
  - [ ] View card details
- [ ] Theme customization
- [ ] Custom battlefield backgrounds

**Files:**
- `desktop/src/main/kotlin/com/commandermtg/ui/settings/` (NEW directory)
- `desktop/src/main/kotlin/com/commandermtg/ui/CardContextMenu.kt` (MODIFY)
- Various UI files

---

## 🔵 CRITICAL - Phase 3: Network Multiplayer (3-4 weeks)

### P2P Networking Foundation
- [ ] Define `GameAction` sealed class
  - [ ] All game actions as serializable messages
  - [ ] Include timestamp, player ID, action data
- [ ] Define `GameStateSync` data class
  - [ ] Full game state serialization
  - [ ] Incremental state updates
- [ ] Implement `GameServer` with Ktor WebSocket
  - [ ] Listen on port 8080
  - [ ] Accept client connections
  - [ ] Maintain connected clients list
  - [ ] Broadcast state updates to all clients
  - [ ] Handle client disconnection
- [ ] Implement `GameClient` with Ktor WebSocket
  - [ ] Connect to host IP:port
  - [ ] Send actions to host
  - [ ] Receive state updates
  - [ ] Handle connection loss
  - [ ] Reconnection logic

**Files:**
- `shared/src/main/kotlin/com/commandermtg/network/GameAction.kt` (NEW)
- `shared/src/main/kotlin/com/commandermtg/network/GameStateSync.kt` (NEW)
- `shared/src/main/kotlin/com/commandermtg/network/GameServer.kt` (NEW)
- `shared/src/main/kotlin/com/commandermtg/network/GameClient.kt` (NEW)
- `shared/src/main/kotlin/com/commandermtg/network/NetworkProtocol.kt` (NEW)

---

### Lobby System
- [ ] Add `PlayerInfo` data class (id, name, deckName, isReady)
- [ ] Track all connected players in MenuViewModel
- [ ] Show connected players in HostLobbyScreen
- [ ] Show each player's deck name
- [ ] Add ready/unready button
- [ ] Add kick button (host only)
- [ ] Disable start until all players ready
- [ ] Pass player list from lobby to game
- [ ] Network sync for lobby state

**Files:**
- `desktop/src/main/kotlin/com/commandermtg/viewmodel/MenuViewModel.kt` (MODIFY)
- `desktop/src/main/kotlin/com/commandermtg/ui/MainScreen.kt` (MODIFY)

---

### Network Integration
- [ ] Broadcast all game actions to network
- [ ] Handle incoming network actions in GameViewModel
- [ ] Sync game state on player join
- [ ] Handle action conflicts/validation
- [ ] Implement action queue for ordering
- [ ] Add network status indicator in UI
- [ ] Handle lag/delay gracefully
- [ ] Implement reconnection recovery
- [ ] Add spectator mode support (optional)

**Files:**
- `desktop/src/main/kotlin/com/commandermtg/viewmodel/GameViewModel.kt` (MODIFY - add network)
- All game action functions (add broadcast)

---

## 🟢 MEDIUM PRIORITY - Phase 4: Polish (1-2 weeks)

### Deck Management
- [ ] Improve deck validation
  - [ ] Check commander legality
  - [ ] Check 100-card requirement
  - [ ] Check color identity
  - [ ] Check banned/restricted cards
- [ ] Support multiple deck formats
  - [ ] Text format (current)
  - [ ] .dec format
  - [ ] .cod format (standard format)
  - [ ] JSON format
- [ ] Deck editor UI (basic)
  - [ ] Add/remove cards
  - [ ] Search card database
  - [ ] Set commander
- [ ] Save/load recent decks

**Files:**
- `shared/src/main/kotlin/com/commandermtg/game/DeckParser.kt` (MODIFY)
- `shared/src/main/kotlin/com/commandermtg/game/DeckValidator.kt` (NEW)
- `desktop/src/main/kotlin/com/commandermtg/ui/DeckEditor.kt` (NEW - optional)

---

### Game Management
- [ ] Save game state to file
- [ ] Load game state from file
- [ ] Export game log
- [ ] Concede button
- [ ] Restart game
- [ ] Confirm quit dialog
- [ ] Game statistics tracking

**Files:**
- `desktop/src/main/kotlin/com/commandermtg/viewmodel/GameViewModel.kt` (MODIFY)
- `shared/src/main/kotlin/com/commandermtg/game/GameStatePersistence.kt` (NEW)

---

### Advanced Features
- [ ] Game replay system
  - [ ] Record all actions
  - [ ] Playback with controls
  - [ ] Step forward/backward
- [ ] Chat system
  - [ ] In-game chat
  - [ ] Lobby chat
  - [ ] Emotes/quick messages
- [ ] Spectator mode
  - [ ] Join game as spectator
  - [ ] View-only mode
  - [ ] Spectator chat
- [ ] Partner commander support
  - [ ] Two commanders per player
  - [ ] Separate commander damage tracking
  - [ ] Color identity from both

**Files:**
- Various

---

## 🟣 LOW PRIORITY - Future Enhancements

### Additional Features
- [ ] Custom tokens with images
- [ ] Planeswalker loyalty counters
- [ ] Monarch/Initiative tracking
- [ ] Energy counter pool
- [ ] Poison counter tracking
- [ ] Experience counter tracking
- [ ] Day/Night indicator
- [ ] Dungeon cards support
- [ ] Planechase support
- [ ] Archenemy support

### Performance
- [ ] Optimize rendering for many cards
- [ ] Lazy loading for large libraries
- [ ] Incremental state updates
- [ ] Connection pooling
- [ ] Image compression

### Accessibility
- [ ] Screen reader support
- [ ] High contrast mode
- [ ] Font size options
- [ ] Colorblind mode
- [ ] Keyboard navigation

---

## Testing Checklist

### Hotseat Mode Testing
- [ ] 2-player game playable start to finish
- [ ] 3-player game playable start to finish
- [ ] 4-player game playable start to finish
- [ ] All phases advance correctly
- [ ] Commander damage tracked correctly
- [ ] Life totals update correctly
- [ ] Cards move between zones correctly
- [ ] Tap/untap works correctly
- [ ] Counters add/remove correctly
- [ ] Game log shows all events

### Network Mode Testing
- [ ] Host can start server
- [ ] Client can connect to host
- [ ] All players see same game state
- [ ] Actions sync across all clients
- [ ] Disconnection handled gracefully
- [ ] Reconnection works
- [ ] Game completes successfully
- [ ] No desync issues

---

## Documentation TODO

- [ ] Update README with current features
- [ ] Add QUICKSTART guide for multiplayer
- [ ] Document network setup (port forwarding, etc.)
- [ ] Add keyboard shortcuts reference
- [ ] Create user manual
- [ ] Add developer documentation
- [ ] Document network protocol
- [ ] Add troubleshooting guide

---

## Current Sprint Focus (Next 2 Weeks)

**Priority:** Make hotseat mode fully playable

1. ✅ **DONE:** Battlefield drag-and-drop (v2.10.5)
2. ✅ **DONE:** Zone card text cutoff fix (v2.10.6)
3. **IN PROGRESS:** Turn/Phase System
4. **NEXT:** Commander Damage UI
5. **NEXT:** Game Log
6. **NEXT:** Commander Tax

**Success Criteria:**
- Can play full Commander game in hotseat mode
- All 4 players have functional zones
- Turn system works smoothly
- Commander damage tracked accurately
- Game log shows all important events

---

## Version Milestones

### v2.11.0 - "Playable Hotseat"
- Turn/Phase system
- Commander damage tracking
- Game log
- Commander tax

### v2.12.0 - "Enhanced Gameplay"
- Additional game actions
- Card images
- Improved battlefield
- UI enhancements

### v3.0.0 - "Network Multiplayer"
- P2P networking
- Lobby system
- Remote multiplayer
- Spectator mode

### v3.1.0 - "Polish"
- Deck validation
- Save/load games
- Chat system
- Performance optimizations

---

## Development Resources

### Helpful Documentation
- Ktor WebSocket: https://ktor.io/docs/websocket.html
- Compose Desktop: https://github.com/JetBrains/compose-multiplatform
- Scryfall API: https://scryfall.com/docs/api
- Commander Rules: https://mtgcommander.net/index.php/rules/

### Design Patterns to Use
- MVVM for UI state management
- Repository pattern for data access
- Command pattern for game actions
- Observer pattern for state updates
- Strategy pattern for network/local modes

---

**Last Updated:** 2025-10-28
**Current Version:** v2.10.6
**Next Version:** v2.11.0 (Phase System)
