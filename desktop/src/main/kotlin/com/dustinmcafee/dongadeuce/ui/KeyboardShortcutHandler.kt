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
 * Keyboard shortcut handler state
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
    var onCloseDialog: () -> Unit = {}
    var onFocusChat: () -> Unit = {}

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

        println("KeyboardShortcut: Received key ${event.key}, ctrl=${event.isCtrlPressed}, alt=${event.isAltPressed}, shift=${event.isShiftPressed}")

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
        val action = shortcutsSettings.getAction(binding)
        if (action == null) {
            println("KeyboardShortcut: No action found for binding $binding")
            return false
        }

        println("KeyboardShortcut: Found action ${action.displayName}")
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

        // Get all selected cards from UI selection state
        val selectedCardIds = selectionState.selectedCards
        val selectedCards = selectedCardIds.mapNotNull { id ->
            gameState.cardInstances.find { it.instanceId == id }
        }
        // First selected card for single-card actions (like dialogs)
        val selectedCard = selectedCards.firstOrNull()

        when (action) {
            // Game Phases
            ShortcutAction.UntapPhase -> viewModel.setPhase(GamePhase.UNTAP)
            ShortcutAction.DrawPhase -> viewModel.setPhase(GamePhase.DRAW)
            ShortcutAction.FirstMainPhase -> viewModel.setPhase(GamePhase.MAIN_1)
            ShortcutAction.CombatPhase -> viewModel.setPhase(GamePhase.COMBAT_BEGIN)
            ShortcutAction.SecondMainPhase -> viewModel.setPhase(GamePhase.MAIN_2)
            ShortcutAction.EndPhase -> viewModel.setPhase(GamePhase.END)
            ShortcutAction.NextPhase -> viewModel.advancePhase()
            ShortcutAction.PassTurn -> viewModel.passTurn()

            // Card Actions (apply to all selected cards)
            ShortcutAction.TapUntapCard -> {
                if (selectedCards.isEmpty()) return false
                selectedCards.forEach { viewModel.toggleTap(it.instanceId) }
            }
            ShortcutAction.UntapAll -> viewModel.untapAll(activePlayer.id)
            ShortcutAction.ToggleDoesntUntap -> {
                if (selectedCards.isEmpty()) return false
                selectedCards.forEach { viewModel.toggleDoesntUntap(it.instanceId) }
            }
            ShortcutAction.FlipCard -> {
                if (selectedCards.isEmpty()) return false
                selectedCards.forEach { viewModel.flipCard(it.instanceId) }
            }
            ShortcutAction.CloneCard -> {
                if (selectedCards.isEmpty()) return false
                selectedCards.forEach { viewModel.cloneCard(it.instanceId, activePlayer.id) }
            }
            ShortcutAction.CreateToken -> onShowTokenDialog()
            ShortcutAction.SetAnnotation -> {
                selectedCard?.let { onShowAnnotationDialog(it) } ?: return false
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

            // Power/Toughness (apply to all selected cards)
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

            // Life & Counters
            ShortcutAction.AddLife -> viewModel.changeLife(activePlayer.id, 1)
            ShortcutAction.RemoveLife -> viewModel.changeLife(activePlayer.id, -1)
            ShortcutAction.SetLife -> onShowSetLifeDialog()
            ShortcutAction.AddCounter -> {
                if (selectedCards.isEmpty()) return false
                selectedCards.forEach { viewModel.addCounter(it.instanceId, "+1/+1") }
            }
            ShortcutAction.RemoveCounter -> {
                if (selectedCards.isEmpty()) return false
                selectedCards.forEach { viewModel.removeCounter(it.instanceId, "+1/+1") }
            }

            // Drawing & Library
            ShortcutAction.DrawCard -> viewModel.drawCard(activePlayer.id)
            ShortcutAction.DrawMultiple -> viewModel.drawCards(activePlayer.id, 7) // Default to 7
            ShortcutAction.UndoDraw -> {
                // Put the last drawn card back on top of library
                val handCards = gameState.cardInstances.filter {
                    it.ownerId == activePlayer.id && it.zone == Zone.HAND
                }
                handCards.lastOrNull()?.let {
                    viewModel.moveCardToLibraryPosition(it.instanceId, 0)
                } ?: return false
            }
            ShortcutAction.Mulligan -> viewModel.mulligan(activePlayer.id)
            ShortcutAction.ShuffleLibrary -> viewModel.shuffleLibrary(activePlayer.id)
            ShortcutAction.PlayTopCard -> {
                val topCard = gameState.cardInstances.find {
                    it.ownerId == activePlayer.id && it.zone == Zone.LIBRARY
                }
                topCard?.let { viewModel.moveCard(it.instanceId, Zone.BATTLEFIELD) } ?: return false
            }
            ShortcutAction.MillTopCard -> viewModel.millCards(activePlayer.id, 1)
            ShortcutAction.MillMultiple -> viewModel.millCards(activePlayer.id, 5) // Default to 5

            // View Zones
            ShortcutAction.ViewLibrary -> onShowLibraryDialog()
            ShortcutAction.ViewGraveyard -> onShowGraveyardDialog()
            ShortcutAction.ViewExile -> onShowExileDialog()
            ShortcutAction.ViewCommandZone -> onShowCommandZoneDialog()
            ShortcutAction.PeekTopCards -> onShowPeekTopDialog()
            ShortcutAction.PeekBottomCards -> onShowPeekBottomDialog()
            ShortcutAction.CloseDialog -> onCloseDialog()

            // Gameplay
            ShortcutAction.RollDice -> onShowDieRollerDialog()
            ShortcutAction.Concede -> viewModel.concede(activePlayer.id)
            ShortcutAction.FocusChat -> onFocusChat()
            ShortcutAction.OpenPlayerCounters -> onShowPlayerCountersDialog()
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
        Key.Equals -> KeyBinding.KEY_EQUALS
        Key.Minus -> KeyBinding.KEY_MINUS
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
 * Uses onPreviewKeyEvent to intercept keys before child components
 */
fun Modifier.keyboardShortcuts(state: KeyboardShortcutState): Modifier {
    return this.onPreviewKeyEvent { event ->
        state.handleKeyEvent(event)
    }
}
