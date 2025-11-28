package com.dustinmcafee.dongadeuce.settings

import kotlinx.serialization.Serializable

/**
 * Represents a keyboard shortcut action
 */
sealed class ShortcutAction(val id: String, val displayName: String, val category: String) {
    // Game Phases
    object UntapPhase : ShortcutAction("untapPhase", "Untap Phase", "Game Phases")
    object DrawPhase : ShortcutAction("drawPhase", "Draw Phase", "Game Phases")
    object FirstMainPhase : ShortcutAction("firstMainPhase", "First Main Phase", "Game Phases")
    object CombatPhase : ShortcutAction("combatPhase", "Combat Phase", "Game Phases")
    object SecondMainPhase : ShortcutAction("secondMainPhase", "Second Main Phase", "Game Phases")
    object EndPhase : ShortcutAction("endPhase", "End Phase", "Game Phases")
    object NextPhase : ShortcutAction("nextPhase", "Next Phase", "Game Phases")
    object PassTurn : ShortcutAction("passTurn", "Pass Turn", "Game Phases")

    // Card Actions
    object TapUntapCard : ShortcutAction("tapUntapCard", "Tap/Untap Card", "Card Actions")
    object UntapAll : ShortcutAction("untapAll", "Untap All", "Card Actions")
    object ToggleDoesntUntap : ShortcutAction("toggleDoesntUntap", "Toggle Doesn't Untap", "Card Actions")
    object FlipCard : ShortcutAction("flipCard", "Flip Card", "Card Actions")
    object CloneCard : ShortcutAction("cloneCard", "Clone Card", "Card Actions")
    object CreateToken : ShortcutAction("createToken", "Create Token", "Card Actions")
    object SetAnnotation : ShortcutAction("setAnnotation", "Set Annotation", "Card Actions")
    object MoveToGraveyard : ShortcutAction("moveToGraveyard", "Move to Graveyard", "Card Actions")
    object MoveToBottomLibrary : ShortcutAction("moveToBottomLibrary", "Move to Bottom of Library", "Card Actions")
    object MoveToExile : ShortcutAction("moveToExile", "Move to Exile", "Card Actions")
    object MoveToHand : ShortcutAction("moveToHand", "Move to Hand", "Card Actions")

    // Power/Toughness
    object AddPower : ShortcutAction("addPower", "Add Power (+1/+0)", "Power/Toughness")
    object RemovePower : ShortcutAction("removePower", "Remove Power (-1/-0)", "Power/Toughness")
    object AddToughness : ShortcutAction("addToughness", "Add Toughness (+0/+1)", "Power/Toughness")
    object RemoveToughness : ShortcutAction("removeToughness", "Remove Toughness (-0/-1)", "Power/Toughness")
    object AddBothPT : ShortcutAction("addBothPT", "Add Both (+1/+1)", "Power/Toughness")
    object RemoveBothPT : ShortcutAction("removeBothPT", "Remove Both (-1/-1)", "Power/Toughness")
    object SetPowerToughness : ShortcutAction("setPowerToughness", "Set Power/Toughness", "Power/Toughness")
    object ResetPowerToughness : ShortcutAction("resetPowerToughness", "Reset Power/Toughness", "Power/Toughness")

    // Life & Counters
    object AddLife : ShortcutAction("addLife", "Add Life (+1)", "Life & Counters")
    object RemoveLife : ShortcutAction("removeLife", "Remove Life (-1)", "Life & Counters")
    object SetLife : ShortcutAction("setLife", "Set Life Total", "Life & Counters")
    object AddCounter : ShortcutAction("addCounter", "Add Counter", "Life & Counters")
    object RemoveCounter : ShortcutAction("removeCounter", "Remove Counter", "Life & Counters")

    // Drawing & Library
    object DrawCard : ShortcutAction("drawCard", "Draw Card", "Drawing & Library")
    object DrawMultiple : ShortcutAction("drawMultiple", "Draw Multiple Cards", "Drawing & Library")
    object UndoDraw : ShortcutAction("undoDraw", "Undo Draw", "Drawing & Library")
    object Mulligan : ShortcutAction("mulligan", "Mulligan", "Drawing & Library")
    object ShuffleLibrary : ShortcutAction("shuffleLibrary", "Shuffle Library", "Drawing & Library")
    object PlayTopCard : ShortcutAction("playTopCard", "Play Top Card", "Drawing & Library")
    object MillTopCard : ShortcutAction("millTopCard", "Mill Top Card", "Drawing & Library")
    object MillMultiple : ShortcutAction("millMultiple", "Mill Multiple Cards", "Drawing & Library")

    // View Zones
    object ViewLibrary : ShortcutAction("viewLibrary", "View Library", "View Zones")
    object ViewGraveyard : ShortcutAction("viewGraveyard", "View Graveyard", "View Zones")
    object ViewExile : ShortcutAction("viewExile", "View Exile", "View Zones")
    object ViewCommandZone : ShortcutAction("viewCommandZone", "View Command Zone", "View Zones")
    object PeekTopCards : ShortcutAction("peekTopCards", "Peek Top Cards", "View Zones")
    object PeekBottomCards : ShortcutAction("peekBottomCards", "Peek Bottom Cards", "View Zones")
    object CloseDialog : ShortcutAction("closeDialog", "Close Dialog", "View Zones")

    // Gameplay
    object RollDice : ShortcutAction("rollDice", "Roll Dice", "Gameplay")
    object Concede : ShortcutAction("concede", "Concede", "Gameplay")
    object FocusChat : ShortcutAction("focusChat", "Focus Chat", "Gameplay")
    object OpenPlayerCounters : ShortcutAction("openPlayerCounters", "Player Counters", "Gameplay")

    companion object {
        val all: List<ShortcutAction> by lazy {
            listOf(
                // Game Phases
                UntapPhase, DrawPhase, FirstMainPhase, CombatPhase, SecondMainPhase, EndPhase, NextPhase, PassTurn,
                // Card Actions
                TapUntapCard, UntapAll, ToggleDoesntUntap, FlipCard, CloneCard, CreateToken, SetAnnotation,
                MoveToGraveyard, MoveToBottomLibrary, MoveToExile, MoveToHand,
                // Power/Toughness
                AddPower, RemovePower, AddToughness, RemoveToughness, AddBothPT, RemoveBothPT, SetPowerToughness, ResetPowerToughness,
                // Life & Counters
                AddLife, RemoveLife, SetLife, AddCounter, RemoveCounter,
                // Drawing & Library
                DrawCard, DrawMultiple, UndoDraw, Mulligan, ShuffleLibrary, PlayTopCard, MillTopCard, MillMultiple,
                // View Zones
                ViewLibrary, ViewGraveyard, ViewExile, ViewCommandZone, PeekTopCards, PeekBottomCards, CloseDialog,
                // Gameplay
                RollDice, Concede, FocusChat, OpenPlayerCounters
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
        const val KEY_EQUALS = 61  // = key
        const val KEY_MINUS = 45   // - key

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
            KEY_0 -> "0"
            in KEY_A..KEY_Z -> ('A' + (keyCode - KEY_A)).toString()
            in 48..57 -> (keyCode - 48).toString()
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
        KeyBinding(KeyBinding.KEY_J, ctrl = true) to ShortcutAction.CloneCard,
        KeyBinding(KeyBinding.KEY_T, ctrl = true) to ShortcutAction.CreateToken,
        KeyBinding(KeyBinding.KEY_N, alt = true) to ShortcutAction.SetAnnotation,
        KeyBinding(KeyBinding.KEY_DELETE) to ShortcutAction.MoveToGraveyard,
        KeyBinding(KeyBinding.KEY_DELETE, ctrl = true) to ShortcutAction.MoveToGraveyard,
        KeyBinding(KeyBinding.KEY_B, ctrl = true) to ShortcutAction.MoveToBottomLibrary,
        KeyBinding(KeyBinding.KEY_X, ctrl = true) to ShortcutAction.MoveToExile,
        KeyBinding(KeyBinding.KEY_H, ctrl = true) to ShortcutAction.MoveToHand,

        // Power/Toughness
        KeyBinding(KeyBinding.KEY_EQUALS, ctrl = true) to ShortcutAction.AddPower,
        KeyBinding(KeyBinding.KEY_MINUS, ctrl = true) to ShortcutAction.RemovePower,
        KeyBinding(KeyBinding.KEY_EQUALS, alt = true) to ShortcutAction.AddToughness,
        KeyBinding(KeyBinding.KEY_MINUS, alt = true) to ShortcutAction.RemoveToughness,
        KeyBinding(KeyBinding.KEY_EQUALS, ctrl = true, alt = true) to ShortcutAction.AddBothPT,
        KeyBinding(KeyBinding.KEY_MINUS, ctrl = true, alt = true) to ShortcutAction.RemoveBothPT,
        KeyBinding(KeyBinding.KEY_P, ctrl = true) to ShortcutAction.SetPowerToughness,
        KeyBinding(KeyBinding.KEY_0, ctrl = true, alt = true) to ShortcutAction.ResetPowerToughness,

        // Life & Counters
        KeyBinding(KeyBinding.KEY_F12) to ShortcutAction.AddLife,
        KeyBinding(KeyBinding.KEY_F11) to ShortcutAction.RemoveLife,
        KeyBinding(KeyBinding.KEY_L, ctrl = true) to ShortcutAction.SetLife,

        // Drawing & Library
        KeyBinding(KeyBinding.KEY_D, ctrl = true) to ShortcutAction.DrawCard,
        KeyBinding(KeyBinding.KEY_E, ctrl = true) to ShortcutAction.DrawMultiple,
        KeyBinding(KeyBinding.KEY_D, ctrl = true, shift = true) to ShortcutAction.UndoDraw,
        KeyBinding(KeyBinding.KEY_M, ctrl = true) to ShortcutAction.Mulligan,
        KeyBinding(KeyBinding.KEY_S, ctrl = true) to ShortcutAction.ShuffleLibrary,
        KeyBinding(KeyBinding.KEY_Y, ctrl = true) to ShortcutAction.PlayTopCard,
        KeyBinding(KeyBinding.KEY_Y, alt = true) to ShortcutAction.MillTopCard,
        KeyBinding(KeyBinding.KEY_M, alt = true) to ShortcutAction.MillMultiple,

        // View Zones
        KeyBinding(KeyBinding.KEY_F3) to ShortcutAction.ViewLibrary,
        KeyBinding(KeyBinding.KEY_F4) to ShortcutAction.ViewGraveyard,
        KeyBinding(KeyBinding.KEY_W, ctrl = true) to ShortcutAction.PeekTopCards,
        KeyBinding(KeyBinding.KEY_W, ctrl = true, shift = true) to ShortcutAction.PeekBottomCards,
        KeyBinding(KeyBinding.KEY_ESCAPE) to ShortcutAction.CloseDialog,

        // Gameplay
        KeyBinding(KeyBinding.KEY_I, ctrl = true) to ShortcutAction.RollDice,
        KeyBinding(KeyBinding.KEY_F2) to ShortcutAction.Concede,
        KeyBinding(KeyBinding.KEY_ENTER, shift = true) to ShortcutAction.FocusChat,
        KeyBinding(KeyBinding.KEY_K, ctrl = true) to ShortcutAction.OpenPlayerCounters
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
