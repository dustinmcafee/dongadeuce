package com.dustinmcafee.dongadeuce.models

import kotlinx.serialization.Serializable

@Serializable
data class Player(
    val id: String,
    val name: String,
    val life: Int = GameConstants.STARTING_LIFE,
    val commanderDamage: Map<String, Int> = emptyMap(), // commanderId -> damage
    val counters: Map<String, Int> = emptyMap(), // counterType -> count (e.g., "poison" -> 5)
    val hasLost: Boolean = false,
    val revealTopCard: Boolean = false, // Show top card of library to all players
    val lookAtTopCard: Boolean = false  // Show top card of library only to this player
) {
    fun takeDamage(amount: Int): Player {
        val newLife = life - amount
        return copy(life = newLife)
    }

    fun takeCommanderDamage(commanderId: String, amount: Int): Player {
        val current = commanderDamage[commanderId] ?: 0
        val newDamage = current + amount
        return copy(commanderDamage = commanderDamage + (commanderId to newDamage))
    }

    fun gainLife(amount: Int): Player {
        val newLife = life + amount
        return copy(life = newLife)
    }

    fun setLife(newLife: Int): Player {
        return copy(life = newLife)
    }

    fun addCounter(counterType: String, amount: Int = 1): Player {
        val current = counters[counterType] ?: 0
        val newAmount = (current + amount).coerceAtLeast(0)
        val newCounters = if (newAmount > 0) {
            counters + (counterType to newAmount)
        } else {
            counters - counterType
        }
        return copy(counters = newCounters)
    }

    fun removeCounter(counterType: String, amount: Int = 1): Player {
        return addCounter(counterType, -amount)
    }

    fun setCounter(counterType: String, amount: Int): Player {
        val newCounters = if (amount > 0) {
            counters + (counterType to amount)
        } else {
            counters - counterType
        }
        return copy(counters = newCounters)
    }

    fun getCounter(counterType: String): Int {
        return counters[counterType] ?: 0
    }
}
