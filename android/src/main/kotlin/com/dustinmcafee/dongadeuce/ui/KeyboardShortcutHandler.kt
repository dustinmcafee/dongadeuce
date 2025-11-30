package com.dustinmcafee.dongadeuce.ui

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.*
import com.dustinmcafee.dongadeuce.models.CardInstance
import com.dustinmcafee.dongadeuce.models.GamePhase
import com.dustinmcafee.dongadeuce.models.Zone
import com.dustinmcafee.dongadeuce.settings.KeyBinding
import com.dustinmcafee.dongadeuce.settings.KeyboardShortcutsSettings
import com.dustinmcafee.dongadeuce.settings.ShortcutAction
import com.dustinmcafee.dongadeuce.viewmodel.GameViewModel

/**
 * Android keyboard shortcut handler state
 * Supports physical keyboards connected to Android devices
 */
class KeyboardShortcutState(
    private val viewModel: GameViewModel,
    private val selectionState: SelectionState,
    private val shortcutsSettings: KeyboardShortcutsSettings = KeyboardShortcutsSettings()
) {
    // Callbacks for UI actions that need dialog handling
    var onShowDieRollerDialog: () -> Unit = {}
    var onShowSetLifeDialog: () -> Unit = {}
    var onShowLibraryDialog: () -> Unit = {}
    var onShowGraveyardDialog: () -> Unit = {}
    var onShowExileDialog: () -> Unit = {}
    var onShowCommandZoneDialog: () -> Unit = {}
    var onShowPeekTopDialog: () -> Unit = {}
    var onShowPeekBottomDialog: () -> Unit = {}
    var onShowPowerToughnessDialog: (CardInstance) -> Unit = {}
    var onShowAnnotationDialog: (CardInstance) -> Unit = {}
    var onShowTokenDialog: () -> Unit = {}
    var onShowPlayerCountersDialog: () -> Unit = {}
    var onShowSideboardDialog: () -> Unit = {}
    var onCloseDialog: () -> Unit = {}
    var onFocusChat: () -> Unit = {}
    var onLeaveGame: () -> Unit = {}
    var onStartDrawArrow: () -> Unit = {}
    var onCreateAnotherToken: () -> Unit = {}
    var onCreateRelatedTokens: () -> Unit = {}

    // Callbacks for number input operations
    var onShowNumberInputDialog: (String, Int, (Int) -> Unit) -> Unit = { _, _, _ -> }

    // Callback for stack until found dialog
    var onShowStackUntilFoundDialog: () -> Unit = {}

    // Track if a dialog is open (shortcuts disabled except Esc)
    var isDialogOpen: Boolean = false

    // Track if text input is focused (shortcuts disabled)
    var isTextInputFocused: Boolean = false

    /**
     * Handle a key event and return true if it was consumed
     */
    fun handleKeyEvent(event: KeyEvent): Boolean {
        // Only handle key down events
        if (event.type != KeyEventType.KeyDown) return false

        // If text input is focused, only handle Esc
        if (isTextInputFocused) {
            if (event.key == Key.Escape) {
                onCloseDialog()
                return true
            }
            return false
        }

        // If dialog is open, only handle Esc to close
        if (isDialogOpen) {
            if (event.key == Key.Escape) {
                onCloseDialog()
                return true
            }
            return false
        }

        // Convert Compose Key to our key code
        val keyCode = composeKeyToKeyCode(event.key)
        if (keyCode == -1) return false

        // Create binding from event
        val binding = KeyBinding(
            keyCode = keyCode,
            ctrl = event.isCtrlPressed,
            alt = event.isAltPressed,
            shift = event.isShiftPressed
        )

        // Look up action
        val action = shortcutsSettings.getAction(binding) ?: return false

        // Execute action
        return executeAction(action)
    }

    /**
     * Execute a shortcut action
     */
    private fun executeAction(action: ShortcutAction): Boolean {
        val uiState = viewModel.uiState.value
        val gameState = uiState.gameState ?: return false
        val activePlayer = gameState.players.getOrNull(gameState.activePlayerIndex) ?: return false
        val localPlayer = uiState.localPlayer ?: activePlayer

        val selectedCardIds = selectionState.selectedCards
        val selectedCards = selectedCardIds.mapNotNull { id ->
            gameState.cardInstances.find { it.instanceId == id }
        }
        val selectedCard = selectedCards.firstOrNull()

        when (action) {
            // Game Phases
            ShortcutAction.UntapPhase -> viewModel.setPhase(GamePhase.UNTAP)
            ShortcutAction.DrawPhase -> viewModel.setPhase(GamePhase.DRAW)
            ShortcutAction.FirstMainPhase -> viewModel.setPhase(GamePhase.MAIN_1)
            ShortcutAction.CombatPhase -> viewModel.setPhase(GamePhase.COMBAT_BEGIN)
            ShortcutAction.AttackPhase -> viewModel.setPhase(GamePhase.COMBAT_DECLARE_ATTACKERS)
            ShortcutAction.BlockPhase -> viewModel.setPhase(GamePhase.COMBAT_DECLARE_BLOCKERS)
            ShortcutAction.DamagePhase -> viewModel.setPhase(GamePhase.COMBAT_DAMAGE)
            ShortcutAction.EndCombatPhase -> viewModel.setPhase(GamePhase.COMBAT_END)
            ShortcutAction.SecondMainPhase -> viewModel.setPhase(GamePhase.MAIN_2)
            ShortcutAction.EndPhase -> viewModel.setPhase(GamePhase.END)
            ShortcutAction.NextPhase -> viewModel.advancePhase()
            ShortcutAction.PassTurn -> viewModel.passTurn()
            ShortcutAction.UpkeepPhase -> viewModel.setPhase(GamePhase.UPKEEP)

            // Card Actions
            ShortcutAction.TapUntapCard -> {
                if (selectedCards.isEmpty()) return false
                selectedCards.forEach { viewModel.toggleTap(it.instanceId) }
            }
            ShortcutAction.UntapAll -> viewModel.untapAll(localPlayer.id)
            ShortcutAction.ToggleDoesntUntap -> {
                if (selectedCards.isEmpty()) return false
                selectedCards.forEach { viewModel.toggleDoesntUntap(it.instanceId) }
            }
            ShortcutAction.FlipCard -> {
                if (selectedCards.isEmpty()) return false
                selectedCards.forEach { viewModel.flipCard(it.instanceId) }
            }
            ShortcutAction.PlayFaceDown -> {
                if (selectedCards.isEmpty()) return false
                selectedCards.forEach { viewModel.playFaceDown(it.instanceId) }
            }
            ShortcutAction.CloneCard -> {
                if (selectedCards.isEmpty()) return false
                selectedCards.forEach { viewModel.cloneCard(it.instanceId, localPlayer.id) }
            }
            ShortcutAction.CreateToken -> onShowTokenDialog()
            ShortcutAction.SetAnnotation -> {
                selectedCard?.let { onShowAnnotationDialog(it) } ?: return false
            }
            ShortcutAction.AttachCard -> return false
            ShortcutAction.DetachCard -> {
                if (selectedCards.isEmpty()) return false
                selectedCards.forEach { viewModel.detachCard(it.instanceId) }
            }
            ShortcutAction.MoveToGraveyard -> {
                if (selectedCards.isEmpty()) return false
                selectedCards.forEach { viewModel.moveCard(it.instanceId, Zone.GRAVEYARD) }
            }
            ShortcutAction.MoveToBottomLibrary -> {
                if (selectedCards.isEmpty()) return false
                selectedCards.forEach { viewModel.moveCardToBottomOfLibrary(it.instanceId) }
            }
            ShortcutAction.MoveToExile -> {
                if (selectedCards.isEmpty()) return false
                selectedCards.forEach { viewModel.moveCard(it.instanceId, Zone.EXILE) }
            }
            ShortcutAction.MoveToHand -> {
                if (selectedCards.isEmpty()) return false
                selectedCards.forEach { viewModel.moveCard(it.instanceId, Zone.HAND) }
            }
            ShortcutAction.MoveTopToBottom -> {
                val topCard = gameState.cardInstances.find {
                    it.ownerId == localPlayer.id && it.zone == Zone.LIBRARY
                }
                topCard?.let { viewModel.moveCardToBottomOfLibrary(it.instanceId) } ?: return false
            }

            // Power/Toughness
            ShortcutAction.AddPower -> {
                if (selectedCards.isEmpty()) return false
                selectedCards.forEach { viewModel.modifyPower(it.instanceId, 1) }
            }
            ShortcutAction.RemovePower -> {
                if (selectedCards.isEmpty()) return false
                selectedCards.forEach { viewModel.modifyPower(it.instanceId, -1) }
            }
            ShortcutAction.AddToughness -> {
                if (selectedCards.isEmpty()) return false
                selectedCards.forEach { viewModel.modifyToughness(it.instanceId, 1) }
            }
            ShortcutAction.RemoveToughness -> {
                if (selectedCards.isEmpty()) return false
                selectedCards.forEach { viewModel.modifyToughness(it.instanceId, -1) }
            }
            ShortcutAction.AddBothPT -> {
                if (selectedCards.isEmpty()) return false
                selectedCards.forEach {
                    viewModel.modifyPower(it.instanceId, 1)
                    viewModel.modifyToughness(it.instanceId, 1)
                }
            }
            ShortcutAction.RemoveBothPT -> {
                if (selectedCards.isEmpty()) return false
                selectedCards.forEach {
                    viewModel.modifyPower(it.instanceId, -1)
                    viewModel.modifyToughness(it.instanceId, -1)
                }
            }
            ShortcutAction.SetPowerToughness -> {
                selectedCard?.let { onShowPowerToughnessDialog(it) } ?: return false
            }
            ShortcutAction.ResetPowerToughness -> {
                if (selectedCards.isEmpty()) return false
                selectedCards.forEach { viewModel.resetPowerToughness(it.instanceId) }
            }
            ShortcutAction.FlowPower -> {
                if (selectedCards.isEmpty()) return false
                selectedCards.forEach { viewModel.flowPower(it.instanceId) }
            }
            ShortcutAction.FlowToughness -> {
                if (selectedCards.isEmpty()) return false
                selectedCards.forEach { viewModel.flowToughness(it.instanceId) }
            }

            // Life & Counters
            ShortcutAction.AddLife -> viewModel.changeLife(localPlayer.id, 1)
            ShortcutAction.RemoveLife -> viewModel.changeLife(localPlayer.id, -1)
            ShortcutAction.SetLife -> onShowSetLifeDialog()
            ShortcutAction.AddCounter -> {
                if (selectedCards.isEmpty()) return false
                selectedCards.forEach { viewModel.addCounter(it.instanceId, "+1/+1") }
            }
            ShortcutAction.RemoveCounter -> {
                if (selectedCards.isEmpty()) return false
                selectedCards.forEach { viewModel.removeCounter(it.instanceId, "+1/+1") }
            }

            // Card Counters (A-F)
            ShortcutAction.AddCounterA -> {
                if (selectedCards.isEmpty()) return false
                selectedCards.forEach { viewModel.addCounter(it.instanceId, "A") }
            }
            ShortcutAction.RemoveCounterA -> {
                if (selectedCards.isEmpty()) return false
                selectedCards.forEach { viewModel.removeCounter(it.instanceId, "A") }
            }
            ShortcutAction.SetCounterA -> {
                val card = selectedCard ?: return false
                val currentCount = card.counters["A"] ?: 0
                onShowNumberInputDialog("Set Counter A", currentCount) { count ->
                    viewModel.setCounter(card.instanceId, "A", count)
                }
            }
            ShortcutAction.AddCounterB -> {
                if (selectedCards.isEmpty()) return false
                selectedCards.forEach { viewModel.addCounter(it.instanceId, "B") }
            }
            ShortcutAction.RemoveCounterB -> {
                if (selectedCards.isEmpty()) return false
                selectedCards.forEach { viewModel.removeCounter(it.instanceId, "B") }
            }
            ShortcutAction.SetCounterB -> {
                val card = selectedCard ?: return false
                val currentCount = card.counters["B"] ?: 0
                onShowNumberInputDialog("Set Counter B", currentCount) { count ->
                    viewModel.setCounter(card.instanceId, "B", count)
                }
            }
            ShortcutAction.AddCounterC -> {
                if (selectedCards.isEmpty()) return false
                selectedCards.forEach { viewModel.addCounter(it.instanceId, "C") }
            }
            ShortcutAction.RemoveCounterC -> {
                if (selectedCards.isEmpty()) return false
                selectedCards.forEach { viewModel.removeCounter(it.instanceId, "C") }
            }
            ShortcutAction.SetCounterC -> {
                val card = selectedCard ?: return false
                val currentCount = card.counters["C"] ?: 0
                onShowNumberInputDialog("Set Counter C", currentCount) { count ->
                    viewModel.setCounter(card.instanceId, "C", count)
                }
            }
            ShortcutAction.AddCounterD -> {
                if (selectedCards.isEmpty()) return false
                selectedCards.forEach { viewModel.addCounter(it.instanceId, "D") }
            }
            ShortcutAction.RemoveCounterD -> {
                if (selectedCards.isEmpty()) return false
                selectedCards.forEach { viewModel.removeCounter(it.instanceId, "D") }
            }
            ShortcutAction.SetCounterD -> {
                val card = selectedCard ?: return false
                val currentCount = card.counters["D"] ?: 0
                onShowNumberInputDialog("Set Counter D", currentCount) { count ->
                    viewModel.setCounter(card.instanceId, "D", count)
                }
            }
            ShortcutAction.AddCounterE -> {
                if (selectedCards.isEmpty()) return false
                selectedCards.forEach { viewModel.addCounter(it.instanceId, "E") }
            }
            ShortcutAction.RemoveCounterE -> {
                if (selectedCards.isEmpty()) return false
                selectedCards.forEach { viewModel.removeCounter(it.instanceId, "E") }
            }
            ShortcutAction.SetCounterE -> {
                val card = selectedCard ?: return false
                val currentCount = card.counters["E"] ?: 0
                onShowNumberInputDialog("Set Counter E", currentCount) { count ->
                    viewModel.setCounter(card.instanceId, "E", count)
                }
            }
            ShortcutAction.AddCounterF -> {
                if (selectedCards.isEmpty()) return false
                selectedCards.forEach { viewModel.addCounter(it.instanceId, "F") }
            }
            ShortcutAction.RemoveCounterF -> {
                if (selectedCards.isEmpty()) return false
                selectedCards.forEach { viewModel.removeCounter(it.instanceId, "F") }
            }
            ShortcutAction.SetCounterF -> {
                val card = selectedCard ?: return false
                val currentCount = card.counters["F"] ?: 0
                onShowNumberInputDialog("Set Counter F", currentCount) { count ->
                    viewModel.setCounter(card.instanceId, "F", count)
                }
            }
            ShortcutAction.IncrementAllCounters -> {
                if (selectedCards.isEmpty()) return false
                selectedCards.forEach { viewModel.incrementAllCounters(it.instanceId) }
            }

            // Mana Counters
            ShortcutAction.AddWhiteMana -> viewModel.addPlayerCounter(localPlayer.id, "white", 1)
            ShortcutAction.RemoveWhiteMana -> viewModel.removePlayerCounter(localPlayer.id, "white", 1)
            ShortcutAction.SetWhiteMana -> {
                val currentCount = localPlayer.getCounter("white")
                onShowNumberInputDialog("Set White Mana", currentCount) { count ->
                    viewModel.setPlayerCounter(localPlayer.id, "white", count)
                }
            }
            ShortcutAction.AddBlueMana -> viewModel.addPlayerCounter(localPlayer.id, "blue", 1)
            ShortcutAction.RemoveBlueMana -> viewModel.removePlayerCounter(localPlayer.id, "blue", 1)
            ShortcutAction.SetBlueMana -> {
                val currentCount = localPlayer.getCounter("blue")
                onShowNumberInputDialog("Set Blue Mana", currentCount) { count ->
                    viewModel.setPlayerCounter(localPlayer.id, "blue", count)
                }
            }
            ShortcutAction.AddBlackMana -> viewModel.addPlayerCounter(localPlayer.id, "black", 1)
            ShortcutAction.RemoveBlackMana -> viewModel.removePlayerCounter(localPlayer.id, "black", 1)
            ShortcutAction.SetBlackMana -> {
                val currentCount = localPlayer.getCounter("black")
                onShowNumberInputDialog("Set Black Mana", currentCount) { count ->
                    viewModel.setPlayerCounter(localPlayer.id, "black", count)
                }
            }
            ShortcutAction.AddRedMana -> viewModel.addPlayerCounter(localPlayer.id, "red", 1)
            ShortcutAction.RemoveRedMana -> viewModel.removePlayerCounter(localPlayer.id, "red", 1)
            ShortcutAction.SetRedMana -> {
                val currentCount = localPlayer.getCounter("red")
                onShowNumberInputDialog("Set Red Mana", currentCount) { count ->
                    viewModel.setPlayerCounter(localPlayer.id, "red", count)
                }
            }
            ShortcutAction.AddGreenMana -> viewModel.addPlayerCounter(localPlayer.id, "green", 1)
            ShortcutAction.RemoveGreenMana -> viewModel.removePlayerCounter(localPlayer.id, "green", 1)
            ShortcutAction.SetGreenMana -> {
                val currentCount = localPlayer.getCounter("green")
                onShowNumberInputDialog("Set Green Mana", currentCount) { count ->
                    viewModel.setPlayerCounter(localPlayer.id, "green", count)
                }
            }
            ShortcutAction.AddColorlessMana -> viewModel.addPlayerCounter(localPlayer.id, "colorless", 1)
            ShortcutAction.RemoveColorlessMana -> viewModel.removePlayerCounter(localPlayer.id, "colorless", 1)
            ShortcutAction.SetColorlessMana -> {
                val currentCount = localPlayer.getCounter("colorless")
                onShowNumberInputDialog("Set Colorless Mana", currentCount) { count ->
                    viewModel.setPlayerCounter(localPlayer.id, "colorless", count)
                }
            }

            // Drawing & Library
            ShortcutAction.DrawCard -> viewModel.drawCard(localPlayer.id)
            ShortcutAction.DrawMultiple -> {
                onShowNumberInputDialog("Draw Cards", 7) { count ->
                    viewModel.drawCards(localPlayer.id, count)
                }
            }
            ShortcutAction.UndoDraw -> {
                val handCards = gameState.cardInstances.filter {
                    it.ownerId == localPlayer.id && it.zone == Zone.HAND
                }
                handCards.lastOrNull()?.let {
                    viewModel.moveCardToLibraryPosition(it.instanceId, 0)
                } ?: return false
            }
            ShortcutAction.Mulligan -> viewModel.mulligan(localPlayer.id)
            ShortcutAction.ShuffleLibrary -> viewModel.shuffleLibrary(localPlayer.id)
            ShortcutAction.ShuffleTopCards -> {
                onShowNumberInputDialog("Shuffle Top Cards", 5) { count ->
                    viewModel.shuffleTopCards(localPlayer.id, count)
                }
            }
            ShortcutAction.ShuffleBottomCards -> {
                onShowNumberInputDialog("Shuffle Bottom Cards", 5) { count ->
                    viewModel.shuffleBottomCards(localPlayer.id, count)
                }
            }
            ShortcutAction.PlayTopCard -> {
                val topCard = gameState.cardInstances.find {
                    it.ownerId == localPlayer.id && it.zone == Zone.LIBRARY
                }
                topCard?.let { viewModel.moveCard(it.instanceId, Zone.BATTLEFIELD) } ?: return false
            }
            ShortcutAction.MillTopCard -> viewModel.millCards(localPlayer.id, 1)
            ShortcutAction.MillMultiple -> {
                onShowNumberInputDialog("Mill Cards", 5) { count ->
                    viewModel.millCards(localPlayer.id, count)
                }
            }
            ShortcutAction.AlwaysRevealTopCard -> viewModel.toggleRevealTopCard(localPlayer.id)
            ShortcutAction.AlwaysLookAtTopCard -> viewModel.toggleLookAtTopCard(localPlayer.id)
            ShortcutAction.StackUntilFound -> onShowStackUntilFoundDialog()

            // Bottom Card Operations
            ShortcutAction.DrawBottomCard -> viewModel.drawFromBottom(localPlayer.id, 1)
            ShortcutAction.DrawBottomMultiple -> {
                onShowNumberInputDialog("Draw from Bottom", 7) { count ->
                    viewModel.drawFromBottom(localPlayer.id, count)
                }
            }
            ShortcutAction.MillBottomCard -> viewModel.millFromBottom(localPlayer.id, 1)
            ShortcutAction.MillBottomMultiple -> {
                onShowNumberInputDialog("Mill from Bottom", 5) { count ->
                    viewModel.millFromBottom(localPlayer.id, count)
                }
            }
            ShortcutAction.ExileBottomCard -> viewModel.exileFromBottom(localPlayer.id, 1)
            ShortcutAction.ExileBottomMultiple -> {
                onShowNumberInputDialog("Exile from Bottom", 5) { count ->
                    viewModel.exileFromBottom(localPlayer.id, count)
                }
            }
            ShortcutAction.BottomToTop -> viewModel.moveBottomCardToTop(localPlayer.id)

            // View Zones
            ShortcutAction.ViewLibrary -> onShowLibraryDialog()
            ShortcutAction.ViewGraveyard -> onShowGraveyardDialog()
            ShortcutAction.ViewExile -> onShowExileDialog()
            ShortcutAction.ViewCommandZone -> onShowCommandZoneDialog()
            ShortcutAction.ViewSideboard -> onShowSideboardDialog()
            ShortcutAction.PeekTopCards -> onShowPeekTopDialog()
            ShortcutAction.PeekBottomCards -> onShowPeekBottomDialog()
            ShortcutAction.CloseDialog -> onCloseDialog()

            // Selection
            ShortcutAction.SelectAll -> {
                val firstCard = selectedCard
                if (firstCard != null) {
                    // For battlefield, use controllerId; for other zones, use ownerId
                    val zoneCards = gameState.cardInstances.filter {
                        val matchesPlayer = if (firstCard.zone == Zone.BATTLEFIELD) {
                            it.controllerId == localPlayer.id
                        } else {
                            it.ownerId == localPlayer.id
                        }
                        matchesPlayer && it.zone == firstCard.zone
                    }
                    selectionState.selectAll(zoneCards.map { it.instanceId })
                } else {
                    // Default to battlefield - use controllerId
                    val battlefieldCards = gameState.cardInstances.filter {
                        it.controllerId == localPlayer.id && it.zone == Zone.BATTLEFIELD
                    }
                    selectionState.selectAll(battlefieldCards.map { it.instanceId })
                }
            }
            ShortcutAction.SelectRow -> {
                val firstCard = selectedCard
                if (firstCard != null && firstCard.zone == Zone.BATTLEFIELD && firstCard.gridY != null) {
                    // Battlefield uses controllerId
                    val rowCards = gameState.cardInstances.filter {
                        it.controllerId == localPlayer.id &&
                        it.zone == Zone.BATTLEFIELD &&
                        it.gridY == firstCard.gridY
                    }
                    selectionState.selectAll(rowCards.map { it.instanceId })
                } else return false
            }
            ShortcutAction.SelectColumn -> {
                val firstCard = selectedCard
                if (firstCard != null && firstCard.zone == Zone.BATTLEFIELD && firstCard.gridX != null) {
                    // Battlefield uses controllerId
                    val columnCards = gameState.cardInstances.filter {
                        it.controllerId == localPlayer.id &&
                        it.zone == Zone.BATTLEFIELD &&
                        it.gridX == firstCard.gridX
                    }
                    selectionState.selectAll(columnCards.map { it.instanceId })
                } else return false
            }

            // Arrows
            ShortcutAction.DrawArrow -> onStartDrawArrow()
            ShortcutAction.RemoveArrows -> viewModel.removeLocalArrows(localPlayer.id)

            // Gameplay
            ShortcutAction.RollDice -> onShowDieRollerDialog()
            ShortcutAction.Concede -> viewModel.concede(localPlayer.id)
            ShortcutAction.LeaveGame -> onLeaveGame()
            ShortcutAction.FocusChat -> onFocusChat()
            ShortcutAction.OpenPlayerCounters -> onShowPlayerCountersDialog()
            ShortcutAction.SortHand -> viewModel.sortHand(localPlayer.id)
            ShortcutAction.CreateAnotherToken -> onCreateAnotherToken()
            ShortcutAction.CreateRelatedTokens -> onCreateRelatedTokens()
        }

        return true
    }

    /**
     * Convert Compose Key to our key code
     */
    private fun composeKeyToKeyCode(key: Key): Int = when (key) {
        Key.F1 -> KeyBinding.KEY_F1
        Key.F2 -> KeyBinding.KEY_F2
        Key.F3 -> KeyBinding.KEY_F3
        Key.F4 -> KeyBinding.KEY_F4
        Key.F5 -> KeyBinding.KEY_F5
        Key.F6 -> KeyBinding.KEY_F6
        Key.F7 -> KeyBinding.KEY_F7
        Key.F8 -> KeyBinding.KEY_F8
        Key.F9 -> KeyBinding.KEY_F9
        Key.F10 -> KeyBinding.KEY_F10
        Key.F11 -> KeyBinding.KEY_F11
        Key.F12 -> KeyBinding.KEY_F12
        Key.Escape -> KeyBinding.KEY_ESCAPE
        Key.Enter -> KeyBinding.KEY_ENTER
        Key.Spacebar -> KeyBinding.KEY_SPACE
        Key.Tab -> KeyBinding.KEY_TAB
        Key.Delete -> KeyBinding.KEY_DELETE
        Key.Backspace -> KeyBinding.KEY_BACKSPACE
        Key.A -> KeyBinding.KEY_A
        Key.B -> KeyBinding.KEY_B
        Key.C -> KeyBinding.KEY_C
        Key.D -> KeyBinding.KEY_D
        Key.E -> KeyBinding.KEY_E
        Key.F -> KeyBinding.KEY_F
        Key.G -> KeyBinding.KEY_G
        Key.H -> KeyBinding.KEY_H
        Key.I -> KeyBinding.KEY_I
        Key.J -> KeyBinding.KEY_J
        Key.K -> KeyBinding.KEY_K
        Key.L -> KeyBinding.KEY_L
        Key.M -> KeyBinding.KEY_M
        Key.N -> KeyBinding.KEY_N
        Key.O -> KeyBinding.KEY_O
        Key.P -> KeyBinding.KEY_P
        Key.Q -> KeyBinding.KEY_Q
        Key.R -> KeyBinding.KEY_R
        Key.S -> KeyBinding.KEY_S
        Key.T -> KeyBinding.KEY_T
        Key.U -> KeyBinding.KEY_U
        Key.V -> KeyBinding.KEY_V
        Key.W -> KeyBinding.KEY_W
        Key.X -> KeyBinding.KEY_X
        Key.Y -> KeyBinding.KEY_Y
        Key.Z -> KeyBinding.KEY_Z
        Key.Zero -> KeyBinding.KEY_0
        Key.One -> KeyBinding.KEY_1
        Key.Two -> KeyBinding.KEY_2
        Key.Three -> KeyBinding.KEY_3
        Key.Four -> KeyBinding.KEY_4
        Key.Five -> KeyBinding.KEY_5
        Key.Six -> KeyBinding.KEY_6
        Key.Seven -> KeyBinding.KEY_7
        Key.Eight -> KeyBinding.KEY_8
        Key.Nine -> KeyBinding.KEY_9
        Key.Equals -> KeyBinding.KEY_EQUALS
        Key.Minus -> KeyBinding.KEY_MINUS
        Key.Period -> KeyBinding.KEY_PERIOD
        Key.Comma -> KeyBinding.KEY_COMMA
        Key.Slash -> KeyBinding.KEY_SLASH
        Key.Semicolon -> KeyBinding.KEY_SEMICOLON
        Key.LeftBracket -> KeyBinding.KEY_BRACKET_LEFT
        Key.RightBracket -> KeyBinding.KEY_BRACKET_RIGHT
        Key.Backslash -> KeyBinding.KEY_BACKSLASH
        Key.NumPadAdd -> KeyBinding.KEY_NUMPAD_ADD
        Key.NumPadSubtract -> KeyBinding.KEY_NUMPAD_SUBTRACT
        else -> -1
    }
}

/**
 * Remember keyboard shortcut state
 */
@Composable
fun rememberKeyboardShortcutState(viewModel: GameViewModel, selectionState: SelectionState): KeyboardShortcutState {
    return remember(viewModel, selectionState) { KeyboardShortcutState(viewModel, selectionState) }
}

/**
 * Modifier to handle keyboard shortcuts
 */
fun Modifier.keyboardShortcuts(state: KeyboardShortcutState): Modifier {
    return this.onPreviewKeyEvent { event ->
        state.handleKeyEvent(event)
    }
}
