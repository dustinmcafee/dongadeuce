package com.dustinmcafee.dongadeuce.mcp

import com.dustinmcafee.dongadeuce.game.DeckFormat
import com.dustinmcafee.dongadeuce.game.DeckParser
import com.dustinmcafee.dongadeuce.game.DeckParseResult
import com.dustinmcafee.dongadeuce.models.Card
import com.dustinmcafee.dongadeuce.models.Deck
import kotlinx.serialization.json.*
import java.io.File

/**
 * Loads decks from text content and enriches card data from the local Scryfall cache.
 * Reads ~/.commandermtg/cache/cards.json directly (avoids Ktor version conflicts).
 */
object DeckLoaderBridge {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    // Lazy-loaded card data cache (name -> Card)
    private var cardMap: Map<String, Card>? = null

    private fun getCacheFile(): File {
        val home = System.getProperty("user.home")
        return File(home, ".commandermtg/cache/cards.json")
    }

    /**
     * Load the card cache into memory. This reads the 519MB Scryfall bulk data file.
     * Only loads once; subsequent calls return immediately.
     */
    fun ensureCacheLoaded() {
        if (cardMap != null) return

        val cacheFile = getCacheFile()
        if (!cacheFile.exists()) {
            System.err.println("[DeckLoader] Card cache not found at ${cacheFile.absolutePath}")
            System.err.println("[DeckLoader] Run the app and download the card cache first.")
            cardMap = emptyMap()
            return
        }

        System.err.println("[DeckLoader] Loading card cache from ${cacheFile.absolutePath}...")
        val startTime = System.currentTimeMillis()

        try {
            val map = mutableMapOf<String, Card>()
            // Track which entries are non-playable so playable versions can overwrite
            val isNonPlayable = mutableSetOf<String>()
            val content = cacheFile.readText()
            val cards = json.decodeFromString<List<JsonObject>>(content)

            val nonPlayableLayouts = setOf("art_series", "double_faced_token", "emblem")

            for (cardObj in cards) {
                val name = cardObj["name"]?.jsonPrimitive?.contentOrNull ?: continue
                val layout = cardObj["layout"]?.jsonPrimitive?.contentOrNull
                val cardIsNonPlayable = layout in nonPlayableLayouts

                // Skip non-playable cards entirely
                if (cardIsNonPlayable) continue

                val nameKey = name.lowercase()

                // Skip if we already have a playable version of this card
                if (nameKey in map && nameKey !in isNonPlayable) continue

                val card = scryfallJsonToCard(cardObj)
                map[nameKey] = card
                if (cardIsNonPlayable) isNonPlayable.add(nameKey) else isNonPlayable.remove(nameKey)

                // Also index by front face name for DFCs (only for playable cards)
                if (name.contains(" // ") && !cardIsNonPlayable) {
                    val faces = name.split(" // ")
                    for (face in faces) {
                        val faceKey = face.trim().lowercase()
                        if (faceKey !in map || faceKey in isNonPlayable) {
                            map[faceKey] = card
                            isNonPlayable.remove(faceKey)
                        }
                    }
                }
            }

            cardMap = map
            val elapsed = System.currentTimeMillis() - startTime
            System.err.println("[DeckLoader] Loaded ${map.size} unique cards in ${elapsed}ms")
        } catch (e: Exception) {
            System.err.println("[DeckLoader] Failed to load cache: ${e.message}")
            cardMap = emptyMap()
        }
    }

    private fun scryfallJsonToCard(obj: JsonObject): Card {
        val name = obj["name"]?.jsonPrimitive?.contentOrNull ?: "Unknown"
        val frontFace = obj["card_faces"]?.jsonArray?.firstOrNull()?.jsonObject
        val backFace = obj["card_faces"]?.jsonArray?.getOrNull(1)?.jsonObject

        // For DFCs, use front face name instead of combined "Front // Back"
        val cardName = if (frontFace != null && name.contains(" // ")) {
            frontFace["name"]?.jsonPrimitive?.contentOrNull ?: name
        } else name

        // Fall back to front face data for DFCs (top-level fields are often null)
        val manaCost = obj["mana_cost"]?.jsonPrimitive?.contentOrNull
            ?: frontFace?.get("mana_cost")?.jsonPrimitive?.contentOrNull
        val cmc = obj["cmc"]?.jsonPrimitive?.doubleOrNull
        val typeLine = obj["type_line"]?.jsonPrimitive?.contentOrNull
            ?: frontFace?.get("type_line")?.jsonPrimitive?.contentOrNull
        val oracleText = obj["oracle_text"]?.jsonPrimitive?.contentOrNull
            ?: frontFace?.get("oracle_text")?.jsonPrimitive?.contentOrNull
        val power = obj["power"]?.jsonPrimitive?.contentOrNull
            ?: frontFace?.get("power")?.jsonPrimitive?.contentOrNull
        val toughness = obj["toughness"]?.jsonPrimitive?.contentOrNull
            ?: frontFace?.get("toughness")?.jsonPrimitive?.contentOrNull
        val colors = obj["colors"]?.jsonArray?.mapNotNull { it.jsonPrimitive.contentOrNull }
            ?: frontFace?.get("colors")?.jsonArray?.mapNotNull { it.jsonPrimitive.contentOrNull }
            ?: emptyList()

        // Image URI
        val imageUri = obj["image_uris"]?.jsonObject?.get("normal")?.jsonPrimitive?.contentOrNull
            ?: frontFace?.get("image_uris")?.jsonObject?.get("normal")?.jsonPrimitive?.contentOrNull

        // Back face data for DFCs
        val backFaceImageUri = backFace?.get("image_uris")?.jsonObject?.get("normal")?.jsonPrimitive?.contentOrNull
        val backFaceName = backFace?.get("name")?.jsonPrimitive?.contentOrNull

        val scryfallId = obj["id"]?.jsonPrimitive?.contentOrNull

        return Card(
            name = cardName,
            manaCost = manaCost,
            cmc = cmc,
            type = typeLine,
            oracleText = oracleText,
            power = power,
            toughness = toughness,
            colors = colors,
            imageUri = imageUri,
            scryfallId = scryfallId,
            backFaceImageUri = backFaceImageUri,
            backFaceName = backFaceName,
            backFaceType = backFace?.get("type_line")?.jsonPrimitive?.contentOrNull,
            backFaceOracleText = backFace?.get("oracle_text")?.jsonPrimitive?.contentOrNull,
            backFaceManaCost = backFace?.get("mana_cost")?.jsonPrimitive?.contentOrNull,
            backFacePower = backFace?.get("power")?.jsonPrimitive?.contentOrNull,
            backFaceToughness = backFace?.get("toughness")?.jsonPrimitive?.contentOrNull
        )
    }

    fun lookupCard(name: String): Card? {
        return cardMap?.get(name.lowercase())
    }

    /**
     * Parse deck content and create a Deck with enriched card data.
     */
    fun loadDeck(
        deckContent: String,
        commanderName: String? = null,
        partnerCommanderName: String? = null,
        deckName: String = "MCP Deck"
    ): Deck {
        // Detect format
        val format = if (deckContent.trimStart().startsWith("<")) {
            DeckFormat.COCKATRICE_XML
        } else {
            DeckFormat.PLAIN_TEXT
        }

        val parseResult = DeckParser.parseContent(deckContent, format, deckName)

        val deck = when (parseResult) {
            is DeckParseResult.Complete -> parseResult.deck
            is DeckParseResult.NeedsCommanderSelection -> {
                val data = parseResult.data
                // Use provided commander name or auto-select
                val selectedCommander = if (commanderName != null) {
                    commanderName
                } else {
                    // Try to find a legendary creature in the deck
                    ensureCacheLoaded()
                    val allNames = data.allCardNamesIncludingSideboard
                    val legendary = allNames.firstOrNull { name ->
                        val card = lookupCard(name)
                        card != null && card.isLegendary && card.canBeCommander
                    }
                    legendary ?: allNames.first() // Fallback to first card
                }
                data.toDeck(selectedCommander, partnerCommanderName)
            }
            is DeckParseResult.Error -> throw IllegalArgumentException(parseResult.message)
        }

        // Enrich card data from cache
        ensureCacheLoaded()

        val enrichedCommander = lookupCard(deck.commander.name) ?: deck.commander
        val enrichedPartner = deck.partnerCommander?.let { lookupCard(it.name) ?: it }
        val enrichedCards = deck.cards.map { card -> lookupCard(card.name) ?: card }
        val enrichedSideboard = deck.sideboard.map { card -> lookupCard(card.name) ?: card }

        return Deck(
            name = deck.name,
            commander = enrichedCommander,
            cards = enrichedCards,
            sideboard = enrichedSideboard,
            partnerCommander = enrichedPartner
        )
    }
}
