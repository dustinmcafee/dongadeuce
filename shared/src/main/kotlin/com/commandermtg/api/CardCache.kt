package com.commandermtg.api

import com.commandermtg.models.Card
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.utils.io.*
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

        // Increase timeouts for very large bulk data download (500MB+ file)
        install(HttpTimeout) {
            requestTimeoutMillis = 900_000 // 15 minutes
            connectTimeoutMillis = 60_000 // 60 seconds
            socketTimeoutMillis = 900_000 // 15 minutes
        }

        // Configure engine for large downloads
        engine {
            pipelining = true
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
    suspend fun updateCache(onProgress: (message: String, percent: Float) -> Unit = { _, _ -> }) {
        try {
            onProgress("Fetching bulk data information...", 0f)

            // Get bulk data info
            val bulkDataList = client.get("https://api.scryfall.com/bulk-data").body<BulkDataList>()
            val defaultCards = bulkDataList.data.find { it.type == "default_cards" }
                ?: throw Exception("Could not find default_cards bulk data")

            val sizeMB = defaultCards.size / 1024 / 1024
            onProgress("Starting download of $sizeMB MB...", 1f)

            println("Downloading from: ${defaultCards.downloadUri}")
            println("Expected size: $sizeMB MB")

            // Download to temporary file first
            val tempFile = File(cacheDir, "download.tmp")
            val cardsJson: String = withContext(Dispatchers.IO) {
                val response: HttpResponse = client.get(defaultCards.downloadUri)
                val contentLength = defaultCards.size
                val channel: ByteReadChannel = response.bodyAsChannel()

                tempFile.outputStream().use { output ->
                    val buffer = ByteArray(1024 * 1024) // 1MB buffer
                    var downloadedBytes = 0L
                    var lastReportedPercent = 0f

                    while (!channel.isClosedForRead) {
                        val bytesRead = channel.readAvailable(buffer, 0, buffer.size)

                        if (bytesRead > 0) {
                            output.write(buffer, 0, bytesRead)
                            downloadedBytes += bytesRead

                            val percent = (downloadedBytes.toFloat() / contentLength * 100).coerceIn(0f, 100f)
                            val currentPercent = percent.toInt().toFloat()

                            // Report progress when percentage changes
                            if (currentPercent > lastReportedPercent) {
                                val downloadedMB = downloadedBytes / 1024 / 1024
                                onProgress("Downloaded $downloadedMB / $sizeMB MB", currentPercent)
                                lastReportedPercent = currentPercent
                                println("Progress: $currentPercent% ($downloadedMB MB / $sizeMB MB)")
                            }
                        }
                    }
                }

                // Read the file
                onProgress("Reading downloaded file...", 92f)
                val json = tempFile.readText()
                tempFile.delete()
                json
            }

            onProgress("Parsing card data...", 95f)

            // Parse to Scryfall format
            val json = Json {
                ignoreUnknownKeys = true
                isLenient = true
            }
            val scryfallCards = json.decodeFromString<List<ScryfallCard>>(cardsJson)

            onProgress("Converting ${scryfallCards.size} cards...", 97f)

            // Convert to our Card model
            val cards = scryfallCards.map { it.toCard() }

            onProgress("Saving cache to disk...", 98f)

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

            onProgress("Cache updated successfully! ${cards.size} cards cached.", 100f)
        } catch (e: Exception) {
            onProgress("Error updating cache: ${e.message}", 0f)
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
