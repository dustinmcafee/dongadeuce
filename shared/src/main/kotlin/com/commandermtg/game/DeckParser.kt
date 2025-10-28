package com.commandermtg.game

import com.commandermtg.models.Card
import com.commandermtg.models.Deck
import com.commandermtg.models.GameConstants
import java.io.File

/**
 * Parses text-based deck format:
 * // Category
 * quantity cardname
 */
object DeckParser {

    fun parseTextFormat(content: String): Deck {
        val lines = content.lines()
        var commander: Card? = null
        val cards = mutableListOf<Card>()
        var currentCategory = ""

        for (line in lines) {
            val trimmed = line.trim()

            // Skip empty lines
            if (trimmed.isEmpty()) continue

            // Category comment
            if (trimmed.startsWith("//")) {
                currentCategory = trimmed.substring(2).trim()
                continue
            }

            // Parse card line: "quantity cardname"
            val parts = trimmed.split(" ", limit = 2)
            if (parts.size != 2) continue

            val quantity = parts[0].toIntOrNull() ?: continue
            val cardName = parts[1].trim()

            // Create a basic Card object (we'll fetch full data from Scryfall later)
            val card = Card(name = cardName)

            // First card in Commander category is the commander
            if (currentCategory.equals("Commander", ignoreCase = true) && commander == null) {
                commander = card
            } else {
                // Add copies based on quantity
                repeat(quantity) {
                    cards.add(card)
                }
            }
        }

        requireNotNull(commander) { "No commander found in deck" }
        require(cards.size == GameConstants.DECK_SIZE) {
            "Deck must have exactly ${GameConstants.DECK_SIZE} cards, found ${cards.size}"
        }

        return Deck(
            name = "Imported Deck",
            commander = commander,
            cards = cards
        )
    }

    fun parseTextFile(file: File): Deck {
        return parseTextFormat(file.readText())
    }

    fun parseTextFile(filePath: String): Deck {
        return parseTextFile(File(filePath))
    }
}
