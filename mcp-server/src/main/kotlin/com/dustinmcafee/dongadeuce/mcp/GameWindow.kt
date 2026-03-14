package com.dustinmcafee.dongadeuce.mcp

import com.dustinmcafee.dongadeuce.models.*
import com.dustinmcafee.dongadeuce.viewmodel.GameUiState
import java.awt.*
import java.awt.image.BufferedImage
import javax.swing.*
import javax.swing.border.TitledBorder
import kotlin.concurrent.thread

/**
 * Swing-based game state viewer window.
 * Shows the live game state updated by MCP tool calls.
 */
class GameWindow(private val bridge: GameBridge) {
    private val frame = JFrame("DongADeuce — MCP Game Viewer")
    private val contentPanel = JPanel(BorderLayout(8, 8))

    // Player panels
    private val playerPanels = mutableMapOf<String, PlayerPanel>()
    private val statusLabel = JLabel("Waiting for game initialization...", SwingConstants.CENTER)
    private val logArea = JTextArea(8, 40)

    fun show() {
        SwingUtilities.invokeLater {
            frame.defaultCloseOperation = JFrame.DISPOSE_ON_CLOSE
            frame.minimumSize = Dimension(1200, 800)

            contentPanel.border = BorderFactory.createEmptyBorder(8, 8, 8, 8)
            contentPanel.background = Color(30, 30, 35)

            // Status bar at top
            statusLabel.font = Font("Monospaced", Font.BOLD, 16)
            statusLabel.foreground = Color(200, 200, 255)
            statusLabel.background = Color(40, 40, 50)
            statusLabel.isOpaque = true
            statusLabel.border = BorderFactory.createEmptyBorder(8, 8, 8, 8)
            contentPanel.add(statusLabel, BorderLayout.NORTH)

            // Game log at bottom
            logArea.font = Font("Monospaced", Font.PLAIN, 11)
            logArea.isEditable = false
            logArea.background = Color(20, 20, 25)
            logArea.foreground = Color(180, 180, 180)
            logArea.lineWrap = true
            logArea.wrapStyleWord = true
            val logScroll = JScrollPane(logArea)
            logScroll.preferredSize = Dimension(0, 150)
            logScroll.border = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color(80, 80, 100)),
                "Game Log", TitledBorder.LEFT, TitledBorder.TOP,
                Font("SansSerif", Font.BOLD, 12), Color(180, 180, 200)
            )
            logScroll.background = Color(20, 20, 25)
            contentPanel.add(logScroll, BorderLayout.SOUTH)

            frame.contentPane = contentPanel
            frame.pack()
            frame.setLocationRelativeTo(null)
            frame.isVisible = true

            // Start polling for state changes
            startPolling()
        }
    }

    private fun startPolling() {
        thread(isDaemon = true, name = "GameWindow-Poller") {
            var lastStateHash = 0
            while (frame.isVisible) {
                try {
                    val state = bridge.state()
                    val hash = state.hashCode()
                    if (hash != lastStateHash) {
                        lastStateHash = hash
                        SwingUtilities.invokeLater { updateDisplay(state) }
                    }
                    Thread.sleep(200)
                } catch (e: Exception) {
                    // Ignore
                }
            }
        }
    }

    private fun updateDisplay(state: GameUiState) {
        val gs = state.gameState
        if (gs == null) {
            statusLabel.text = "Waiting for game initialization..."
            return
        }

        // Update status bar
        statusLabel.text = "Turn ${gs.turnNumber} | ${gs.activePlayer.name}'s turn | Phase: ${gs.phase.name} | Players: ${gs.players.size}"

        // Update or create player panels
        val playersPanel = contentPanel.components.find { it is JPanel && (it as JPanel).name == "players" } as? JPanel
            ?: JPanel(GridLayout(1, gs.players.size, 8, 0)).also {
                it.name = "players"
                it.background = Color(30, 30, 35)
                contentPanel.add(it, BorderLayout.CENTER)
            }

        // Rebuild if player count changed
        if (playersPanel.componentCount != gs.players.size) {
            playersPanel.removeAll()
            playerPanels.clear()
            playersPanel.layout = GridLayout(1, gs.players.size, 8, 0)
            gs.players.forEach { player ->
                val panel = PlayerPanel(player.name)
                playerPanels[player.id] = panel
                playersPanel.add(panel.panel)
            }
        }

        // Update each player panel
        gs.players.forEach { player ->
            val panel = playerPanels[player.id] ?: return@forEach
            val isActive = player.id == gs.activePlayer.id

            val battlefieldCards = gs.cardInstances.filter { it.controllerId == player.id && it.zone == Zone.BATTLEFIELD }
            val handCards = gs.cardInstances.filter { it.ownerId == player.id && it.zone == Zone.HAND }
            val graveyardCards = gs.cardInstances.filter { it.ownerId == player.id && it.zone == Zone.GRAVEYARD }
            val exileCards = gs.cardInstances.filter { it.ownerId == player.id && it.zone == Zone.EXILE }
            val libraryCount = gs.cardInstances.count { it.ownerId == player.id && it.zone == Zone.LIBRARY }
            val commandZoneCards = gs.cardInstances.filter { it.ownerId == player.id && it.zone == Zone.COMMAND_ZONE }

            panel.update(player, isActive, battlefieldCards, handCards, graveyardCards, exileCards, libraryCount, commandZoneCards)
        }

        // Update log
        val events = gs.gameLog.takeLast(50)
        val logText = events.joinToString("\n") { it.toDisplayString() }
        if (logArea.text != logText) {
            logArea.text = logText
            logArea.caretPosition = logArea.document.length
        }

        contentPanel.revalidate()
        contentPanel.repaint()
    }
}

/**
 * Panel displaying one player's game state.
 */
class PlayerPanel(private val playerName: String) {
    val panel = JPanel(BorderLayout(4, 4))

    private val headerLabel = JLabel()
    private val battlefieldArea = JTextArea()
    private val handArea = JTextArea()
    private val zonesLabel = JLabel()

    init {
        panel.background = Color(40, 40, 50)
        panel.border = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color(80, 80, 100), 2),
            BorderFactory.createEmptyBorder(6, 6, 6, 6)
        )

        // Header
        headerLabel.font = Font("SansSerif", Font.BOLD, 14)
        headerLabel.foreground = Color.WHITE
        headerLabel.horizontalAlignment = SwingConstants.CENTER
        headerLabel.border = BorderFactory.createEmptyBorder(4, 4, 4, 4)
        panel.add(headerLabel, BorderLayout.NORTH)

        // Center: battlefield + hand
        val centerPanel = JPanel(BorderLayout(4, 4))
        centerPanel.background = Color(40, 40, 50)

        // Battlefield
        battlefieldArea.font = Font("Monospaced", Font.PLAIN, 11)
        battlefieldArea.isEditable = false
        battlefieldArea.background = Color(25, 35, 25)
        battlefieldArea.foreground = Color(180, 220, 180)
        battlefieldArea.lineWrap = true
        battlefieldArea.wrapStyleWord = true
        val bfScroll = JScrollPane(battlefieldArea)
        bfScroll.border = BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(Color(60, 100, 60)),
            "Battlefield", TitledBorder.LEFT, TitledBorder.TOP,
            Font("SansSerif", Font.BOLD, 11), Color(140, 200, 140)
        )
        centerPanel.add(bfScroll, BorderLayout.CENTER)

        // Hand
        handArea.font = Font("Monospaced", Font.PLAIN, 11)
        handArea.isEditable = false
        handArea.background = Color(25, 25, 40)
        handArea.foreground = Color(180, 180, 220)
        handArea.lineWrap = true
        handArea.wrapStyleWord = true
        val handScroll = JScrollPane(handArea)
        handScroll.preferredSize = Dimension(0, 120)
        handScroll.border = BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(Color(60, 60, 100)),
            "Hand", TitledBorder.LEFT, TitledBorder.TOP,
            Font("SansSerif", Font.BOLD, 11), Color(140, 140, 200)
        )
        centerPanel.add(handScroll, BorderLayout.SOUTH)

        panel.add(centerPanel, BorderLayout.CENTER)

        // Bottom: zone counts
        zonesLabel.font = Font("Monospaced", Font.PLAIN, 11)
        zonesLabel.foreground = Color(160, 160, 180)
        zonesLabel.border = BorderFactory.createEmptyBorder(4, 4, 4, 4)
        panel.add(zonesLabel, BorderLayout.SOUTH)
    }

    fun update(
        player: Player, isActive: Boolean,
        battlefield: List<CardInstance>, hand: List<CardInstance>,
        graveyard: List<CardInstance>, exile: List<CardInstance>,
        libraryCount: Int, commandZone: List<CardInstance>
    ) {
        // Header
        val activeMarker = if (isActive) " ★ ACTIVE" else ""
        val countersStr = if (player.counters.isNotEmpty()) {
            " | " + player.counters.entries.joinToString(", ") { "${it.key}: ${it.value}" }
        } else ""
        val lostStr = if (player.hasLost) " [DEFEATED]" else ""
        headerLabel.text = "${player.name}$activeMarker — Life: ${player.life}$countersStr$lostStr"
        headerLabel.background = if (isActive) Color(50, 60, 80) else Color(40, 40, 50)
        headerLabel.isOpaque = true
        panel.border = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(if (isActive) Color(100, 140, 220) else Color(80, 80, 100), 2),
            BorderFactory.createEmptyBorder(6, 6, 6, 6)
        )

        // Battlefield
        if (battlefield.isEmpty()) {
            battlefieldArea.text = "(empty)"
        } else {
            battlefieldArea.text = battlefield.joinToString("\n") { card ->
                val tapped = if (card.isTapped) " [T]" else ""
                val counters = if (card.counters.isNotEmpty()) {
                    " " + card.counters.entries.joinToString(" ") { "(${it.value} ${it.key})" }
                } else ""
                val pt = if (card.card.power != null) {
                    val p = (card.card.power?.toIntOrNull() ?: 0) + card.powerModifier
                    val t = (card.card.toughness?.toIntOrNull() ?: 0) + card.toughnessModifier
                    " [$p/$t]"
                } else ""
                val token = if (card.isToken) " •Token" else ""
                "  ${card.card.name}$pt$tapped$counters$token"
            }
        }

        // Hand
        if (hand.isEmpty()) {
            handArea.text = "(empty)"
        } else {
            handArea.text = hand.joinToString("\n") { "  ${it.card.name}" + (it.card.manaCost?.let { mc -> " $mc" } ?: "") }
        }

        // Zone counts
        val cmdStr = if (commandZone.isNotEmpty()) {
            "Cmd: " + commandZone.joinToString(", ") { it.card.name }
        } else "Cmd: -"
        zonesLabel.text = "Library: $libraryCount | Graveyard: ${graveyard.size} | Exile: ${exile.size} | $cmdStr"
    }
}
