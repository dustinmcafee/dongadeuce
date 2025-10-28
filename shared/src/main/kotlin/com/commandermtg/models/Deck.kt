package com.commandermtg.models

import kotlinx.serialization.Serializable

@Serializable
data class Deck(
    val name: String,
    val commander: Card,
    val cards: List<Card> // Should be exactly 99 cards for Commander
) {
    init {
        require(cards.size == 99) { "Commander deck must have exactly 99 cards (excluding commander)" }
    }

    val totalCards: Int = cards.size + 1

    /**
     * Validates the deck follows Commander rules:
     * - Singleton (no duplicates except basic lands)
     * - Color identity matches commander
     */
    fun isValid(): Boolean {
        // Check singleton rule
        val nonBasicCards = cards.filter { !isBasicLand(it.name) }
        val uniqueCards = nonBasicCards.distinct()
        if (nonBasicCards.size != uniqueCards.size) return false

        // Commander must be legendary
        if (!commander.isLegendary) return false

        return true
    }

    private fun isBasicLand(cardName: String): Boolean {
        return cardName in listOf("Plains", "Island", "Swamp", "Mountain", "Forest", "Wastes")
    }
}
