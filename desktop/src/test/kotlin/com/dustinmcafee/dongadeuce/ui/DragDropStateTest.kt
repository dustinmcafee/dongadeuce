package com.dustinmcafee.dongadeuce.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.dustinmcafee.dongadeuce.models.Card
import com.dustinmcafee.dongadeuce.models.CardInstance
import com.dustinmcafee.dongadeuce.models.Zone
import kotlin.test.*

class DragDropStateTest {

    private lateinit var dragDropState: DragDropState

    private fun createTestCard(instanceId: String = "card-1"): CardInstance {
        return CardInstance(
            instanceId = instanceId,
            card = Card(name = "Test Card"),
            ownerId = "player-1",
            zone = Zone.HAND
        )
    }

    @BeforeTest
    fun setup() {
        dragDropState = DragDropState()
    }

    // ==================== Initial State Tests ====================

    @Test
    fun `initial state is not dragging`() {
        assertFalse(dragDropState.isDragging)
        assertNull(dragDropState.draggedCard)
        assertTrue(dragDropState.draggedCardIds.isEmpty())
        assertEquals(Offset.Zero, dragDropState.dragOffset)
        assertNull(dragDropState.hoveredZone)
        assertFalse(dragDropState.wasHandledByZone)
    }

    // ==================== Start Drag Tests ====================

    @Test
    fun `startDrag sets dragging state`() {
        val card = createTestCard()
        dragDropState.startDrag(card)

        assertTrue(dragDropState.isDragging)
        assertEquals(card, dragDropState.draggedCard)
        assertEquals(setOf("card-1"), dragDropState.draggedCardIds)
    }

    @Test
    fun `startDrag with offset sets initial offset`() {
        val card = createTestCard()
        val offset = Offset(100f, 200f)
        dragDropState.startDrag(card, offset)

        assertEquals(offset, dragDropState.dragOffset)
    }

    @Test
    fun `startDrag resets wasHandledByZone`() {
        dragDropState.markHandledByZone()
        val card = createTestCard()
        dragDropState.startDrag(card)

        assertFalse(dragDropState.wasHandledByZone)
    }

    @Test
    fun `startDrag resets hoveredZone`() {
        dragDropState.setHoveredZone(Zone.GRAVEYARD)
        val card = createTestCard()
        dragDropState.startDrag(card)

        assertNull(dragDropState.hoveredZone)
    }

    // ==================== Start Drag Multiple Tests ====================

    @Test
    fun `startDragMultiple sets multiple card ids`() {
        val cardIds = setOf("card-1", "card-2", "card-3")
        dragDropState.startDragMultiple(cardIds)

        assertTrue(dragDropState.isDragging)
        assertEquals(cardIds, dragDropState.draggedCardIds)
        assertNull(dragDropState.draggedCard) // Single card not tracked in multi-drag
    }

    @Test
    fun `startDragMultiple with offset sets initial offset`() {
        val cardIds = setOf("card-1", "card-2")
        val offset = Offset(50f, 75f)
        dragDropState.startDragMultiple(cardIds, offset)

        assertEquals(offset, dragDropState.dragOffset)
    }

    @Test
    fun `startDragMultiple with empty set still sets dragging`() {
        dragDropState.startDragMultiple(emptySet())

        assertTrue(dragDropState.isDragging)
        assertTrue(dragDropState.draggedCardIds.isEmpty())
    }

    // ==================== Update Drag Position Tests ====================

    @Test
    fun `updateDragPosition changes offset`() {
        val card = createTestCard()
        dragDropState.startDrag(card)

        val newOffset = Offset(300f, 400f)
        dragDropState.updateDragPosition(newOffset)

        assertEquals(newOffset, dragDropState.dragOffset)
    }

    @Test
    fun `updateDragPosition can be called multiple times`() {
        val card = createTestCard()
        dragDropState.startDrag(card)

        dragDropState.updateDragPosition(Offset(100f, 100f))
        dragDropState.updateDragPosition(Offset(200f, 200f))
        dragDropState.updateDragPosition(Offset(300f, 300f))

        assertEquals(Offset(300f, 300f), dragDropState.dragOffset)
    }

    @Test
    fun `updateDragPosition handles negative offsets`() {
        val card = createTestCard()
        dragDropState.startDrag(card)

        val negativeOffset = Offset(-50f, -100f)
        dragDropState.updateDragPosition(negativeOffset)

        assertEquals(negativeOffset, dragDropState.dragOffset)
    }

    // ==================== End Drag Tests ====================

    @Test
    fun `endDrag resets all state`() {
        val card = createTestCard()
        dragDropState.startDrag(card, Offset(100f, 200f))
        dragDropState.setHoveredZone(Zone.BATTLEFIELD)
        dragDropState.markHandledByZone()

        dragDropState.endDrag()

        assertFalse(dragDropState.isDragging)
        assertNull(dragDropState.draggedCard)
        assertTrue(dragDropState.draggedCardIds.isEmpty())
        assertEquals(Offset.Zero, dragDropState.dragOffset)
        assertNull(dragDropState.hoveredZone)
        assertFalse(dragDropState.wasHandledByZone)
    }

    @Test
    fun `endDrag on non-dragging state is safe`() {
        dragDropState.endDrag()

        assertFalse(dragDropState.isDragging)
    }

    // ==================== Hovered Zone Tests ====================

    @Test
    fun `setHoveredZone updates hovered zone`() {
        dragDropState.setHoveredZone(Zone.GRAVEYARD)
        assertEquals(Zone.GRAVEYARD, dragDropState.hoveredZone)
    }

    @Test
    fun `setHoveredZone to null clears hovered zone`() {
        dragDropState.setHoveredZone(Zone.BATTLEFIELD)
        dragDropState.setHoveredZone(null)

        assertNull(dragDropState.hoveredZone)
    }

    @Test
    fun `setHoveredZone can change zones`() {
        dragDropState.setHoveredZone(Zone.HAND)
        dragDropState.setHoveredZone(Zone.EXILE)

        assertEquals(Zone.EXILE, dragDropState.hoveredZone)
    }

    // ==================== Zone Handled Tests ====================

    @Test
    fun `markHandledByZone sets flag`() {
        dragDropState.markHandledByZone()
        assertTrue(dragDropState.wasHandledByZone)
    }

    @Test
    fun `wasHandledByZone is reset on startDrag`() {
        dragDropState.markHandledByZone()
        dragDropState.startDrag(createTestCard())

        assertFalse(dragDropState.wasHandledByZone)
    }

    // ==================== Zone Bounds Tests ====================

    @Test
    fun `registerZoneBounds stores zone bounds`() {
        val bounds = Rect(0f, 0f, 100f, 100f)
        dragDropState.registerZoneBounds(Zone.GRAVEYARD, bounds)

        // Test via getZoneAtPosition
        assertEquals(Zone.GRAVEYARD, dragDropState.getZoneAtPosition(Offset(50f, 50f)))
    }

    @Test
    fun `registerZoneBounds overwrites previous bounds`() {
        dragDropState.registerZoneBounds(Zone.GRAVEYARD, Rect(0f, 0f, 100f, 100f))
        dragDropState.registerZoneBounds(Zone.GRAVEYARD, Rect(200f, 200f, 300f, 300f))

        // Old position should not match
        assertNull(dragDropState.getZoneAtPosition(Offset(50f, 50f)))
        // New position should match
        assertEquals(Zone.GRAVEYARD, dragDropState.getZoneAtPosition(Offset(250f, 250f)))
    }

    @Test
    fun `getZoneAtPosition returns correct zone`() {
        dragDropState.registerZoneBounds(Zone.GRAVEYARD, Rect(0f, 0f, 100f, 100f))
        dragDropState.registerZoneBounds(Zone.EXILE, Rect(100f, 0f, 200f, 100f))
        dragDropState.registerZoneBounds(Zone.BATTLEFIELD, Rect(0f, 100f, 200f, 300f))

        assertEquals(Zone.GRAVEYARD, dragDropState.getZoneAtPosition(Offset(50f, 50f)))
        assertEquals(Zone.EXILE, dragDropState.getZoneAtPosition(Offset(150f, 50f)))
        assertEquals(Zone.BATTLEFIELD, dragDropState.getZoneAtPosition(Offset(100f, 200f)))
    }

    @Test
    fun `getZoneAtPosition returns null for position outside all zones`() {
        dragDropState.registerZoneBounds(Zone.GRAVEYARD, Rect(0f, 0f, 100f, 100f))

        assertNull(dragDropState.getZoneAtPosition(Offset(500f, 500f)))
    }

    @Test
    fun `getZoneAtPosition returns null when no zones registered`() {
        assertNull(dragDropState.getZoneAtPosition(Offset(50f, 50f)))
    }

    @Test
    fun `getZoneAtPosition handles edge of bounds`() {
        dragDropState.registerZoneBounds(Zone.GRAVEYARD, Rect(0f, 0f, 100f, 100f))

        // On the boundary - depends on Rect.contains implementation
        // Typically includes left/top edge, excludes right/bottom
        assertEquals(Zone.GRAVEYARD, dragDropState.getZoneAtPosition(Offset(0f, 0f)))
    }

    // ==================== DropTarget Enum Tests ====================

    @Test
    fun `DropTarget BATTLEFIELD converts to Zone BATTLEFIELD`() {
        assertEquals(Zone.BATTLEFIELD, DropTarget.BATTLEFIELD.toZone())
    }

    @Test
    fun `DropTarget GRAVEYARD converts to Zone GRAVEYARD`() {
        assertEquals(Zone.GRAVEYARD, DropTarget.GRAVEYARD.toZone())
    }

    @Test
    fun `DropTarget EXILE converts to Zone EXILE`() {
        assertEquals(Zone.EXILE, DropTarget.EXILE.toZone())
    }

    @Test
    fun `DropTarget LIBRARY converts to Zone LIBRARY`() {
        assertEquals(Zone.LIBRARY, DropTarget.LIBRARY.toZone())
    }

    @Test
    fun `DropTarget COMMAND_ZONE converts to Zone COMMAND_ZONE`() {
        assertEquals(Zone.COMMAND_ZONE, DropTarget.COMMAND_ZONE.toZone())
    }

    @Test
    fun `DropTarget NONE converts to null`() {
        assertNull(DropTarget.NONE.toZone())
    }

    // ==================== Complex Scenarios ====================

    @Test
    fun `complete drag lifecycle single card`() {
        val card = createTestCard()

        // Start
        dragDropState.startDrag(card, Offset(10f, 10f))
        assertTrue(dragDropState.isDragging)

        // Update position
        dragDropState.updateDragPosition(Offset(100f, 100f))

        // Hover over zone
        dragDropState.setHoveredZone(Zone.GRAVEYARD)
        assertEquals(Zone.GRAVEYARD, dragDropState.hoveredZone)

        // Mark as handled
        dragDropState.markHandledByZone()
        assertTrue(dragDropState.wasHandledByZone)

        // End
        dragDropState.endDrag()
        assertFalse(dragDropState.isDragging)
    }

    @Test
    fun `complete drag lifecycle multiple cards`() {
        val cardIds = setOf("card-1", "card-2", "card-3")

        // Start multi-drag
        dragDropState.startDragMultiple(cardIds, Offset(0f, 0f))
        assertTrue(dragDropState.isDragging)
        assertEquals(3, dragDropState.draggedCardIds.size)

        // Simulate drag movement
        dragDropState.updateDragPosition(Offset(50f, 50f))
        dragDropState.updateDragPosition(Offset(100f, 100f))

        // End
        dragDropState.endDrag()
        assertFalse(dragDropState.isDragging)
        assertTrue(dragDropState.draggedCardIds.isEmpty())
    }

    @Test
    fun `starting new drag cancels previous drag`() {
        val card1 = createTestCard("card-1")
        val card2 = createTestCard("card-2")

        dragDropState.startDrag(card1, Offset(10f, 10f))
        dragDropState.updateDragPosition(Offset(50f, 50f))

        // Start new drag (without ending first)
        dragDropState.startDrag(card2, Offset(0f, 0f))

        assertEquals(card2, dragDropState.draggedCard)
        assertEquals(setOf("card-2"), dragDropState.draggedCardIds)
        assertEquals(Offset(0f, 0f), dragDropState.dragOffset)
    }
}
