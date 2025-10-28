package com.commandermtg.models

import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Represents a specific instance of a card in the game
 * Each physical card gets a unique ID to track it across zones
 */
@Serializable
data class CardInstance(
    val instanceId: String = UUID.randomUUID().toString(),
    val card: Card,
    val ownerId: String,
    val controllerId: String = ownerId,
    val zone: Zone,
    val isTapped: Boolean = false,
    val isFlipped: Boolean = false,
    val isFaceDown: Boolean = false,
    val counters: Map<String, Int> = emptyMap(), // e.g., "+1/+1" -> 3
    val attachedTo: String? = null, // instanceId of card this is attached to
    val gridX: Int? = null, // Grid X position on battlefield (null = auto-arrange)
    val gridY: Int? = null  // Grid Y position on battlefield (null = auto-arrange)
) {
    fun tap() = copy(isTapped = true)
    fun untap() = copy(isTapped = false)
    fun flip() = copy(isFlipped = !isFlipped)
    fun moveToZone(newZone: Zone) = copy(zone = newZone)
    fun addCounter(counterType: String, amount: Int = 1): CardInstance {
        val current = counters[counterType] ?: 0
        return copy(counters = counters + (counterType to current + amount))
    }
    fun setGridPosition(x: Int, y: Int) = copy(gridX = x, gridY = y)
}
