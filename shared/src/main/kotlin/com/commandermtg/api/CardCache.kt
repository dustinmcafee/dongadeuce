package com.commandermtg.api

import com.commandermtg.models.Card
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import java.io.File

/**
 * Card cache system that downloads and stores bulk card data from Scryfall
 * This allows offline deck loading and eliminates per-card API calls
 */
class CardCache(
    private val cacheDir: File = File(System.getProperty("user.home"), ".commandermtg/cache")
) {
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }

    private val cacheFile = File(cacheDir, "cards.json")
    private val metadataFile = File(cacheDir, "cache-metadata.json")

    // In-memory cache for fast lookups
    private var cardMap: Map<String, Card>? = null

    init {
        cacheDir.mkdirs()
    }

    /**
     * Download and cache all cards from Scryfall bulk data
     * Returns progress updates via callback
     */
    suspend fun updateCache(onProgress: (String) -> Unit = {}) {
        try {
            onProgress("Fetching bulk data information...")

            // Get bulk data info
            val bulkDataList = client.get("https://api.scryfall.com/bulk-data").body<BulkDataList>()
            val defaultCards = bulkDataList.data.find { it.type == "default_cards" }
                ?: throw Exception("Could not find default_cards bulk data")

            onProgress("Downloading ${defaultCards.size} cards (${defaultCards.size / 1024 / 1024}MB)...")

            // Download the bulk data file
            val cardsJson: String = client.get(defaultCards.downloadUri).body()

            onProgress("Parsing card data...")

            // Parse to Scryfall format
            val json = Json {
                ignoreUnknownKeys = true
                isLenient = true
            }
            val scryfallCards = json.decodeFromString<List<ScryfallCard>>(cardsJson)

            onProgress("Converting ${scryfallCards.size} cards...")

            // Convert to our Card model
            val cards = scryfallCards.map { it.toCard() }

            onProgress("Saving cache to disk...")

            // Save to disk
            withContext(Dispatchers.IO) {
                val cardsToSave = cards.map { CardCacheEntry.fromCard(it) }
                val jsonString = json.encodeToString(cardsToSave)
                cacheFile.writeText(jsonString)

                // Save metadata
                val metadata = CacheMetadata(
                    lastUpdated = System.currentTimeMillis(),
                    cardCount = cards.size,
                    bulkDataUpdatedAt = defaultCards.updatedAt
                )
                metadataFile.writeText(json.encodeToString(metadata))
            }

            // Update in-memory cache
            cardMap = cards.associateBy { it.name.lowercase() }

            onProgress("Cache updated successfully! ${cards.size} cards cached.")
        } catch (e: Exception) {
            onProgress("Error updating cache: ${e.message}")
            throw e
        }
    }

    /**
     * Load cache into memory if not already loaded
     */
    suspend fun loadCache(): Boolean {
        if (cardMap != null) return true

        if (!cacheFile.exists()) {
            return false
        }

        return try {
            withContext(Dispatchers.IO) {
                val json = Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                }
                val jsonString = cacheFile.readText()
                val cacheEntries = json.decodeFromString<List<CardCacheEntry>>(jsonString)
                cardMap = cacheEntries.associate {
                    it.name.lowercase() to it.toCard()
                }
            }
            true
        } catch (e: Exception) {
            println("Failed to load cache: ${e.message}")
            false
        }
    }

    /**
     * Get a card by name from the cache
     */
    suspend fun getCardByName(name: String): Card? {
        // Ensure cache is loaded
        if (cardMap == null) {
            loadCache()
        }

        return cardMap?.get(name.lowercase())
    }

    /**
     * Get multiple cards by name from the cache
     */
    suspend fun getCardsByNames(names: List<String>): List<Card> {
        // Ensure cache is loaded
        if (cardMap == null) {
            loadCache()
        }

        return names.mapNotNull { name ->
            cardMap?.get(name.lowercase()) ?: Card(name = name)
        }
    }

    /**
     * Check if cache exists and when it was last updated
     */
    fun getCacheMetadata(): CacheMetadata? {
        if (!metadataFile.exists()) return null

        return try {
            val json = Json {
                ignoreUnknownKeys = true
                isLenient = true
            }
            json.decodeFromString<CacheMetadata>(metadataFile.readText())
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Check if cache exists
     */
    fun isCacheAvailable(): Boolean {
        return cacheFile.exists() && cacheFile.length() > 0
    }

    fun close() {
        client.close()
    }
}

@Serializable
data class BulkDataList(
    val data: List<BulkDataInfo>
)

@Serializable
data class BulkDataInfo(
    val type: String,
    @SerialName("download_uri") val downloadUri: String,
    @SerialName("updated_at") val updatedAt: String,
    val size: Long
)

@Serializable
data class CacheMetadata(
    val lastUpdated: Long,
    val cardCount: Int,
    val bulkDataUpdatedAt: String
)

/**
 * Simplified card representation for cache storage
 */
@Serializable
data class CardCacheEntry(
    val name: String,
    val manaCost: String? = null,
    val cmc: Double? = null,
    val type: String? = null,
    val oracleText: String? = null,
    val power: String? = null,
    val toughness: String? = null,
    val colors: List<String> = emptyList(),
    val imageUri: String? = null,
    val scryfallId: String? = null
) {
    fun toCard() = Card(
        name = name,
        manaCost = manaCost,
        cmc = cmc,
        type = type,
        oracleText = oracleText,
        power = power,
        toughness = toughness,
        colors = colors,
        imageUri = imageUri,
        scryfallId = scryfallId
    )

    companion object {
        fun fromCard(card: Card) = CardCacheEntry(
            name = card.name,
            manaCost = card.manaCost,
            cmc = card.cmc,
            type = card.type,
            oracleText = card.oracleText,
            power = card.power,
            toughness = card.toughness,
            colors = card.colors,
            imageUri = card.imageUri,
            scryfallId = card.scryfallId
        )
    }
}
