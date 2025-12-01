package com.dustinmcafee.dongadeuce.game

import com.dustinmcafee.dongadeuce.models.GameConstants
import java.io.File
import kotlin.test.*

class DeckParserTest {

    /**
     * Creates a valid deck content string with the specified commander and 99 cards
     */
    private fun createValidDeckContent(
        commanderName: String = "Test Commander",
        cards: List<Pair<Int, String>> = listOf(99 to "Sol Ring")
    ): String {
        val sb = StringBuilder()
        sb.appendLine("// Commander")
        sb.appendLine("1 $commanderName")
        sb.appendLine()
        sb.appendLine("// Cards")
        for ((quantity, cardName) in cards) {
            sb.appendLine("$quantity $cardName")
        }
        return sb.toString()
    }

    /**
     * Creates a deck content with exactly 99 varied cards for realistic tests
     */
    private fun createVariedDeckContent(commanderName: String = "Test Commander"): String {
        val sb = StringBuilder()
        sb.appendLine("// Commander")
        sb.appendLine("1 $commanderName")
        sb.appendLine()
        sb.appendLine("// Lands")
        sb.appendLine("30 Plains")
        sb.appendLine()
        sb.appendLine("// Creatures")
        sb.appendLine("20 Llanowar Elves")
        sb.appendLine()
        sb.appendLine("// Artifacts")
        sb.appendLine("15 Sol Ring")
        sb.appendLine()
        sb.appendLine("// Spells")
        sb.appendLine("34 Lightning Bolt")
        return sb.toString()
    }

    // ==================== Valid Parsing Tests ====================

    @Test
    fun `parse valid deck with commander and 99 cards`() {
        val content = createValidDeckContent()

        val deck = DeckParser.parseTextFormat(content)

        assertEquals("Test Commander", deck.commander.name)
        assertEquals(GameConstants.DECK_SIZE, deck.cards.size)
    }

    @Test
    fun `parse deck with varied card types`() {
        val content = createVariedDeckContent("Atraxa, Praetors' Voice")

        val deck = DeckParser.parseTextFormat(content)

        assertEquals("Atraxa, Praetors' Voice", deck.commander.name)
        assertEquals(99, deck.cards.size)
        // Check card distribution
        assertEquals(30, deck.cards.count { it.name == "Plains" })
        assertEquals(20, deck.cards.count { it.name == "Llanowar Elves" })
        assertEquals(15, deck.cards.count { it.name == "Sol Ring" })
        assertEquals(34, deck.cards.count { it.name == "Lightning Bolt" })
    }

    @Test
    fun `parse deck with basic lands allows duplicates`() {
        // In Commander, basic lands are the exception to singleton rule
        val content = createValidDeckContent(cards = listOf(99 to "Plains"))

        val deck = DeckParser.parseTextFormat(content)

        assertEquals(99, deck.cards.size)
        assertTrue(deck.cards.all { it.name == "Plains" })
    }

    @Test
    fun `parse deck handles blank lines`() {
        val content = """
            // Commander
            1 Test Commander


            // Lands

            50 Island

            // Spells

            49 Counterspell

        """.trimIndent()

        val deck = DeckParser.parseTextFormat(content)

        assertEquals("Test Commander", deck.commander.name)
        assertEquals(99, deck.cards.size)
    }

    @Test
    fun `parse deck handles comment lines`() {
        val content = """
            // Commander
            1 Test Commander
            // Lands
            50 Island
            // Creatures
            49 Birds of Paradise
        """.trimIndent()

        val deck = DeckParser.parseTextFormat(content)

        assertEquals("Test Commander", deck.commander.name)
        assertEquals(99, deck.cards.size)
    }

    @Test
    fun `parse deck handles category headers`() {
        val content = """
            // Commander
            1 Test Commander
            // Artifacts
            10 Sol Ring
            // Lands
            40 Forest
            // Creatures
            25 Llanowar Elves
            // Instants
            14 Giant Growth
            // Sorceries
            10 Cultivate
        """.trimIndent()

        val deck = DeckParser.parseTextFormat(content)

        assertEquals(99, deck.cards.size)
        assertEquals(10, deck.cards.count { it.name == "Sol Ring" })
        assertEquals(40, deck.cards.count { it.name == "Forest" })
    }

    @Test
    fun `parse handles quantity with single digit`() {
        val content = """
            // Commander
            1 Test Commander
            // Cards
            1 Sol Ring
            98 Plains
        """.trimIndent()

        val deck = DeckParser.parseTextFormat(content)

        assertEquals(99, deck.cards.size)
        assertEquals(1, deck.cards.count { it.name == "Sol Ring" })
    }

    @Test
    fun `parse handles multi-word card names`() {
        val content = """
            // Commander
            1 Edgar Markov
            // Cards
            99 Sorin, Lord of Innistrad
        """.trimIndent()

        val deck = DeckParser.parseTextFormat(content)

        assertEquals("Edgar Markov", deck.commander.name)
        assertEquals(99, deck.cards.count { it.name == "Sorin, Lord of Innistrad" })
    }

    @Test
    fun `parse handles card names with special characters`() {
        val content = """
            // Commander
            1 Atraxa, Praetors' Voice
            // Cards
            99 Sol Ring
        """.trimIndent()

        val deck = DeckParser.parseTextFormat(content)

        assertEquals("Atraxa, Praetors' Voice", deck.commander.name)
    }

    @Test
    fun `parse deck name defaults to Imported Deck`() {
        val content = createValidDeckContent()

        val deck = DeckParser.parseTextFormat(content)

        assertEquals("Imported Deck", deck.name)
    }

    // ==================== Invalid Parsing Tests ====================

    @Test
    fun `parse fails on empty content`() {
        assertFailsWith<IllegalArgumentException> {
            DeckParser.parseTextFormat("")
        }
    }

    @Test
    fun `parse fails on blank content`() {
        assertFailsWith<IllegalArgumentException> {
            DeckParser.parseTextFormat("   \n   \n   ")
        }
    }

    @Test
    fun `parse fails on missing commander`() {
        val content = """
            // Lands
            99 Plains
        """.trimIndent()

        val exception = assertFailsWith<IllegalArgumentException> {
            DeckParser.parseTextFormat(content)
        }
        assertTrue(exception.message?.contains("No commander found") == true)
    }

    @Test
    fun `parse fails on wrong deck size - too few cards`() {
        val content = """
            // Commander
            1 Test Commander
            // Cards
            50 Plains
        """.trimIndent()

        val exception = assertFailsWith<IllegalArgumentException> {
            DeckParser.parseTextFormat(content)
        }
        assertTrue(exception.message?.contains("found 50") == true)
    }

    @Test
    fun `parse fails on wrong deck size - too many cards`() {
        val content = """
            // Commander
            1 Test Commander
            // Cards
            150 Plains
        """.trimIndent()

        val exception = assertFailsWith<IllegalArgumentException> {
            DeckParser.parseTextFormat(content)
        }
        assertTrue(exception.message?.contains("found 150") == true)
    }

    @Test
    fun `parse fails on negative quantity`() {
        val content = """
            // Commander
            1 Test Commander
            // Cards
            -1 Sol Ring
            100 Plains
        """.trimIndent()

        val exception = assertFailsWith<IllegalArgumentException> {
            DeckParser.parseTextFormat(content)
        }
        assertTrue(exception.message?.contains("Invalid quantity") == true)
    }

    @Test
    fun `parse fails on zero quantity`() {
        val content = """
            // Commander
            1 Test Commander
            // Cards
            0 Sol Ring
            99 Plains
        """.trimIndent()

        val exception = assertFailsWith<IllegalArgumentException> {
            DeckParser.parseTextFormat(content)
        }
        assertTrue(exception.message?.contains("Invalid quantity") == true)
    }

    @Test
    fun `parse fails on invalid quantity format`() {
        val content = """
            // Commander
            1 Test Commander
            // Cards
            abc Sol Ring
        """.trimIndent()

        val exception = assertFailsWith<IllegalArgumentException> {
            DeckParser.parseTextFormat(content)
        }
        assertTrue(exception.message?.contains("Invalid quantity") == true)
    }

    @Test
    fun `parse fails on commander with quantity greater than 1`() {
        val content = """
            // Commander
            2 Test Commander
            // Cards
            99 Plains
        """.trimIndent()

        val exception = assertFailsWith<IllegalArgumentException> {
            DeckParser.parseTextFormat(content)
        }
        assertTrue(exception.message?.contains("Commander must have quantity of 1") == true)
    }

    // ==================== File Operation Tests ====================

    @Test
    fun `parseTextFile reads valid file`() {
        val tempFile = File.createTempFile("test_deck", ".txt")
        try {
            tempFile.writeText(createValidDeckContent("File Commander"))

            val deck = DeckParser.parseTextFile(tempFile.absolutePath)

            assertEquals("File Commander", deck.commander.name)
            assertEquals(99, deck.cards.size)
        } finally {
            tempFile.delete()
        }
    }

    @Test
    fun `parseTextFile with path reads valid file`() {
        val tempFile = File.createTempFile("test_deck", ".txt")
        try {
            tempFile.writeText(createValidDeckContent("Path Commander"))

            val deck = DeckParser.parseTextFile(tempFile.absolutePath)

            assertEquals("Path Commander", deck.commander.name)
        } finally {
            tempFile.delete()
        }
    }

    @Test
    fun `parseTextFile throws on missing file`() {
        val nonExistentFile = File("/nonexistent/path/deck.txt")

        assertFailsWith<IllegalArgumentException> {
            DeckParser.parseTextFile(nonExistentFile.absolutePath)
        }
    }

    @Test
    fun `parseTextFile throws on blank path`() {
        assertFailsWith<IllegalArgumentException> {
            DeckParser.parseTextFile("")
        }
    }

    @Test
    fun `parseTextFile throws on whitespace only path`() {
        assertFailsWith<IllegalArgumentException> {
            DeckParser.parseTextFile("   ")
        }
    }

    @Test
    fun `parseTextFile throws on directory instead of file`() {
        val tempDir = File.createTempFile("test", "dir")
        tempDir.delete()
        tempDir.mkdir()
        try {
            assertFailsWith<IllegalArgumentException> {
                DeckParser.parseTextFile(tempDir.absolutePath)
            }
        } finally {
            tempDir.delete()
        }
    }

    // ==================== Edge Cases ====================

    @Test
    fun `parse skips lines without space delimiter`() {
        val content = """
            // Commander
            1 Test Commander
            // Cards
            NoSpaceHere
            99 Plains
        """.trimIndent()

        // Lines without space delimiter are skipped silently
        val deck = DeckParser.parseTextFormat(content)

        assertEquals(99, deck.cards.size)
    }

    @Test
    fun `parse handles commander section case insensitively`() {
        val content = """
            // COMMANDER
            1 Test Commander
            // Cards
            99 Plains
        """.trimIndent()

        val deck = DeckParser.parseTextFormat(content)

        assertEquals("Test Commander", deck.commander.name)
    }

    @Test
    fun `parse handles lowercase commander section`() {
        val content = """
            // commander
            1 Test Commander
            // Cards
            99 Plains
        """.trimIndent()

        val deck = DeckParser.parseTextFormat(content)

        assertEquals("Test Commander", deck.commander.name)
    }

    @Test
    fun `parse preserves card name whitespace trimming`() {
        val content = """
            // Commander
            1   Commander with Spaces
            // Cards
            99   Sol Ring
        """.trimIndent()

        val deck = DeckParser.parseTextFormat(content)

        assertEquals("Commander with Spaces", deck.commander.name)
        assertTrue(deck.cards[0].name == "Sol Ring")
    }

    @Test
    fun `second card in commander section goes to main deck`() {
        val content = """
            // Commander
            1 Test Commander
            1 Partner Commander
            // Cards
            98 Plains
        """.trimIndent()

        val deck = DeckParser.parseTextFormat(content)

        assertEquals("Test Commander", deck.commander.name)
        // Partner Commander becomes a regular card
        assertEquals(1, deck.cards.count { it.name == "Partner Commander" })
        assertEquals(99, deck.cards.size)
    }

    // ==================== Multi-Format Tests ====================

    @Test
    fun `detectFormat returns COCKATRICE_XML for cod files`() {
        assertEquals(DeckFormat.COCKATRICE_XML, DeckParser.detectFormat("/path/to/deck.cod"))
        assertEquals(DeckFormat.COCKATRICE_XML, DeckParser.detectFormat("deck.COD"))
    }

    @Test
    fun `detectFormat returns PLAIN_TEXT for dec files`() {
        assertEquals(DeckFormat.PLAIN_TEXT, DeckParser.detectFormat("/path/to/deck.dec"))
        assertEquals(DeckFormat.PLAIN_TEXT, DeckParser.detectFormat("deck.DEC"))
    }

    @Test
    fun `detectFormat returns PLAIN_TEXT for dek files`() {
        assertEquals(DeckFormat.PLAIN_TEXT, DeckParser.detectFormat("/path/to/deck.dek"))
    }

    @Test
    fun `detectFormat returns PLAIN_TEXT for txt files`() {
        assertEquals(DeckFormat.PLAIN_TEXT, DeckParser.detectFormat("/path/to/deck.txt"))
    }

    @Test
    fun `detectFormat returns PLAIN_TEXT for mwDeck files`() {
        assertEquals(DeckFormat.PLAIN_TEXT, DeckParser.detectFormat("/path/to/deck.mwDeck"))
    }

    @Test
    fun `detectFormat returns NATIVE for unknown extensions`() {
        assertEquals(DeckFormat.NATIVE, DeckParser.detectFormat("/path/to/deck.unknown"))
        assertEquals(DeckFormat.NATIVE, DeckParser.detectFormat("/path/to/deck"))
    }

    // ==================== Plain Text Format Tests ====================

    @Test
    fun `parseContent plain text with Nx format`() {
        val content = """
            // My Deck
            4x Sol Ring
            4X Llanowar Elves
            91 Plains
        """.trimIndent()

        val result = DeckParser.parseContent(content, DeckFormat.PLAIN_TEXT, "Test Deck")

        assertTrue(result is DeckParseResult.NeedsCommanderSelection)
        val data = (result as DeckParseResult.NeedsCommanderSelection).data
        assertEquals(99, data.mainboardSize)
        assertEquals(4, data.mainboard.find { it.name == "Sol Ring" }?.quantity)
        assertEquals(4, data.mainboard.find { it.name == "Llanowar Elves" }?.quantity)
    }

    @Test
    fun `parseContent plain text with SB prefix`() {
        val content = """
            4 Sol Ring
            95 Plains
            SB: 2 Counterspell
            sb: 3 Force of Will
        """.trimIndent()

        val result = DeckParser.parseContent(content, DeckFormat.PLAIN_TEXT, "Test Deck")

        assertTrue(result is DeckParseResult.NeedsCommanderSelection)
        val data = (result as DeckParseResult.NeedsCommanderSelection).data
        assertEquals(99, data.mainboardSize)
        assertEquals(5, data.sideboardSize)
        assertEquals(2, data.sideboard.find { it.name == "Counterspell" }?.quantity)
        assertEquals(3, data.sideboard.find { it.name == "Force of Will" }?.quantity)
    }

    @Test
    fun `parseContent plain text with blank line sideboard detection`() {
        val content = """
            4 Sol Ring
            95 Plains

            2 Counterspell
            3 Force of Will
        """.trimIndent()

        val result = DeckParser.parseContent(content, DeckFormat.PLAIN_TEXT, "Test Deck")

        assertTrue(result is DeckParseResult.NeedsCommanderSelection)
        val data = (result as DeckParseResult.NeedsCommanderSelection).data
        assertEquals(99, data.mainboardSize)
        assertEquals(5, data.sideboardSize)
    }

    @Test
    fun `parseContent plain text with sideboard section header`() {
        val content = """
            // Mainboard
            4 Sol Ring
            95 Plains
            // Sideboard
            2 Counterspell
            3 Force of Will
        """.trimIndent()

        val result = DeckParser.parseContent(content, DeckFormat.PLAIN_TEXT, "Test Deck")

        assertTrue(result is DeckParseResult.NeedsCommanderSelection)
        val data = (result as DeckParseResult.NeedsCommanderSelection).data
        assertEquals(99, data.mainboardSize)
        assertEquals(5, data.sideboardSize)
    }

    @Test
    fun `parseContent plain text with set code and collector number`() {
        val content = """
            4 Sol Ring (CMD) 123
            4 Mana Crypt (2XM) 270a
            91 Plains
        """.trimIndent()

        val result = DeckParser.parseContent(content, DeckFormat.PLAIN_TEXT, "Test Deck")

        assertTrue(result is DeckParseResult.NeedsCommanderSelection)
        val data = (result as DeckParseResult.NeedsCommanderSelection).data
        val solRing = data.mainboard.find { it.name == "Sol Ring" }
        assertNotNull(solRing)
        assertEquals("CMD", solRing.setCode)
        assertEquals("123", solRing.collectorNumber)
    }

    @Test
    fun `parseContent plain text extracts deck name from first comment`() {
        val content = """
            // My Awesome Deck
            // This is a comment
            99 Plains
        """.trimIndent()

        val result = DeckParser.parseContent(content, DeckFormat.PLAIN_TEXT, "Default Name")

        assertTrue(result is DeckParseResult.NeedsCommanderSelection)
        val data = (result as DeckParseResult.NeedsCommanderSelection).data
        assertEquals("My Awesome Deck", data.name)
    }

    @Test
    fun `parseContent plain text handles card names without quantity`() {
        val content = """
            Sol Ring
            Plains
            Island
        """.trimIndent()

        val result = DeckParser.parseContent(content, DeckFormat.PLAIN_TEXT, "Test Deck")

        assertTrue(result is DeckParseResult.NeedsCommanderSelection)
        val data = (result as DeckParseResult.NeedsCommanderSelection).data
        assertEquals(3, data.mainboardSize)
        assertEquals(1, data.mainboard.find { it.name == "Sol Ring" }?.quantity)
    }

    @Test
    fun `parseContent plain text normalizes card names`() {
        val content = """
            4 Æther Adept
            95 Plains
        """.trimIndent()

        val result = DeckParser.parseContent(content, DeckFormat.PLAIN_TEXT, "Test Deck")

        assertTrue(result is DeckParseResult.NeedsCommanderSelection)
        val data = (result as DeckParseResult.NeedsCommanderSelection).data
        assertEquals("Aether Adept", data.mainboard.find { it.name.contains("Adept") }?.name)
    }

    // ==================== Cockatrice XML Format Tests ====================

    @Test
    fun `parseContent cockatrice XML basic format`() {
        val content = """
            <?xml version="1.0" encoding="UTF-8"?>
            <cockatrice_deck version="1">
                <deckname>My Commander Deck</deckname>
                <comments>Test deck</comments>
                <zone name="main">
                    <card name="Sol Ring" number="4"/>
                    <card name="Plains" number="95"/>
                </zone>
                <zone name="side">
                    <card name="Counterspell" number="2"/>
                </zone>
            </cockatrice_deck>
        """.trimIndent()

        val result = DeckParser.parseContent(content, DeckFormat.COCKATRICE_XML, "Default Name")

        assertTrue(result is DeckParseResult.NeedsCommanderSelection)
        val data = (result as DeckParseResult.NeedsCommanderSelection).data
        assertEquals("My Commander Deck", data.name)
        assertEquals(99, data.mainboardSize)
        assertEquals(2, data.sideboardSize)
        assertEquals(4, data.mainboard.find { it.name == "Sol Ring" }?.quantity)
    }

    @Test
    fun `parseContent cockatrice XML with set info`() {
        val content = """
            <?xml version="1.0" encoding="UTF-8"?>
            <cockatrice_deck version="1">
                <zone name="main">
                    <card name="Sol Ring" number="4" setShortName="CMD" collectorNumber="123"/>
                    <card name="Plains" number="95"/>
                </zone>
            </cockatrice_deck>
        """.trimIndent()

        val result = DeckParser.parseContent(content, DeckFormat.COCKATRICE_XML, "Test Deck")

        assertTrue(result is DeckParseResult.NeedsCommanderSelection)
        val data = (result as DeckParseResult.NeedsCommanderSelection).data
        val solRing = data.mainboard.find { it.name == "Sol Ring" }
        assertNotNull(solRing)
        assertEquals("CMD", solRing.setCode)
        assertEquals("123", solRing.collectorNumber)
    }

    @Test
    fun `parseContent cockatrice XML with banner card stores commander in parsed data`() {
        val content = """
            <?xml version="1.0" encoding="UTF-8"?>
            <cockatrice_deck version="1">
                <deckname>Commander Deck</deckname>
                <bannerCard>Edgar Markov</bannerCard>
                <zone name="main">
                    <card name="Edgar Markov" number="1"/>
                    <card name="Sol Ring" number="4"/>
                    <card name="Plains" number="94"/>
                </zone>
            </cockatrice_deck>
        """.trimIndent()

        val result = DeckParser.parseContent(content, DeckFormat.COCKATRICE_XML, "Test Deck")

        // With 99 cards (not 100), it needs commander selection
        // The banner card is stored in the parsed data for reference
        assertTrue(result is DeckParseResult.NeedsCommanderSelection)
        val data = (result as DeckParseResult.NeedsCommanderSelection).data
        assertNotNull(data.commander)
        assertEquals("Edgar Markov", data.commander?.name)
    }

    @Test
    fun `parseContent cockatrice XML ignores tokens zone`() {
        val content = """
            <?xml version="1.0" encoding="UTF-8"?>
            <cockatrice_deck version="1">
                <zone name="main">
                    <card name="Plains" number="99"/>
                </zone>
                <zone name="tokens">
                    <card name="Soldier Token" number="5"/>
                </zone>
            </cockatrice_deck>
        """.trimIndent()

        val result = DeckParser.parseContent(content, DeckFormat.COCKATRICE_XML, "Test Deck")

        assertTrue(result is DeckParseResult.NeedsCommanderSelection)
        val data = (result as DeckParseResult.NeedsCommanderSelection).data
        assertEquals(99, data.mainboardSize)
        assertEquals(0, data.sideboardSize)
    }

    @Test
    fun `parseContent cockatrice XML decodes XML entities`() {
        val content = """
            <?xml version="1.0" encoding="UTF-8"?>
            <cockatrice_deck version="1">
                <deckname>Deck &amp; More</deckname>
                <zone name="main">
                    <card name="Sol Ring" number="99"/>
                </zone>
            </cockatrice_deck>
        """.trimIndent()

        val result = DeckParser.parseContent(content, DeckFormat.COCKATRICE_XML, "Test Deck")

        assertTrue(result is DeckParseResult.NeedsCommanderSelection)
        val data = (result as DeckParseResult.NeedsCommanderSelection).data
        assertEquals("Deck & More", data.name)
    }

    @Test
    fun `parseContent cockatrice XML falls back to plain text if not valid XML`() {
        val content = """
            4 Sol Ring
            95 Plains
        """.trimIndent()

        // Even though we say it's COCKATRICE_XML format, it should fall back to plain text
        val result = DeckParser.parseContent(content, DeckFormat.COCKATRICE_XML, "Test Deck")

        assertTrue(result is DeckParseResult.NeedsCommanderSelection)
        val data = (result as DeckParseResult.NeedsCommanderSelection).data
        assertEquals(99, data.mainboardSize)
    }

    // ==================== ParsedDeckData Tests ====================

    @Test
    fun `ParsedDeckData calculates mainboard size correctly`() {
        val data = ParsedDeckData(
            name = "Test",
            mainboard = listOf(
                ParsedDeckData.CardEntry("Sol Ring", 4),
                ParsedDeckData.CardEntry("Plains", 95)
            ),
            sideboard = emptyList()
        )

        assertEquals(99, data.mainboardSize)
    }

    @Test
    fun `ParsedDeckData calculates sideboard size correctly`() {
        val data = ParsedDeckData(
            name = "Test",
            mainboard = listOf(ParsedDeckData.CardEntry("Plains", 99)),
            sideboard = listOf(
                ParsedDeckData.CardEntry("Counterspell", 2),
                ParsedDeckData.CardEntry("Force of Will", 3)
            )
        )

        assertEquals(5, data.sideboardSize)
    }

    @Test
    fun `ParsedDeckData isCommanderDeck returns true for 99-100 cards`() {
        val data99 = ParsedDeckData(
            name = "Test",
            mainboard = listOf(ParsedDeckData.CardEntry("Plains", 99)),
            sideboard = emptyList()
        )
        val data100 = ParsedDeckData(
            name = "Test",
            mainboard = listOf(ParsedDeckData.CardEntry("Plains", 100)),
            sideboard = emptyList()
        )
        val data60 = ParsedDeckData(
            name = "Test",
            mainboard = listOf(ParsedDeckData.CardEntry("Plains", 60)),
            sideboard = emptyList()
        )

        assertTrue(data99.isCommanderDeck)
        assertTrue(data100.isCommanderDeck)
        assertFalse(data60.isCommanderDeck)
    }

    @Test
    fun `ParsedDeckData toDeck creates valid deck`() {
        val data = ParsedDeckData(
            name = "Test Deck",
            mainboard = listOf(
                ParsedDeckData.CardEntry("Edgar Markov", 1),
                ParsedDeckData.CardEntry("Sol Ring", 4),
                ParsedDeckData.CardEntry("Plains", 95)
            ),
            sideboard = listOf(ParsedDeckData.CardEntry("Counterspell", 2))
        )

        val deck = data.toDeck("Edgar Markov")

        assertEquals("Test Deck", deck.name)
        assertEquals("Edgar Markov", deck.commander.name)
        assertEquals(99, deck.cards.size)
        assertEquals(2, deck.sideboard.size)
        // Verify commander was removed from main deck
        assertEquals(0, deck.cards.count { it.name == "Edgar Markov" })
    }

    @Test
    fun `ParsedDeckData toDeck throws on missing commander`() {
        val data = ParsedDeckData(
            name = "Test Deck",
            mainboard = listOf(ParsedDeckData.CardEntry("Plains", 100)),
            sideboard = emptyList()
        )

        assertFailsWith<IllegalArgumentException> {
            data.toDeck("Nonexistent Commander")
        }
    }

    @Test
    fun `ParsedDeckData allCardNames returns unique names`() {
        val data = ParsedDeckData(
            name = "Test",
            mainboard = listOf(
                ParsedDeckData.CardEntry("Sol Ring", 4),
                ParsedDeckData.CardEntry("Plains", 95),
                ParsedDeckData.CardEntry("Sol Ring", 4) // Duplicate entry
            ),
            sideboard = emptyList()
        )

        val names = data.allCardNames
        assertEquals(2, names.size)
        assertTrue(names.contains("Sol Ring"))
        assertTrue(names.contains("Plains"))
    }

    // ==================== Error Handling Tests ====================

    @Test
    fun `parseContent returns error for empty content`() {
        val result = DeckParser.parseContent("", DeckFormat.PLAIN_TEXT, "Test")

        assertTrue(result is DeckParseResult.Error)
    }

    @Test
    fun `parseContent returns error for content with no cards`() {
        val content = """
            // Just comments
            // No cards here
        """.trimIndent()

        val result = DeckParser.parseContent(content, DeckFormat.PLAIN_TEXT, "Test")

        assertTrue(result is DeckParseResult.Error)
    }

    // ==================== Commander in Sideboard Tests ====================

    @Test
    fun `ParsedDeckData toDeck finds commander in sideboard`() {
        // This is common in Cockatrice where commander is stored in sideboard zone
        val data = ParsedDeckData(
            name = "Zedruu Deck",
            mainboard = listOf(
                ParsedDeckData.CardEntry("Sol Ring", 4),
                ParsedDeckData.CardEntry("Plains", 95)
            ),
            sideboard = listOf(
                ParsedDeckData.CardEntry("Zedruu the Greathearted", 1),
                ParsedDeckData.CardEntry("Counterspell", 2)
            )
        )

        val deck = data.toDeck("Zedruu the Greathearted")

        assertEquals("Zedruu Deck", deck.name)
        assertEquals("Zedruu the Greathearted", deck.commander.name)
        assertEquals(99, deck.cards.size)
        // Commander should be removed from sideboard
        assertEquals(2, deck.sideboard.size)
        assertEquals(0, deck.sideboard.count { it.name == "Zedruu the Greathearted" })
    }

    @Test
    fun `ParsedDeckData allCardNamesIncludingSideboard includes both zones`() {
        val data = ParsedDeckData(
            name = "Test",
            mainboard = listOf(ParsedDeckData.CardEntry("Sol Ring", 4)),
            sideboard = listOf(ParsedDeckData.CardEntry("Commander Card", 1))
        )

        val names = data.allCardNamesIncludingSideboard
        assertEquals(2, names.size)
        assertTrue(names.contains("Sol Ring"))
        assertTrue(names.contains("Commander Card"))
    }

    @Test
    fun `ParsedDeckData sideboardCardNames returns only sideboard names`() {
        val data = ParsedDeckData(
            name = "Test",
            mainboard = listOf(ParsedDeckData.CardEntry("Sol Ring", 4)),
            sideboard = listOf(
                ParsedDeckData.CardEntry("Commander Card", 1),
                ParsedDeckData.CardEntry("Counterspell", 2)
            )
        )

        val names = data.sideboardCardNames
        assertEquals(2, names.size)
        assertTrue(names.contains("Commander Card"))
        assertTrue(names.contains("Counterspell"))
        assertFalse(names.contains("Sol Ring"))
    }

    @Test
    fun `ParsedDeckData toDeck prefers mainboard commander over sideboard`() {
        // If commander is in both zones (unlikely but possible), prefer mainboard
        val data = ParsedDeckData(
            name = "Test",
            mainboard = listOf(
                ParsedDeckData.CardEntry("Edgar Markov", 1),
                ParsedDeckData.CardEntry("Plains", 99)
            ),
            sideboard = listOf(
                ParsedDeckData.CardEntry("Edgar Markov", 1)
            )
        )

        val deck = data.toDeck("Edgar Markov")

        assertEquals("Edgar Markov", deck.commander.name)
        assertEquals(99, deck.cards.size)
        // Sideboard should still have its copy
        assertEquals(1, deck.sideboard.size)
    }

    @Test
    fun `parseContent cockatrice XML with commander in sideboard zone`() {
        val content = """
            <?xml version="1.0" encoding="UTF-8"?>
            <cockatrice_deck version="1">
                <deckname>Zedruu Politics</deckname>
                <zone name="main">
                    <card name="Sol Ring" number="4"/>
                    <card name="Plains" number="95"/>
                </zone>
                <zone name="side">
                    <card name="Zedruu the Greathearted" number="1"/>
                    <card name="Counterspell" number="2"/>
                </zone>
            </cockatrice_deck>
        """.trimIndent()

        val result = DeckParser.parseContent(content, DeckFormat.COCKATRICE_XML, "Default Name")

        assertTrue(result is DeckParseResult.NeedsCommanderSelection)
        val data = (result as DeckParseResult.NeedsCommanderSelection).data
        assertEquals("Zedruu Politics", data.name)
        assertEquals(99, data.mainboardSize)
        assertEquals(3, data.sideboardSize)
        // Commander should be found in sideboard
        assertTrue(data.sideboardCardNames.contains("Zedruu the Greathearted"))
    }
}
