# Critical Issues - Not Ready for 2+ Player Multiplayer

## Status: ❌ **NOT FUNCTIONAL FOR MULTIPLAYER COCKATRICE-STYLE GAMEPLAY**

The current implementation is a **single-player demo** at best. Here are all the critical issues preventing proper 2+ player Commander gameplay:

---

## 🔴 CRITICAL ISSUES

### 1. **Only 1 Opponent Displayed (Even if More Exist)**
**Location:** `GameScreen.kt:57`
```kotlin
val opponent = uiState.opponents.firstOrNull()
```
**Problem:** Only shows the FIRST opponent. If you have 3 opponents (4-player game), only 1 is shown.
**Expected:** Display all opponents with their zones and info.

### 2. **Hardcoded Single Opponent**
**Location:** `GameScreen.kt:30`
```kotlin
viewModel.initializeGame(
    localPlayerName = "You",
    opponentNames = listOf("Opponent")  // Only 1 opponent!
)
```
**Problem:** Game is always initialized with exactly 1 opponent.
**Expected:** Initialize with actual connected players from the lobby.

### 3. **No Deck Loading for Opponents**
**Location:** `GameScreen.kt:44`
```kotlin
viewModel.loadDeck(loadedDeck)  // Only loads local player's deck
```
**Problem:** Only local player's deck is loaded. Opponents have no cards!
**Expected:** Each player loads their own deck (via network in P2P).

### 4. **Lobby Doesn't Pass Players to Game**
**Location:** `MenuViewModel.kt:180`
```kotlin
fun startGame() {
    _uiState.update { it.copy(currentScreen = Screen.Game) }
}
```
**Problem:** Just switches screens. Doesn't pass:
- Connected player names
- Player count
- Any lobby information

**Expected:** Pass connected players to GameViewModel to initialize proper multiplayer game.

### 5. **No Battlefield Card Visibility**
**Location:** `GameScreen.kt:73-80`
```kotlin
BattlefieldArea(/* empty */)
```
**Problem:** Battlefield shows nothing. You can "play" cards but can't see them.
**Expected:** Display all permanents on battlefield with:
- Card names/images
- Tapped state
- Counters
- Who controls them

### 6. **No Turn System UI**
**Problem:** GameState has turn tracking, but no UI to:
- Show whose turn it is
- Advance turns
- Show current phase
- Pass priority

**Expected:** Turn/phase indicator and "Pass Turn" button.

### 7. **No Zone Viewers**
**Problem:** Can't view:
- Graveyard contents (yours or opponents')
- Exile zone
- Library (for searching)
- Opponent's hand size

**Expected:** Click zones to view their contents (except opponent hands).

### 8. **No Network Integration**
**Location:** Multiple places with `// TODO`
**Problem:**
- No server/client code
- No way for players to actually connect
- No game state synchronization
- Lobby just shows placeholder UI

**Expected:** Ktor WebSocket P2P networking to sync game state.

---

## 🟡 MAJOR ISSUES

### 9. **Fixed 2-Player UI Layout**
**Location:** `GameScreen.kt` entire layout
```
[Opponent Area]
[Battlefield]
[Your Area]
```
**Problem:** This layout doesn't scale to 3-4 players.
**Expected:** Cockatrice-style layout with players arranged around table.

### 10. **No Commander Damage Tracking UI**
**Problem:** Player model has commander damage tracking, but no UI to:
- View commander damage received from each opponent
- Set/modify commander damage

### 11. **No Card Details on Hover/Click**
**Problem:** Can't see card details, oracle text, or images anywhere except in hand dialog.

### 12. **No Card Context Menus**
**Problem:** In Cockatrice, right-clicking a card gives options like:
- Move to graveyard
- Move to exile
- Tap/untap
- Add counters
- Shuffle into library
- etc.

---

## 🟢 WORKING FEATURES

✅ Scryfall API integration
✅ Deck loading from file (for local player only)
✅ Starting hand draw (for local player only)
✅ Draw card button
✅ Hand dialog view
✅ Play card to battlefield (but invisible)
✅ Life tracking with +/- buttons
✅ Zone card counts

---

## WHAT'S NEEDED FOR 2+ PLAYER MULTIPLAYER

### Minimum Viable Multiplayer (Priority Order):

1. **P2P Networking with Ktor WebSockets**
   - Server for host
   - Client for joiners
   - Game state synchronization
   - Action broadcasting

2. **Dynamic Player Initialization**
   - Pass connected players from lobby to game
   - Initialize game with N players
   - Load each player's deck

3. **Multi-Player UI Layout**
   - Display all opponents (not just first)
   - Arrange players around virtual table
   - Show each player's zones

4. **Battlefield Visualization**
   - Display all permanents
   - Show tapped/untapped state
   - Show which player controls each permanent

5. **Zone Viewers**
   - Click graveyard to see contents
   - Click exile to see contents
   - View opponent hand sizes

6. **Turn System UI**
   - Show active player
   - Show current phase
   - "Pass Turn" button
   - Turn/phase advancement

7. **Network Deck Loading**
   - Each client loads their deck
   - Broadcast deck loaded state
   - Sync starting hands

---

## RECOMMENDED APPROACH

Since networking is not implemented, there are two options:

### Option A: Local Hotseat Multiplayer (Faster)
- All players share one computer
- Each player takes turns using the mouse
- No networking needed
- Good for testing game logic

### Option B: Network P2P Multiplayer (Proper)
- Implement Ktor WebSocket server/client
- Sync all game actions
- Players on different computers
- Full Cockatrice-like experience

---

## CURRENT REALITY

The app is currently a **deck viewer / solitaire mode**:
- Load a deck ✅
- Draw cards ✅
- View hand ✅
- Track life ✅
- But NO actual multiplayer gameplay ❌

**Estimate:** ~40% complete for Cockatrice-like multiplayer Commander.
