# Comprehensive Missing Features Analysis

**Current Version:** 1.6.0
**Overall Completion:** ~70%
**Last Updated:** 2025-10-27

---

## Executive Summary

The Commander MTG application currently has a **solid foundation** with core gameplay mechanics, visual polish, and Commander-specific features. However, it is **single-player only** and lacks the networking infrastructure required for true multiplayer gameplay.

**Critical Gaps:**
- ❌ **Zero networking implementation** - Completely blocks multiplayer
- ❌ **No multi-player initialization** - Game hardcoded for 1 opponent
- ❌ **No dynamic UI layouts** - Can only display 2 players
- ⚠️ **Missing ~15 ViewModel functions** - Limits gameplay options
- ⚠️ **No right-click context menus** - All actions require dialogs
- ⚠️ **No library search functionality** - Common MTG mechanic missing

---

## 🔴 CRITICAL - Blocking Multiplayer (Must Have for 2+ Players)

### 1. Networking Foundation (0% Complete)

**Status:** Not started
**Estimated Effort:** 2-3 weeks
**Blocks:** All multiplayer functionality

#### Missing Files:
- `shared/src/main/kotlin/com/commandermtg/network/GameServer.kt`
- `shared/src/main/kotlin/com/commandermtg/network/GameClient.kt`
- `shared/src/main/kotlin/com/commandermtg/network/GameMessage.kt`

#### Required Functionality:

**GameServer.kt:**
```kotlin
class GameServer(val port: Int = 8080) {
    // Missing:
    - Accept WebSocket connections from clients
    - Maintain list of connected players
    - Broadcast game state updates to all clients
    - Handle player disconnects gracefully
    - Validate game actions
    - Ensure turn order synchronization
}
```

**GameClient.kt:**
```kotlin
class GameClient {
    // Missing:
    - Connect to host server via WebSocket
    - Send local player actions to server
    - Receive and apply game state updates
    - Handle reconnection logic
    - Ping/heartbeat to detect disconnects
}
```

**GameMessage.kt:**
```kotlin
sealed class GameMessage {
    // Missing message types:
    - PlayerJoined(playerId: String, playerName: String)
    - PlayerLeft(playerId: String)
    - DeckLoaded(playerId: String)
    - GameStarted(players: List<Player>)
    - DrawCard(playerId: String)
    - PlayCard(cardId: String, targetZone: Zone)
    - MoveCard(cardId: String, fromZone: Zone, toZone: Zone)
    - TapCard(cardId: String)
    - UpdateLife(playerId: String, newLife: Int)
    - CommanderDamage(targetPlayerId: String, commanderId: String, damage: Int)
    - NextPhase()
    - PassTurn()
    - ChatMessage(playerId: String, message: String)
}
```

#### Integration Points:

**MenuViewModel.kt:**
- Line 136: `// TODO: Start network server` in `startHosting()`
- Line 166: `// TODO: Connect to network server` in `connectToGame()`
- **Missing:** Server startup logic
- **Missing:** Client connection logic
- **Missing:** Lobby synchronization

**GameViewModel.kt:**
- **Missing:** Network action broadcasting
- **Missing:** Network action listener
- **Missing:** State synchronization logic
- All game actions (draw, play, tap, etc.) currently local only

---

### 2. Multi-Player Game Initialization (10% Complete)

**Status:** Hardcoded for 2 players only
**Estimated Effort:** 3-5 days
**Blocks:** 3+ player games

#### Current Issues:

**GameScreen.kt:**
```kotlin
// Line 27-31: Hardcoded opponent initialization
viewModel.initializeGame(
    localPlayerName = "You",
    opponentNames = listOf("Opponent")  // ❌ Only 1 opponent
)

// Line 62: Only displays first opponent
val opponent = uiState.opponents.firstOrNull()  // ❌ Ignores other opponents
```

#### Required Changes:

1. **Pass connected players from lobby to game:**
   - MainScreen.kt needs to pass actual player list
   - GameScreen must accept `playerList: List<String>` parameter
   - Remove hardcoded opponent initialization

2. **Initialize game with all players:**
   ```kotlin
   // In GameScreen.kt
   viewModel.initializeGame(
       localPlayerName = currentPlayer.name,
       opponentNames = lobbyPlayers.filter { it != currentPlayer.name }
   )
   ```

3. **Load deck for each player:**
   ```kotlin
   // Missing function in GameViewModel:
   fun loadDeckForPlayer(playerId: String, deck: Deck) {
       // Each player loads their own deck
       // Broadcast deck loaded status to network
       // Wait for all players before starting
   }
   ```

---

### 3. Multi-Player UI Layout (5% Complete)

**Status:** Only 2-player layout exists
**Estimated Effort:** 1 week
**Blocks:** 3-4 player games

#### Current Limitation:

**GameScreen.kt:**
- Hardcoded layout: Opponent (top), Battlefield (middle), LocalPlayer (bottom)
- Only displays first opponent from list
- Cannot accommodate 3-4 players

#### Required Files:

```kotlin
// desktop/src/main/kotlin/com/commandermtg/ui/layouts/TwoPlayerLayout.kt
@Composable
fun TwoPlayerLayout(
    localPlayer: Player,
    opponent: Player,
    viewModel: GameViewModel
)

// desktop/src/main/kotlin/com/commandermtg/ui/layouts/ThreePlayerLayout.kt
@Composable
fun ThreePlayerLayout(
    localPlayer: Player,
    opponents: List<Player>,  // 2 opponents
    viewModel: GameViewModel
) {
    // Layout: Opponent1 (top-left), Opponent2 (top-right)
    //         Battlefield (center)
    //         LocalPlayer (bottom)
}

// desktop/src/main/kotlin/com/commandermtg/ui/layouts/FourPlayerLayout.kt
@Composable
fun FourPlayerLayout(
    localPlayer: Player,
    opponents: List<Player>,  // 3 opponents
    viewModel: GameViewModel
) {
    // Layout: Opponent1 (top), Opponent2 (left), Opponent3 (right)
    //         Battlefield (center)
    //         LocalPlayer (bottom)
}
```

#### Required GameScreen Changes:

```kotlin
@Composable
fun GameScreen(
    loadedDeck: Deck?,
    playerList: List<String>,  // ✅ Add parameter
    viewModel: GameViewModel
) {
    val uiState by viewModel.uiState.collectAsState()

    // Dynamic layout selection
    when (uiState.allPlayers.size) {
        2 -> TwoPlayerLayout(uiState.localPlayer!!, uiState.opponents[0], viewModel)
        3 -> ThreePlayerLayout(uiState.localPlayer!!, uiState.opponents, viewModel)
        4 -> FourPlayerLayout(uiState.localPlayer!!, uiState.opponents, viewModel)
    }
}
```

---

## 🟡 HIGH PRIORITY - Core Gameplay Enhancement

### 4. GameViewModel Functions (95% Complete)

**Status:** 23 out of 24 essential functions implemented
**Estimated Effort:** 1-2 days for remaining function (requires UI component)
**Impact:** All essential gameplay mechanics now available

#### Implemented Functions ✅:
1. `initializeGame()`
2. `loadDeck()`
3. `drawStartingHand()`
4. `updateLife()`
5. `drawCard()`
6. `moveCard()`
7. `toggleTap()`
8. `getCardCount()`
9. `getCards()`
10. `getBattlefieldCards()`
11. `nextPhase()`
12. `passTurn()`
13. `untapAll()`
14. `selectCard()`
15. `getAllCommanders()`
16. `updateCommanderDamage()`
17. `shuffleLibrary()` ✨ NEW in v1.6.0
18. `getPlayerBattlefieldCards()` ✨ NEW in v1.6.0
19. `addCounter()` ✨ NEW in v1.6.0
20. `removeCounter()` ✨ NEW in v1.6.0
21. `attachCard()` ✨ NEW in v1.6.0
22. `detachCard()` ✨ NEW in v1.6.0
23. `flipCard()` ✨ NEW in v1.6.0
24. `millCards()` ✨ NEW in v1.6.0
25. `mulligan()` ✨ NEW in v1.6.0

#### Missing Functions ❌:

**1. `searchLibrary(playerId: String, predicate: (Card) -> Boolean): List<CardInstance>`**
```kotlin
fun searchLibrary(playerId: String, predicate: (Card) -> Boolean): List<CardInstance> {
    val gameState = _uiState.value.gameState ?: return emptyList()
    return gameState.cardInstances
        .filter { it.ownerId == playerId && it.zone == Zone.LIBRARY && predicate(it.card) }
}
```
**Use Case:** "Search your library for a basic land", tutor effects
**Blocker:** Requires Library Search Dialog UI component (see section 5 below)
**Note:** Function itself is trivial to implement, but needs UI to be useful

---

### 5. Library Search Dialog (0% Complete)

**Status:** Not implemented
**Estimated Effort:** 1-2 days
**Impact:** Common MTG mechanic missing

#### Required File:

```kotlin
// desktop/src/main/kotlin/com/commandermtg/ui/LibrarySearchDialog.kt
@Composable
fun LibrarySearchDialog(
    cards: List<CardInstance>,
    playerName: String,
    searchFilter: String,  // e.g., "basic land", "creature"
    onDismiss: () -> Unit,
    onSelectCard: (CardInstance) -> Unit  // Put into hand
) {
    // Search input field
    // Filtered card list
    // Card preview
    // "Add to Hand" button
    // "Shuffle Library" button on close
}
```

#### Integration:
- Add "Search Library" button to PlayerArea
- Add onClick handler to Library ZoneCard
- Call `viewModel.searchLibrary()` with predicate

---

### 6. Card Context Menu (0% Complete)

**Status:** Not implemented
**Estimated Effort:** 2-3 days
**Impact:** All actions currently require opening dialogs

#### Required File:

```kotlin
// desktop/src/main/kotlin/com/commandermtg/ui/CardContextMenu.kt
@Composable
fun CardContextMenu(
    card: CardInstance,
    visible: Boolean,
    onDismiss: () -> Unit,
    viewModel: GameViewModel
) {
    DropdownMenu(expanded = visible, onDismissRequest = onDismiss) {
        // Move to zone actions
        DropdownMenuItem("Move to Hand") { viewModel.moveCard(card.instanceId, Zone.HAND) }
        DropdownMenuItem("Move to Graveyard") { viewModel.moveCard(card.instanceId, Zone.GRAVEYARD) }
        DropdownMenuItem("Move to Exile") { viewModel.moveCard(card.instanceId, Zone.EXILE) }
        DropdownMenuItem("Move to Library Top") { /* TODO */ }
        DropdownMenuItem("Move to Library Bottom") { /* TODO */ }

        Divider()

        // Tap/Untap
        DropdownMenuItem(if (card.isTapped) "Untap" else "Tap") {
            viewModel.toggleTap(card.instanceId)
        }

        Divider()

        // Counters
        DropdownMenuItem("Add +1/+1 Counter") { viewModel.addCounter(card.instanceId, "+1/+1") }
        DropdownMenuItem("Add -1/-1 Counter") { viewModel.addCounter(card.instanceId, "-1/-1") }
        DropdownMenuItem("Remove Counter") { /* Show submenu */ }

        Divider()

        // Attach
        if (card.card.type?.contains("Aura") == true ||
            card.card.type?.contains("Equipment") == true) {
            DropdownMenuItem("Attach to...") { /* Show target selection */ }
        }

        // View details
        DropdownMenuItem("View Details") { /* Show full card */ }
    }
}
```

#### Integration Points:
- BattlefieldCard: Add right-click/long-press handler
- HandDialog cards: Add right-click handler
- ZoneViewer cards: Add right-click handler

**Current workaround:** Users must open dialogs and use specific buttons

---

### 7. Enhanced Hand UI (30% Complete)

**Status:** Only "Play to Battlefield" button exists
**Estimated Effort:** 1 day

#### Current HandDialog:

```kotlin
// desktop/src/main/kotlin/com/commandermtg/ui/GameScreen.kt:460-540
@Composable
fun HandDialog(...) {
    // ✅ Shows card list
    // ✅ "Play" button (moves to battlefield)
    // ❌ No "Discard" button
    // ❌ No "Exile" button
    // ❌ No "Put on Top/Bottom of Library" buttons
    // ❌ No card reordering
    // ❌ No multi-select
}
```

#### Required Enhancements:

```kotlin
Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
    Button(onClick = { onPlayCard(cardInstance) }) { Text("Play") }

    OutlinedButton(onClick = {
        viewModel.moveCard(cardInstance.instanceId, Zone.GRAVEYARD)
    }) { Text("Discard") }

    OutlinedButton(onClick = {
        viewModel.moveCard(cardInstance.instanceId, Zone.EXILE)
    }) { Text("Exile") }

    // Dropdown for library placement
    var showLibraryMenu by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { showLibraryMenu = true }) {
            Text("To Library...")
        }
        DropdownMenu(expanded = showLibraryMenu, onDismissRequest = { showLibraryMenu = false }) {
            DropdownMenuItem("Top of Library") { /* TODO */ }
            DropdownMenuItem("Bottom of Library") { /* TODO */ }
        }
    }
}
```

#### Missing Features:
- **Card reordering:** Drag-and-drop to reorder cards in hand
- **Multi-select:** Select multiple cards for batch operations
- **Keyboard shortcuts:** Number keys to quick-select cards

---

### 8. Battlefield Enhancements (15% Missing)

**Status:** Core display complete, missing interactions
**Estimated Effort:** 2-3 days

#### Missing Features:

1. **Attached cards display:**
   ```kotlin
   // In BattlefieldCard.kt
   val attachedCards = viewModel.getAttachedCards(cardInstance.instanceId)
   if (attachedCards.isNotEmpty()) {
       // Show small indicators/badges for attached cards
       // Stacked visual representation
   }
   ```

2. **Right-click context menu:**
   - Currently: Single click toggles tap/untap
   - Missing: Right-click for full action menu

3. **Drag to reorder/group:**
   - Currently: Cards displayed in order added
   - Missing: Manual card positioning
   - Missing: Visual grouping (lands, creatures, artifacts)

4. **Card hover preview:**
   - Currently: Small card on battlefield
   - Missing: Hover to show large preview
   - Missing: Oracle text on hover

---

## 🔵 LOW PRIORITY - Quality of Life Features

### 9. Game Log (0% Complete)

**Estimated Effort:** 2-3 days

#### Required File:

```kotlin
// desktop/src/main/kotlin/com/commandermtg/ui/GameLog.kt
@Composable
fun GameLog(
    actions: List<GameAction>,
    modifier: Modifier = Modifier
) {
    // Scrollable action history
    // Color-coded by player
    // Timestamps
    // Filter options
}

data class GameAction(
    val timestamp: Long,
    val playerId: String,
    val playerName: String,
    val action: String,  // "drew 3 cards", "played Sol Ring", "passed turn"
    val details: String? = null
)
```

#### Integration:
- Add GameLog to GameScreen sidebar (collapsible)
- Track all game actions in GameViewModel
- Persist log for replay/export

**Use Cases:**
- Review what happened during complex turns
- Resolve disputes about game state
- Export for analysis

---

### 10. Chat System (0% Complete)

**Estimated Effort:** 1-2 days
**Requires:** Networking (CRITICAL dependency)

#### Required File:

```kotlin
// desktop/src/main/kotlin/com/commandermtg/ui/ChatPanel.kt
@Composable
fun ChatPanel(
    messages: List<ChatMessage>,
    onSendMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        // Scrollable message history
        LazyColumn {
            messages.forEach { msg ->
                ChatBubble(
                    playerName = msg.playerName,
                    message = msg.text,
                    color = msg.playerColor
                )
            }
        }

        // Input field
        Row {
            TextField(value = messageText, onValueChange = { ... })
            Button(onClick = { onSendMessage(messageText) }) { Text("Send") }
        }
    }
}
```

#### Chat Commands:
- `/roll 1d20` - Roll dice
- `/flip` - Flip coin
- `/clear` - Clear chat history

**Dependency:** Requires networking to send messages

---

### 11. Keyboard Shortcuts (0% Complete)

**Estimated Effort:** 1 day

#### Proposed Shortcuts:

```kotlin
// In GameScreen.kt
modifier = Modifier.onKeyEvent { event ->
    when (event.key) {
        Key.Spacebar -> viewModel.nextPhase()  // Pass priority
        Key.Enter -> viewModel.passTurn()  // Pass turn
        Key.T -> selectedCard?.let { viewModel.toggleTap(it) }  // Tap selected
        Key.U -> viewModel.untapAll(localPlayer.id)  // Untap all
        Key.D -> viewModel.drawCard(localPlayer.id)  // Draw card
        Key.M -> viewModel.mulligan(localPlayer.id)  // Mulligan
        Key.One..Key.Nine -> selectCardByIndex(event.key.ordinal)  // Select card
    }
}
```

**Current workaround:** All actions require mouse clicks

---

### 12. Persistence & Replay (0% Complete)

**Estimated Effort:** 3-4 days

#### Required Features:

1. **Save Game State:**
   ```kotlin
   fun saveGame(filepath: String) {
       val gameState = _uiState.value.gameState
       val json = Json.encodeToString(gameState)
       File(filepath).writeText(json)
   }
   ```

2. **Load Game:**
   ```kotlin
   fun loadGame(filepath: String) {
       val json = File(filepath).readText()
       val gameState = Json.decodeFromString<GameState>(json)
       _uiState.update { it.copy(gameState = gameState) }
   }
   ```

3. **Export Game Log:**
   - Save all game actions to text/JSON
   - Human-readable format
   - Import into analysis tools

4. **Game Replay:**
   - Step through game action by action
   - Review decisions
   - Teaching tool

---

## 🎨 POLISH - Future Enhancements (v2.0.0)

### 13. Animations (0% Complete)

**Estimated Effort:** 1-2 weeks

- Card movement animations (hand → battlefield)
- Tap/untap rotation animation (currently instant)
- Zone transitions (fade in/out)
- Life counter animations (number changes)
- Turn change transition

**Framework:** Compose Animation APIs

---

### 14. Sound Effects (0% Complete)

**Estimated Effort:** 3-4 days

- Card draw sound
- Card play sound
- Tap sound
- Life change sound
- Turn change sound
- Phase change sound
- Critical life warning (< 10)

**Library:** Javax.sound or Compose Desktop audio

---

### 15. Themes/Settings (0% Complete)

**Estimated Effort:** 1 week

#### Settings Menu:

```kotlin
data class AppSettings(
    val theme: Theme = Theme.DARK,
    val soundVolume: Float = 0.7f,
    val animationSpeed: Float = 1.0f,
    val defaultDeckDirectory: String = "",
    val networkPort: Int = 8080,
    val playerName: String = "Player 1",
    val autoUntap: Boolean = true,
    val confirmDestructiveActions: Boolean = true
)
```

#### Themes:
- Dark mode (current)
- Light mode
- Custom backgrounds
- Custom card backs
- Color blind modes

---

### 16. In-App Deck Builder (0% Complete)

**Estimated Effort:** 2-3 weeks

**Major Feature - Not essential for initial release**

#### Features:
- Search Scryfall database
- Add/remove cards from deck
- Validate Commander rules (100 cards, singleton, color identity)
- Save to Cockatrice format
- Import from Cockatrice
- Deck statistics (mana curve, color distribution)

---

## Summary by Priority

### 🔴 MUST HAVE (Blocks Multiplayer):
1. **Networking Foundation** - 0% complete, 2-3 weeks
2. **Multi-Player Initialization** - 10% complete, 3-5 days
3. **Multi-Player UI Layouts** - 5% complete, 1 week

**Total: 3-4 weeks for basic multiplayer**

---

### 🟡 SHOULD HAVE (Enhances Gameplay):
1. **Missing ViewModel Functions** - 60% complete, 3-4 days
2. **Library Search Dialog** - 0% complete, 1-2 days
3. **Card Context Menu** - 0% complete, 2-3 days
4. **Enhanced Hand UI** - 30% complete, 1 day
5. **Battlefield Enhancements** - 85% complete, 2-3 days

**Total: 1.5-2 weeks for enhanced gameplay**

---

### 🔵 NICE TO HAVE (Quality of Life):
1. **Game Log** - 0% complete, 2-3 days
2. **Chat System** - 0% complete, 1-2 days (needs networking)
3. **Keyboard Shortcuts** - 0% complete, 1 day
4. **Persistence/Replay** - 0% complete, 3-4 days

**Total: 1-1.5 weeks for QOL features**

---

### 🎨 POLISH (Future):
1. **Animations** - 1-2 weeks
2. **Sound Effects** - 3-4 days
3. **Themes/Settings** - 1 week
4. **Deck Builder** - 2-3 weeks

**Total: 4-6 weeks for polish**

---

## Recommended Implementation Order

### Phase 1: Enable Multiplayer (3-4 weeks)
1. Implement networking foundation (GameServer, GameClient, GameMessage)
2. Add multi-player game initialization
3. Create dynamic UI layouts (2-4 players)
4. Test with 2-4 players over network

**Result:** Functional multiplayer game

---

### Phase 2: Enhance Gameplay (2 weeks)
1. Add missing ViewModel functions (shuffle, search, mill, counters, etc.)
2. Create Library Search Dialog
3. Implement Card Context Menu
4. Enhance Hand UI with multiple action buttons
5. Add battlefield enhancements (attached cards, drag-drop)

**Result:** Feature-complete gameplay

---

### Phase 3: Quality of Life (1-2 weeks)
1. Add Game Log
2. Implement Chat System
3. Add Keyboard Shortcuts
4. Create Persistence/Replay

**Result:** Professional user experience

---

### Phase 4: Polish (4-6 weeks)
1. Add animations
2. Implement sound effects
3. Create themes and settings
4. Build in-app deck builder

**Result:** Production-ready application

---

## Current Blockers

### For Local Hotseat Multiplayer:
1. ✅ **None** - Can play 2 players on same computer right now
2. ⚠️ **Multi-Player UI** - Can only see 1 opponent (3-4 players not supported)

### For Network Multiplayer:
1. ❌ **Networking Foundation** - Zero implementation
2. ❌ **State Synchronization** - No network protocol
3. ❌ **Lobby System** - UI exists but not functional

### For Competitive Play:
1. ⚠️ **Game Log** - No action history
2. ⚠️ **Replay/Undo** - No way to review past actions
3. ⚠️ **Rules Enforcement** - No validation (honor system only)

---

## Conclusion

The application is **65% complete** and has excellent single-player/hotseat functionality with polished UI and all Commander-specific features. However, **true multiplayer over network is completely blocked** by the lack of networking infrastructure.

**Minimum Viable Product (MVP) Path:**
- **For Hotseat Play:** Only need Multi-Player UI Layouts (1 week)
- **For Network Play:** Need all CRITICAL features (3-4 weeks)
- **For Competitive Play:** Need CRITICAL + SHOULD HAVE features (5-6 weeks)

**The code is well-architected and ready for networking integration.** The MVVM structure with StateFlow makes it straightforward to broadcast state changes over the network.
