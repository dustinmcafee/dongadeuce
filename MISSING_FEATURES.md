# Exact Missing Features for 2+ Player Cockatrice-Style Commander

## 1. NETWORKING (Complete Category Missing)

### Server Implementation
**Location:** `shared/src/main/kotlin/com/commandermtg/network/GameServer.kt` (doesn't exist)
**What's needed:**
```kotlin
class GameServer {
    // Ktor WebSocket server on port 8080
    // Accept incoming player connections
    // Maintain list of connected clients
    // Broadcast game state updates to all clients
    // Receive and relay player actions
    // Handle player disconnects/reconnects
}
```

### Client Implementation
**Location:** `shared/src/main/kotlin/com/commandermtg/network/GameClient.kt` (doesn't exist)
**What's needed:**
```kotlin
class GameClient {
    // Connect to host via WebSocket
    // Send local player actions to host
    // Receive game state updates
    // Handle connection loss
    // Reconnection logic
}
```

### Network Protocol
**Location:** `shared/src/main/kotlin/com/commandermtg/network/GameMessage.kt` (doesn't exist)
**What's needed:**
```kotlin
sealed class GameMessage {
    // PlayerJoined(playerId, playerName, deckInfo)
    // PlayerLeft(playerId)
    // GameStarted(playerOrder, initialState)
    // CardDrawn(playerId, cardInstanceId)
    // CardPlayed(playerId, cardInstanceId, targetZone)
    // CardMoved(cardInstanceId, fromZone, toZone)
    // CardTapped(cardInstanceId, isTapped)
    // LifeChanged(playerId, newLife)
    // PhaseChanged(newPhase)
    // TurnAdvanced(newActivePlayer, turnNumber)
    // ChatMessage(playerId, message)
    // etc.
}
```

### Network Integration in ViewModels
**Location:** `desktop/src/main/kotlin/com/commandermtg/viewmodel/GameViewModel.kt`
**Missing:**
- Network client instance
- Methods to send actions over network
- Listener for incoming network messages
- State update from network events

**Location:** `desktop/src/main/kotlin/com/commandermtg/viewmodel/MenuViewModel.kt`
**Missing in `startHosting()`:**
```kotlin
// Start GameServer instance
// Listen for player connections
// Add connected players to lobby list
```

**Missing in `connectToGame()`:**
```kotlin
// Connect GameClient to host IP
// Send local player info and deck
// Wait for game start
```

---

## 2. MULTIPLAYER GAME INITIALIZATION

### Pass Players from Lobby to Game
**Location:** `desktop/src/main/kotlin/com/commandermtg/ui/MainScreen.kt:27`
**Current:**
```kotlin
Screen.Game -> GameScreen(loadedDeck = uiState.loadedDeck)
```
**Needs:**
```kotlin
Screen.Game -> GameScreen(
    loadedDeck = uiState.loadedDeck,
    allPlayers = uiState.allPlayerInfo,  // MISSING
    localPlayerId = uiState.playerName,   // MISSING
    isHost = uiState.isHosting
)
```

### GameViewModel Player Initialization
**Location:** `desktop/src/main/kotlin/com/commandermtg/viewmodel/GameViewModel.kt:26`
**Current:** Hardcoded single opponent
```kotlin
viewModel.initializeGame(
    localPlayerName = "You",
    opponentNames = listOf("Opponent")
)
```
**Needs:**
```kotlin
viewModel.initializeGame(
    localPlayerName = actualPlayerName,
    localPlayerId = actualPlayerId,
    allPlayers = listFromLobby  // List of ALL players with IDs
)
```

### Deck Loading for All Players
**Location:** `desktop/src/main/kotlin/com/commandermtg/ui/GameScreen.kt:44`
**Current:** Only local player
```kotlin
viewModel.loadDeck(loadedDeck)
```
**Needs:**
- Each player loads their own deck
- Over network: Send deck info to all players
- Store all players' decks in GameState
- Function: `viewModel.loadDeckForPlayer(playerId, deck)`

---

## 3. UI COMPONENTS MISSING

### Multi-Player Layout
**Location:** `desktop/src/main/kotlin/com/commandermtg/ui/GameScreen.kt:51-93`
**Current:** Fixed 2-player vertical layout
```
[Opponent Area]     ← Only shows 1 opponent
[Battlefield]
[Your Area]
```

**Needs:** Dynamic layout for 2-4 players
```
For 2 players:          For 3 players:          For 4 players:
[Opponent]              [Opponent 1]            [Opp 2][Opp 3]
[Battlefield]           [Opp 2][Battlefield]    [Battlefield]
[You]                   [You]                   [Opp 1][You]
```

**Exact code needed:**
```kotlin
// In GameScreen.kt
when (uiState.opponents.size) {
    1 -> TwoPlayerLayout(...)
    2 -> ThreePlayerLayout(...)
    3 -> FourPlayerLayout(...)
    else -> error("Unsupported player count")
}
```

### Battlefield Card Display
**Location:** `desktop/src/main/kotlin/com/commandermtg/ui/GameScreen.kt:73-80`
**Current:**
```kotlin
@Composable
fun BattlefieldArea(modifier: Modifier) {
    Card(/* empty green box */)
}
```

**Needs:**
```kotlin
@Composable
fun BattlefieldArea(
    allCards: List<CardInstance>,  // All permanents on battlefield
    viewModel: GameViewModel,
    modifier: Modifier
) {
    LazyVerticalGrid(columns = GridCells.Adaptive(100.dp)) {
        items(allCards) { cardInstance ->
            BattlefieldCard(
                cardInstance = cardInstance,
                onTap = { viewModel.toggleTap(it.instanceId) },
                onRightClick = { /* show context menu */ }
            )
        }
    }
}

@Composable
fun BattlefieldCard(
    cardInstance: CardInstance,
    onTap: (CardInstance) -> Unit,
    onRightClick: (CardInstance) -> Unit
) {
    // Card image or placeholder
    // Show tapped rotation
    // Show counters
    // Show attached cards (auras/equipment)
    // Show controller color border
}
```

### Zone Viewer Dialogs
**Location:** `desktop/src/main/kotlin/com/commandermtg/ui/ZoneViewer.kt` (doesn't exist)
**Needs:**
```kotlin
@Composable
fun GraveyardDialog(
    cards: List<CardInstance>,
    playerName: String,
    onDismiss: () -> Unit,
    onCardAction: (CardInstance, Action) -> Unit
)

@Composable
fun ExileDialog(/* similar */)

@Composable
fun LibrarySearchDialog(/* for searching library */)
```

### Turn/Phase Indicator
**Location:** `desktop/src/main/kotlin/com/commandermtg/ui/TurnIndicator.kt` (doesn't exist)
**Needs:**
```kotlin
@Composable
fun TurnIndicator(
    activePlayer: Player,
    currentPhase: GamePhase,
    turnNumber: Int,
    onPassTurn: () -> Unit,
    onNextPhase: () -> Unit
) {
    // Show "Turn 5 - Alice's Main Phase 1"
    // Highlight active player
    // "Next Phase" button
    // "Pass Turn" button
    // Phase list showing current phase
}
```

### Commander Damage Tracker
**Location:** `desktop/src/main/kotlin/com/commandermtg/ui/CommanderDamageDialog.kt` (doesn't exist)
**Needs:**
```kotlin
@Composable
fun CommanderDamageDialog(
    player: Player,
    allCommanders: List<CardInstance>,  // All commanders in game
    onDamageChange: (commanderId: String, amount: Int) -> Unit,
    onDismiss: () -> Unit
) {
    // Matrix showing damage from each commander
    // Click to add/subtract damage
    // Highlight if 21+ damage from any commander
}
```

### Card Context Menu
**Location:** `desktop/src/main/kotlin/com/commandermtg/ui/CardContextMenu.kt` (doesn't exist)
**Needs:**
```kotlin
@Composable
fun CardContextMenu(
    cardInstance: CardInstance,
    availableActions: List<CardAction>,
    onActionSelected: (CardAction) -> Unit,
    onDismiss: () -> Unit
) {
    // Move to Graveyard
    // Move to Exile
    // Tap/Untap
    // Add +1/+1 counter
    // Add -1/-1 counter
    // Attach to...
    // View card details
    // etc.
}

enum class CardAction {
    MOVE_TO_GRAVEYARD,
    MOVE_TO_EXILE,
    MOVE_TO_HAND,
    MOVE_TO_LIBRARY_TOP,
    MOVE_TO_LIBRARY_BOTTOM,
    SHUFFLE_INTO_LIBRARY,
    TAP,
    UNTAP,
    FLIP,
    ADD_COUNTER_PLUS_ONE,
    ADD_COUNTER_MINUS_ONE,
    ADD_LOYALTY_COUNTER,
    REMOVE_COUNTER,
    ATTACH_TO,
    VIEW_DETAILS
}
```

---

## 4. GAMEVIEWMODEL MISSING FUNCTIONS

**Location:** `desktop/src/main/kotlin/com/commandermtg/viewmodel/GameViewModel.kt`

**Missing functions:**
```kotlin
// Load deck for specific player (for network sync)
fun loadDeckForPlayer(playerId: String, deck: Deck)

// Get all battlefield cards
fun getBattlefieldCards(): List<CardInstance>

// Get battlefield cards for specific player
fun getPlayerBattlefieldCards(playerId: String): List<CardInstance>

// Shuffle library
fun shuffleLibrary(playerId: String)

// Search library
fun searchLibrary(playerId: String, predicate: (Card) -> Boolean): List<CardInstance>

// Mill cards (library to graveyard)
fun millCards(playerId: String, count: Int)

// Mulligan (shuffle hand back and draw N-1)
fun mulligan(playerId: String)

// Add counter to card
fun addCounter(cardInstanceId: String, counterType: String, amount: Int)

// Remove counter from card
fun removeCounter(cardInstanceId: String, counterType: String, amount: Int)

// Flip card
fun flipCard(cardInstanceId: String)

// Attach card to another (auras/equipment)
fun attachCard(sourceId: String, targetId: String)

// Detach card
fun detachCard(cardInstanceId: String)

// Take commander damage
fun takeCommanderDamage(playerId: String, commanderId: String, amount: Int)

// Advance phase
fun advancePhase()

// Pass turn
fun passTurn()

// Untap all permanents for player
fun untapAll(playerId: String)

// Set active player
fun setActivePlayer(playerId: String)

// Broadcast action to network (if hosting/connected)
private fun broadcastAction(action: GameMessage)

// Handle incoming network action
fun handleNetworkAction(action: GameMessage)
```

---

## 5. GAME STATE UPDATES

**Location:** `shared/src/main/kotlin/com/commandermtg/models/GameState.kt`

**Missing functions:**
```kotlin
// Get all cards in specific zone across all players
fun getAllCardsInZone(zone: Zone): List<CardInstance>

// Get all battlefield permanents
fun getAllBattlefieldCards(): List<CardInstance>

// Find card by instance ID
fun findCard(instanceId: String): CardInstance?

// Move card and maintain game rules
fun moveCardWithRules(instanceId: String, targetZone: Zone): GameState

// Handle state transition rules (e.g., auras fall off when creature dies)
fun applyStateBasedActions(): GameState
```

---

## 6. MENU/LOBBY ENHANCEMENTS

### MenuViewModel Missing
**Location:** `desktop/src/main/kotlin/com/commandermtg/viewmodel/MenuViewModel.kt`

```kotlin
// Track all connected players
data class PlayerInfo(
    val id: String,
    val name: String,
    val deckName: String?,
    val isReady: Boolean
)

// In MenuUiState:
val allPlayers: List<PlayerInfo> = emptyList()
val isReady: Boolean = false

// Functions:
fun setPlayerReady(isReady: Boolean)
fun kickPlayer(playerId: String)  // Host only
fun updatePlayerDeck(playerId: String, deckName: String)  // Network event
```

### HostLobbyScreen Enhancements
**Location:** `desktop/src/main/kotlin/com/commandermtg/ui/MainScreen.kt:141`

**Current:** Shows placeholder player list
**Needs:**
- Show actual connected players with ready status
- Show each player's loaded deck name
- Kick button (for host)
- Can only start when all players ready
- Player count selector (2-4 players)

---

## 7. CLICK INTERACTIONS MISSING

### Zones Not Clickable
**Location:** `desktop/src/main/kotlin/com/commandermtg/ui/GameScreen.kt:263-289`

**Current:** `ZoneCard` is just static display
**Needs:**
```kotlin
@Composable
fun ZoneCard(
    label: String,
    zone: Zone,
    cardCount: Int,
    onClick: () -> Unit,  // MISSING
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clickable { onClick() }  // MISSING
            .border(...)
    ) {
        // ... existing content
    }
}
```

**Then in GameScreen:**
```kotlin
ZoneCard(
    "Graveyard",
    Zone.GRAVEYARD,
    graveyardCount,
    onClick = { showGraveyardDialog = true }  // MISSING
)
```

### Battlefield Cards Not Clickable
**Needs:** Right-click context menu on battlefield cards

### Hand Cards Limited Actions
**Location:** `desktop/src/main/kotlin/com/commandermtg/ui/GameScreen.kt:342-348`

**Current:** Only "Play" button
**Needs:**
- Discard to graveyard
- Exile from hand
- Put on top/bottom of library
- Context menu with all options

---

## 8. CARD IMAGES

**Location:** Throughout UI

**Current:** Cards show as text only
**Needs:**
```kotlin
@Composable
fun CardImage(
    card: Card,
    modifier: Modifier = Modifier
) {
    // Use card.imageUri from Scryfall
    // AsyncImage loading (Ktor client)
    // Cache images locally
    // Placeholder while loading
    // Fallback if image fails
}
```

**Image caching:**
```kotlin
// Location: shared/src/main/kotlin/com/commandermtg/game/ImageCache.kt
object ImageCache {
    private val cacheDir = File("resources/cache/images")

    suspend fun getCardImage(imageUri: String): File {
        // Check local cache first
        // Download if not cached
        // Return file path
    }
}
```

---

## 9. GAME ACTIONS NOT IMPLEMENTED

### In GameViewModel - All Missing:
1. **Shuffle library** - exists in concept, not exposed
2. **Scry** (look at top N cards, reorder)
3. **Reveal cards**
4. **Create tokens**
5. **Copy permanents**
6. **Transform/flip cards**
7. **Manifest cards**
8. **Foretell**
9. **Mill** (library to graveyard)
10. **Tutor** (search library)
11. **Reanimate** (graveyard to battlefield)
12. **Blink** (exile and return)
13. **Bounce** (battlefield to hand)

---

## 10. MISSING UI/UX FEATURES

### No Keyboard Shortcuts
- Space: Pass priority
- Enter: Pass turn
- T: Tap selected card
- U: Untap selected card
- D: Draw card
- M: Mulligan

### No Card Selection
- Can't select multiple cards
- Can't select "all creatures"
- No shift/ctrl-click multi-select

### No Zoom/Preview
- Can't hover to see card details
- Can't zoom in on card images
- No oracle text display on hover

### No Game Log
- No history of actions taken
- Can't see "Alice drew 3 cards"
- Can't see "Bob played Sol Ring"
- No undo/history

### No Chat
- Can't communicate with opponents
- No emotes
- No game messages

### No Search/Filter
- Can't search battlefield for "all artifacts"
- Can't filter graveyard by card type
- No advanced search

---

## 11. MISSING GAME RULES ENFORCEMENT

**Currently:** NO rules enforcement at all

**Missing:**
- Can't prevent playing lands on opponent's turn
- Can't prevent playing more than 1 land per turn
- No mana system (can play anything anytime)
- No stack (spells resolve instantly)
- No priority system
- No "until end of turn" effects
- No triggered abilities
- No state-based actions (creatures with 0 toughness die)

**Note:** Cockatrice doesn't enforce these either - it's manual like real paper Magic. But the UI should support the manual workflow.

---

## 12. PERSISTENCE/SAVE

**Missing:**
- No save game state
- No load game
- No game replay
- No export game log
- No deck builder (currently file-only)

---

## SUMMARY COUNT

### Code Files That Don't Exist But Are Needed:
1. `GameServer.kt` (Server implementation)
2. `GameClient.kt` (Client implementation)
3. `GameMessage.kt` (Network protocol)
4. `ZoneViewer.kt` (Zone dialog components)
5. `TurnIndicator.kt` (Turn/phase UI)
6. `CommanderDamageDialog.kt` (Commander damage tracker)
7. `CardContextMenu.kt` (Right-click menu)
8. `CardImage.kt` (Image loading/display)
9. `ImageCache.kt` (Image caching system)
10. `BattlefieldCard.kt` (Battlefield card component)

### Major Functions Missing from Existing Files:
- **GameViewModel:** 20+ functions
- **MenuViewModel:** 5+ functions
- **GameState:** 8+ functions
- **UI Components:** 15+ click handlers

### UI Components Missing:
- Multi-player layout (3 variants)
- Battlefield card display
- 4 zone viewer dialogs
- Turn/phase indicator
- Commander damage tracker
- Card context menu
- Card image component
- Game log
- Chat

### Features Completely Missing:
- Networking (100%)
- Multi-player support (95%)
- Battlefield visualization (100%)
- Zone viewers (100%)
- Turn system UI (100%)
- Card images (100%)
- Commander damage tracking UI (100%)
- Game actions (80%)
- Rules support (100% - by design like Cockatrice)

**Total Missing:** Roughly 50-60% of a complete Cockatrice-like application.
