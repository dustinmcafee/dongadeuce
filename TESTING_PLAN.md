# Unit Testing Plan - Dong-A-Deuce

## Current Coverage Summary

| Module | Tested | Total | Coverage |
|--------|--------|-------|----------|
| Player Model | 6 | 8 | 75% |
| GameState Model | 6 | 8 | 75% |
| Deck Model | 2 | 2 | 100% |
| CardInstance Model | 0 | 8 | 0% |
| GameViewModel | 13 | 80+ | ~15% |
| MenuViewModel | 0 | 20+ | 0% |
| DeckParser | 0 | 3 | 0% |
| Network (Server/Client) | 0 | 12+ | 0% |
| Network Messages | 0 | 60+ | 0% |

**Existing Tests:** 44 passing
**Estimated Tests Needed:** 200+

---

## Phase 1: Core Models (Priority: Critical)

### 1.1 CardInstance Tests
**File:** `shared/src/test/kotlin/com/dustinmcafee/dongadeuce/models/CardInstanceTest.kt`

| Test | Description |
|------|-------------|
| `tap sets isTapped to true` | Verify tap() works |
| `untap sets isTapped to false` | Verify untap() works |
| `flip toggles isFlipped` | Verify flip() toggles |
| `moveToZone updates zone` | Verify zone transition |
| `moveToZone resets tap state` | Cards untap when leaving battlefield |
| `addCounter increments counter` | Verify counter addition |
| `addCounter creates new counter type` | Verify new counter creation |
| `removeCounter decrements counter` | Verify counter removal |
| `removeCounter cannot go below zero` | Verify floor at 0 |
| `setGridPosition updates coordinates` | Verify position update |
| `changeController updates controllerId` | Verify control change |
| `createClone creates new instance` | Verify new instanceId |
| `createClone sets isClone flag` | Verify isClone = true |
| `createClone references original` | Verify clonedFromId set |
| `createClone resets state` | Verify untapped, no counters |

### 1.2 Player Counter Tests (Add to PlayerTest.kt)

| Test | Description |
|------|-------------|
| `addCounter adds poison counters` | Basic poison addition |
| `addCounter at 10 poison causes loss` | Poison threshold |
| `addCounter above 10 poison causes loss` | Over threshold |
| `addCounter 9 poison does not lose` | Under threshold |
| `removeCounter removes poison` | Poison removal |
| `removeCounter cannot go below zero` | Floor at 0 |
| `setCounter sets exact value` | Direct setting |
| `setCounter at 10 poison causes loss` | Threshold on set |
| `getCounter returns 0 for missing type` | Default value |
| `energy counters have no threshold` | Energy doesn't cause loss |
| `experience counters have no threshold` | Experience doesn't cause loss |

### 1.3 GameState Additional Tests

| Test | Description |
|------|-------------|
| `getPlayerBattlefield filters by controllerId` | Control vs ownership |
| `addEvent appends to event log` | Event logging |
| `addEvents appends multiple events` | Bulk event logging |

---

## Phase 2: DeckParser (Priority: Critical)

**File:** `shared/src/test/kotlin/com/dustinmcafee/dongadeuce/game/DeckParserTest.kt`

### 2.1 Valid Deck Parsing

| Test | Description |
|------|-------------|
| `parse valid deck with commander and 99 cards` | Happy path |
| `parse deck with basic lands allows duplicates` | Plains, Island, etc. |
| `parse deck with snow-covered lands` | Snow-Covered Plains, etc. |
| `parse deck with Wastes` | Colorless basic |
| `parse handles blank lines` | Ignore empty lines |
| `parse handles comment lines` | Lines starting with // |
| `parse handles category headers` | // Creatures, // Lands |
| `parse handles quantity format "4 Card Name"` | Standard format |
| `parse handles quantity format "4x Card Name"` | Alternative format |

### 2.2 Invalid Deck Parsing

| Test | Description |
|------|-------------|
| `parse fails on empty content` | Empty string |
| `parse fails on missing commander` | No commander section |
| `parse fails on wrong deck size` | Not exactly 99 cards |
| `parse fails on duplicate non-basic` | 2x Sol Ring |
| `parse fails on non-legendary commander` | Lightning Bolt as commander |
| `parse fails on negative quantity` | -1 Card Name |
| `parse fails on zero quantity` | 0 Card Name |
| `parse fails on invalid quantity format` | "abc Card Name" |

### 2.3 File Operations

| Test | Description |
|------|-------------|
| `parseTextFile reads valid file` | File parsing |
| `parseTextFile throws on missing file` | FileNotFoundException |
| `parseTextFile throws on unreadable file` | Permission error |
| `parseTextFile throws on blank path` | Validation |

---

## Phase 3: GameViewModel - Card Movement (Priority: Critical)

**File:** `desktop/src/test/kotlin/com/dustinmcafee/dongadeuce/viewmodel/GameViewModelTest.kt`

### 3.1 Card Movement

| Test | Description |
|------|-------------|
| `moveCard moves card to target zone` | Basic movement |
| `moveCard clears tap state on zone change` | Untap on move |
| `moveCard clears modifiers on zone change` | Reset P/T mods |
| `moveCard preserves ownership` | ownerId unchanged |
| `moveCardToBottomOfLibrary places at index 0` | Bottom = first |
| `moveCardToLibraryPosition inserts correctly` | Position from top |
| `moveCardToLibraryPositionFromBottom inserts correctly` | Position from bottom |
| `moveTopCardsToZone moves N cards` | Batch move |
| `moveBottomCardsToZone moves N cards` | Batch move |
| `moveBottomCardToTop repositions card` | Bottom to top |
| `drawCards draws multiple cards` | Batch draw |

### 3.2 Card State

| Test | Description |
|------|-------------|
| `toggleFaceDown toggles state` | Face down toggle |
| `flip toggles isFlipped` | Flip toggle |
| `toggleDoesntUntap toggles flag` | Stasis effect |
| `setAnnotation sets note` | Custom annotation |
| `setAnnotation clears note with null` | Clear annotation |
| `updateCardGridPosition updates position` | Grid coordinates |
| `playFaceDown moves to battlefield face down` | Morph mechanic |

### 3.3 Card Counters

| Test | Description |
|------|-------------|
| `addCounter adds +1/+1 counter` | Basic counter |
| `addCounter adds multiple at once` | amount > 1 |
| `removeCounter removes counter` | Basic removal |
| `removeCounter cannot go below zero` | Floor at 0 |
| `setCounter sets exact value` | Direct set |
| `setCounter logs event with old and new values` | Event logging |

### 3.4 Power/Toughness

| Test | Description |
|------|-------------|
| `modifyPower changes power modifier` | +X/+0 |
| `modifyToughness changes toughness modifier` | +0/+X |
| `modifyPowerToughness changes both` | +X/+X |
| `setPowerToughness sets exact values` | Direct set |
| `resetPowerToughness clears modifiers` | Reset to base |
| `flowPower adds power removes toughness` | +1/-1 |
| `flowToughness adds toughness removes power` | -1/+1 |

---

## Phase 4: GameViewModel - Player & Combat (Priority: High)

### 4.1 Player Counters

| Test | Description |
|------|-------------|
| `addPlayerCounter adds poison` | Poison addition |
| `addPlayerCounter at 10 poison causes loss` | Threshold |
| `removePlayerCounter removes poison` | Poison removal |
| `setPlayerCounter sets exact poison` | Direct set |
| `addPlayerCounter adds energy` | No threshold |
| `addPlayerCounter adds experience` | No threshold |

### 4.2 Combat & Permanents

| Test | Description |
|------|-------------|
| `untapAll untaps all controlled cards` | Untap mechanic |
| `untapAll respects doesntUntap flag` | Stasis cards |
| `untapAll only affects battlefield` | Zone filtering |
| `concede sets player life to 0` | Concede logic |
| `concede marks player as lost` | Loss state |
| `concede logs PlayerLost event` | Event logging |

### 4.3 Attachments & Control

| Test | Description |
|------|-------------|
| `attachCard sets attachedTo reference` | Aura/Equipment |
| `detachCard clears attachedTo` | Detach |
| `giveControlTo changes controllerId` | Control change |
| `giveControlTo moves to controller battlefield` | Zone handling |
| `giveControlTo logs ControlChanged event` | Event logging |

---

## Phase 5: GameViewModel - Turn & Phase (Priority: High)

### 5.1 Phase Management

| Test | Description |
|------|-------------|
| `nextPhase advances to next phase` | Phase progression |
| `nextPhase wraps from CLEANUP to UNTAP` | Phase wrap |
| `nextPhase increments turn on wrap` | Turn counter |
| `nextPhase auto-untaps in hotseat mode` | Hotseat behavior |
| `setPhase sets specific phase` | Direct set |

### 5.2 Turn Management

| Test | Description |
|------|-------------|
| `passTurn advances to next player` | Player rotation |
| `passTurn sets phase to UNTAP` | Phase reset |
| `passTurn increments turn number` | Turn counter |
| `passTurn rotates local player in hotseat` | Hotseat rotation |
| `passTurn logs TurnPassed event` | Event logging |

---

## Phase 6: GameViewModel - Library Operations (Priority: High)

| Test | Description |
|------|-------------|
| `shuffleTopCards shuffles only top N` | Partial shuffle |
| `shuffleBottomCards shuffles only bottom N` | Partial shuffle |
| `shuffleTopCards logs event` | Event logging |

---

## Phase 7: GameViewModel - Tokens & Clones (Priority: Medium)

| Test | Description |
|------|-------------|
| `createToken creates token on battlefield` | Token creation |
| `createToken creates multiple tokens` | quantity > 1 |
| `createToken logs TokenCreated event` | Event logging |
| `cloneCard creates clone on battlefield` | Clone creation |
| `cloneCard sets isClone flag` | Clone indicator |
| `cloneCard logs CardCloned event` | Event logging |

---

## Phase 8: GameViewModel - Reveal Mechanics (Priority: Medium)

| Test | Description |
|------|-------------|
| `revealHand sets revealed cards state` | Hand reveal |
| `revealHand to all players has empty targetIds` | All players |
| `revealHand to specific player has targetId` | Specific player |
| `revealHand logs GenericAction event` | Event logging |
| `revealCards reveals specific cards` | Card reveal |
| `revealCards with multiple cards` | Multi-card reveal |
| `dismissRevealedCards clears state` | Dismiss dialog |

---

## Phase 9: GameViewModel - Game State Queries (Priority: Low)

| Test | Description |
|------|-------------|
| `getCardCount returns correct count` | Card counting |
| `getCards returns cards in zone` | Zone query |
| `getPlayerBattlefieldCards filters by controller` | Control filtering |
| `getBattlefieldCards returns all battlefield cards` | All players |
| `getAllCommanders returns commanders` | Commander query |

---

## Phase 10: Network Serialization (Priority: Medium)

**File:** `shared/src/test/kotlin/com/dustinmcafee/dongadeuce/network/GameMessageTest.kt`

| Test | Description |
|------|-------------|
| `PlayerJoin serializes correctly` | JSON roundtrip |
| `PlayerJoined serializes correctly` | JSON roundtrip |
| `LobbyState serializes correctly` | JSON roundtrip |
| `GameAction serializes correctly` | JSON roundtrip |
| `StateUpdate serializes correctly` | JSON roundtrip |

**File:** `shared/src/test/kotlin/com/dustinmcafee/dongadeuce/network/NetworkActionTest.kt`

| Test | Description |
|------|-------------|
| `DrawCard serializes correctly` | JSON roundtrip |
| `MoveCard serializes correctly` | JSON roundtrip |
| `TapCard serializes correctly` | JSON roundtrip |
| `AddCounter serializes correctly` | JSON roundtrip |
| ... (all 45+ action types) | JSON roundtrip |

---

## Phase 11: Integration Tests (Priority: Low)

### CardCache Integration
- Mock HTTP responses
- Test bulk download parsing
- Test cache file I/O

### ScryfallApi Integration
- Mock HTTP responses
- Test rate limiting
- Test error handling

### GameServer/GameClient Integration
- Use embedded server
- Test connection lifecycle
- Test action synchronization

---

## Test Infrastructure

### Required Dependencies (add to build.gradle.kts)

```kotlin
testImplementation("io.mockk:mockk:1.13.8")
testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
testImplementation("app.cash.turbine:turbine:1.0.0") // For Flow testing
```

### Test Helpers to Create

```kotlin
// TestHelpers.kt
object TestHelpers {
    fun createTestCard(
        name: String = "Test Card",
        type: String? = "Creature",
        power: String? = "2",
        toughness: String? = "2"
    ): Card

    fun createTestCardInstance(
        card: Card = createTestCard(),
        ownerId: String = "player-1",
        zone: Zone = Zone.BATTLEFIELD
    ): CardInstance

    fun createTestPlayer(
        id: String = UUID.randomUUID().toString(),
        name: String = "Test Player",
        life: Int = 40
    ): Player

    fun createTestGameState(
        players: List<Player> = listOf(createTestPlayer()),
        cardInstances: List<CardInstance> = emptyList()
    ): GameState

    fun createTestDeck(
        commanderName: String = "Test Commander",
        cardCount: Int = 99
    ): Deck
}
```

---

## Execution Order

1. **Week 1:** CardInstance, Player counters, GameState additions
2. **Week 2:** DeckParser (all tests)
3. **Week 3:** GameViewModel card movement and state
4. **Week 4:** GameViewModel counters, P/T, attachments
5. **Week 5:** GameViewModel turn/phase, library ops
6. **Week 6:** GameViewModel tokens, clones, reveals
7. **Week 7:** Network serialization tests
8. **Week 8:** Integration tests, cleanup

---

## Success Criteria

- [ ] All 44 existing tests still pass
- [ ] 200+ new tests added
- [ ] Coverage > 80% for models
- [ ] Coverage > 70% for GameViewModel
- [ ] Coverage > 90% for DeckParser
- [ ] All tests run in < 30 seconds
- [ ] No flaky tests
