package com.commandermtg.models

import kotlinx.serialization.Serializable

@Serializable
data class Player(
    val id: String,
    val name: String,
    val life: Int = 40,
    val commanderDamage: Map<String, Int> = emptyMap(), // commanderId -> damage
    val hasLost: Boolean = false
) {
    fun takeDamage(amount: Int) = copy(life = life - amount)

    fun takeCommanderDamage(commanderId: String, amount: Int): Player {
        val current = commanderDamage[commanderId] ?: 0
        val newDamage = current + amount
        return copy(
            commanderDamage = commanderDamage + (commanderId to newDamage),
            hasLost = hasLost || newDamage >= 21
        )
    }

    fun gainLife(amount: Int) = copy(life = life + amount)

    fun setLife(newLife: Int) = copy(life = newLife)
}
