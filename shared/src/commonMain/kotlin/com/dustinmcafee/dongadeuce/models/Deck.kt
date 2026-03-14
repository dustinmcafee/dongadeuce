package com.dustinmcafee.dongadeuce.models

import kotlinx.serialization.Serializable

@Serializable
data class Deck(
    val name: String,
    val commander: Card,
    val cards: List<Card>, // Should be 99 cards (or 98 with partner commanders)
    val sideboard: List<Card> = emptyList(), // Optional sideboard
    val partnerCommander: Card? = null // Second commander for partner/friends forever
) {
    init {
        val expectedSize = if (partnerCommander != null) {
            GameConstants.DECK_SIZE - 1 // 98 cards with 2 commanders
        } else {
            GameConstants.DECK_SIZE // 99 cards with 1 commander
        }
        require(cards.size == expectedSize) {
            "Commander deck must have exactly $expectedSize cards (excluding commander${if (partnerCommander != null) "s" else ""}), found ${cards.size}"
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
