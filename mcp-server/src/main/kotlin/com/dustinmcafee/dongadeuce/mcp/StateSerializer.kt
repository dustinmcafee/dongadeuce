package com.dustinmcafee.dongadeuce.mcp

import com.dustinmcafee.dongadeuce.models.*
import com.dustinmcafee.dongadeuce.viewmodel.GameUiState
import kotlinx.serialization.json.*

/**
 * Helpers to serialize game state into compact, readable JSON for MCP responses.
 */
object StateSerializer {
    private val json = Json { prettyPrint = true; encodeDefaults = false }

    fun gameStateOverview(state: GameUiState): String {
        val gs = state.gameState ?: return """{"error": "No active game"}"""
        return buildJsonObject {
            put("turnNumber", gs.turnNumber)
            put("phase", gs.phase.name)
            put("activePlayer", gs.activePlayer.name)
            put("activePlayerId", gs.activePlayer.id)
            putJsonArray("players") {
                gs.players.forEach { p ->
                    addJsonObject {
                        put("id", p.id)
                        put("name", p.name)
                        put("life", p.life)
                        put("hasLost", p.hasLost)
                        if (p.counters.isNotEmpty()) {
                            putJsonObject("counters") {
                                p.counters.forEach { (k, v) -> put(k, v) }
                            }
                        }
                        // Card counts per zone
                        putJsonObject("cardCounts") {
                            Zone.values().forEach { zone ->
                                val count = if (zone == Zone.BATTLEFIELD) {
                                    gs.cardInstances.count { it.controllerId == p.id && it.zone == zone }
                                } else {
                                    gs.cardInstances.count { it.ownerId == p.id && it.zone == zone }
                                }
                                if (count > 0) put(zone.name, count)
                            }
                        }
                    }
                }
            }
        }.let { json.encodeToString(JsonObject.serializer(), it) }
    }

    fun playersJson(state: GameUiState): String {
        val gs = state.gameState ?: return "[]"
        return buildJsonArray {
            gs.players.forEach { p ->
                addJsonObject {
                    put("id", p.id)
                    put("name", p.name)
                    put("life", p.life)
                    put("hasLost", p.hasLost)
                    if (p.counters.isNotEmpty()) {
                        putJsonObject("counters") {
                            p.counters.forEach { (k, v) -> put(k, v) }
                        }
                    }
                    if (p.commanderDamage.isNotEmpty()) {
                        putJsonObject("commanderDamage") {
                            p.commanderDamage.forEach { (k, v) -> put(k, v) }
                        }
                    }
                }
            }
        }.let { json.encodeToString(JsonArray.serializer(), it) }
    }

    fun cardsInZoneJson(state: GameUiState, playerId: String, zone: Zone): String {
        val gs = state.gameState ?: return "[]"
        val cards = if (zone == Zone.BATTLEFIELD) {
            gs.cardInstances.filter { it.controllerId == playerId && it.zone == zone }
        } else {
            gs.cardInstances.filter { it.ownerId == playerId && it.zone == zone }
        }
        return cardsToJson(cards)
    }

    fun cardToJsonObject(card: CardInstance): JsonObject = buildJsonObject {
        put("instanceId", card.instanceId)
        put("name", card.card.name)
        card.card.type?.let { put("type", it) }
        card.card.manaCost?.let { put("manaCost", it) }
        put("zone", card.zone.name)
        put("ownerId", card.ownerId)
        if (card.controllerId != card.ownerId) put("controllerId", card.controllerId)
        if (card.isTapped) put("isTapped", true)
        if (card.isFlipped) put("isFlipped", true)
        if (card.isFaceDown) put("isFaceDown", true)
        if (card.isToken) put("isToken", true)
        if (card.isClone) put("isClone", true)
        if (card.doesntUntap) put("doesntUntap", true)
        if (card.counters.isNotEmpty()) {
            putJsonObject("counters") {
                card.counters.forEach { (k, v) -> put(k, v) }
            }
        }
        // Power/Toughness
        card.card.power?.let { basePower ->
            val mod = card.powerModifier
            if (mod != 0) {
                put("power", "${basePower.toIntOrNull()?.plus(mod) ?: basePower}")
                put("basePower", basePower)
            } else {
                put("power", basePower)
            }
        }
        card.card.toughness?.let { baseToughness ->
            val mod = card.toughnessModifier
            if (mod != 0) {
                put("toughness", "${baseToughness.toIntOrNull()?.plus(mod) ?: baseToughness}")
                put("baseToughness", baseToughness)
            } else {
                put("toughness", baseToughness)
            }
        }
        card.annotation?.let { put("annotation", it) }
        card.attachedTo?.let { put("attachedTo", it) }
        if (card.gridX != null && card.gridY != null) {
            put("gridX", card.gridX!!)
            put("gridY", card.gridY!!)
        }
        card.card.oracleText?.let { put("oracleText", it) }
    }

    fun cardsToJson(cards: List<CardInstance>): String {
        return buildJsonArray {
            cards.forEach { add(cardToJsonObject(it)) }
        }.let { json.encodeToString(JsonArray.serializer(), it) }
    }

    fun gameLogJson(state: GameUiState, count: Int = 50): String {
        val gs = state.gameState ?: return "[]"
        val events = gs.gameLog.takeLast(count)
        return buildJsonArray {
            events.forEach { event ->
                addJsonObject {
                    put("type", event::class.simpleName ?: "Unknown")
                    put("player", event.playerName)
                    put("display", event.toDisplayString())
                }
            }
        }.let { json.encodeToString(JsonArray.serializer(), it) }
    }
}
