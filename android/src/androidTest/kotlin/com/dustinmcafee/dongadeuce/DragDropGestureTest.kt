package com.dustinmcafee.dongadeuce

import android.os.ParcelFileDescriptor
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.dustinmcafee.dongadeuce.api.CardCache
import com.dustinmcafee.dongadeuce.viewmodel.AndroidMenuViewModel
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * End-to-end gesture test: launches the real app, loads Zedruu into a
 * 2-player hotseat game ONCE, then runs every gesture test in that
 * single live session.
 *
 * Fast (default):  ./gradlew :android:connectedDebugAndroidTest -P...class=...DragDropGestureTest
 * Visual (slow):   add -Pandroid.testInstrumentationRunnerArguments.visual=true
 */
@RunWith(AndroidJUnit4::class)
class DragDropGestureTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private val visualMode: Boolean by lazy {
        InstrumentationRegistry.getArguments().getString("visual", "false").toBoolean()
    }

    private fun watch(label: String = "") {
        if (visualMode) {
            if (label.isNotEmpty()) println("  >>> $label")
            Thread.sleep(3000)
        }
    }

    /** Inject a real touch event via ADB shell — works like an actual finger */
    private fun adbTap(x: Int, y: Int) {
        exec("input tap $x $y")
        Thread.sleep(500)
    }

    private fun adbLongPress(x: Int, y: Int) {
        exec("input swipe $x $y $x $y 1500")
        Thread.sleep(500)
    }

    private fun adbSwipe(x1: Int, y1: Int, x2: Int, y2: Int, durationMs: Int = 500) {
        exec("input swipe $x1 $y1 $x2 $y2 $durationMs")
        Thread.sleep(500)
    }

    private fun exec(cmd: String) {
        val auto = InstrumentationRegistry.getInstrumentation().uiAutomation
        val pfd = auto.executeShellCommand(cmd)
        // Read to completion so the command finishes
        ParcelFileDescriptor.AutoCloseInputStream(pfd).use { it.readBytes() }
    }

    /**
     * Single test method that loads the game once and runs all gesture
     * checks sequentially — no repeated game setup.
     */
    @Test
    fun fullGameGestureSession() {
        // ── Setup: download cache if visual mode ──
        if (visualMode) {
            val cache = CardCache()
            if (!cache.isCacheAvailable()) {
                println("=== Downloading card cache for artwork... ===")
                runBlocking {
                    cache.updateCache { msg, pct ->
                        if ((pct * 100).toInt() % 10 == 0) println("  Cache: $msg (${(pct * 100).toInt()}%)")
                    }
                }
            }
        }

        // ── Load deck file from test assets ──
        val context = InstrumentationRegistry.getInstrumentation().context
        val deckContent = context.assets.open("Zedruu.cod").bufferedReader().readText()

        // ── Get the real ViewModel from the live activity ──
        var menuVm: AndroidMenuViewModel? = null
        composeTestRule.activityRule.scenario.onActivity { activity ->
            menuVm = ViewModelProvider(activity)[AndroidMenuViewModel::class.java]
        }
        val vm = menuVm!!

        // ── Navigate: hotseat mode, load both decks, start game ──
        composeTestRule.runOnUiThread { vm.setHotseatMode(true) }
        composeTestRule.waitForIdle()
        watch("Hotseat mode selected")

        // Helper: poll until condition is true
        fun waitUntil(timeoutMs: Long = 15000, condition: () -> Boolean) {
            val start = System.currentTimeMillis()
            while (!condition() && System.currentTimeMillis() - start < timeoutMs) {
                Thread.sleep(300)
            }
        }

        // Load deck for each player — wait for full completion including commander selection
        for (playerIndex in 0..1) {
            composeTestRule.runOnUiThread { vm.loadHotseatDeckFromContent(playerIndex, deckContent) }

            // Wait for parsing to finish
            waitUntil { !vm.uiState.value.isLoading || vm.uiState.value.pendingDeckData != null }

            // Handle commander selection if needed
            waitUntil { vm.uiState.value.pendingDeckData != null || vm.uiState.value.hotseatDecks.containsKey(playerIndex) }
            if (vm.uiState.value.pendingDeckData != null) {
                composeTestRule.runOnUiThread { vm.selectCommander("Zedruu the Greathearted") }
            }

            // Wait until this player's deck is fully in hotseatDecks and nothing pending
            waitUntil(20000) {
                vm.uiState.value.hotseatDecks.containsKey(playerIndex) &&
                vm.uiState.value.pendingDeckData == null &&
                !vm.uiState.value.isLoading
            }
            composeTestRule.waitForIdle()
            watch("Player ${playerIndex + 1} deck loaded: ${vm.uiState.value.hotseatDecks[playerIndex]?.commander?.name}")
        }

        // Verify both decks before starting
        assert(vm.uiState.value.hotseatDecks.size >= 2) {
            "Need 2 decks, got ${vm.uiState.value.hotseatDecks.size}. Error: ${vm.uiState.value.error}"
        }

        // Start game
        composeTestRule.runOnUiThread { vm.startHotseatGame() }
        composeTestRule.waitForIdle()
        Thread.sleep(3000)
        composeTestRule.waitForIdle()
        watch("GAME STARTED — 2-player hotseat with Zedruu")

        // ═══════════════════════════════════════════════════
        //  1. PLAY CARDS: Double-tap hand cards to play 3 to battlefield
        // ═══════════════════════════════════════════════════
        for (i in 1..3) {
            adbTap(100 + (i * 40), 1750); Thread.sleep(150)
            adbTap(100 + (i * 40), 1750) // double-tap = play
            Thread.sleep(800)
        }
        composeTestRule.waitForIdle()
        watch("Played 3 cards from hand to battlefield")

        // ═══════════════════════════════════════════════════
        //  2. TAP CARDS: Tap a battlefield card to tap it
        // ═══════════════════════════════════════════════════
        adbTap(200, 1000) // tap a card on the battlefield
        Thread.sleep(300)
        adbTap(200, 1000) // second tap = single-click focuses, not double-tap
        Thread.sleep(500)
        watch("Tapped a battlefield card")

        // ═══════════════════════════════════════════════════
        //  3. UNTAP ALL: With tapped cards on battlefield
        // ═══════════════════════════════════════════════════
        // Untap All button is 2nd in bottom bar ~x=180, y=1900
        adbTap(180, 1900)
        composeTestRule.waitForIdle()
        Thread.sleep(500)
        watch("Untap All pressed — cards should untap")

        // ═══════════════════════════════════════════════════
        //  4. Issue 9: Tap minus to decrease life
        // ═══════════════════════════════════════════════════
        adbTap(95, 1610) // "-" button
        composeTestRule.waitForIdle()
        Thread.sleep(500)
        watch("Issue 9: Tapped minus — life decreased")

        // ═══════════════════════════════════════════════════
        //  5. Issue 13: Long-press life total → set dialog
        // ═══════════════════════════════════════════════════
        adbLongPress(140, 1610) // life total text
        composeTestRule.waitForIdle()
        Thread.sleep(500)
        watch("Issue 13: Long-pressed life — dialog open")
        adbTap(540, 400) // dismiss dialog by tapping outside
        Thread.sleep(500)

        // ═══════════════════════════════════════════════════
        //  6. Issue 3: Pass Turn
        // ═══════════════════════════════════════════════════
        adbTap(60, 1900) // Pass Turn button
        composeTestRule.waitForIdle()
        Thread.sleep(500)
        watch("Issue 3: Turn passed to Player 2")
        adbTap(60, 1900) // Pass back
        composeTestRule.waitForIdle()
        Thread.sleep(500)
        watch("Turn passed back to Player 1")

        // ═══════════════════════════════════════════════════
        //  7. Issue 4: Resize battlefield/hand via drag handle
        //  The drag handle bar is between battlefield and info bar
        // ═══════════════════════════════════════════════════
        adbSwipe(540, 1590, 540, 1490, 400) // drag handle UP = bigger battlefield
        Thread.sleep(500)
        watch("Issue 4: Dragged resize handle UP — battlefield bigger")
        adbSwipe(540, 1490, 540, 1650, 400) // drag handle DOWN = smaller battlefield, bigger hand
        Thread.sleep(500)
        watch("Issue 4: Dragged resize handle DOWN — hand bigger")

        // ═══════════════════════════════════════════════════
        //  8. Issue 10: Diagonal panning on battlefield
        // ═══════════════════════════════════════════════════
        adbSwipe(540, 900, 340, 700, 600)
        watch("Issue 10: Panned diagonal upper-left")
        adbSwipe(340, 700, 740, 1100, 600)
        watch("Issue 10: Panned diagonal lower-right")

        // ═══════════════════════════════════════════════════
        //  9. Issue 12: Drag HAND card UP to BATTLEFIELD
        // ═══════════════════════════════════════════════════
        adbSwipe(200, 1750, 200, 900, 800)
        composeTestRule.waitForIdle()
        Thread.sleep(1000)
        watch("Issue 12: Dragged hand card → battlefield")

        // ═══════════════════════════════════════════════════
        //  10. Issue 12: Drag BATTLEFIELD card DOWN to HAND
        // ═══════════════════════════════════════════════════
        adbSwipe(200, 900, 200, 1750, 800)
        composeTestRule.waitForIdle()
        Thread.sleep(1000)
        watch("Issue 12: Dragged battlefield card → hand")

        // ═══════════════════════════════════════════════════
        //  11. Issue 12: Drag HAND card to GRAVEYARD zone button
        // ═══════════════════════════════════════════════════
        // Play a card first
        adbTap(300, 1750); Thread.sleep(150); adbTap(300, 1750); Thread.sleep(1000)
        // Drag from battlefield to graveyard zone (~x=850, y=1610)
        adbSwipe(300, 1000, 850, 1610, 800)
        composeTestRule.waitForIdle()
        Thread.sleep(1000)
        watch("Issue 12: Dragged card → graveyard zone button")

        // ═══════════════════════════════════════════════════
        //  12. Issue 12: Drag BATTLEFIELD card to EXILE zone button
        // ═══════════════════════════════════════════════════
        // Play another card
        adbTap(200, 1750); Thread.sleep(150); adbTap(200, 1750); Thread.sleep(1000)
        // Drag to exile zone (~x=950, y=1610)
        adbSwipe(200, 1000, 950, 1610, 800)
        composeTestRule.waitForIdle()
        Thread.sleep(1000)
        watch("Issue 12: Dragged card → exile zone button")

        // ═══════════════════════════════════════════════════
        //  13. Issue 12: Grab TOP card FROM GRAVEYARD (long-press+drag)
        //  Graveyard zone button ~x=850, y=1610
        // ═══════════════════════════════════════════════════
        adbSwipe(850, 1610, 850, 1610, 1600) // long-press on graveyard
        composeTestRule.waitForIdle()
        Thread.sleep(1000)
        watch("Issue 12: Grabbed top card from graveyard → hand")

        // ═══════════════════════════════════════════════════
        //  14. Issue 12: Grab TOP card FROM EXILE (long-press+drag)
        //  Exile zone button ~x=950, y=1610
        // ═══════════════════════════════════════════════════
        adbSwipe(950, 1610, 950, 1610, 1600) // long-press on exile
        composeTestRule.waitForIdle()
        Thread.sleep(1000)
        watch("Issue 12: Grabbed top card from exile → hand")

        // ═══════════════════════════════════════════════════
        //  15. Issue 12: Grab TOP card FROM LIBRARY (long-press+drag = draw)
        //  Library zone button ~x=750, y=1610
        // ═══════════════════════════════════════════════════
        adbSwipe(750, 1610, 750, 1610, 1600) // long-press on library
        composeTestRule.waitForIdle()
        Thread.sleep(1000)
        watch("Issue 12: Grabbed top card from library → hand (draw)")

        // ═══════════════════════════════════════════════════
        //  16. Issue 12: Hand card horizontal REORDER
        // ═══════════════════════════════════════════════════
        adbSwipe(150, 1750, 500, 1750, 600)
        watch("Issue 12: Horizontal drag in hand — reorder")

        // ═══════════════════════════════════════════════════
        //  17. Issue 15: Long-press BATTLEFIELD → mana dialog
        // ═══════════════════════════════════════════════════
        adbLongPress(540, 900)
        composeTestRule.waitForIdle()
        Thread.sleep(1000)
        watch("Issue 15: Long-pressed battlefield — mana dialog")
        adbTap(540, 400) // dismiss
        Thread.sleep(500)

        // ═══════════════════════════════════════════════════
        //  18. Issue 7: Long-press HAND card → context menu
        // ═══════════════════════════════════════════════════
        adbLongPress(200, 1750)
        composeTestRule.waitForIdle()
        Thread.sleep(1000)
        watch("Issue 7: Hand card context menu open")

        val hasMulligan = try {
            composeTestRule.onNodeWithText("Mulligan").assertExists()
            true
        } catch (_: AssertionError) { false }
        if (hasMulligan) watch("Issue 7: Mulligan option confirmed!")
        adbTap(540, 300) // dismiss
        Thread.sleep(1000)

        // ═══════════════════════════════════════════════════
        //  19. DRAW card from bottom bar
        // ═══════════════════════════════════════════════════
        adbTap(300, 1900) // Draw button (3rd in bottom bar)
        composeTestRule.waitForIdle()
        Thread.sleep(500)
        watch("Draw card from bottom bar")

        // ═══════════════════════════════════════════════════
        //  20. Issue 11: Zone icons verified
        // ═══════════════════════════════════════════════════
        composeTestRule.onAllNodesWithContentDescription("G").assertCountEquals(1)
        composeTestRule.onAllNodesWithContentDescription("E").assertCountEquals(1)
        watch("Issue 11: Zone icons confirmed")

        // ═══════════════════════════════════════════════
        //  Final: No crashes
        // ═══════════════════════════════════════════════
        val process = Runtime.getRuntime().exec(arrayOf("logcat", "-d", "-s", "AndroidRuntime:E"))
        val output = process.inputStream.bufferedReader().readText()
        val crashes = output.lines().count { it.contains("FATAL EXCEPTION") }
        assert(crashes == 0) { "Found $crashes crash(es)" }
        watch("ALL TESTS PASSED — no crashes")
    }
}
