package com.dustinmcafee.dongadeuce.mcp

import com.dustinmcafee.dongadeuce.models.*
import io.modelcontextprotocol.kotlin.sdk.*
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.server.StdioServerTransport
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered
import kotlinx.serialization.json.*

private val bridge = GameBridge()

fun main() {
    System.err.println("[MCP] Starting DongADeuce MCP Server...")

    // Launch the game viewer window on the AWT thread
    val gameWindow = GameWindow(bridge)
    gameWindow.show()
    System.err.println("[MCP] Game viewer window launched")

    val server = Server(
        Implementation(name = "dongadeuce", version = "1.0.0"),
        ServerOptions(
            capabilities = ServerCapabilities(
                tools = ServerCapabilities.Tools(listChanged = true),
                resources = ServerCapabilities.Resources(subscribe = false, listChanged = false)
            )
        )
    )

    registerTools(server)
    registerResources(server)

    val transport = StdioServerTransport(
        System.`in`.asSource().buffered(),
        System.out.asSink().buffered()
    )

    System.err.println("[MCP] Server configured with ${server} tools. Connecting via stdio...")
    runBlocking {
        server.connect(transport)
        System.err.println("[MCP] Connected. Waiting for requests...")
        val done = Job()
        server.onClose { done.complete() }
        done.join()
    }
}

// ==================== TOOLS ====================

private fun registerTools(server: Server) {
    // --- Game Setup ---

    server.addTool(
        name = "initialize_game",
        description = "Initialize a new Commander game with named players. Returns player IDs needed for other tools.",
        inputSchema = Tool.Input(
            properties = buildJsonObject {
                putJsonObject("localPlayer") {
                    put("type", "string")
                    put("description", "Name of the first player")
                }
                putJsonObject("opponents") {
                    put("type", "array")
                    putJsonObject("items") { put("type", "string") }
                    put("description", "Names of opponent players (1-5 opponents)")
                }
            },
            required = listOf("localPlayer", "opponents")
        )
    ) { request ->
        val localPlayer = request.arguments["localPlayer"]?.jsonPrimitive?.content
            ?: return@addTool errorResult("'localPlayer' is required")
        val opponents = request.arguments["opponents"]?.jsonArray?.map { it.jsonPrimitive.content }
            ?: return@addTool errorResult("'opponents' is required")
        val result = bridge.initializeGame(localPlayer, opponents)
        textResult(result + "\n\n" + StateSerializer.gameStateOverview(bridge.state()))
    }

    server.addTool(
        name = "load_deck",
        description = "Load a Commander deck for a player from deck list text. Supports plain text (1 Card Name) and Cockatrice XML formats. Enriches cards from local Scryfall cache.",
        inputSchema = Tool.Input(
            properties = buildJsonObject {
                putJsonObject("playerId") {
                    put("type", "string")
                    put("description", "Player ID to load the deck for")
                }
                putJsonObject("deckContent") {
                    put("type", "string")
                    put("description", "Deck list content (plain text or Cockatrice XML)")
                }
                putJsonObject("commanderName") {
                    put("type", "string")
                    put("description", "Name of the commander card (auto-detected if omitted)")
                }
                putJsonObject("partnerCommanderName") {
                    put("type", "string")
                    put("description", "Name of partner/companion commander (for partner pairs)")
                }
                putJsonObject("deckName") {
                    put("type", "string")
                    put("description", "Name for the deck (optional)")
                }
            },
            required = listOf("playerId", "deckContent")
        )
    ) { request ->
        val playerId = request.arguments["playerId"]?.jsonPrimitive?.content
            ?: return@addTool errorResult("'playerId' is required")
        val deckContent = request.arguments["deckContent"]?.jsonPrimitive?.content
            ?: return@addTool errorResult("'deckContent' is required")
        val commanderName = request.arguments["commanderName"]?.jsonPrimitive?.contentOrNull
        val partnerCommanderName = request.arguments["partnerCommanderName"]?.jsonPrimitive?.contentOrNull
        val deckName = request.arguments["deckName"]?.jsonPrimitive?.contentOrNull ?: "MCP Deck"

        try {
            val deck = DeckLoaderBridge.loadDeck(deckContent, commanderName, partnerCommanderName, deckName)
            val result = bridge.loadDeckForPlayer(playerId, deck)
            textResult(result)
        } catch (e: Exception) {
            errorResult("Failed to load deck: ${e.message}")
        }
    }

    server.addTool(
        name = "draw_starting_hand",
        description = "Draw a starting hand of 7 cards for a player",
        inputSchema = Tool.Input(
            properties = buildJsonObject {
                putJsonObject("playerId") {
                    put("type", "string")
                    put("description", "Player ID")
                }
            },
            required = listOf("playerId")
        )
    ) { request ->
        val playerId = request.arguments["playerId"]?.jsonPrimitive?.content
            ?: return@addTool errorResult("'playerId' is required")
        textResult(bridge.drawStartingHand(playerId))
    }

    // --- Card Drawing ---

    server.addTool(
        name = "draw_card",
        description = "Draw card(s) from library to hand",
        inputSchema = Tool.Input(
            properties = buildJsonObject {
                putJsonObject("playerId") {
                    put("type", "string")
                    put("description", "Player ID")
                }
                putJsonObject("count") {
                    put("type", "integer")
                    put("description", "Number of cards to draw (default 1)")
                }
            },
            required = listOf("playerId")
        )
    ) { request ->
        val playerId = request.arguments["playerId"]?.jsonPrimitive?.content
            ?: return@addTool errorResult("'playerId' is required")
        val count = request.arguments["count"]?.jsonPrimitive?.intOrNull ?: 1
        textResult(bridge.drawCard(playerId, count))
    }

    // --- Card Movement ---

    server.addTool(
        name = "move_card",
        description = "Move a card to a different zone. Valid zones: HAND, BATTLEFIELD, GRAVEYARD, EXILE, LIBRARY, COMMAND_ZONE",
        inputSchema = Tool.Input(
            properties = buildJsonObject {
                putJsonObject("cardId") {
                    put("type", "string")
                    put("description", "Card instance ID")
                }
                putJsonObject("targetZone") {
                    put("type", "string")
                    put("enum", buildJsonArray { Zone.values().forEach { add(it.name) } })
                    put("description", "Target zone")
                }
            },
            required = listOf("cardId", "targetZone")
        )
    ) { request ->
        val cardId = request.arguments["cardId"]?.jsonPrimitive?.content
            ?: return@addTool errorResult("'cardId' is required")
        val zoneName = request.arguments["targetZone"]?.jsonPrimitive?.content
            ?: return@addTool errorResult("'targetZone' is required")
        val zone = try { Zone.valueOf(zoneName.uppercase()) } catch (e: Exception) {
            return@addTool errorResult("Invalid zone: $zoneName. Valid: ${Zone.values().joinToString()}")
        }
        textResult(bridge.moveCard(cardId, zone))
    }

    // --- Card State ---

    server.addTool(
        name = "toggle_tap",
        description = "Tap or untap a card on the battlefield",
        inputSchema = Tool.Input(
            properties = buildJsonObject {
                putJsonObject("cardId") {
                    put("type", "string")
                    put("description", "Card instance ID")
                }
            },
            required = listOf("cardId")
        )
    ) { request ->
        val cardId = request.arguments["cardId"]?.jsonPrimitive?.content
            ?: return@addTool errorResult("'cardId' is required")
        textResult(bridge.toggleTap(cardId))
    }

    server.addTool(
        name = "untap_all",
        description = "Untap all permanents for a player (respects 'doesn't untap' flags)",
        inputSchema = Tool.Input(
            properties = buildJsonObject {
                putJsonObject("playerId") {
                    put("type", "string")
                    put("description", "Player ID")
                }
            },
            required = listOf("playerId")
        )
    ) { request ->
        val playerId = request.arguments["playerId"]?.jsonPrimitive?.content
            ?: return@addTool errorResult("'playerId' is required")
        textResult(bridge.untapAll(playerId))
    }

    // --- Life ---

    server.addTool(
        name = "update_life",
        description = "Set a player's life total to a specific value",
        inputSchema = Tool.Input(
            properties = buildJsonObject {
                putJsonObject("playerId") {
                    put("type", "string")
                    put("description", "Player ID")
                }
                putJsonObject("life") {
                    put("type", "integer")
                    put("description", "New life total")
                }
            },
            required = listOf("playerId", "life")
        )
    ) { request ->
        val playerId = request.arguments["playerId"]?.jsonPrimitive?.content
            ?: return@addTool errorResult("'playerId' is required")
        val life = request.arguments["life"]?.jsonPrimitive?.intOrNull
            ?: return@addTool errorResult("'life' is required")
        textResult(bridge.updateLife(playerId, life))
    }

    server.addTool(
        name = "change_life",
        description = "Change a player's life by a relative amount (positive or negative)",
        inputSchema = Tool.Input(
            properties = buildJsonObject {
                putJsonObject("playerId") {
                    put("type", "string")
                    put("description", "Player ID")
                }
                putJsonObject("amount") {
                    put("type", "integer")
                    put("description", "Amount to change (positive=gain, negative=lose)")
                }
            },
            required = listOf("playerId", "amount")
        )
    ) { request ->
        val playerId = request.arguments["playerId"]?.jsonPrimitive?.content
            ?: return@addTool errorResult("'playerId' is required")
        val amount = request.arguments["amount"]?.jsonPrimitive?.intOrNull
            ?: return@addTool errorResult("'amount' is required")
        textResult(bridge.changeLife(playerId, amount))
    }

    // --- Turn/Phase ---

    server.addTool(
        name = "pass_turn",
        description = "Pass the turn to the next player",
        inputSchema = Tool.Input(properties = buildJsonObject {}, required = emptyList())
    ) { _ -> textResult(bridge.passTurn()) }

    server.addTool(
        name = "next_phase",
        description = "Advance to the next game phase",
        inputSchema = Tool.Input(properties = buildJsonObject {}, required = emptyList())
    ) { _ -> textResult(bridge.nextPhase()) }

    server.addTool(
        name = "set_phase",
        description = "Set the current phase directly. Phases: UNTAP, UPKEEP, DRAW, MAIN_1, COMBAT_BEGIN, COMBAT_DECLARE_ATTACKERS, COMBAT_DECLARE_BLOCKERS, COMBAT_DAMAGE, COMBAT_END, MAIN_2, END, CLEANUP",
        inputSchema = Tool.Input(
            properties = buildJsonObject {
                putJsonObject("phase") {
                    put("type", "string")
                    put("description", "Phase name")
                }
            },
            required = listOf("phase")
        )
    ) { request ->
        val phase = request.arguments["phase"]?.jsonPrimitive?.content
            ?: return@addTool errorResult("'phase' is required")
        textResult(bridge.setPhase(phase))
    }

    // --- Library ---

    server.addTool(
        name = "shuffle_library",
        description = "Shuffle a player's library",
        inputSchema = Tool.Input(
            properties = buildJsonObject {
                putJsonObject("playerId") { put("type", "string") }
            },
            required = listOf("playerId")
        )
    ) { request ->
        val playerId = request.arguments["playerId"]?.jsonPrimitive?.content
            ?: return@addTool errorResult("'playerId' is required")
        textResult(bridge.shuffleLibrary(playerId))
    }

    server.addTool(
        name = "mill_cards",
        description = "Mill cards from top of library to graveyard",
        inputSchema = Tool.Input(
            properties = buildJsonObject {
                putJsonObject("playerId") { put("type", "string") }
                putJsonObject("count") { put("type", "integer") }
            },
            required = listOf("playerId", "count")
        )
    ) { request ->
        val playerId = request.arguments["playerId"]?.jsonPrimitive?.content
            ?: return@addTool errorResult("'playerId' is required")
        val count = request.arguments["count"]?.jsonPrimitive?.intOrNull
            ?: return@addTool errorResult("'count' is required")
        textResult(bridge.millCards(playerId, count))
    }

    server.addTool(
        name = "mulligan",
        description = "Take a mulligan - return hand to library, shuffle, draw 7 new cards",
        inputSchema = Tool.Input(
            properties = buildJsonObject {
                putJsonObject("playerId") { put("type", "string") }
            },
            required = listOf("playerId")
        )
    ) { request ->
        val playerId = request.arguments["playerId"]?.jsonPrimitive?.content
            ?: return@addTool errorResult("'playerId' is required")
        textResult(bridge.mulligan(playerId))
    }

    // --- Counters ---

    server.addTool(
        name = "add_counter",
        description = "Add counter(s) to a card. Common types: +1/+1, -1/-1, charge, loyalty, poison, custom",
        inputSchema = Tool.Input(
            properties = buildJsonObject {
                putJsonObject("cardId") { put("type", "string") }
                putJsonObject("type") { put("type", "string"); put("description", "Counter type") }
                putJsonObject("amount") { put("type", "integer"); put("description", "Amount (default 1)") }
            },
            required = listOf("cardId", "type")
        )
    ) { request ->
        val cardId = request.arguments["cardId"]?.jsonPrimitive?.content ?: return@addTool errorResult("'cardId' required")
        val type = request.arguments["type"]?.jsonPrimitive?.content ?: return@addTool errorResult("'type' required")
        val amount = request.arguments["amount"]?.jsonPrimitive?.intOrNull ?: 1
        textResult(bridge.addCounter(cardId, type, amount))
    }

    server.addTool(
        name = "add_player_counter",
        description = "Add counter(s) to a player. Types: poison, energy, experience",
        inputSchema = Tool.Input(
            properties = buildJsonObject {
                putJsonObject("playerId") { put("type", "string") }
                putJsonObject("type") { put("type", "string") }
                putJsonObject("amount") { put("type", "integer") }
            },
            required = listOf("playerId", "type")
        )
    ) { request ->
        val playerId = request.arguments["playerId"]?.jsonPrimitive?.content ?: return@addTool errorResult("'playerId' required")
        val type = request.arguments["type"]?.jsonPrimitive?.content ?: return@addTool errorResult("'type' required")
        val amount = request.arguments["amount"]?.jsonPrimitive?.intOrNull ?: 1
        textResult(bridge.addPlayerCounter(playerId, type, amount))
    }

    // --- Tokens & Clones ---

    server.addTool(
        name = "create_token",
        description = "Create token creature(s) on the battlefield",
        inputSchema = Tool.Input(
            properties = buildJsonObject {
                putJsonObject("playerId") { put("type", "string") }
                putJsonObject("name") { put("type", "string"); put("description", "Token name (e.g., Soldier)") }
                putJsonObject("type") { put("type", "string"); put("description", "Type line (e.g., Creature — Soldier)") }
                putJsonObject("power") { put("type", "string"); put("description", "Power (e.g., 1)") }
                putJsonObject("toughness") { put("type", "string"); put("description", "Toughness (e.g., 1)") }
                putJsonObject("color") { put("type", "string"); put("description", "Color (e.g., White)") }
                putJsonObject("quantity") { put("type", "integer"); put("description", "How many tokens (default 1)") }
            },
            required = listOf("playerId", "name", "type", "color")
        )
    ) { request ->
        val playerId = request.arguments["playerId"]?.jsonPrimitive?.content ?: return@addTool errorResult("'playerId' required")
        val name = request.arguments["name"]?.jsonPrimitive?.content ?: return@addTool errorResult("'name' required")
        val type = request.arguments["type"]?.jsonPrimitive?.content ?: return@addTool errorResult("'type' required")
        val power = request.arguments["power"]?.jsonPrimitive?.contentOrNull
        val toughness = request.arguments["toughness"]?.jsonPrimitive?.contentOrNull
        val color = request.arguments["color"]?.jsonPrimitive?.content ?: return@addTool errorResult("'color' required")
        val quantity = request.arguments["quantity"]?.jsonPrimitive?.intOrNull ?: 1
        textResult(bridge.createToken(playerId, name, type, power, toughness, color, quantity))
    }

    // --- Advanced ---

    server.addTool(
        name = "flip_card",
        description = "Flip a double-faced card to show its other face",
        inputSchema = Tool.Input(
            properties = buildJsonObject { putJsonObject("cardId") { put("type", "string") } },
            required = listOf("cardId")
        )
    ) { request ->
        val cardId = request.arguments["cardId"]?.jsonPrimitive?.content ?: return@addTool errorResult("'cardId' required")
        textResult(bridge.flipCard(cardId))
    }

    server.addTool(
        name = "concede",
        description = "A player concedes the game (sets life to 0, marks as lost)",
        inputSchema = Tool.Input(
            properties = buildJsonObject { putJsonObject("playerId") { put("type", "string") } },
            required = listOf("playerId")
        )
    ) { request ->
        val playerId = request.arguments["playerId"]?.jsonPrimitive?.content ?: return@addTool errorResult("'playerId' required")
        textResult(bridge.concede(playerId))
    }

    // --- Query tool for finding cards ---

    server.addTool(
        name = "get_game_state",
        description = "Get the current game state overview including all players, life totals, turn/phase, and card counts per zone",
        inputSchema = Tool.Input(properties = buildJsonObject {}, required = emptyList())
    ) { _ ->
        textResult(StateSerializer.gameStateOverview(bridge.state()))
    }

    server.addTool(
        name = "get_cards_in_zone",
        description = "Get all cards in a specific zone for a player. Returns card details including instanceId (needed for other tools). For LIBRARY zone, returns only the count by default (use includeAll=true to get full card list).",
        inputSchema = Tool.Input(
            properties = buildJsonObject {
                putJsonObject("playerId") { put("type", "string") }
                putJsonObject("zone") {
                    put("type", "string")
                    put("enum", buildJsonArray { Zone.values().forEach { add(it.name) } })
                }
                putJsonObject("includeAll") {
                    put("type", "boolean")
                    put("description", "If true, return full card list even for LIBRARY (default false)")
                }
            },
            required = listOf("playerId", "zone")
        )
    ) { request ->
        val playerId = request.arguments["playerId"]?.jsonPrimitive?.content ?: return@addTool errorResult("'playerId' required")
        val zoneName = request.arguments["zone"]?.jsonPrimitive?.content ?: return@addTool errorResult("'zone' required")
        val includeAll = request.arguments["includeAll"]?.jsonPrimitive?.booleanOrNull ?: false
        val zone = try { Zone.valueOf(zoneName.uppercase()) } catch (e: Exception) {
            return@addTool errorResult("Invalid zone: $zoneName")
        }
        // For LIBRARY, return just count unless includeAll is set
        if (zone == Zone.LIBRARY && !includeAll) {
            val gs = bridge.state().gameState
            val count = gs?.cardInstances?.count { it.ownerId == playerId && it.zone == zone } ?: 0
            textResult("{\"zone\": \"LIBRARY\", \"count\": $count, \"hint\": \"Use includeAll=true to see all cards\"}")
        } else {
            textResult(StateSerializer.cardsInZoneJson(bridge.state(), playerId, zone))
        }
    }

    server.addTool(
        name = "get_game_log",
        description = "Get recent game events (log/history)",
        inputSchema = Tool.Input(
            properties = buildJsonObject {
                putJsonObject("count") { put("type", "integer"); put("description", "Number of recent events (default 20)") }
            },
            required = emptyList()
        )
    ) { request ->
        val count = request.arguments["count"]?.jsonPrimitive?.intOrNull ?: 20
        textResult(StateSerializer.gameLogJson(bridge.state(), count))
    }
}

// ==================== RESOURCES ====================

private fun registerResources(server: Server) {
    server.addResource(
        uri = "game://state",
        name = "Game State",
        description = "Full game state overview with players, turn, phase, and card counts",
        mimeType = "application/json"
    ) { _ ->
        ReadResourceResult(
            contents = listOf(TextResourceContents(
                text = StateSerializer.gameStateOverview(bridge.state()),
                uri = "game://state",
                mimeType = "application/json"
            ))
        )
    }

    server.addResource(
        uri = "game://players",
        name = "Players",
        description = "All player information (life, counters, loss state)",
        mimeType = "application/json"
    ) { _ ->
        ReadResourceResult(
            contents = listOf(TextResourceContents(
                text = StateSerializer.playersJson(bridge.state()),
                uri = "game://players",
                mimeType = "application/json"
            ))
        )
    }

    server.addResource(
        uri = "game://log",
        name = "Game Log",
        description = "Recent game events",
        mimeType = "application/json"
    ) { _ ->
        ReadResourceResult(
            contents = listOf(TextResourceContents(
                text = StateSerializer.gameLogJson(bridge.state()),
                uri = "game://log",
                mimeType = "application/json"
            ))
        )
    }
}

// ==================== HELPERS ====================

private fun textResult(text: String) = CallToolResult(content = listOf(TextContent(text)))
private fun errorResult(message: String) = CallToolResult(content = listOf(TextContent("Error: $message")), isError = true)
