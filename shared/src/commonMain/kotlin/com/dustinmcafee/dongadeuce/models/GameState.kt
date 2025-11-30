package com.dustinmcafee.dongadeuce.models

import kotlinx.serialization.Serializable

/**
 * Represents an arrow drawn between two cards or from a card to a player
 */
@Serializable
data class Arrow(
    val id: String,
    val fromPlayerId: String, // Player who drew the arrow
    val fromCardId: String?,  // Source card (null if from player)
    val toCardId: String?,    // Target card (null if to player)
    val toPlayerId: String?   // Target player (null if to card)
)

@Serializable
data class GameState(
    val gameId: String,
    val players: List<Player>,
    val cardInstances: List<CardInstance>,
    val activePlayerIndex: Int = 0,
    val turnNumber: Int = 1,
    val phase: GamePhase = GamePhase.UNTAP,
    val gameLog: List<GameEvent> = emptyList(),
    // Arrows: List of (fromPlayerId, fromCardId, toPlayerId, toCardId)
    val arrows: List<Arrow> = emptyList()
) {
    val activePlayer: Player
        get() {
            // Coerce activePlayerIndex to valid range to prevent crashes
            val validIndex = activePlayerIndex.coerceIn(0, players.size - 1)
            return players.getOrNull(validIndex)
                ?: throw IllegalStateException("No players in game (empty players list)")
        }

    fun getPlayerCards(playerId: String, zone: Zone? = null): List<CardInstance> {
        return cardInstances.filter {
            it.ownerId == playerId && (zone == null || it.zone == zone)
        }
    }

    /**
     * Compute grid positions for battlefield cards controlled by a specific player.
     * Cards with explicit gridX/gridY use those positions.
     * Cards without positions are auto-assigned based on card type:
     * - Row 0 (bottom): Lands
     * - Row 1 (middle): Artifacts, Enchantments
     * - Row 2 (top): Creatures, Planeswalkers
     * Returns a map of cardInstanceId -> (col, row)
     */
    fun computeBattlefieldPositions(controllerId: String): Map<String, Pair<Int, Int>> {
        val battlefieldCards = cardInstances.filter { it.zone == Zone.BATTLEFIELD && it.controllerId == controllerId }
        val positionMap = mutableMapOf<String, Pair<Int, Int>>()
        val positionCounts = mutableMapOf<Pair<Int, Int>, Int>()

        // First pass: place cards with explicit positions
        battlefieldCards.forEach { card ->
            if (card.gridX != null && card.gridY != null) {
                val pos = Pair(card.gridX, card.gridY)
                positionMap[card.instanceId] = pos
                positionCounts[pos] = (positionCounts[pos] ?: 0) + 1
            }
        }

        // Second pass: auto-assign positions to cards without explicit positions
        // Use type-based row assignment and find available column
        battlefieldCards.forEach { card ->
            if (card.gridX == null || card.gridY == null) {
                val targetRow = card.getTargetBattlefieldRow()
                // Find first available column in the target row (max 3 cards per position)
                var foundPos: Pair<Int, Int>? = null
                var col = 0
                while (foundPos == null) {
                    val pos = Pair(col, targetRow)
                    val count = positionCounts[pos] ?: 0
                    if (count < 3) {
                        foundPos = pos
                        positionCounts[pos] = count + 1
                    } else {
                        col++
                        // Safety limit to prevent infinite loop
                        if (col > 100) {
                            foundPos = Pair(0, targetRow)
                            break
                        }
                    }
                }
                positionMap[card.instanceId] = foundPos ?: Pair(0, targetRow)
            }
        }

        return positionMap
    }

    /**
     * Find the next available grid position on a player's battlefield for a card.
     * Uses type-based row assignment (max 3 cards per position).
     * Optionally exclude a card (useful when moving a card).
     */
    fun findNextGridPosition(controllerId: String, card: CardInstance, excludeCardId: String? = null): Pair<Int, Int> {
        val positions = computeBattlefieldPositions(controllerId)
        val positionCounts = mutableMapOf<Pair<Int, Int>, Int>()

        positions.forEach { (cardId, pos) ->
            if (cardId != excludeCardId) {
                positionCounts[pos] = (positionCounts[pos] ?: 0) + 1
            }
        }

        val targetRow = card.getTargetBattlefieldRow()
        var col = 0
        while (true) {
            val pos = Pair(col, targetRow)
            val count = positionCounts[pos] ?: 0
            if (count < 3) {
                return pos
            }
            col++
            // Safety limit
            if (col > 100) break
        }
        return Pair(0, targetRow) // Fallback
    }

    /**
     * Legacy findNextGridPosition for cases where card is not available.
     * @deprecated Use findNextGridPosition(controllerId, card, excludeCardId) instead
     */
    fun findNextGridPosition(controllerId: String, excludeCardId: String? = null): Pair<Int, Int> {
        val positions = computeBattlefieldPositions(controllerId)
        val positionCounts = mutableMapOf<Pair<Int, Int>, Int>()

        positions.forEach { (cardId, pos) ->
            if (cardId != excludeCardId) {
                positionCounts[pos] = (positionCounts[pos] ?: 0) + 1
            }
        }

        // Default to middle row (row 1) for legacy calls
        val targetRow = 1
        var col = 0
        while (true) {
            val pos = Pair(col, targetRow)
            val count = positionCounts[pos] ?: 0
            if (count < 3) {
                return pos
            }
            col++
            if (col > 100) break
        }
        return Pair(0, targetRow)
    }

    /**
     * Get cards controlled by a player on the battlefield
     * For battlefield, we filter by controllerId (not ownerId) because control can change
     * For other zones, use getPlayerCards() which filters by ownerId
     */
    fun getPlayerBattlefield(playerId: String): List<CardInstance> {
        return cardInstances.filter {
            it.controllerId == playerId && it.zone == Zone.BATTLEFIELD
        }
    }

    fun updateCardInstance(instanceId: String, update: (CardInstance) -> CardInstance): GameState {
        return copy(
            cardInstances = cardInstances.map {
                if (it.instanceId == instanceId) update(it) else it
            }
        )
    }

    fun updatePlayer(playerId: String, update: (Player) -> Player): GameState {
        return copy(
            players = players.map {
                if (it.id == playerId) update(it) else it
            }
        )
    }

    fun nextPhase(): GameState {
        val nextPhase = phase.next()
        return if (nextPhase == GamePhase.UNTAP) {
            // New turn
            copy(
                phase = nextPhase,
                activePlayerIndex = (activePlayerIndex + 1) % players.size,
                turnNumber = turnNumber + 1
            )
        } else {
            copy(phase = nextPhase)
        }
    }

    /**
     * Add an event to the game log
     */
    fun addEvent(event: GameEvent): GameState {
        return copy(gameLog = gameLog + event)
    }

    /**
     * Add multiple events to the game log
     */
    fun addEvents(events: List<GameEvent>): GameState {
        return copy(gameLog = gameLog + events)
    }
}

@Serializable
enum class GamePhase {
    UNTAP,
    UPKEEP,
    DRAW,
    MAIN_1,
    COMBAT_BEGIN,
    COMBAT_DECLARE_ATTACKERS,
    COMBAT_DECLARE_BLOCKERS,
    COMBAT_DAMAGE,
    COMBAT_END,
    MAIN_2,
    END,
    CLEANUP;

    fun next(): GamePhase {
        val values = values()
        val nextIndex = (ordinal + 1) % values.size
        return values[nextIndex]
    }
}
