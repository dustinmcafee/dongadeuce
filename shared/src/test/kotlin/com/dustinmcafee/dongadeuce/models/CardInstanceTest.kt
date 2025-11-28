package com.dustinmcafee.dongadeuce.models

import kotlin.test.*

class CardInstanceTest {

    private fun createTestCard(name: String = "Test Card"): Card {
        return Card(name = name, type = "Creature", power = "2", toughness = "2")
    }

    private fun createTestCardInstance(
        card: Card = createTestCard(),
        ownerId: String = "player-1",
        zone: Zone = Zone.BATTLEFIELD
    ): CardInstance {
        return CardInstance(
            card = card,
            ownerId = ownerId,
            zone = zone
        )
    }

    // Tap/Untap Tests

    @Test
    fun `tap sets isTapped to true`() {
        val card = createTestCardInstance()
        assertFalse(card.isTapped, "Card should start untapped")

        val tapped = card.tap()

        assertTrue(tapped.isTapped, "Card should be tapped after tap()")
    }

    @Test
    fun `untap sets isTapped to false`() {
        val card = createTestCardInstance().tap()
        assertTrue(card.isTapped, "Card should be tapped")

        val untapped = card.untap()

        assertFalse(untapped.isTapped, "Card should be untapped after untap()")
    }

    @Test
    fun `tap on already tapped card stays tapped`() {
        val card = createTestCardInstance().tap()
        assertTrue(card.isTapped)

        val stillTapped = card.tap()

        assertTrue(stillTapped.isTapped, "Card should remain tapped")
    }

    // Flip Tests

    @Test
    fun `flip toggles isFlipped from false to true`() {
        val card = createTestCardInstance()
        assertFalse(card.isFlipped, "Card should start unflipped")

        val flipped = card.flip()

        assertTrue(flipped.isFlipped, "Card should be flipped after flip()")
    }

    @Test
    fun `flip toggles isFlipped from true to false`() {
        val card = createTestCardInstance().flip()
        assertTrue(card.isFlipped)

        val unflipped = card.flip()

        assertFalse(unflipped.isFlipped, "Card should be unflipped after second flip()")
    }

    // Zone Tests

    @Test
    fun `moveToZone updates zone`() {
        val card = createTestCardInstance(zone = Zone.HAND)
        assertEquals(Zone.HAND, card.zone)

        val moved = card.moveToZone(Zone.BATTLEFIELD)

        assertEquals(Zone.BATTLEFIELD, moved.zone, "Card should be on battlefield")
    }

    @Test
    fun `moveToZone preserves other properties`() {
        val card = createTestCardInstance(zone = Zone.HAND)
            .tap()
            .addCounter("+1/+1", 3)

        val moved = card.moveToZone(Zone.GRAVEYARD)

        assertEquals(Zone.GRAVEYARD, moved.zone)
        assertTrue(moved.isTapped, "Tap state should be preserved")
        assertEquals(3, moved.counters["+1/+1"], "Counters should be preserved")
    }

    // Counter Tests

    @Test
    fun `addCounter increments counter`() {
        val card = createTestCardInstance()
            .addCounter("+1/+1", 2)

        val updated = card.addCounter("+1/+1", 3)

        assertEquals(5, updated.counters["+1/+1"], "Counter should be incremented to 5")
    }

    @Test
    fun `addCounter creates new counter type`() {
        val card = createTestCardInstance()
        assertTrue(card.counters.isEmpty(), "Card should start with no counters")

        val updated = card.addCounter("charge", 4)

        assertEquals(4, updated.counters["charge"], "Should have 4 charge counters")
    }

    @Test
    fun `addCounter with default amount adds 1`() {
        val card = createTestCardInstance()

        val updated = card.addCounter("+1/+1")

        assertEquals(1, updated.counters["+1/+1"], "Should have 1 counter with default amount")
    }

    @Test
    fun `addCounter with negative amount removes counters`() {
        val card = createTestCardInstance()
            .addCounter("+1/+1", 5)

        val updated = card.addCounter("+1/+1", -2)

        assertEquals(3, updated.counters["+1/+1"], "Should have 3 counters after removing 2")
    }

    @Test
    fun `addCounter can go negative`() {
        val card = createTestCardInstance()
            .addCounter("+1/+1", 2)

        val updated = card.addCounter("+1/+1", -5)

        assertEquals(-3, updated.counters["+1/+1"], "Counter can go negative")
    }

    @Test
    fun `multiple counter types tracked independently`() {
        val card = createTestCardInstance()
            .addCounter("+1/+1", 3)
            .addCounter("charge", 5)
            .addCounter("loyalty", 2)

        assertEquals(3, card.counters["+1/+1"])
        assertEquals(5, card.counters["charge"])
        assertEquals(2, card.counters["loyalty"])
    }

    // Grid Position Tests

    @Test
    fun `setGridPosition updates coordinates`() {
        val card = createTestCardInstance()
        assertNull(card.gridX)
        assertNull(card.gridY)

        val positioned = card.setGridPosition(5, 3)

        assertEquals(5, positioned.gridX, "GridX should be 5")
        assertEquals(3, positioned.gridY, "GridY should be 3")
    }

    @Test
    fun `setGridPosition updates placedTimestamp`() {
        val card = createTestCardInstance()
        val originalTimestamp = card.placedTimestamp

        Thread.sleep(10) // Ensure some time passes
        val positioned = card.setGridPosition(1, 1)

        assertTrue(positioned.placedTimestamp > originalTimestamp, "Timestamp should be updated")
    }

    // Controller Tests

    @Test
    fun `changeController updates controllerId`() {
        val card = createTestCardInstance(ownerId = "player-1")
        assertEquals("player-1", card.controllerId, "Controller should default to owner")

        val controlled = card.changeController("player-2")

        assertEquals("player-2", controlled.controllerId, "Controller should be changed")
        assertEquals("player-1", controlled.ownerId, "Owner should remain unchanged")
    }

    // Clone Tests

    @Test
    fun `createClone creates new instance with different ID`() {
        val original = createTestCardInstance()

        val clone = original.createClone("player-2")

        assertNotEquals(original.instanceId, clone.instanceId, "Clone should have different ID")
    }

    @Test
    fun `createClone sets isClone flag`() {
        val original = createTestCardInstance()
        assertFalse(original.isClone, "Original should not be a clone")

        val clone = original.createClone("player-2")

        assertTrue(clone.isClone, "Clone should have isClone = true")
    }

    @Test
    fun `createClone references original via clonedFromId`() {
        val original = createTestCardInstance()

        val clone = original.createClone("player-2")

        assertEquals(original.instanceId, clone.clonedFromId, "Clone should reference original")
    }

    @Test
    fun `createClone resets state`() {
        val original = createTestCardInstance()
            .tap()
            .flip()
            .addCounter("+1/+1", 5)
            .copy(powerModifier = 3, toughnessModifier = 2, doesntUntap = true, annotation = "note")

        val clone = original.createClone("player-2")

        assertFalse(clone.isTapped, "Clone should be untapped")
        assertFalse(clone.isFlipped, "Clone should not be flipped")
        assertFalse(clone.isFaceDown, "Clone should not be face down")
        assertTrue(clone.counters.isEmpty(), "Clone should have no counters")
        assertEquals(0, clone.powerModifier, "Clone should have no power modifier")
        assertEquals(0, clone.toughnessModifier, "Clone should have no toughness modifier")
        assertFalse(clone.doesntUntap, "Clone should untap normally")
        assertNull(clone.annotation, "Clone should have no annotation")
        assertNull(clone.gridX, "Clone should have no grid position")
        assertNull(clone.gridY, "Clone should have no grid position")
    }

    @Test
    fun `createClone preserves card data`() {
        val original = createTestCardInstance(card = createTestCard("Sol Ring"))

        val clone = original.createClone("player-2")

        assertEquals("Sol Ring", clone.card.name, "Clone should have same card")
    }

    @Test
    fun `createClone sets owner and controller to new player`() {
        val original = createTestCardInstance(ownerId = "player-1")

        val clone = original.createClone("player-2")

        assertEquals("player-2", clone.ownerId, "Clone owner should be new player")
        assertEquals("player-2", clone.controllerId, "Clone controller should be new player")
    }

    @Test
    fun `createClone uses specified zone`() {
        val original = createTestCardInstance(zone = Zone.GRAVEYARD)

        val cloneToHand = original.createClone("player-2", Zone.HAND)
        val cloneToBattlefield = original.createClone("player-2", Zone.BATTLEFIELD)

        assertEquals(Zone.HAND, cloneToHand.zone, "Clone should be in hand")
        assertEquals(Zone.BATTLEFIELD, cloneToBattlefield.zone, "Clone should be on battlefield")
    }

    @Test
    fun `createClone default zone is battlefield`() {
        val original = createTestCardInstance(zone = Zone.GRAVEYARD)

        val clone = original.createClone("player-2")

        assertEquals(Zone.BATTLEFIELD, clone.zone, "Default clone zone should be battlefield")
    }

    // Copy Tests (data class functionality)

    @Test
    fun `copy preserves all fields`() {
        val original = CardInstance(
            card = createTestCard("Test"),
            ownerId = "player-1",
            controllerId = "player-2",
            zone = Zone.BATTLEFIELD,
            isTapped = true,
            isFlipped = true,
            isFaceDown = true,
            counters = mapOf("+1/+1" to 3),
            attachedTo = "some-id",
            gridX = 5,
            gridY = 10,
            powerModifier = 2,
            toughnessModifier = -1,
            doesntUntap = true,
            annotation = "test note",
            isClone = true,
            clonedFromId = "original-id",
            handPosition = 3
        )

        val copy = original.copy()

        assertEquals(original.instanceId, copy.instanceId)
        assertEquals(original.card.name, copy.card.name)
        assertEquals(original.ownerId, copy.ownerId)
        assertEquals(original.controllerId, copy.controllerId)
        assertEquals(original.zone, copy.zone)
        assertEquals(original.isTapped, copy.isTapped)
        assertEquals(original.isFlipped, copy.isFlipped)
        assertEquals(original.isFaceDown, copy.isFaceDown)
        assertEquals(original.counters, copy.counters)
        assertEquals(original.attachedTo, copy.attachedTo)
        assertEquals(original.gridX, copy.gridX)
        assertEquals(original.gridY, copy.gridY)
        assertEquals(original.powerModifier, copy.powerModifier)
        assertEquals(original.toughnessModifier, copy.toughnessModifier)
        assertEquals(original.doesntUntap, copy.doesntUntap)
        assertEquals(original.annotation, copy.annotation)
        assertEquals(original.isClone, copy.isClone)
        assertEquals(original.clonedFromId, copy.clonedFromId)
        assertEquals(original.handPosition, copy.handPosition)
    }

    // Default Values Tests

    @Test
    fun `new CardInstance has correct defaults`() {
        val card = CardInstance(
            card = createTestCard(),
            ownerId = "player-1",
            zone = Zone.HAND
        )

        assertEquals("player-1", card.controllerId, "Controller defaults to owner")
        assertFalse(card.isTapped, "Should start untapped")
        assertFalse(card.isFlipped, "Should start unflipped")
        assertFalse(card.isFaceDown, "Should start face up")
        assertTrue(card.counters.isEmpty(), "Should have no counters")
        assertNull(card.attachedTo, "Should not be attached")
        assertNull(card.gridX, "Should have no grid position")
        assertNull(card.gridY, "Should have no grid position")
        assertEquals(0, card.powerModifier, "Should have no power modifier")
        assertEquals(0, card.toughnessModifier, "Should have no toughness modifier")
        assertFalse(card.doesntUntap, "Should untap normally")
        assertNull(card.annotation, "Should have no annotation")
        assertFalse(card.isClone, "Should not be a clone")
        assertNull(card.clonedFromId, "Should have no clone source")
        assertNull(card.handPosition, "Should have no hand position")
    }
}
