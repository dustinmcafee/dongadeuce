package com.dustinmcafee.dongadeuce.api

import com.dustinmcafee.dongadeuce.models.Card
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
// Import Scryfall types from ScryfallApi
import com.dustinmcafee.dongadeuce.api.ScryfallCard

/**
 * Card cache system that downloads and stores bulk card data from Scryfall
 * This allows offline deck loading and eliminates per-card API calls
 */
class CardCache(
    private val cacheDir: File = File(System.getProperty("user.home"), ".commandermtg/cache")
) {
    // Shared JSON configuration
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(json)
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
            println("[CardCache] Starting cache update...")

            // Get bulk data info
            val bulkDataList = client.get("https://api.scryfall.com/bulk-data").body<BulkDataList>()
            println("[CardCache] Got bulk data list: ${bulkDataList.data.size} items")

            val defaultCards = bulkDataList.data.find { it.type == "default_cards" }
                ?: throw Exception("Could not find default_cards bulk data")

            val sizeMB = defaultCards.size / 1024 / 1024
            println("[CardCache] Downloading from: ${defaultCards.downloadUri}")
            println("[CardCache] Expected size: $sizeMB MB")

            onProgress("Connecting to download server...", 0f)

            // Download Scryfall JSON directly to cache file (they're already in correct format)
            withContext(Dispatchers.IO) {
                println("[CardCache] Starting HTTP GET request...")
                val response: HttpResponse = client.get(defaultCards.downloadUri)
                println("[CardCache] Got response, status: ${response.status}")

                onProgress("Connected! Starting download...", 1f)

                val contentLength = defaultCards.size
                val channel: ByteReadChannel = response.bodyAsChannel()
                println("[CardCache] Starting stream download to: ${cacheFile.absolutePath}")

                cacheFile.outputStream().buffered(1024 * 1024).use { output ->
                    val buffer = ByteArray(1024 * 1024) // 1MB buffer
                    var downloadedBytes = 0L
                    var lastReportedTime = System.currentTimeMillis()
                    var readCount = 0

                    while (true) {
                        val bytesRead = channel.readAvailable(buffer, 0, buffer.size)
                        readCount++

                        if (readCount % 100 == 0) {
                            println("[CardCache] Read operation #$readCount, downloaded: ${downloadedBytes / 1024 / 1024} MB")
                        }

                        // Break on end of stream
                        if (bytesRead == -1) {
                            println("[CardCache] End of stream reached. Total downloaded: ${downloadedBytes / 1024 / 1024} MB")
                            break
                        }

                        if (bytesRead > 0) {
                            output.write(buffer, 0, bytesRead)
                            downloadedBytes += bytesRead

                            val currentTime = System.currentTimeMillis()
                            val downloadedMB = downloadedBytes / 1024 / 1024
                            val percent = (downloadedBytes.toFloat() / contentLength * 100).coerceIn(0f, 100f)

                            // Report progress every 500ms OR every MB, whichever comes first
                            if (currentTime - lastReportedTime >= 500) {
                                onProgress("Downloaded $downloadedMB / $sizeMB MB", percent)
                                lastReportedTime = currentTime
                                println("[CardCache] Progress: ${percent.toInt()}% ($downloadedMB MB / $sizeMB MB)")
                            }
                        }
                    }
                }

                println("[CardCache] File written. File size: ${cacheFile.length() / 1024 / 1024} MB")
            }

            onProgress("Download complete!", 90f)

            // Save metadata (don't load cache yet - too much memory)
            withContext(Dispatchers.IO) {
                val metadata = CacheMetadata(
                    lastUpdated = System.currentTimeMillis(),
                    cardCount = 0, // Unknown without parsing entire file
                    bulkDataUpdatedAt = defaultCards.updatedAt
                )
                metadataFile.writeText(json.encodeToString(metadata))
                println("[CardCache] Metadata saved")
            }

            println("[CardCache] Cache update complete!")
            onProgress("Cache ready for use!", 100f)
        } catch (e: Exception) {
            println("[CardCache] ERROR: ${e.javaClass.simpleName}: ${e.message}")
            e.printStackTrace()
            onProgress("Error updating cache: ${e.message}", 0f)
            throw e
        }
    }

    /**
     * Load cache into memory if not already loaded
     * Parses Scryfall bulk JSON format
     */
    suspend fun loadCache(): Boolean {
        if (cardMap != null) return true

        if (!cacheFile.exists()) {
            return false
        }

        return try {
            withContext(Dispatchers.IO) {
                val jsonString = cacheFile.readText()
                // Parse Scryfall format directly
                val scryfallCards = json.decodeFromString<List<ScryfallCard>>(jsonString)
                cardMap = scryfallCards.associate {
                    it.name.lowercase() to it.toCard()
                }
            }
            true
        } catch (e: Exception) {
            println("Failed to load cache: ${e.message}")
            e.printStackTrace()
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
