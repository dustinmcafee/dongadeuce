package com.commandermtg.models

import kotlin.test.*

class DeckTest {

    @Test
    fun `deck with exactly 99 cards is created successfully`() {
        val commander = Card(name = "Test Commander", type = "Legendary Creature")
        val cards = (1..99).map { Card(name = "Card $it") }

        val deck = Deck(name = "Test Deck", commander = commander, cards = cards)

        assertEquals(99, deck.cards.size, "Deck should have 99 cards")
        assertEquals(100, deck.totalCards, "Total cards should be 100 (99 + commander)")
    }

    @Test
    fun `deck with less than 99 cards throws exception`() {
        val commander = Card(name = "Test Commander", type = "Legendary Creature")
        val cards = (1..98).map { Card(name = "Card $it") }

        assertFailsWith<IllegalArgumentException> {
            Deck(name = "Test Deck", commander = commander, cards = cards)
        }
    }

    @Test
    fun `deck with more than 99 cards throws exception`() {
        val commander = Card(name = "Test Commander", type = "Legendary Creature")
        val cards = (1..100).map { Card(name = "Card $it") }

        assertFailsWith<IllegalArgumentException> {
            Deck(name = "Test Deck", commander = commander, cards = cards)
        }
    }

    @Test
    fun `totalCards includes commander`() {
        val commander = Card(name = "Test Commander", type = "Legendary Creature")
        val cards = (1..99).map { Card(name = "Card $it") }
        val deck = Deck(name = "Test Deck", commander = commander, cards = cards)

        assertEquals(100, deck.totalCards, "Total cards should be 100")
    }

    @Test
    fun `isValid returns true for valid singleton deck with legendary commander`() {
        val commander = Card(name = "Test Commander", type = "Legendary Creature - Human")
        val cards = (1..99).map { Card(name = "Card $it") }
        val deck = Deck(name = "Test Deck", commander = commander, cards = cards)

        assertTrue(deck.isValid(), "Deck with unique cards and legendary commander should be valid")
    }

    @Test
    fun `isValid returns false for non-legendary commander`() {
        val commander = Card(name = "Test Commander", type = "Creature - Human")
        val cards = (1..99).map { Card(name = "Card $it") }
        val deck = Deck(name = "Test Deck", commander = commander, cards = cards)

        assertFalse(deck.isValid(), "Deck with non-legendary commander should be invalid")
    }

    @Test
    fun `isValid returns false for duplicate non-basic cards`() {
        val commander = Card(name = "Test Commander", type = "Legendary Creature")
        val cards = mutableListOf<Card>()

        // Add 98 unique cards
        (1..98).forEach { cards.add(Card(name = "Card $it")) }

        // Add a duplicate of Card 1
        cards.add(Card(name = "Card 1"))

        val deck = Deck(name = "Test Deck", commander = commander, cards = cards)

        assertFalse(deck.isValid(), "Deck with duplicate non-basic cards should be invalid")
    }

    @Test
    fun `isValid allows multiple basic lands`() {
        val commander = Card(name = "Test Commander", type = "Legendary Creature")
        val cards = mutableListOf<Card>()

        // Add 20 Plains
        repeat(20) { cards.add(Card(name = "Plains", type = "Basic Land")) }

        // Add 20 Islands
        repeat(20) { cards.add(Card(name = "Island", type = "Basic Land")) }

        // Add 20 Swamps
        repeat(20) { cards.add(Card(name = "Swamp", type = "Basic Land")) }

        // Add 20 Mountains
        repeat(20) { cards.add(Card(name = "Mountain", type = "Basic Land")) }

        // Add 19 Forests
        repeat(19) { cards.add(Card(name = "Forest", type = "Basic Land")) }

        val deck = Deck(name = "Test Deck", commander = commander, cards = cards)

        assertTrue(deck.isValid(), "Deck with multiple basic lands should be valid")
    }

    @Test
    fun `isValid allows Wastes duplicates`() {
        val commander = Card(name = "Test Commander", type = "Legendary Creature")
        val cards = mutableListOf<Card>()

        // Add 50 Wastes
        repeat(50) { cards.add(Card(name = "Wastes", type = "Basic Land")) }

        // Add 49 unique cards
        (1..49).forEach { cards.add(Card(name = "Card $it")) }

        val deck = Deck(name = "Test Deck", commander = commander, cards = cards)

        assertTrue(deck.isValid(), "Deck with multiple Wastes should be valid")
    }

    @Test
    fun `isValid handles mix of basic and non-basic lands`() {
        val commander = Card(name = "Test Commander", type = "Legendary Creature")
        val cards = mutableListOf<Card>()

        // Add 30 Plains
        repeat(30) { cards.add(Card(name = "Plains", type = "Basic Land")) }

        // Add 69 unique non-basic cards
        (1..69).forEach { cards.add(Card(name = "Card $it")) }

        val deck = Deck(name = "Test Deck", commander = commander, cards = cards)

        assertTrue(deck.isValid(), "Deck with mix of basic and non-basic cards should be valid")
    }

    @Test
    fun `isValid catches duplicate with one basic and one non-basic duplicate`() {
        val commander = Card(name = "Test Commander", type = "Legendary Creature")
        val cards = mutableListOf<Card>()

        // Add 10 Plains (valid duplicates)
        repeat(10) { cards.add(Card(name = "Plains", type = "Basic Land")) }

        // Add 88 unique cards
        (1..88).forEach { cards.add(Card(name = "Card $it")) }

        // Add a duplicate of Card 1 (invalid)
        cards.add(Card(name = "Card 1"))

        val deck = Deck(name = "Test Deck", commander = commander, cards = cards)

        assertFalse(deck.isValid(), "Deck with duplicate non-basic card should be invalid even with valid basic duplicates")
    }

    @Test
    fun `isValid with all unique cards and legendary commander`() {
        val commander = Card(name = "Commander", type = "Legendary Creature - Dragon")
        val cards = (1..99).map { Card(name = "Card $it", type = "Creature") }
        val deck = Deck(name = "Dragon Deck", commander = commander, cards = cards)

        assertTrue(deck.isValid(), "Deck should be valid")
    }

    @Test
    fun `commander with Legendary in type is detected correctly`() {
        val commander1 = Card(name = "Test", type = "Legendary Creature")
        assertTrue(commander1.isLegendary, "Should be legendary")

        val commander2 = Card(name = "Test", type = "Legendary Planeswalker")
        assertTrue(commander2.isLegendary, "Should be legendary")

        val commander3 = Card(name = "Test", type = "Creature")
        assertFalse(commander3.isLegendary, "Should not be legendary")
    }

    @Test
    fun `basic land names are recognized correctly`() {
        val commander = Card(name = "Test Commander", type = "Legendary Creature")
        val cards = mutableListOf<Card>()

        // Test all basic land types
        val basicLandNames = listOf("Plains", "Island", "Swamp", "Mountain", "Forest", "Wastes")
        basicLandNames.forEach { landName ->
            // Add 10 of each basic land type
            repeat(10) { cards.add(Card(name = landName, type = "Basic Land")) }
        }

        // Fill remaining slots with unique cards
        val remaining = 99 - cards.size
        (1..remaining).forEach { cards.add(Card(name = "Card $it")) }

        val deck = Deck(name = "Test Deck", commander = commander, cards = cards)

        assertTrue(deck.isValid(), "All basic land types should be recognized")
    }
}
