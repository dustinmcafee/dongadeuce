package com.dustinmcafee.dongadeuce.api

import com.dustinmcafee.dongadeuce.models.Card
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.delay
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Scryfall API client for fetching MTG card data
 * API docs: https://scryfall.com/docs/api
 */
class ScryfallApi {
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }

    private val baseUrl = "https://api.scryfall.com"

    // Scryfall requests that we wait 50-100ms between requests
    private var lastRequestTime = 0L
    private val minDelayMs = 100L

    /**
     * Search for a card by exact name
     */
    suspend fun getCardByName(name: String): Card? {
        rateLimitDelay()

        return try {
            val response = client.get("$baseUrl/cards/named") {
                parameter("exact", name)
            }

            val scryfallCard = response.body<ScryfallCard>()
            scryfallCard.toCard()
        } catch (e: Exception) {
            println("Failed to fetch card '$name': ${e.message}")
            // Return a basic card with just the name if fetch fails
            Card(name = name)
        }
    }

    /**
     * Fetch multiple cards by name (with rate limiting)
     */
    suspend fun getCardsByNames(names: List<String>): List<Card> {
        return names.map { name ->
            getCardByName(name) ?: Card(name = name)
        }
    }

    /**
     * Search for tokens by name
     */
    suspend fun searchTokens(query: String): List<Card> {
        if (query.isBlank()) return emptyList()

        rateLimitDelay()

        return try {
            val response = client.get("$baseUrl/cards/search") {
                parameter("q", "t:token $query")
                parameter("unique", "cards")
            }

            val searchResults = response.body<ScryfallSearchResults>()
            searchResults.data.map { it.toCard() }.take(20) // Limit to 20 results
        } catch (e: Exception) {
            println("Failed to search tokens for '$query': ${e.message}")
            emptyList()
        }
    }

    /**
     * Ensure we don't exceed Scryfall's rate limit
     */
    private suspend fun rateLimitDelay() {
        val now = System.currentTimeMillis()
        val timeSinceLastRequest = now - lastRequestTime

        if (timeSinceLastRequest < minDelayMs) {
            delay(minDelayMs - timeSinceLastRequest)
        }

        lastRequestTime = System.currentTimeMillis()
    }

    fun close() {
        client.close()
    }
}

/**
 * Scryfall API response model (simplified)
 */
@Serializable
data class ScryfallCard(
    val id: String,
    val name: String,
    @SerialName("mana_cost") val manaCost: String? = null,
    val cmc: Double = 0.0,
    @SerialName("type_line") val typeLine: String? = null,
    @SerialName("oracle_text") val oracleText: String? = null,
    val power: String? = null,
    val toughness: String? = null,
    val colors: List<String> = emptyList(),
    @SerialName("image_uris") val imageUris: ScryfallImageUris? = null,
    @SerialName("card_faces") val cardFaces: List<ScryfallCardFace>? = null
) {
    fun toCard(): Card {
        // Handle double-faced cards
        val mainImageUri = imageUris?.normal
            ?: cardFaces?.firstOrNull()?.imageUris?.normal

        return Card(
            name = name,
            manaCost = manaCost,
            cmc = cmc,
            type = typeLine,
            oracleText = oracleText,
            power = power,
            toughness = toughness,
            colors = colors,
            imageUri = mainImageUri,
            scryfallId = id
        )
    }
}

@Serializable
data class ScryfallSearchResults(
    val data: List<ScryfallCard> = emptyList(),
    @SerialName("has_more") val hasMore: Boolean = false
)

@Serializable
data class ScryfallImageUris(
    val small: String? = null,
    val normal: String? = null,
    val large: String? = null,
    @SerialName("png") val png: String? = null
)

@Serializable
data class ScryfallCardFace(
    val name: String,
    @SerialName("mana_cost") val manaCost: String? = null,
    @SerialName("type_line") val typeLine: String? = null,
    @SerialName("oracle_text") val oracleText: String? = null,
    val power: String? = null,
    val toughness: String? = null,
    val colors: List<String> = emptyList(),
    @SerialName("image_uris") val imageUris: ScryfallImageUris? = null
)
