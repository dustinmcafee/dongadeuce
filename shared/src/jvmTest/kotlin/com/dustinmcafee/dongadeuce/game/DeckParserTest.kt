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
}
