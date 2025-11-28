package com.dustinmcafee.dongadeuce.settings

import kotlinx.serialization.Serializable

/**
 * Represents a keyboard shortcut action
 */
sealed class ShortcutAction(val id: String, val displayName: String, val category: String) {
    // Game Phases
    object UntapPhase : ShortcutAction("untapPhase", "Untap Phase", "Game Phases")
    object UpkeepPhase : ShortcutAction("upkeepPhase", "Upkeep Phase", "Game Phases")
    object DrawPhase : ShortcutAction("drawPhase", "Draw Phase", "Game Phases")
    object FirstMainPhase : ShortcutAction("firstMainPhase", "First Main Phase", "Game Phases")
    object CombatPhase : ShortcutAction("combatPhase", "Begin Combat", "Game Phases")
    object AttackPhase : ShortcutAction("attackPhase", "Declare Attackers", "Game Phases")
    object BlockPhase : ShortcutAction("blockPhase", "Declare Blockers", "Game Phases")
    object DamagePhase : ShortcutAction("damagePhase", "Combat Damage", "Game Phases")
    object EndCombatPhase : ShortcutAction("endCombatPhase", "End Combat", "Game Phases")
    object SecondMainPhase : ShortcutAction("secondMainPhase", "Second Main Phase", "Game Phases")
    object EndPhase : ShortcutAction("endPhase", "End Phase", "Game Phases")
    object NextPhase : ShortcutAction("nextPhase", "Next Phase", "Game Phases")
    object PassTurn : ShortcutAction("passTurn", "Pass Turn", "Game Phases")

    // Card Actions
    object TapUntapCard : ShortcutAction("tapUntapCard", "Tap/Untap Card", "Card Actions")
    object UntapAll : ShortcutAction("untapAll", "Untap All", "Card Actions")
    object ToggleDoesntUntap : ShortcutAction("toggleDoesntUntap", "Toggle Doesn't Untap", "Card Actions")
    object FlipCard : ShortcutAction("flipCard", "Flip Card", "Card Actions")
    object PlayFaceDown : ShortcutAction("playFaceDown", "Play Face Down", "Card Actions")
    object CloneCard : ShortcutAction("cloneCard", "Clone Card", "Card Actions")
    object CreateToken : ShortcutAction("createToken", "Create Token", "Card Actions")
    object SetAnnotation : ShortcutAction("setAnnotation", "Set Annotation", "Card Actions")
    object AttachCard : ShortcutAction("attachCard", "Attach Card", "Card Actions")
    object DetachCard : ShortcutAction("detachCard", "Detach Card", "Card Actions")
    object MoveToGraveyard : ShortcutAction("moveToGraveyard", "Move to Graveyard", "Card Actions")
    object MoveToBottomLibrary : ShortcutAction("moveToBottomLibrary", "Move to Bottom of Library", "Card Actions")
    object MoveToExile : ShortcutAction("moveToExile", "Move to Exile", "Card Actions")
    object MoveToHand : ShortcutAction("moveToHand", "Move to Hand", "Card Actions")
    object MoveTopToBottom : ShortcutAction("moveTopToBottom", "Move Top Card to Bottom", "Card Actions")

    // Power/Toughness
    object AddPower : ShortcutAction("addPower", "Add Power (+1/+0)", "Power/Toughness")
    object RemovePower : ShortcutAction("removePower", "Remove Power (-1/-0)", "Power/Toughness")
    object AddToughness : ShortcutAction("addToughness", "Add Toughness (+0/+1)", "Power/Toughness")
    object RemoveToughness : ShortcutAction("removeToughness", "Remove Toughness (-0/-1)", "Power/Toughness")
    object AddBothPT : ShortcutAction("addBothPT", "Add Both (+1/+1)", "Power/Toughness")
    object RemoveBothPT : ShortcutAction("removeBothPT", "Remove Both (-1/-1)", "Power/Toughness")
    object SetPowerToughness : ShortcutAction("setPowerToughness", "Set Power/Toughness", "Power/Toughness")
    object ResetPowerToughness : ShortcutAction("resetPowerToughness", "Reset Power/Toughness", "Power/Toughness")
    object FlowPower : ShortcutAction("flowPower", "Flow to Power (+1/-1)", "Power/Toughness")
    object FlowToughness : ShortcutAction("flowToughness", "Flow to Toughness (-1/+1)", "Power/Toughness")

    // Life & Counters
    object AddLife : ShortcutAction("addLife", "Add Life (+1)", "Life & Counters")
    object RemoveLife : ShortcutAction("removeLife", "Remove Life (-1)", "Life & Counters")
    object SetLife : ShortcutAction("setLife", "Set Life Total", "Life & Counters")
    object AddCounter : ShortcutAction("addCounter", "Add Counter (+1/+1)", "Life & Counters")
    object RemoveCounter : ShortcutAction("removeCounter", "Remove Counter (+1/+1)", "Life & Counters")

    // Card Counters (A-F colored counters)
    object AddCounterA : ShortcutAction("addCounterA", "Add Counter (A/Red)", "Card Counters")
    object RemoveCounterA : ShortcutAction("removeCounterA", "Remove Counter (A/Red)", "Card Counters")
    object AddCounterB : ShortcutAction("addCounterB", "Add Counter (B/Yellow)", "Card Counters")
    object RemoveCounterB : ShortcutAction("removeCounterB", "Remove Counter (B/Yellow)", "Card Counters")
    object AddCounterC : ShortcutAction("addCounterC", "Add Counter (C/Green)", "Card Counters")
    object RemoveCounterC : ShortcutAction("removeCounterC", "Remove Counter (C/Green)", "Card Counters")
    object AddCounterD : ShortcutAction("addCounterD", "Add Counter (D/Cyan)", "Card Counters")
    object RemoveCounterD : ShortcutAction("removeCounterD", "Remove Counter (D/Cyan)", "Card Counters")
    object AddCounterE : ShortcutAction("addCounterE", "Add Counter (E/Purple)", "Card Counters")
    object RemoveCounterE : ShortcutAction("removeCounterE", "Remove Counter (E/Purple)", "Card Counters")
    object AddCounterF : ShortcutAction("addCounterF", "Add Counter (F/Magenta)", "Card Counters")
    object RemoveCounterF : ShortcutAction("removeCounterF", "Remove Counter (F/Magenta)", "Card Counters")
    object IncrementAllCounters : ShortcutAction("incrementAllCounters", "Increment All Card Counters", "Card Counters")

    // Drawing & Library
    object DrawCard : ShortcutAction("drawCard", "Draw Card", "Drawing & Library")
    object DrawMultiple : ShortcutAction("drawMultiple", "Draw Multiple Cards", "Drawing & Library")
    object UndoDraw : ShortcutAction("undoDraw", "Undo Draw", "Drawing & Library")
    object Mulligan : ShortcutAction("mulligan", "Mulligan", "Drawing & Library")
    object ShuffleLibrary : ShortcutAction("shuffleLibrary", "Shuffle Library", "Drawing & Library")
    object ShuffleTopCards : ShortcutAction("shuffleTopCards", "Shuffle Top Cards of Library", "Drawing & Library")
    object ShuffleBottomCards : ShortcutAction("shuffleBottomCards", "Shuffle Bottom Cards of Library", "Drawing & Library")
    object PlayTopCard : ShortcutAction("playTopCard", "Play Top Card", "Drawing & Library")
    object MillTopCard : ShortcutAction("millTopCard", "Mill Top Card", "Drawing & Library")
    object MillMultiple : ShortcutAction("millMultiple", "Mill Multiple Cards", "Drawing & Library")
    object AlwaysRevealTopCard : ShortcutAction("alwaysRevealTopCard", "Always Reveal Top Card", "Drawing & Library")
    object AlwaysLookAtTopCard : ShortcutAction("alwaysLookAtTopCard", "Always Look At Top Card", "Drawing & Library")

    // Bottom Card Operations
    object DrawBottomCard : ShortcutAction("drawBottomCard", "Draw Bottom Card", "Bottom Card Operations")
    object DrawBottomMultiple : ShortcutAction("drawBottomMultiple", "Draw Multiple from Bottom", "Bottom Card Operations")
    object MillBottomCard : ShortcutAction("millBottomCard", "Mill Bottom Card", "Bottom Card Operations")
    object MillBottomMultiple : ShortcutAction("millBottomMultiple", "Mill Multiple from Bottom", "Bottom Card Operations")
    object ExileBottomCard : ShortcutAction("exileBottomCard", "Exile Bottom Card", "Bottom Card Operations")
    object ExileBottomMultiple : ShortcutAction("exileBottomMultiple", "Exile Multiple from Bottom", "Bottom Card Operations")
    object BottomToTop : ShortcutAction("bottomToTop", "Move Bottom Card to Top", "Bottom Card Operations")

    // View Zones
    object ViewLibrary : ShortcutAction("viewLibrary", "View Library", "View Zones")
    object ViewGraveyard : ShortcutAction("viewGraveyard", "View Graveyard", "View Zones")
    object ViewExile : ShortcutAction("viewExile", "View Exile", "View Zones")
    object ViewCommandZone : ShortcutAction("viewCommandZone", "View Command Zone", "View Zones")
    object ViewSideboard : ShortcutAction("viewSideboard", "View Sideboard", "View Zones")
    object PeekTopCards : ShortcutAction("peekTopCards", "Peek Top Cards", "View Zones")
    object PeekBottomCards : ShortcutAction("peekBottomCards", "Peek Bottom Cards", "View Zones")
    object CloseDialog : ShortcutAction("closeDialog", "Close Dialog", "View Zones")

    // Selection
    object SelectAll : ShortcutAction("selectAll", "Select All Cards in Zone", "Selection")
    object SelectRow : ShortcutAction("selectRow", "Select All Cards in Row", "Selection")
    object SelectColumn : ShortcutAction("selectColumn", "Select All Cards in Column", "Selection")

    // Arrows
    object DrawArrow : ShortcutAction("drawArrow", "Draw Arrow...", "Arrows")
    object RemoveArrows : ShortcutAction("removeArrows", "Remove Local Arrows", "Arrows")

    // Gameplay
    object RollDice : ShortcutAction("rollDice", "Roll Dice", "Gameplay")
    object Concede : ShortcutAction("concede", "Concede", "Gameplay")
    object LeaveGame : ShortcutAction("leaveGame", "Leave Game", "Gameplay")
    object FocusChat : ShortcutAction("focusChat", "Focus Chat", "Gameplay")
    object OpenPlayerCounters : ShortcutAction("openPlayerCounters", "Player Counters", "Gameplay")
    object SortHand : ShortcutAction("sortHand", "Sort Hand", "Gameplay")
    object CreateAnotherToken : ShortcutAction("createAnotherToken", "Create Another Token", "Gameplay")
    object CreateRelatedTokens : ShortcutAction("createRelatedTokens", "Create All Related Tokens", "Gameplay")

    companion object {
        val all: List<ShortcutAction> by lazy {
            listOf(
                // Game Phases
                UntapPhase, UpkeepPhase, DrawPhase, FirstMainPhase, CombatPhase, AttackPhase, BlockPhase,
                DamagePhase, EndCombatPhase, SecondMainPhase, EndPhase, NextPhase, PassTurn,
                // Card Actions
                TapUntapCard, UntapAll, ToggleDoesntUntap, FlipCard, PlayFaceDown, CloneCard, CreateToken, SetAnnotation,
                AttachCard, DetachCard, MoveToGraveyard, MoveToBottomLibrary, MoveToExile, MoveToHand, MoveTopToBottom,
                // Power/Toughness
                AddPower, RemovePower, AddToughness, RemoveToughness, AddBothPT, RemoveBothPT,
                SetPowerToughness, ResetPowerToughness, FlowPower, FlowToughness,
                // Life & Counters
                AddLife, RemoveLife, SetLife, AddCounter, RemoveCounter,
                // Card Counters (A-F)
                AddCounterA, RemoveCounterA, AddCounterB, RemoveCounterB, AddCounterC, RemoveCounterC,
                AddCounterD, RemoveCounterD, AddCounterE, RemoveCounterE, AddCounterF, RemoveCounterF,
                IncrementAllCounters,
                // Drawing & Library
                DrawCard, DrawMultiple, UndoDraw, Mulligan, ShuffleLibrary, ShuffleTopCards, ShuffleBottomCards,
                PlayTopCard, MillTopCard, MillMultiple, AlwaysRevealTopCard, AlwaysLookAtTopCard,
                // Bottom Card Operations
                DrawBottomCard, DrawBottomMultiple, MillBottomCard, MillBottomMultiple,
                ExileBottomCard, ExileBottomMultiple, BottomToTop,
                // View Zones
                ViewLibrary, ViewGraveyard, ViewExile, ViewCommandZone, ViewSideboard, PeekTopCards, PeekBottomCards, CloseDialog,
                // Selection
                SelectAll, SelectRow, SelectColumn,
                // Arrows
                DrawArrow, RemoveArrows,
                // Gameplay
                RollDice, Concede, LeaveGame, FocusChat, OpenPlayerCounters, SortHand, CreateAnotherToken, CreateRelatedTokens
            )
        }

        fun fromId(id: String): ShortcutAction? = all.find { it.id == id }

        val byCategory: Map<String, List<ShortcutAction>> by lazy { all.groupBy { it.category } }
    }
}

/**
 * Represents a key binding (key + modifiers)
 */
@Serializable
data class KeyBinding(
    val keyCode: Int,
    val ctrl: Boolean = false,
    val alt: Boolean = false,
    val shift: Boolean = false
) {
    fun toDisplayString(): String {
        val parts = mutableListOf<String>()
        if (ctrl) parts.add("Ctrl")
        if (alt) parts.add("Alt")
        if (shift) parts.add("Shift")
        parts.add(keyCodeToName(keyCode))
        return parts.joinToString("+")
    }

    companion object {
        // Common key codes (matching java.awt.event.KeyEvent values)
        const val KEY_F1 = 112
        const val KEY_F2 = 113
        const val KEY_F3 = 114
        const val KEY_F4 = 115
        const val KEY_F5 = 116
        const val KEY_F6 = 117
        const val KEY_F7 = 118
        const val KEY_F8 = 119
        const val KEY_F9 = 120
        const val KEY_F10 = 121
        const val KEY_F11 = 122
        const val KEY_F12 = 123
        const val KEY_ESCAPE = 27
        const val KEY_ENTER = 10
        const val KEY_SPACE = 32
        const val KEY_TAB = 9
        const val KEY_DELETE = 127
        const val KEY_BACKSPACE = 8

        // Letters (A-Z are 65-90)
        const val KEY_A = 65
        const val KEY_B = 66
        const val KEY_C = 67
        const val KEY_D = 68
        const val KEY_E = 69
        const val KEY_F = 70
        const val KEY_G = 71
        const val KEY_H = 72
        const val KEY_I = 73
        const val KEY_J = 74
        const val KEY_K = 75
        const val KEY_L = 76
        const val KEY_M = 77
        const val KEY_N = 78
        const val KEY_O = 79
        const val KEY_P = 80
        const val KEY_Q = 81
        const val KEY_R = 82
        const val KEY_S = 83
        const val KEY_T = 84
        const val KEY_U = 85
        const val KEY_V = 86
        const val KEY_W = 87
        const val KEY_X = 88
        const val KEY_Y = 89
        const val KEY_Z = 90

        // Numbers and symbols
        const val KEY_0 = 48
        const val KEY_1 = 49
        const val KEY_2 = 50
        const val KEY_3 = 51
        const val KEY_4 = 52
        const val KEY_5 = 53
        const val KEY_6 = 54
        const val KEY_7 = 55
        const val KEY_8 = 56
        const val KEY_9 = 57
        const val KEY_EQUALS = 61  // = key
        const val KEY_MINUS = 45   // - key
        const val KEY_PERIOD = 46  // . key
        const val KEY_COMMA = 44   // , key
        const val KEY_SLASH = 47   // / key
        const val KEY_SEMICOLON = 59  // ; key
        const val KEY_BRACKET_LEFT = 91  // [ key
        const val KEY_BRACKET_RIGHT = 93  // ] key
        const val KEY_BACKSLASH = 92  // \ key
        const val KEY_NUMPAD_ADD = 107  // Numpad +
        const val KEY_NUMPAD_SUBTRACT = 109  // Numpad -

        fun keyCodeToName(keyCode: Int): String = when (keyCode) {
            KEY_F1 -> "F1"
            KEY_F2 -> "F2"
            KEY_F3 -> "F3"
            KEY_F4 -> "F4"
            KEY_F5 -> "F5"
            KEY_F6 -> "F6"
            KEY_F7 -> "F7"
            KEY_F8 -> "F8"
            KEY_F9 -> "F9"
            KEY_F10 -> "F10"
            KEY_F11 -> "F11"
            KEY_F12 -> "F12"
            KEY_ESCAPE -> "Esc"
            KEY_ENTER -> "Enter"
            KEY_SPACE -> "Space"
            KEY_TAB -> "Tab"
            KEY_DELETE -> "Delete"
            KEY_BACKSPACE -> "Backspace"
            KEY_EQUALS -> "="
            KEY_MINUS -> "-"
            KEY_PERIOD -> "."
            KEY_COMMA -> ","
            KEY_SLASH -> "/"
            KEY_SEMICOLON -> ";"
            KEY_BRACKET_LEFT -> "["
            KEY_BRACKET_RIGHT -> "]"
            KEY_BACKSLASH -> "\\"
            KEY_NUMPAD_ADD -> "Num+"
            KEY_NUMPAD_SUBTRACT -> "Num-"
            in KEY_A..KEY_Z -> ('A' + (keyCode - KEY_A)).toString()
            in KEY_0..KEY_9 -> (keyCode - KEY_0).toString()
            else -> "Key$keyCode"
        }
    }
}

/**
 * Default keyboard shortcuts
 */
object DefaultShortcuts {
    val bindings: Map<KeyBinding, ShortcutAction> = mapOf(
        // Game Phases
        KeyBinding(KeyBinding.KEY_F5) to ShortcutAction.UntapPhase,
        KeyBinding(KeyBinding.KEY_F6) to ShortcutAction.DrawPhase,
        KeyBinding(KeyBinding.KEY_F7) to ShortcutAction.FirstMainPhase,
        KeyBinding(KeyBinding.KEY_F8) to ShortcutAction.CombatPhase,
        KeyBinding(KeyBinding.KEY_F9) to ShortcutAction.SecondMainPhase,
        KeyBinding(KeyBinding.KEY_F10) to ShortcutAction.EndPhase,
        KeyBinding(KeyBinding.KEY_SPACE, ctrl = true) to ShortcutAction.NextPhase,
        KeyBinding(KeyBinding.KEY_TAB) to ShortcutAction.NextPhase,
        KeyBinding(KeyBinding.KEY_ENTER, ctrl = true) to ShortcutAction.PassTurn,

        // Card Actions
        KeyBinding(KeyBinding.KEY_T) to ShortcutAction.TapUntapCard,
        KeyBinding(KeyBinding.KEY_U, ctrl = true) to ShortcutAction.UntapAll,
        KeyBinding(KeyBinding.KEY_U, alt = true) to ShortcutAction.ToggleDoesntUntap,
        KeyBinding(KeyBinding.KEY_F, alt = true) to ShortcutAction.FlipCard,
        KeyBinding(KeyBinding.KEY_F, ctrl = true, shift = true) to ShortcutAction.PlayFaceDown,
        KeyBinding(KeyBinding.KEY_J, ctrl = true) to ShortcutAction.CloneCard,
        KeyBinding(KeyBinding.KEY_T, ctrl = true) to ShortcutAction.CreateToken,
        KeyBinding(KeyBinding.KEY_N, alt = true) to ShortcutAction.SetAnnotation,
        KeyBinding(KeyBinding.KEY_A, ctrl = true, alt = true) to ShortcutAction.AttachCard,
        KeyBinding(KeyBinding.KEY_U, ctrl = true, alt = true) to ShortcutAction.DetachCard,
        KeyBinding(KeyBinding.KEY_DELETE) to ShortcutAction.MoveToGraveyard,
        KeyBinding(KeyBinding.KEY_DELETE, ctrl = true) to ShortcutAction.MoveToGraveyard,
        KeyBinding(KeyBinding.KEY_B, ctrl = true) to ShortcutAction.MoveToBottomLibrary,
        KeyBinding(KeyBinding.KEY_X, ctrl = true) to ShortcutAction.MoveToExile,
        KeyBinding(KeyBinding.KEY_H, ctrl = true) to ShortcutAction.MoveToHand,
        KeyBinding(KeyBinding.KEY_B, ctrl = true, shift = true) to ShortcutAction.MoveTopToBottom,

        // Power/Toughness
        KeyBinding(KeyBinding.KEY_EQUALS, ctrl = true) to ShortcutAction.AddPower,
        KeyBinding(KeyBinding.KEY_EQUALS, ctrl = true, shift = true) to ShortcutAction.AddPower, // Ctrl++ (shift+=)
        KeyBinding(KeyBinding.KEY_NUMPAD_ADD, ctrl = true) to ShortcutAction.AddPower, // Ctrl+Numpad+
        KeyBinding(KeyBinding.KEY_MINUS, ctrl = true) to ShortcutAction.RemovePower,
        KeyBinding(KeyBinding.KEY_MINUS, ctrl = true, shift = true) to ShortcutAction.RemovePower, // Ctrl+Shift+-
        KeyBinding(KeyBinding.KEY_NUMPAD_SUBTRACT, ctrl = true) to ShortcutAction.RemovePower, // Ctrl+Numpad-
        KeyBinding(KeyBinding.KEY_EQUALS, alt = true) to ShortcutAction.AddToughness,
        KeyBinding(KeyBinding.KEY_EQUALS, alt = true, shift = true) to ShortcutAction.AddToughness, // Alt++ (shift+=)
        KeyBinding(KeyBinding.KEY_NUMPAD_ADD, alt = true) to ShortcutAction.AddToughness, // Alt+Numpad+
        KeyBinding(KeyBinding.KEY_MINUS, alt = true) to ShortcutAction.RemoveToughness,
        KeyBinding(KeyBinding.KEY_MINUS, alt = true, shift = true) to ShortcutAction.RemoveToughness, // Alt+Shift+-
        KeyBinding(KeyBinding.KEY_NUMPAD_SUBTRACT, alt = true) to ShortcutAction.RemoveToughness, // Alt+Numpad-
        KeyBinding(KeyBinding.KEY_EQUALS, ctrl = true, alt = true) to ShortcutAction.AddBothPT,
        KeyBinding(KeyBinding.KEY_EQUALS, ctrl = true, alt = true, shift = true) to ShortcutAction.AddBothPT, // Ctrl+Alt++ (shift+=)
        KeyBinding(KeyBinding.KEY_NUMPAD_ADD, ctrl = true, alt = true) to ShortcutAction.AddBothPT, // Ctrl+Alt+Numpad+
        KeyBinding(KeyBinding.KEY_MINUS, ctrl = true, alt = true) to ShortcutAction.RemoveBothPT,
        KeyBinding(KeyBinding.KEY_MINUS, ctrl = true, alt = true, shift = true) to ShortcutAction.RemoveBothPT, // Ctrl+Alt+Shift+-
        KeyBinding(KeyBinding.KEY_NUMPAD_SUBTRACT, ctrl = true, alt = true) to ShortcutAction.RemoveBothPT, // Ctrl+Alt+Numpad-
        KeyBinding(KeyBinding.KEY_P, ctrl = true) to ShortcutAction.SetPowerToughness,
        KeyBinding(KeyBinding.KEY_0, ctrl = true, alt = true) to ShortcutAction.ResetPowerToughness,

        // Life & Counters
        KeyBinding(KeyBinding.KEY_F12) to ShortcutAction.AddLife,
        KeyBinding(KeyBinding.KEY_F11) to ShortcutAction.RemoveLife,
        KeyBinding(KeyBinding.KEY_L, ctrl = true) to ShortcutAction.SetLife,

        // Card Counters (A-F colored counters) - matching Cockatrice
        // Counter A (Red): Alt+. / Alt+,
        KeyBinding(KeyBinding.KEY_PERIOD, alt = true) to ShortcutAction.AddCounterA,
        KeyBinding(KeyBinding.KEY_COMMA, alt = true) to ShortcutAction.RemoveCounterA,
        // Counter B (Yellow): Ctrl+. / Ctrl+,
        KeyBinding(KeyBinding.KEY_PERIOD, ctrl = true) to ShortcutAction.AddCounterB,
        KeyBinding(KeyBinding.KEY_COMMA, ctrl = true) to ShortcutAction.RemoveCounterB,
        // Counter C (Green): Ctrl+Shift+. / Ctrl+Shift+, (approximation of Ctrl+> / Ctrl+<)
        KeyBinding(KeyBinding.KEY_PERIOD, ctrl = true, shift = true) to ShortcutAction.AddCounterC,
        KeyBinding(KeyBinding.KEY_COMMA, ctrl = true, shift = true) to ShortcutAction.RemoveCounterC,
        // Counters D, E, F don't have default shortcuts in Cockatrice
        // Increment All Card Counters
        KeyBinding(KeyBinding.KEY_A, ctrl = true, shift = true) to ShortcutAction.IncrementAllCounters,

        // Drawing & Library
        KeyBinding(KeyBinding.KEY_D, ctrl = true) to ShortcutAction.DrawCard,
        KeyBinding(KeyBinding.KEY_E, ctrl = true) to ShortcutAction.DrawMultiple,
        KeyBinding(KeyBinding.KEY_D, ctrl = true, shift = true) to ShortcutAction.UndoDraw,
        KeyBinding(KeyBinding.KEY_M, ctrl = true) to ShortcutAction.Mulligan,
        KeyBinding(KeyBinding.KEY_S, ctrl = true) to ShortcutAction.ShuffleLibrary,
        KeyBinding(KeyBinding.KEY_Y, ctrl = true) to ShortcutAction.PlayTopCard,
        KeyBinding(KeyBinding.KEY_Y, alt = true) to ShortcutAction.MillTopCard,
        KeyBinding(KeyBinding.KEY_M, alt = true) to ShortcutAction.MillMultiple,
        KeyBinding(KeyBinding.KEY_N, ctrl = true) to ShortcutAction.AlwaysRevealTopCard,
        KeyBinding(KeyBinding.KEY_N, ctrl = true, shift = true) to ShortcutAction.AlwaysLookAtTopCard,

        // View Zones
        KeyBinding(KeyBinding.KEY_F3) to ShortcutAction.ViewLibrary,
        KeyBinding(KeyBinding.KEY_F4) to ShortcutAction.ViewGraveyard,
        KeyBinding(KeyBinding.KEY_F3, ctrl = true) to ShortcutAction.ViewSideboard,
        KeyBinding(KeyBinding.KEY_W, ctrl = true) to ShortcutAction.PeekTopCards,
        KeyBinding(KeyBinding.KEY_W, ctrl = true, shift = true) to ShortcutAction.PeekBottomCards,
        KeyBinding(KeyBinding.KEY_ESCAPE) to ShortcutAction.CloseDialog,

        // Selection
        KeyBinding(KeyBinding.KEY_A, ctrl = true) to ShortcutAction.SelectAll,
        KeyBinding(KeyBinding.KEY_X, ctrl = true, shift = true) to ShortcutAction.SelectRow,
        // Note: Ctrl+Shift+C conflicts with copy, using different binding
        // KeyBinding(KeyBinding.KEY_C, ctrl = true, shift = true) to ShortcutAction.SelectColumn,

        // Arrows
        KeyBinding(KeyBinding.KEY_A, alt = true) to ShortcutAction.DrawArrow,
        KeyBinding(KeyBinding.KEY_R, ctrl = true) to ShortcutAction.RemoveArrows,

        // Gameplay
        KeyBinding(KeyBinding.KEY_I, ctrl = true) to ShortcutAction.RollDice,
        KeyBinding(KeyBinding.KEY_F2) to ShortcutAction.Concede,
        KeyBinding(KeyBinding.KEY_Q, ctrl = true) to ShortcutAction.LeaveGame,
        KeyBinding(KeyBinding.KEY_ENTER, shift = true) to ShortcutAction.FocusChat,
        KeyBinding(KeyBinding.KEY_K, ctrl = true) to ShortcutAction.OpenPlayerCounters,
        KeyBinding(KeyBinding.KEY_H, ctrl = true, shift = true) to ShortcutAction.SortHand,
        KeyBinding(KeyBinding.KEY_G, ctrl = true) to ShortcutAction.CreateAnotherToken,
        KeyBinding(KeyBinding.KEY_T, ctrl = true, shift = true) to ShortcutAction.CreateRelatedTokens
    )

    /**
     * Get action for a key binding
     */
    fun getAction(binding: KeyBinding): ShortcutAction? = bindings[binding]

    /**
     * Get binding for an action
     */
    fun getBinding(action: ShortcutAction): KeyBinding? =
        bindings.entries.find { it.value == action }?.key

    /**
     * Get all bindings for an action (some actions have multiple bindings)
     */
    fun getAllBindings(action: ShortcutAction): List<KeyBinding> =
        bindings.entries.filter { it.value == action }.map { it.key }
}

/**
 * Keyboard shortcuts settings manager
 */
class KeyboardShortcutsSettings {
    // For now, use default shortcuts. Custom shortcuts can be added later.
    private val customBindings: MutableMap<KeyBinding, ShortcutAction> =
        DefaultShortcuts.bindings.toMutableMap()

    /**
     * Get action for a key binding
     */
    fun getAction(binding: KeyBinding): ShortcutAction? = customBindings[binding]

    /**
     * Get action for key code and modifiers
     */
    fun getAction(keyCode: Int, ctrl: Boolean, alt: Boolean, shift: Boolean): ShortcutAction? =
        getAction(KeyBinding(keyCode, ctrl, alt, shift))

    /**
     * Get display string for an action's binding
     */
    fun getBindingDisplay(action: ShortcutAction): String? =
        customBindings.entries.find { it.value == action }?.key?.toDisplayString()

    /**
     * Reset to default shortcuts
     */
    fun resetToDefaults() {
        customBindings.clear()
        customBindings.putAll(DefaultShortcuts.bindings)
    }
}
