package com.dustinmcafee.dongadeuce.game

import com.dustinmcafee.dongadeuce.models.Card
import com.dustinmcafee.dongadeuce.models.Deck
import com.dustinmcafee.dongadeuce.models.GameConstants
import com.dustinmcafee.dongadeuce.platform.FileHandle
import com.dustinmcafee.dongadeuce.platform.createFileHandle

/**
 * Raw parsed deck data before commander selection.
 * Used when parsing formats that don't specify a commander.
 */
data class ParsedDeckData(
    val name: String,
    val mainboard: List<CardEntry>,
    val sideboard: List<CardEntry>,
    val commander: CardEntry? = null, // Set if format explicitly specifies commander
    val comments: String = ""
) {
    /** A card entry with quantity */
    data class CardEntry(
        val name: String,
        val quantity: Int,
        val setCode: String? = null,
        val collectorNumber: String? = null
    )

    /** Total number of cards in mainboard */
    val mainboardSize: Int get() = mainboard.sumOf { it.quantity }

    /** Total number of cards in sideboard */
    val sideboardSize: Int get() = sideboard.sumOf { it.quantity }

    /** Check if this looks like a Commander deck (99-100 cards) */
    val isCommanderDeck: Boolean get() = mainboardSize in 99..100

    /** Get all unique card names from mainboard */
    val allCardNames: List<String> get() = mainboard.map { it.name }.distinct()

    /** Get all unique card names from both mainboard and sideboard */
    val allCardNamesIncludingSideboard: List<String> get() =
        (mainboard.map { it.name } + sideboard.map { it.name }).distinct()

    /** Get sideboard card names (often where commander is stored in Cockatrice) */
    val sideboardCardNames: List<String> get() = sideboard.map { it.name }.distinct()

    /** Convert to a Deck with the given commander (and optional partner) */
    fun toDeck(commanderName: String, partnerCommanderName: String? = null): Deck {
        // Collect all commander names to remove
        val commanderNames = mutableListOf(commanderName)
        if (partnerCommanderName != null) commanderNames.add(partnerCommanderName)

        // Find the primary commander
        val commanderEntry = mainboard.find { it.name.equals(commanderName, ignoreCase = true) }
            ?: sideboard.find { it.name.equals(commanderName, ignoreCase = true) }
            ?: throw IllegalArgumentException("Commander '$commanderName' not found in deck")
        val commander = Card(name = commanderEntry.name)

        // Find the partner commander if specified
        var partnerCommander: Card? = null
        if (partnerCommanderName != null) {
            val partnerEntry = mainboard.find { it.name.equals(partnerCommanderName, ignoreCase = true) }
                ?: sideboard.find { it.name.equals(partnerCommanderName, ignoreCase = true) }
                ?: throw IllegalArgumentException("Partner commander '$partnerCommanderName' not found in deck")
            partnerCommander = Card(name = partnerEntry.name)
        }

        // Build card list, removing all commanders (from mainboard or sideboard)
        val cards = mutableListOf<Card>()
        val removedFromMainboard = mutableSetOf<String>()

        for (entry in mainboard) {
            val card = Card(name = entry.name)
            repeat(entry.quantity) {
                val matchesCommander = commanderNames.any { cmdName ->
                    entry.name.equals(cmdName, ignoreCase = true) && cmdName.lowercase() !in removedFromMainboard
                }
                if (matchesCommander) {
                    removedFromMainboard.add(entry.name.lowercase())
                } else {
                    cards.add(card)
                }
            }
        }

        // Build sideboard, removing commanders that weren't found in mainboard
        val sideboardCards = mutableListOf<Card>()
        val removedFromSideboard = mutableSetOf<String>()

        for (entry in sideboard) {
            val card = Card(name = entry.name)
            repeat(entry.quantity) {
                val matchesCommander = commanderNames.any { cmdName ->
                    entry.name.equals(cmdName, ignoreCase = true) &&
                    cmdName.lowercase() !in removedFromMainboard &&
                    cmdName.lowercase() !in removedFromSideboard
                }
                if (matchesCommander) {
                    removedFromSideboard.add(entry.name.lowercase())
                } else {
                    sideboardCards.add(card)
                }
            }
        }

        // Check if we have the right number of cards
        val expectedSize = if (partnerCommander != null) GameConstants.DECK_SIZE - 1 else GameConstants.DECK_SIZE
        if (cards.size != expectedSize) {
            throw IllegalArgumentException(
                "Deck must have exactly $expectedSize cards (excluding commander${if (partnerCommander != null) "s" else ""}), found ${cards.size}"
            )
        }

        return Deck(
            name = name,
            commander = commander,
            cards = cards,
            sideboard = sideboardCards,
            partnerCommander = partnerCommander
        )
    }
}

/**
 * Supported deck file formats
 */
enum class DeckFormat {
    /** Our native format with // Commander section */
    NATIVE,
    /** Cockatrice XML format (.cod) */
    COCKATRICE_XML,
    /** Plain text format (.dec, .dek, .txt, .mwDeck) - Cockatrice style */
    PLAIN_TEXT
}

/**
 * Result of parsing a deck file
 */
sealed class DeckParseResult {
    /** Successfully parsed with commander already specified */
    data class Complete(val deck: Deck) : DeckParseResult()

    /** Parsed but commander selection is needed */
    data class NeedsCommanderSelection(val data: ParsedDeckData) : DeckParseResult()

    /** Parsing failed */
    data class Error(val message: String) : DeckParseResult()
}

/**
 * Parses deck files in multiple formats:
 * - Native format with // Commander section
 * - Cockatrice XML format (.cod)
 * - Plain text format (.dec, .dek, .txt, .mwDeck)
 */
object DeckParser {

    // Card line regex patterns
    private val QUANTITY_PATTERN = Regex("""^(\d+)\s*[xX]?\s+(.+)$""")
    private val QUANTITY_WITH_X_PATTERN = Regex("""^(\d+)[xX]\s+(.+)$""")
    private val SET_CODE_PATTERN = Regex("""\s*\(([A-Z0-9]{2,6})\)\s*(\d+[a-zA-Z★]*)?$""")
    private val SB_PREFIX_PATTERN = Regex("""^[Ss][Bb]:\s*(.+)$""")

    /**
     * Detect format from file extension
     */
    fun detectFormat(filePath: String): DeckFormat {
        val lowerPath = filePath.lowercase()
        return when {
            lowerPath.endsWith(".cod") -> DeckFormat.COCKATRICE_XML
            lowerPath.endsWith(".dec") ||
            lowerPath.endsWith(".dek") ||
            lowerPath.endsWith(".txt") ||
            lowerPath.endsWith(".mwdeck") -> DeckFormat.PLAIN_TEXT
            else -> DeckFormat.NATIVE // Default to our format
        }
    }

    /**
     * Parse a deck file with automatic format detection
     */
    fun parseFile(filePath: String): DeckParseResult {
        return try {
            val file = createFileHandle(filePath)
            if (!file.exists()) {
                return DeckParseResult.Error("Deck file does not exist: $filePath")
            }

            val content = file.readText()
            val format = detectFormat(filePath)
            val fileName = filePath.substringAfterLast("/").substringAfterLast("\\")
                .substringBeforeLast(".")

            parseContent(content, format, fileName)
        } catch (e: Exception) {
            DeckParseResult.Error("Failed to parse deck: ${e.message}")
        }
    }

    /**
     * Parse deck content with specified format
     */
    fun parseContent(content: String, format: DeckFormat, deckName: String = "Imported Deck"): DeckParseResult {
        return try {
            when (format) {
                DeckFormat.NATIVE -> parseNativeFormat(content)
                DeckFormat.COCKATRICE_XML -> parseCockatriceXml(content, deckName)
                DeckFormat.PLAIN_TEXT -> parsePlainText(content, deckName)
            }
        } catch (e: Exception) {
            DeckParseResult.Error("Failed to parse deck: ${e.message}")
        }
    }

    /**
     * Parse our native format with // Commander section
     */
    private fun parseNativeFormat(content: String): DeckParseResult {
        require(content.isNotBlank()) { "Deck file content cannot be empty" }

        val lines = content.lines()
        var commander: Card? = null
        val cards = mutableListOf<Card>()
        val sideboard = mutableListOf<Card>()
        var currentCategory = ""
        var lineNumber = 0

        for (line in lines) {
            lineNumber++
            val trimmed = line.trim()

            if (trimmed.isEmpty()) continue

            if (trimmed.startsWith("//")) {
                currentCategory = trimmed.substring(2).trim()
                continue
            }

            val parts = trimmed.split(" ", limit = 2)
            if (parts.size != 2) continue

            val quantity = parts[0].toIntOrNull()
            if (quantity == null || quantity <= 0) {
                throw IllegalArgumentException("Invalid quantity on line $lineNumber: '${parts[0]}'")
            }

            val cardName = parts[1].trim()
            if (cardName.isEmpty()) continue

            val card = Card(name = cardName)

            if (currentCategory.equals("Commander", ignoreCase = true) && commander == null) {
                require(quantity == 1) { "Commander must have quantity of 1, found $quantity on line $lineNumber" }
                commander = card
            } else if (currentCategory.equals("Sideboard", ignoreCase = true)) {
                repeat(quantity) { sideboard.add(card) }
            } else {
                repeat(quantity) { cards.add(card) }
            }
        }

        requireNotNull(commander) { "No commander found. Ensure there is a '// Commander' section." }
        require(cards.size == GameConstants.DECK_SIZE) {
            "Deck must have exactly ${GameConstants.DECK_SIZE} cards (excluding commander), found ${cards.size}"
        }

        return DeckParseResult.Complete(
            Deck(
                name = "Imported Deck",
                commander = commander,
                cards = cards,
                sideboard = sideboard
            )
        )
    }

    /**
     * Parse Cockatrice XML format (.cod)
     */
    private fun parseCockatriceXml(content: String, defaultName: String): DeckParseResult {
        require(content.isNotBlank()) { "Deck file content cannot be empty" }

        // Check if it's actually XML
        if (!content.trimStart().startsWith("<?xml") && !content.trimStart().startsWith("<cockatrice_deck")) {
            // Not XML, try plain text fallback
            return parsePlainText(content, defaultName)
        }

        val mainboard = mutableListOf<ParsedDeckData.CardEntry>()
        val sideboard = mutableListOf<ParsedDeckData.CardEntry>()
        var deckName = defaultName
        var comments = ""
        var bannerCard: String? = null

        // Extract deck name
        val nameMatch = Regex("""<deckname>([^<]*)</deckname>""").find(content)
        if (nameMatch != null) {
            deckName = decodeXmlEntities(nameMatch.groupValues[1].trim())
        }

        // Extract comments
        val commentsMatch = Regex("""<comments>([^<]*)</comments>""").find(content)
        if (commentsMatch != null) {
            comments = decodeXmlEntities(commentsMatch.groupValues[1].trim())
        }

        // Extract banner card (commander)
        val bannerMatch = Regex("""<bannerCard[^>]*>([^<]*)</bannerCard>""").find(content)
        if (bannerMatch != null) {
            bannerCard = decodeXmlEntities(bannerMatch.groupValues[1].trim())
                .takeIf { it.isNotEmpty() }
        }

        // Extract zones and cards
        val zonePattern = Regex("""<zone\s+name="([^"]+)">(.*?)</zone>""", RegexOption.DOT_MATCHES_ALL)
        for (zoneMatch in zonePattern.findAll(content)) {
            val zoneName = zoneMatch.groupValues[1]
            val zoneContent = zoneMatch.groupValues[2]

            val cardPattern = Regex("""<card\s+([^/>]+)/>|<card\s+([^>]+)>[^<]*</card>""")
            for (cardMatch in cardPattern.findAll(zoneContent)) {
                val attrs = cardMatch.groupValues[1].ifEmpty { cardMatch.groupValues[2] }
                val entry = parseCardAttributes(attrs) ?: continue

                when (zoneName.lowercase()) {
                    "main" -> mainboard.add(entry)
                    "side" -> sideboard.add(entry)
                    // Ignore "tokens" zone
                }
            }
        }

        if (mainboard.isEmpty()) {
            return DeckParseResult.Error("No cards found in deck")
        }

        val parsedData = ParsedDeckData(
            name = deckName.ifEmpty { defaultName },
            mainboard = mainboard,
            sideboard = sideboard,
            commander = bannerCard?.let { ParsedDeckData.CardEntry(it, 1) },
            comments = comments
        )

        // If we have a banner card and the deck is 100 cards, try to use it as commander
        if (bannerCard != null && parsedData.mainboardSize == 100) {
            return try {
                DeckParseResult.Complete(parsedData.toDeck(bannerCard))
            } catch (_: Exception) {
                DeckParseResult.NeedsCommanderSelection(parsedData)
            }
        }

        return DeckParseResult.NeedsCommanderSelection(parsedData)
    }

    /**
     * Parse card attributes from XML
     */
    private fun parseCardAttributes(attrs: String): ParsedDeckData.CardEntry? {
        val nameMatch = Regex("""name="([^"]+)"""").find(attrs) ?: return null
        val name = decodeXmlEntities(nameMatch.groupValues[1])

        val numberMatch = Regex("""number="(\d+)"""").find(attrs)
        val quantity = numberMatch?.groupValues?.get(1)?.toIntOrNull() ?: 1

        val setMatch = Regex("""setShortName="([^"]+)"""").find(attrs)
        val setCode = setMatch?.groupValues?.get(1)

        val collectorMatch = Regex("""collectorNumber="([^"]+)"""").find(attrs)
        val collectorNumber = collectorMatch?.groupValues?.get(1)

        return ParsedDeckData.CardEntry(name, quantity, setCode, collectorNumber)
    }

    /**
     * Decode XML entities
     */
    private fun decodeXmlEntities(text: String): String {
        return text
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
    }

    /**
     * Parse plain text format (.dec, .dek, .txt, .mwDeck)
     * Supports multiple card line formats and sideboard detection
     */
    private fun parsePlainText(content: String, defaultName: String): DeckParseResult {
        require(content.isNotBlank()) { "Deck file content cannot be empty" }

        val mainboard = mutableListOf<ParsedDeckData.CardEntry>()
        val sideboard = mutableListOf<ParsedDeckData.CardEntry>()
        var deckName = defaultName
        var comments = StringBuilder()

        var inSideboard = false
        var foundMainboardCards = false
        var linesSinceLastCard = 0
        var hasExplicitSbMarkers = false
        var hasExplicitSideboardSection = false
        var lineNumber = 0

        // First pass: check for explicit SB: markers
        for (line in content.lines()) {
            if (SB_PREFIX_PATTERN.matches(line.trim())) {
                hasExplicitSbMarkers = true
                break
            }
            if (line.trim().lowercase().let {
                it == "sideboard" || it == "// sideboard" || it.startsWith("sideboard:")
            }) {
                hasExplicitSideboardSection = true
            }
        }

        // Second pass: parse cards
        for (line in content.lines()) {
            lineNumber++
            val trimmed = line.trim()

            // Skip empty lines but track them for blank-line sideboard detection
            if (trimmed.isEmpty()) {
                if (foundMainboardCards && !hasExplicitSbMarkers && !hasExplicitSideboardSection) {
                    linesSinceLastCard++
                }
                continue
            }

            // Comments
            if (trimmed.startsWith("//")) {
                val commentContent = trimmed.substring(2).trim()

                // Section comments reset blank line counter (they're part of deck structure)
                linesSinceLastCard = 0

                // Check for section markers
                when (commentContent.lowercase()) {
                    "sideboard" -> {
                        inSideboard = true
                        continue
                    }
                    "mainboard", "main deck", "maindeck", "deck" -> {
                        inSideboard = false
                        continue
                    }
                    "commander" -> {
                        // Skip commander section marker - we'll select commander later
                        continue
                    }
                }

                // First non-section comment becomes deck name, rest are comments
                if (deckName == defaultName && !foundMainboardCards) {
                    deckName = commentContent
                } else if (!foundMainboardCards) {
                    if (comments.isNotEmpty()) comments.append("\n")
                    comments.append(commentContent)
                }
                continue
            }

            // Check for "Sideboard" or "Sideboard:" line
            if (trimmed.lowercase().let { it == "sideboard" || it.startsWith("sideboard:") }) {
                inSideboard = true
                continue
            }

            // Check for SB: prefix
            val sbMatch = SB_PREFIX_PATTERN.find(trimmed)
            if (sbMatch != null) {
                val cardLine = sbMatch.groupValues[1].trim()
                val entry = parseCardLine(cardLine)
                if (entry != null) {
                    sideboard.add(entry)
                }
                continue
            }

            // Try to parse as a card line
            val entry = parseCardLine(trimmed)
            if (entry != null) {
                // Blank line detection for sideboard (only if no explicit markers)
                if (!hasExplicitSbMarkers && !hasExplicitSideboardSection &&
                    foundMainboardCards && linesSinceLastCard >= 1) {
                    inSideboard = true
                }

                if (inSideboard) {
                    sideboard.add(entry)
                } else {
                    mainboard.add(entry)
                }
                foundMainboardCards = true
                linesSinceLastCard = 0
            }
        }

        if (mainboard.isEmpty()) {
            return DeckParseResult.Error("No cards found in deck")
        }

        val parsedData = ParsedDeckData(
            name = deckName.ifEmpty { defaultName },
            mainboard = mainboard,
            sideboard = sideboard,
            comments = comments.toString()
        )

        // Check if this looks like a Commander deck
        return if (parsedData.isCommanderDeck) {
            DeckParseResult.NeedsCommanderSelection(parsedData)
        } else {
            // Non-Commander format - still needs commander selection for this app
            DeckParseResult.NeedsCommanderSelection(parsedData)
        }
    }

    /**
     * Parse a single card line in various formats:
     * - "4 Card Name"
     * - "4x Card Name"
     * - "4 Card Name (SET) 123"
     * - "4 Card Name (SET)"
     */
    private fun parseCardLine(line: String): ParsedDeckData.CardEntry? {
        var text = line.trim()

        // Skip obviously non-card lines
        if (text.isEmpty() ||
            text.startsWith("#") ||
            text.startsWith("//") ||
            text.lowercase().startsWith("deck") ||
            text.lowercase() == "mainboard" ||
            text.lowercase() == "sideboard") {
            return null
        }

        // Try quantity patterns
        val quantityMatch = QUANTITY_WITH_X_PATTERN.find(text)
            ?: QUANTITY_PATTERN.find(text)

        if (quantityMatch == null) {
            // Try to handle "Card Name" without quantity (assume 1)
            // But only if it doesn't start with a number
            if (!text[0].isDigit()) {
                val cardName = normalizeCardName(extractCardName(text))
                if (cardName.isNotEmpty()) {
                    return ParsedDeckData.CardEntry(cardName, 1)
                }
            }
            return null
        }

        val quantity = quantityMatch.groupValues[1].toIntOrNull() ?: return null
        if (quantity <= 0) return null

        var cardNamePart = quantityMatch.groupValues[2].trim()

        // Extract set code and collector number if present
        var setCode: String? = null
        var collectorNumber: String? = null

        val setMatch = SET_CODE_PATTERN.find(cardNamePart)
        if (setMatch != null) {
            setCode = setMatch.groupValues[1]
            collectorNumber = setMatch.groupValues[2].takeIf { it.isNotEmpty() }
            cardNamePart = cardNamePart.substring(0, setMatch.range.first).trim()
        }

        val cardName = normalizeCardName(extractCardName(cardNamePart))
        if (cardName.isEmpty()) return null

        return ParsedDeckData.CardEntry(cardName, quantity, setCode, collectorNumber)
    }

    /**
     * Extract card name, handling special cases like split cards
     */
    private fun extractCardName(text: String): String {
        var name = text.trim()

        // Handle color indicators like [B], [W], etc.
        name = name.replace(Regex("""^\[[A-Z]+\]\s*"""), "")

        // Normalize split card separators
        // Replace | with // (Cockatrice uses | for split/transform cards)
        name = name.replace("|", " // ")
        // Replace single / with // only if // is not already present
        // (to handle formats that use single / for split cards, but not break existing //)
        if (!name.contains("//")) {
            name = name.replace("/", " // ")
        }
        // Normalize whitespace around //
        name = name.replace(Regex("""\s*//\s*"""), " // ")

        return name.trim()
    }

    /**
     * Normalize card names for consistency
     */
    private fun normalizeCardName(name: String): String {
        return name
            .replace("Æ", "Ae")
            .replace("æ", "ae")
            .replace("'", "'")
            .replace("'", "'")
            .replace(""", "\"")
            .replace(""", "\"")
            .trim()
    }

    // ===== BACKWARD COMPATIBILITY =====

    /**
     * Parse our native text format (backward compatible)
     * @throws IllegalArgumentException if parsing fails
     */
    fun parseTextFormat(content: String): Deck {
        return when (val result = parseNativeFormat(content)) {
            is DeckParseResult.Complete -> result.deck
            is DeckParseResult.NeedsCommanderSelection ->
                throw IllegalArgumentException("No commander found. Use parseFile() for formats without commander.")
            is DeckParseResult.Error -> throw IllegalArgumentException(result.message)
        }
    }

    /**
     * Parse a text file in our native format (backward compatible)
     */
    fun parseTextFile(file: FileHandle): Deck {
        require(file.exists()) { "Deck file does not exist: ${file.path}" }

        val content = try {
            file.readText()
        } catch (e: Exception) {
            throw IllegalArgumentException("Failed to read deck file: ${file.path}", e)
        }

        return parseTextFormat(content)
    }

    /**
     * Parse a text file in our native format (backward compatible)
     */
    fun parseTextFile(filePath: String): Deck {
        require(filePath.isNotBlank()) { "File path cannot be empty" }
        return parseTextFile(createFileHandle(filePath))
    }
}
