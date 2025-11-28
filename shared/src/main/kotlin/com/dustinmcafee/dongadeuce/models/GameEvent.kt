package com.dustinmcafee.dongadeuce.models

import kotlinx.serialization.Serializable

/**
 * Represents a game event for the game log/history system
 */
@Serializable
sealed class GameEvent {
    abstract val timestamp: Long
    abstract val playerId: String
    abstract val playerName: String

    /**
     * Card was drawn from library
     */
    @Serializable
    data class CardDrawn(
        override val timestamp: Long = System.currentTimeMillis(),
        override val playerId: String,
        override val playerName: String,
        val cardName: String
    ) : GameEvent()

    /**
     * Card was played to battlefield
     */
    @Serializable
    data class CardPlayed(
        override val timestamp: Long = System.currentTimeMillis(),
        override val playerId: String,
        override val playerName: String,
        val cardName: String,
        val fromZone: Zone
    ) : GameEvent()

    /**
     * Card was moved between zones
     */
    @Serializable
    data class CardMoved(
        override val timestamp: Long = System.currentTimeMillis(),
        override val playerId: String,
        override val playerName: String,
        val cardName: String,
        val fromZone: Zone,
        val toZone: Zone
    ) : GameEvent()

    /**
     * Player's life total changed
     */
    @Serializable
    data class LifeChanged(
        override val timestamp: Long = System.currentTimeMillis(),
        override val playerId: String,
        override val playerName: String,
        val oldLife: Int,
        val newLife: Int,
        val change: Int = newLife - oldLife
    ) : GameEvent()

    /**
     * Commander damage was dealt
     */
    @Serializable
    data class CommanderDamageDealt(
        override val timestamp: Long = System.currentTimeMillis(),
        override val playerId: String,
        override val playerName: String,
        val sourceCommanderName: String,
        val targetPlayerName: String,
        val damage: Int,
        val totalDamage: Int
    ) : GameEvent()

    /**
     * Game phase changed
     */
    @Serializable
    data class PhaseChanged(
        override val timestamp: Long = System.currentTimeMillis(),
        override val playerId: String,
        override val playerName: String,
        val newPhase: GamePhase
    ) : GameEvent()

    /**
     * Turn passed to next player
     */
    @Serializable
    data class TurnPassed(
        override val timestamp: Long = System.currentTimeMillis(),
        override val playerId: String,
        override val playerName: String,
        val toPlayerId: String,
        val toPlayerName: String,
        val turnNumber: Int
    ) : GameEvent()

    /**
     * Counter added/removed from a card
     */
    @Serializable
    data class CardCounterChanged(
        override val timestamp: Long = System.currentTimeMillis(),
        override val playerId: String,
        override val playerName: String,
        val cardName: String,
        val counterType: String,
        val oldAmount: Int,
        val newAmount: Int
    ) : GameEvent()

    /**
     * Player counter changed (poison, energy, experience)
     */
    @Serializable
    data class PlayerCounterChanged(
        override val timestamp: Long = System.currentTimeMillis(),
        override val playerId: String,
        override val playerName: String,
        val counterType: String,
        val oldAmount: Int,
        val newAmount: Int
    ) : GameEvent()

    /**
     * Card was tapped or untapped
     */
    @Serializable
    data class CardTapped(
        override val timestamp: Long = System.currentTimeMillis(),
        override val playerId: String,
        override val playerName: String,
        val cardName: String,
        val isTapped: Boolean
    ) : GameEvent()

    /**
     * Player untapped all their permanents
     */
    @Serializable
    data class UntapAll(
        override val timestamp: Long = System.currentTimeMillis(),
        override val playerId: String,
        override val playerName: String,
        val cardCount: Int
    ) : GameEvent()

    /**
     * Token was created
     */
    @Serializable
    data class TokenCreated(
        override val timestamp: Long = System.currentTimeMillis(),
        override val playerId: String,
        override val playerName: String,
        val tokenName: String,
        val quantity: Int
    ) : GameEvent()

    /**
     * Card was cloned/copied
     */
    @Serializable
    data class CardCloned(
        override val timestamp: Long = System.currentTimeMillis(),
        override val playerId: String,
        override val playerName: String,
        val cardName: String,
        val quantity: Int
    ) : GameEvent()

    /**
     * Player lost the game
     */
    @Serializable
    data class PlayerLost(
        override val timestamp: Long = System.currentTimeMillis(),
        override val playerId: String,
        override val playerName: String,
        val reason: String
    ) : GameEvent()

    /**
     * Game started
     */
    @Serializable
    data class GameStarted(
        override val timestamp: Long = System.currentTimeMillis(),
        override val playerId: String = "",
        override val playerName: String = "System",
        val playerNames: List<String>,
        val playerCount: Int
    ) : GameEvent()

    /**
     * Die was rolled
     */
    @Serializable
    data class DieRolled(
        override val timestamp: Long = System.currentTimeMillis(),
        override val playerId: String,
        override val playerName: String,
        val dieType: String,
        val result: Int,
        val numberOfDice: Int = 1
    ) : GameEvent()

    /**
     * Card control was given to another player
     */
    @Serializable
    data class ControlChanged(
        override val timestamp: Long = System.currentTimeMillis(),
        override val playerId: String,
        override val playerName: String,
        val cardName: String,
        val toPlayerName: String
    ) : GameEvent()

    /**
     * Cards were milled from library
     */
    @Serializable
    data class CardsMilled(
        override val timestamp: Long = System.currentTimeMillis(),
        override val playerId: String,
        override val playerName: String,
        val cardCount: Int
    ) : GameEvent()

    /**
     * Library was shuffled
     */
    @Serializable
    data class LibraryShuffled(
        override val timestamp: Long = System.currentTimeMillis(),
        override val playerId: String,
        override val playerName: String
    ) : GameEvent()

    /**
     * Mulligan was taken
     */
    @Serializable
    data class MulliganTaken(
        override val timestamp: Long = System.currentTimeMillis(),
        override val playerId: String,
        override val playerName: String,
        val newHandSize: Int
    ) : GameEvent()

    /**
     * Player sent a chat message
     */
    @Serializable
    data class ChatMessage(
        override val timestamp: Long = System.currentTimeMillis(),
        override val playerId: String,
        override val playerName: String,
        val message: String
    ) : GameEvent()

    /**
     * Generic action for miscellaneous game events
     */
    @Serializable
    data class GenericAction(
        override val timestamp: Long = System.currentTimeMillis(),
        override val playerId: String,
        override val playerName: String,
        val description: String
    ) : GameEvent()
}

/**
 * Helper function to format a GameEvent for display
 */
fun GameEvent.toDisplayString(): String {
    return when (this) {
        is GameEvent.CardDrawn -> "$playerName drew $cardName"
        is GameEvent.CardPlayed -> "$playerName played $cardName to the battlefield"
        is GameEvent.CardMoved -> "$playerName moved $cardName from ${fromZone.displayName} to ${toZone.displayName}"
        is GameEvent.LifeChanged -> {
            val changeStr = if (change >= 0) "+$change" else "$change"
            "$playerName's life: $oldLife → $newLife ($changeStr)"
        }
        is GameEvent.CommanderDamageDealt -> "$playerName dealt $damage commander damage to $targetPlayerName with $sourceCommanderName (total: $totalDamage)"
        is GameEvent.PhaseChanged -> "$playerName moved to ${newPhase.displayName}"
        is GameEvent.TurnPassed -> "Turn $turnNumber: $toPlayerName's turn"
        is GameEvent.CardCounterChanged -> {
            val change = newAmount - oldAmount
            val changeStr = if (change >= 0) "+$change" else "$change"
            "$playerName: $cardName now has $newAmount $counterType counters ($changeStr)"
        }
        is GameEvent.PlayerCounterChanged -> {
            val change = newAmount - oldAmount
            val changeStr = if (change >= 0) "+$change" else "$change"
            "$playerName now has $newAmount $counterType counters ($changeStr)"
        }
        is GameEvent.CardTapped -> if (isTapped) "$playerName tapped $cardName" else "$playerName untapped $cardName"
        is GameEvent.UntapAll -> "$playerName untapped $cardCount permanents"
        is GameEvent.TokenCreated -> "$playerName created $quantity $tokenName token(s)"
        is GameEvent.CardCloned -> "$playerName created $quantity copy/copies of $cardName"
        is GameEvent.PlayerLost -> "$playerName has lost the game ($reason)"
        is GameEvent.GameStarted -> "Game started with $playerCount players: ${playerNames.joinToString(", ")}"
        is GameEvent.DieRolled -> if (numberOfDice == 1) "$playerName rolled $dieType: $result" else "$playerName rolled ${numberOfDice}x $dieType: $result"
        is GameEvent.ControlChanged -> "$playerName gave control of $cardName to $toPlayerName"
        is GameEvent.CardsMilled -> "$playerName milled $cardCount card(s)"
        is GameEvent.LibraryShuffled -> "$playerName shuffled their library"
        is GameEvent.MulliganTaken -> "$playerName took a mulligan (new hand size: $newHandSize)"
        is GameEvent.ChatMessage -> "$playerName: $message"
        is GameEvent.GenericAction -> "$playerName $description"
    }
}

/**
 * Extension property for Zone display name
 */
val Zone.displayName: String
    get() = when (this) {
        Zone.LIBRARY -> "Library"
        Zone.HAND -> "Hand"
        Zone.BATTLEFIELD -> "Battlefield"
        Zone.GRAVEYARD -> "Graveyard"
        Zone.EXILE -> "Exile"
        Zone.COMMAND_ZONE -> "Command Zone"
        Zone.STACK -> "Stack"
    }

/**
 * Extension property for GamePhase display name
 */
val GamePhase.displayName: String
    get() = when (this) {
        GamePhase.UNTAP -> "Untap"
        GamePhase.UPKEEP -> "Upkeep"
        GamePhase.DRAW -> "Draw"
        GamePhase.MAIN_1 -> "Main Phase 1"
        GamePhase.COMBAT_BEGIN -> "Beginning of Combat"
        GamePhase.COMBAT_DECLARE_ATTACKERS -> "Declare Attackers"
        GamePhase.COMBAT_DECLARE_BLOCKERS -> "Declare Blockers"
        GamePhase.COMBAT_DAMAGE -> "Combat Damage"
        GamePhase.COMBAT_END -> "End of Combat"
        GamePhase.MAIN_2 -> "Main Phase 2"
        GamePhase.END -> "End Step"
        GamePhase.CLEANUP -> "Cleanup"
    }
