package com.dustinmcafee.dongadeuce.models

import kotlinx.serialization.Serializable

@Serializable
data class Player(
    val id: String,
    val name: String,
    val life: Int = GameConstants.STARTING_LIFE,
    val commanderDamage: Map<String, Int> = emptyMap(), // commanderId -> damage
    val hasLost: Boolean = false
) {
    fun takeDamage(amount: Int): Player {
        val newLife = life - amount
        return copy(
            life = newLife,
            hasLost = hasLost || newLife <= 0
        )
    }

    fun takeCommanderDamage(commanderId: String, amount: Int): Player {
        val current = commanderDamage[commanderId] ?: 0
        val newDamage = current + amount
        return copy(
            commanderDamage = commanderDamage + (commanderId to newDamage),
            hasLost = hasLost || newDamage >= GameConstants.COMMANDER_DAMAGE_THRESHOLD
        )
    }

    fun gainLife(amount: Int): Player {
        val newLife = life + amount
        return copy(life = newLife)
    }

    fun setLife(newLife: Int): Player {
        return copy(
            life = newLife,
            hasLost = hasLost || newLife <= 0
        )
    }
}
