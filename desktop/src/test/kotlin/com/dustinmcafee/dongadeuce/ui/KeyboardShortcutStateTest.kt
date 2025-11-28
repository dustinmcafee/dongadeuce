package com.dustinmcafee.dongadeuce.ui

import com.dustinmcafee.dongadeuce.models.*
import com.dustinmcafee.dongadeuce.settings.KeyBinding
import com.dustinmcafee.dongadeuce.settings.DefaultShortcuts
import com.dustinmcafee.dongadeuce.settings.KeyboardShortcutsSettings
import com.dustinmcafee.dongadeuce.settings.ShortcutAction
import kotlin.test.*

/**
 * Tests for keyboard shortcut binding configuration and lookup.
 *
 * Note: Testing the actual KeyEvent handling requires mocking Compose's KeyEvent
 * which has extension properties that are difficult to mock. Instead, we test:
 * 1. KeyBinding data class behavior
 * 2. ShortcutAction configuration
 * 3. KeyboardShortcutsSettings lookup
 * 4. DefaultShortcuts configuration
 * 5. SelectionState interactions (tested separately)
 */
class KeyboardShortcutStateTest {

    // ==================== KeyBinding Tests ====================

    @Test
    fun `KeyBinding equality works correctly`() {
        val binding1 = KeyBinding(KeyBinding.KEY_D, ctrl = true)
        val binding2 = KeyBinding(KeyBinding.KEY_D, ctrl = true)

        assertEquals(binding1, binding2)
    }

    @Test
    fun `KeyBinding with different modifiers are not equal`() {
        val binding1 = KeyBinding(KeyBinding.KEY_D, ctrl = true)
        val binding2 = KeyBinding(KeyBinding.KEY_D, alt = true)

        assertNotEquals(binding1, binding2)
    }

    @Test
    fun `KeyBinding toDisplayString shows correct format`() {
        assertEquals("Ctrl+D", KeyBinding(KeyBinding.KEY_D, ctrl = true).toDisplayString())
        assertEquals("Alt+F", KeyBinding(KeyBinding.KEY_F, alt = true).toDisplayString())
        assertEquals("Shift+Enter", KeyBinding(KeyBinding.KEY_ENTER, shift = true).toDisplayString())
        assertEquals("Ctrl+Alt+Shift+=", KeyBinding(KeyBinding.KEY_EQUALS, ctrl = true, alt = true, shift = true).toDisplayString())
    }

    @Test
    fun `KeyBinding toDisplayString shows function keys correctly`() {
        assertEquals("F5", KeyBinding(KeyBinding.KEY_F5).toDisplayString())
        assertEquals("F12", KeyBinding(KeyBinding.KEY_F12).toDisplayString())
    }

    @Test
    fun `KeyBinding toDisplayString shows special keys correctly`() {
        assertEquals("Esc", KeyBinding(KeyBinding.KEY_ESCAPE).toDisplayString())
        assertEquals("Enter", KeyBinding(KeyBinding.KEY_ENTER).toDisplayString())
        assertEquals("Space", KeyBinding(KeyBinding.KEY_SPACE).toDisplayString())
        assertEquals("Tab", KeyBinding(KeyBinding.KEY_TAB).toDisplayString())
        assertEquals("Delete", KeyBinding(KeyBinding.KEY_DELETE).toDisplayString())
    }

    // ==================== DefaultShortcuts Configuration Tests ====================

    @Test
    fun `DefaultShortcuts has F5 bound to Untap Phase`() {
        val action = DefaultShortcuts.getAction(KeyBinding(KeyBinding.KEY_F5))
        assertEquals(ShortcutAction.UntapPhase, action)
    }

    @Test
    fun `DefaultShortcuts has F6 bound to Draw Phase`() {
        val action = DefaultShortcuts.getAction(KeyBinding(KeyBinding.KEY_F6))
        assertEquals(ShortcutAction.DrawPhase, action)
    }

    @Test
    fun `DefaultShortcuts has F7 bound to First Main Phase`() {
        val action = DefaultShortcuts.getAction(KeyBinding(KeyBinding.KEY_F7))
        assertEquals(ShortcutAction.FirstMainPhase, action)
    }

    @Test
    fun `DefaultShortcuts has F8 bound to Combat Phase`() {
        val action = DefaultShortcuts.getAction(KeyBinding(KeyBinding.KEY_F8))
        assertEquals(ShortcutAction.CombatPhase, action)
    }

    @Test
    fun `DefaultShortcuts has F9 bound to Second Main Phase`() {
        val action = DefaultShortcuts.getAction(KeyBinding(KeyBinding.KEY_F9))
        assertEquals(ShortcutAction.SecondMainPhase, action)
    }

    @Test
    fun `DefaultShortcuts has F10 bound to End Phase`() {
        val action = DefaultShortcuts.getAction(KeyBinding(KeyBinding.KEY_F10))
        assertEquals(ShortcutAction.EndPhase, action)
    }

    @Test
    fun `DefaultShortcuts has Tab bound to Next Phase`() {
        val action = DefaultShortcuts.getAction(KeyBinding(KeyBinding.KEY_TAB))
        assertEquals(ShortcutAction.NextPhase, action)
    }

    @Test
    fun `DefaultShortcuts has Ctrl+Space bound to Next Phase`() {
        val action = DefaultShortcuts.getAction(KeyBinding(KeyBinding.KEY_SPACE, ctrl = true))
        assertEquals(ShortcutAction.NextPhase, action)
    }

    @Test
    fun `DefaultShortcuts has Ctrl+Enter bound to Pass Turn`() {
        val action = DefaultShortcuts.getAction(KeyBinding(KeyBinding.KEY_ENTER, ctrl = true))
        assertEquals(ShortcutAction.PassTurn, action)
    }

    @Test
    fun `DefaultShortcuts has T bound to Tap Untap`() {
        val action = DefaultShortcuts.getAction(KeyBinding(KeyBinding.KEY_T))
        assertEquals(ShortcutAction.TapUntapCard, action)
    }

    @Test
    fun `DefaultShortcuts has Ctrl+U bound to Untap All`() {
        val action = DefaultShortcuts.getAction(KeyBinding(KeyBinding.KEY_U, ctrl = true))
        assertEquals(ShortcutAction.UntapAll, action)
    }

    @Test
    fun `DefaultShortcuts has Delete bound to Move To Graveyard`() {
        val action = DefaultShortcuts.getAction(KeyBinding(KeyBinding.KEY_DELETE))
        assertEquals(ShortcutAction.MoveToGraveyard, action)
    }

    @Test
    fun `DefaultShortcuts has Ctrl+X bound to Move To Exile`() {
        val action = DefaultShortcuts.getAction(KeyBinding(KeyBinding.KEY_X, ctrl = true))
        assertEquals(ShortcutAction.MoveToExile, action)
    }

    @Test
    fun `DefaultShortcuts has Ctrl+H bound to Move To Hand`() {
        val action = DefaultShortcuts.getAction(KeyBinding(KeyBinding.KEY_H, ctrl = true))
        assertEquals(ShortcutAction.MoveToHand, action)
    }

    @Test
    fun `DefaultShortcuts has Alt+F bound to Flip Card`() {
        val action = DefaultShortcuts.getAction(KeyBinding(KeyBinding.KEY_F, alt = true))
        assertEquals(ShortcutAction.FlipCard, action)
    }

    @Test
    fun `DefaultShortcuts has Ctrl+J bound to Clone Card`() {
        val action = DefaultShortcuts.getAction(KeyBinding(KeyBinding.KEY_J, ctrl = true))
        assertEquals(ShortcutAction.CloneCard, action)
    }

    @Test
    fun `DefaultShortcuts has Ctrl+T bound to Create Token`() {
        val action = DefaultShortcuts.getAction(KeyBinding(KeyBinding.KEY_T, ctrl = true))
        assertEquals(ShortcutAction.CreateToken, action)
    }

    // Power/Toughness bindings
    @Test
    fun `DefaultShortcuts has Ctrl+Equals bound to Add Power`() {
        val action = DefaultShortcuts.getAction(KeyBinding(KeyBinding.KEY_EQUALS, ctrl = true))
        assertEquals(ShortcutAction.AddPower, action)
    }

    @Test
    fun `DefaultShortcuts has Ctrl+Minus bound to Remove Power`() {
        val action = DefaultShortcuts.getAction(KeyBinding(KeyBinding.KEY_MINUS, ctrl = true))
        assertEquals(ShortcutAction.RemovePower, action)
    }

    @Test
    fun `DefaultShortcuts has Alt+Equals bound to Add Toughness`() {
        val action = DefaultShortcuts.getAction(KeyBinding(KeyBinding.KEY_EQUALS, alt = true))
        assertEquals(ShortcutAction.AddToughness, action)
    }

    @Test
    fun `DefaultShortcuts has Alt+Minus bound to Remove Toughness`() {
        val action = DefaultShortcuts.getAction(KeyBinding(KeyBinding.KEY_MINUS, alt = true))
        assertEquals(ShortcutAction.RemoveToughness, action)
    }

    @Test
    fun `DefaultShortcuts has Ctrl+Alt+Equals bound to Add Both PT`() {
        val action = DefaultShortcuts.getAction(KeyBinding(KeyBinding.KEY_EQUALS, ctrl = true, alt = true))
        assertEquals(ShortcutAction.AddBothPT, action)
    }

    @Test
    fun `DefaultShortcuts has Ctrl+Alt+Minus bound to Remove Both PT`() {
        val action = DefaultShortcuts.getAction(KeyBinding(KeyBinding.KEY_MINUS, ctrl = true, alt = true))
        assertEquals(ShortcutAction.RemoveBothPT, action)
    }

    // Life bindings
    @Test
    fun `DefaultShortcuts has F12 bound to Add Life`() {
        val action = DefaultShortcuts.getAction(KeyBinding(KeyBinding.KEY_F12))
        assertEquals(ShortcutAction.AddLife, action)
    }

    @Test
    fun `DefaultShortcuts has F11 bound to Remove Life`() {
        val action = DefaultShortcuts.getAction(KeyBinding(KeyBinding.KEY_F11))
        assertEquals(ShortcutAction.RemoveLife, action)
    }

    @Test
    fun `DefaultShortcuts has Ctrl+L bound to Set Life`() {
        val action = DefaultShortcuts.getAction(KeyBinding(KeyBinding.KEY_L, ctrl = true))
        assertEquals(ShortcutAction.SetLife, action)
    }

    // Card counter bindings
    @Test
    fun `DefaultShortcuts has Alt+Period bound to Add Counter A`() {
        val action = DefaultShortcuts.getAction(KeyBinding(KeyBinding.KEY_PERIOD, alt = true))
        assertEquals(ShortcutAction.AddCounterA, action)
    }

    @Test
    fun `DefaultShortcuts has Alt+Comma bound to Remove Counter A`() {
        val action = DefaultShortcuts.getAction(KeyBinding(KeyBinding.KEY_COMMA, alt = true))
        assertEquals(ShortcutAction.RemoveCounterA, action)
    }

    @Test
    fun `DefaultShortcuts has Ctrl+Period bound to Add Counter B`() {
        val action = DefaultShortcuts.getAction(KeyBinding(KeyBinding.KEY_PERIOD, ctrl = true))
        assertEquals(ShortcutAction.AddCounterB, action)
    }

    @Test
    fun `DefaultShortcuts has Ctrl+Comma bound to Remove Counter B`() {
        val action = DefaultShortcuts.getAction(KeyBinding(KeyBinding.KEY_COMMA, ctrl = true))
        assertEquals(ShortcutAction.RemoveCounterB, action)
    }

    // Drawing & Library bindings
    @Test
    fun `DefaultShortcuts has Ctrl+D bound to Draw Card`() {
        val action = DefaultShortcuts.getAction(KeyBinding(KeyBinding.KEY_D, ctrl = true))
        assertEquals(ShortcutAction.DrawCard, action)
    }

    @Test
    fun `DefaultShortcuts has Ctrl+E bound to Draw Multiple`() {
        val action = DefaultShortcuts.getAction(KeyBinding(KeyBinding.KEY_E, ctrl = true))
        assertEquals(ShortcutAction.DrawMultiple, action)
    }

    @Test
    fun `DefaultShortcuts has Ctrl+M bound to Mulligan`() {
        val action = DefaultShortcuts.getAction(KeyBinding(KeyBinding.KEY_M, ctrl = true))
        assertEquals(ShortcutAction.Mulligan, action)
    }

    @Test
    fun `DefaultShortcuts has Ctrl+S bound to Shuffle Library`() {
        val action = DefaultShortcuts.getAction(KeyBinding(KeyBinding.KEY_S, ctrl = true))
        assertEquals(ShortcutAction.ShuffleLibrary, action)
    }

    @Test
    fun `DefaultShortcuts has Ctrl+Y bound to Play Top Card`() {
        val action = DefaultShortcuts.getAction(KeyBinding(KeyBinding.KEY_Y, ctrl = true))
        assertEquals(ShortcutAction.PlayTopCard, action)
    }

    @Test
    fun `DefaultShortcuts has Alt+Y bound to Mill Top Card`() {
        val action = DefaultShortcuts.getAction(KeyBinding(KeyBinding.KEY_Y, alt = true))
        assertEquals(ShortcutAction.MillTopCard, action)
    }

    @Test
    fun `DefaultShortcuts has Ctrl+N bound to Always Reveal Top Card`() {
        val action = DefaultShortcuts.getAction(KeyBinding(KeyBinding.KEY_N, ctrl = true))
        assertEquals(ShortcutAction.AlwaysRevealTopCard, action)
    }

    @Test
    fun `DefaultShortcuts has Ctrl+Shift+N bound to Always Look At Top Card`() {
        val action = DefaultShortcuts.getAction(KeyBinding(KeyBinding.KEY_N, ctrl = true, shift = true))
        assertEquals(ShortcutAction.AlwaysLookAtTopCard, action)
    }

    // View zone bindings
    @Test
    fun `DefaultShortcuts has F3 bound to View Library`() {
        val action = DefaultShortcuts.getAction(KeyBinding(KeyBinding.KEY_F3))
        assertEquals(ShortcutAction.ViewLibrary, action)
    }

    @Test
    fun `DefaultShortcuts has F4 bound to View Graveyard`() {
        val action = DefaultShortcuts.getAction(KeyBinding(KeyBinding.KEY_F4))
        assertEquals(ShortcutAction.ViewGraveyard, action)
    }

    @Test
    fun `DefaultShortcuts has Ctrl+W bound to Peek Top Cards`() {
        val action = DefaultShortcuts.getAction(KeyBinding(KeyBinding.KEY_W, ctrl = true))
        assertEquals(ShortcutAction.PeekTopCards, action)
    }

    @Test
    fun `DefaultShortcuts has Ctrl+Shift+W bound to Peek Bottom Cards`() {
        val action = DefaultShortcuts.getAction(KeyBinding(KeyBinding.KEY_W, ctrl = true, shift = true))
        assertEquals(ShortcutAction.PeekBottomCards, action)
    }

    @Test
    fun `DefaultShortcuts has Escape bound to Close Dialog`() {
        val action = DefaultShortcuts.getAction(KeyBinding(KeyBinding.KEY_ESCAPE))
        assertEquals(ShortcutAction.CloseDialog, action)
    }

    // Selection bindings
    @Test
    fun `DefaultShortcuts has Ctrl+A bound to Select All`() {
        val action = DefaultShortcuts.getAction(KeyBinding(KeyBinding.KEY_A, ctrl = true))
        assertEquals(ShortcutAction.SelectAll, action)
    }

    @Test
    fun `DefaultShortcuts has Ctrl+Shift+X bound to Select Row`() {
        val action = DefaultShortcuts.getAction(KeyBinding(KeyBinding.KEY_X, ctrl = true, shift = true))
        assertEquals(ShortcutAction.SelectRow, action)
    }

    // Arrow bindings
    @Test
    fun `DefaultShortcuts has Alt+A bound to Draw Arrow`() {
        val action = DefaultShortcuts.getAction(KeyBinding(KeyBinding.KEY_A, alt = true))
        assertEquals(ShortcutAction.DrawArrow, action)
    }

    @Test
    fun `DefaultShortcuts has Ctrl+R bound to Remove Arrows`() {
        val action = DefaultShortcuts.getAction(KeyBinding(KeyBinding.KEY_R, ctrl = true))
        assertEquals(ShortcutAction.RemoveArrows, action)
    }

    // Gameplay bindings
    @Test
    fun `DefaultShortcuts has Ctrl+I bound to Roll Dice`() {
        val action = DefaultShortcuts.getAction(KeyBinding(KeyBinding.KEY_I, ctrl = true))
        assertEquals(ShortcutAction.RollDice, action)
    }

    @Test
    fun `DefaultShortcuts has F2 bound to Concede`() {
        val action = DefaultShortcuts.getAction(KeyBinding(KeyBinding.KEY_F2))
        assertEquals(ShortcutAction.Concede, action)
    }

    @Test
    fun `DefaultShortcuts has Ctrl+Q bound to Leave Game`() {
        val action = DefaultShortcuts.getAction(KeyBinding(KeyBinding.KEY_Q, ctrl = true))
        assertEquals(ShortcutAction.LeaveGame, action)
    }

    @Test
    fun `DefaultShortcuts has Ctrl+Shift+H bound to Sort Hand`() {
        val action = DefaultShortcuts.getAction(KeyBinding(KeyBinding.KEY_H, ctrl = true, shift = true))
        assertEquals(ShortcutAction.SortHand, action)
    }

    // ==================== KeyboardShortcutsSettings Tests ====================

    @Test
    fun `KeyboardShortcutsSettings uses default shortcuts`() {
        val settings = KeyboardShortcutsSettings()

        assertEquals(ShortcutAction.DrawCard, settings.getAction(KeyBinding.KEY_D, ctrl = true, alt = false, shift = false))
        assertEquals(ShortcutAction.Mulligan, settings.getAction(KeyBinding.KEY_M, ctrl = true, alt = false, shift = false))
    }

    @Test
    fun `KeyboardShortcutsSettings getBindingDisplay returns correct string`() {
        val settings = KeyboardShortcutsSettings()

        assertEquals("Ctrl+D", settings.getBindingDisplay(ShortcutAction.DrawCard))
        assertEquals("Ctrl+M", settings.getBindingDisplay(ShortcutAction.Mulligan))
        assertEquals("F5", settings.getBindingDisplay(ShortcutAction.UntapPhase))
    }

    @Test
    fun `KeyboardShortcutsSettings returns null for unbound keys`() {
        val settings = KeyboardShortcutsSettings()

        // Just the letter Z alone is not bound
        assertNull(settings.getAction(KeyBinding.KEY_Z, ctrl = false, alt = false, shift = false))
    }

    @Test
    fun `KeyboardShortcutsSettings resetToDefaults works`() {
        val settings = KeyboardShortcutsSettings()
        settings.resetToDefaults()

        // After reset, should still have default bindings
        assertEquals(ShortcutAction.DrawCard, settings.getAction(KeyBinding.KEY_D, ctrl = true, alt = false, shift = false))
    }

    // ==================== ShortcutAction Tests ====================

    @Test
    fun `ShortcutAction all contains all actions`() {
        val allActions = ShortcutAction.all

        assertTrue(allActions.contains(ShortcutAction.DrawCard))
        assertTrue(allActions.contains(ShortcutAction.Mulligan))
        assertTrue(allActions.contains(ShortcutAction.UntapPhase))
        assertTrue(allActions.contains(ShortcutAction.PassTurn))
        assertTrue(allActions.contains(ShortcutAction.TapUntapCard))
    }

    @Test
    fun `ShortcutAction fromId returns correct action`() {
        assertEquals(ShortcutAction.DrawCard, ShortcutAction.fromId("drawCard"))
        assertEquals(ShortcutAction.Mulligan, ShortcutAction.fromId("mulligan"))
        assertEquals(ShortcutAction.UntapPhase, ShortcutAction.fromId("untapPhase"))
    }

    @Test
    fun `ShortcutAction fromId returns null for unknown id`() {
        assertNull(ShortcutAction.fromId("unknownAction"))
    }

    @Test
    fun `ShortcutAction byCategory groups actions correctly`() {
        val byCategory = ShortcutAction.byCategory

        assertTrue(byCategory.containsKey("Game Phases"))
        assertTrue(byCategory.containsKey("Card Actions"))
        assertTrue(byCategory.containsKey("Drawing & Library"))

        assertTrue(byCategory["Game Phases"]!!.contains(ShortcutAction.UntapPhase))
        assertTrue(byCategory["Card Actions"]!!.contains(ShortcutAction.TapUntapCard))
        assertTrue(byCategory["Drawing & Library"]!!.contains(ShortcutAction.DrawCard))
    }

    @Test
    fun `ShortcutAction has correct display names`() {
        assertEquals("Draw Card", ShortcutAction.DrawCard.displayName)
        assertEquals("Mulligan", ShortcutAction.Mulligan.displayName)
        assertEquals("Untap Phase", ShortcutAction.UntapPhase.displayName)
        assertEquals("Pass Turn", ShortcutAction.PassTurn.displayName)
    }

    @Test
    fun `ShortcutAction has correct categories`() {
        assertEquals("Drawing & Library", ShortcutAction.DrawCard.category)
        assertEquals("Game Phases", ShortcutAction.UntapPhase.category)
        assertEquals("Card Actions", ShortcutAction.TapUntapCard.category)
    }

    // ==================== DefaultShortcuts Reverse Lookup Tests ====================

    @Test
    fun `DefaultShortcuts getBinding returns correct binding for action`() {
        val binding = DefaultShortcuts.getBinding(ShortcutAction.DrawCard)
        assertEquals(KeyBinding(KeyBinding.KEY_D, ctrl = true), binding)
    }

    @Test
    fun `DefaultShortcuts getAllBindings returns all bindings for action with multiple bindings`() {
        // NextPhase has both Tab and Ctrl+Space
        val bindings = DefaultShortcuts.getAllBindings(ShortcutAction.NextPhase)

        assertTrue(bindings.contains(KeyBinding(KeyBinding.KEY_TAB)))
        assertTrue(bindings.contains(KeyBinding(KeyBinding.KEY_SPACE, ctrl = true)))
        assertEquals(2, bindings.size)
    }

    @Test
    fun `DefaultShortcuts getAllBindings returns single binding for action with one binding`() {
        val bindings = DefaultShortcuts.getAllBindings(ShortcutAction.Mulligan)

        assertEquals(1, bindings.size)
        assertEquals(KeyBinding(KeyBinding.KEY_M, ctrl = true), bindings[0])
    }

    // ==================== Key Code Constants Tests ====================

    @Test
    fun `KeyBinding key codes match expected values`() {
        // Function keys
        assertEquals(112, KeyBinding.KEY_F1)
        assertEquals(123, KeyBinding.KEY_F12)

        // Special keys
        assertEquals(27, KeyBinding.KEY_ESCAPE)
        assertEquals(10, KeyBinding.KEY_ENTER)
        assertEquals(32, KeyBinding.KEY_SPACE)
        assertEquals(9, KeyBinding.KEY_TAB)
        assertEquals(127, KeyBinding.KEY_DELETE)

        // Letters
        assertEquals(65, KeyBinding.KEY_A)
        assertEquals(90, KeyBinding.KEY_Z)

        // Numbers
        assertEquals(48, KeyBinding.KEY_0)
        assertEquals(57, KeyBinding.KEY_9)

        // Symbols
        assertEquals(61, KeyBinding.KEY_EQUALS)
        assertEquals(45, KeyBinding.KEY_MINUS)
        assertEquals(46, KeyBinding.KEY_PERIOD)
        assertEquals(44, KeyBinding.KEY_COMMA)
    }

    @Test
    fun `KeyBinding keyCodeToName handles all keys`() {
        assertEquals("A", KeyBinding.keyCodeToName(KeyBinding.KEY_A))
        assertEquals("Z", KeyBinding.keyCodeToName(KeyBinding.KEY_Z))
        assertEquals("0", KeyBinding.keyCodeToName(KeyBinding.KEY_0))
        assertEquals("9", KeyBinding.keyCodeToName(KeyBinding.KEY_9))
        assertEquals("F1", KeyBinding.keyCodeToName(KeyBinding.KEY_F1))
        assertEquals("F12", KeyBinding.keyCodeToName(KeyBinding.KEY_F12))
        assertEquals("Esc", KeyBinding.keyCodeToName(KeyBinding.KEY_ESCAPE))
        assertEquals("=", KeyBinding.keyCodeToName(KeyBinding.KEY_EQUALS))
        assertEquals("-", KeyBinding.keyCodeToName(KeyBinding.KEY_MINUS))
    }

    // ==================== Binding Count Tests ====================

    @Test
    fun `DefaultShortcuts has reasonable number of bindings`() {
        // We expect around 80+ bindings based on the code
        assertTrue(DefaultShortcuts.bindings.size >= 80, "Expected at least 80 bindings, got ${DefaultShortcuts.bindings.size}")
    }

    @Test
    fun `ShortcutAction all has reasonable number of actions`() {
        // Based on the code, there are many actions across categories
        assertTrue(ShortcutAction.all.size >= 90, "Expected at least 90 actions, got ${ShortcutAction.all.size}")
    }
}
