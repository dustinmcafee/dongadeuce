package com.dustinmcafee.dongadeuce.models

import kotlinx.serialization.Serializable

@Serializable
data class Deck(
    val name: String,
    val commander: Card,
    val cards: List<Card> // Should be exactly 99 cards for Commander
) {
    init {
        require(cards.size == GameConstants.DECK_SIZE) {
            "Commander deck must have exactly ${GameConstants.DECK_SIZE} cards (excluding commander)"
        }
    }

    val totalCards: Int = GameConstants.TOTAL_DECK_SIZE

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
        val basicLandNames = setOf(
            "Plains", "Island", "Swamp", "Mountain", "Forest", "Wastes",
            "Snow-Covered Plains", "Snow-Covered Island", "Snow-Covered Swamp",
            "Snow-Covered Mountain", "Snow-Covered Forest"
        )
        return cardName in basicLandNames
    }
}
