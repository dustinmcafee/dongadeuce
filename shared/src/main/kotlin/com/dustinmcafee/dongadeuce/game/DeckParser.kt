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
        require(content.isNotBlank()) { "Deck file content cannot be empty" }

        val lines = content.lines()
        var commander: Card? = null
        val cards = mutableListOf<Card>()
        var currentCategory = ""
        var lineNumber = 0

        for (line in lines) {
            lineNumber++
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
            if (parts.size != 2) {
                // Skip malformed lines silently (could be comments or headers)
                continue
            }

            val quantity = parts[0].toIntOrNull()
            if (quantity == null || quantity <= 0) {
                throw IllegalArgumentException("Invalid quantity on line $lineNumber: '${parts[0]}' must be a positive number")
            }

            val cardName = parts[1].trim()
            if (cardName.isEmpty()) {
                throw IllegalArgumentException("Card name cannot be empty on line $lineNumber")
            }

            // Create a basic Card object (we'll fetch full data from Scryfall later)
            val card = Card(name = cardName)

            // First card in Commander category is the commander
            if (currentCategory.equals("Commander", ignoreCase = true) && commander == null) {
                require(quantity == 1) { "Commander must have quantity of 1, found $quantity on line $lineNumber" }
                commander = card
            } else {
                // Add copies based on quantity
                repeat(quantity) {
                    cards.add(card)
                }
            }
        }

        requireNotNull(commander) { "No commander found in deck. Ensure there is a '// Commander' section with exactly one card." }
        require(cards.size == GameConstants.DECK_SIZE) {
            "Deck must have exactly ${GameConstants.DECK_SIZE} cards (excluding commander), found ${cards.size}"
        }

        return Deck(
            name = "Imported Deck",
            commander = commander,
            cards = cards
        )
    }

    fun parseTextFile(file: File): Deck {
        require(file.exists()) { "Deck file does not exist: ${file.absolutePath}" }
        require(file.isFile) { "Path is not a file: ${file.absolutePath}" }
        require(file.canRead()) { "Cannot read deck file: ${file.absolutePath}" }

        val content = try {
            file.readText()
        } catch (e: Exception) {
            throw IllegalArgumentException("Failed to read deck file: ${file.absolutePath}", e)
        }

        return parseTextFormat(content)
    }

    fun parseTextFile(filePath: String): Deck {
        require(filePath.isNotBlank()) { "File path cannot be empty" }
        return parseTextFile(File(filePath))
    }
}
