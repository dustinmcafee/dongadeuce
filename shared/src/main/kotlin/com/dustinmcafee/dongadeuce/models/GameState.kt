package com.dustinmcafee.dongadeuce.models

import kotlinx.serialization.Serializable

@Serializable
data class GameState(
    val gameId: String,
    val players: List<Player>,
    val cardInstances: List<CardInstance>,
    val activePlayerIndex: Int = 0,
    val turnNumber: Int = 1,
    val phase: GamePhase = GamePhase.UNTAP
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
