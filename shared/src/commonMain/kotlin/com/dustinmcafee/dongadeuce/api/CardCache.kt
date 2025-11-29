package com.dustinmcafee.dongadeuce.api

import com.dustinmcafee.dongadeuce.models.Card
import com.dustinmcafee.dongadeuce.platform.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

/**
 * Card cache system that downloads and stores bulk card data from Scryfall
 * This allows offline deck loading and eliminates per-card API calls
 */
class CardCache(
    private val cacheDir: FileHandle = getAppDataDirectory().child("cache")
) {
    companion object {
        /** Enable verbose debug logging */
        var DEBUG = false

        private fun log(message: String) {
            if (DEBUG) println("[CardCache] $message")
        }

        private fun logAlways(message: String) {
            println("[CardCache] $message")
        }
    }

    // Shared JSON configuration
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val client = HttpClient(createHttpClientEngine()) {
        install(ContentNegotiation) {
            json(json)
        }

        // Increase timeouts for very large bulk data download (500MB+ file)
        install(HttpTimeout) {
            requestTimeoutMillis = 900_000 // 15 minutes
            connectTimeoutMillis = 60_000 // 60 seconds
            socketTimeoutMillis = 900_000 // 15 minutes
        }
    }

    private val cacheFile = cacheDir.child("cards.json")
    private val metadataFile = cacheDir.child("cache-metadata.json")

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
            logAlways("Starting cache update...")

            // Get bulk data info
            val bulkDataList = client.get("https://api.scryfall.com/bulk-data").body<BulkDataList>()
            log("Got bulk data list: ${bulkDataList.data.size} items")

            val defaultCards = bulkDataList.data.find { it.type == "default_cards" }
                ?: throw Exception("Could not find default_cards bulk data")

            val sizeMB = defaultCards.size / 1024 / 1024
            log("Downloading from: ${defaultCards.downloadUri}")
            log("Expected size: $sizeMB MB")

            onProgress("Connecting to download server...", 0f)

            // Use platform-specific streaming download to bypass Ktor's internal buffering
            // which causes OOM for very large files (500MB+)
            log("Starting streaming download to: ${cacheFile.path}")

            if (cacheFile.exists()) {
                cacheFile.delete()
            }

            streamingDownload(
                url = defaultCards.downloadUri,
                outputFile = cacheFile,
                expectedSize = defaultCards.size
            ) { downloadedBytes, totalBytes ->
                val downloadedMB = downloadedBytes / 1024 / 1024
                val percent = (downloadedBytes.toFloat() / totalBytes * 100).coerceIn(0f, 100f)
                onProgress("Downloaded $downloadedMB / $sizeMB MB", percent)
                log("Progress: ${percent.toInt()}% ($downloadedMB MB / $sizeMB MB)")
            }

            log("File written. File size: ${cacheFile.length() / 1024 / 1024} MB")

            onProgress("Download complete! Counting cards...", 90f)

            // Count cards by streaming through the file (memory-efficient)
            val cardCount = countCardsInFile()
            log("Counted $cardCount cards in file")

            // Save metadata with card count
            withContext(ioDispatcher) {
                val metadata = CacheMetadata(
                    lastUpdated = currentTimeMillis(),
                    cardCount = cardCount,
                    bulkDataUpdatedAt = defaultCards.updatedAt
                )
                metadataFile.writeText(json.encodeToString(metadata))
                log("Metadata saved with card count: $cardCount")
            }

            logAlways("Cache update complete!")
            onProgress("Cache ready! $cardCount cards available", 100f)
        } catch (e: Exception) {
            logAlways("ERROR: ${e::class.simpleName}: ${e.message}")
            e.printStackTrace()
            onProgress("Error updating cache: ${e.message}", 0f)
            throw e
        }
    }

    /**
     * Load cache into memory if not already loaded
     * Parses Scryfall bulk JSON format
     * Prefers: paper printings over digital, most recent release date
     */
    suspend fun loadCache(): Boolean {
        if (cardMap != null) return true

        if (!cacheFile.exists()) {
            return false
        }

        return try {
            withContext(ioDispatcher) {
                val jsonString = cacheFile.readText()
                // Parse Scryfall format directly
                val scryfallCards = json.decodeFromString<List<ScryfallCard>>(jsonString)

                // Group cards by name and select the best printing for each
                val cardsByName = mutableMapOf<String, ScryfallCard>()

                for (card in scryfallCards) {
                    val nameKey = card.name.lowercase()
                    val existing = cardsByName[nameKey]

                    if (existing == null) {
                        // First time seeing this card
                        cardsByName[nameKey] = card
                    } else {
                        // Compare and keep the better printing
                        val newCard = selectBetterPrinting(existing, card)
                        cardsByName[nameKey] = newCard
                    }
                }

                cardMap = cardsByName.mapValues { it.value.toCard() }

                // Update metadata with actual card count now that we've parsed the file
                val existingMetadata = getCacheMetadata()
                if (existingMetadata != null && existingMetadata.cardCount == 0) {
                    val updatedMetadata = existingMetadata.copy(cardCount = cardsByName.size)
                    metadataFile.writeText(json.encodeToString(updatedMetadata))
                    log("Updated metadata with card count: ${cardsByName.size}")
                }
            }
            true
        } catch (e: Exception) {
            logAlways("Failed to load cache: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    /**
     * Select the better printing between two cards with the same name.
     * Priority:
     * 1. Paper over digital-only
     * 2. More recent release date
     */
    private fun selectBetterPrinting(existing: ScryfallCard, candidate: ScryfallCard): ScryfallCard {
        val existingIsPaper = existing.isPaper()
        val candidateIsPaper = candidate.isPaper()

        // If one is paper and the other isn't, prefer paper
        if (existingIsPaper && !candidateIsPaper) {
            return existing
        }
        if (candidateIsPaper && !existingIsPaper) {
            return candidate
        }

        // Both are same type (both paper or both digital), compare release dates
        // Prefer more recent (later date string comparison works for YYYY-MM-DD format)
        val existingDate = existing.releasedAt ?: "0000-00-00"
        val candidateDate = candidate.releasedAt ?: "0000-00-00"

        return if (candidateDate > existingDate) candidate else existing
    }

    /**
     * Get a card by name from the cache.
     * Uses in-memory cache if loaded, otherwise streams through the file.
     */
    suspend fun getCardByName(name: String): Card? {
        // Try in-memory cache first
        cardMap?.get(name.lowercase())?.let { return it }

        // Otherwise search the file (streaming, no OOM)
        return findCardInFile(name)
    }

    /**
     * Get multiple cards by name from the cache.
     * Uses streaming lookups to avoid OOM on Android.
     */
    suspend fun getCardsByNames(names: List<String>): List<Card> {
        // Batch lookup - search file once for all names
        return findCardsInFile(names)
    }

    /**
     * Stream through the cache file to find a specific card by name.
     * Memory-efficient but slower than in-memory lookup.
     */
    private suspend fun findCardInFile(name: String): Card? {
        val results = findCardsInFile(listOf(name))
        return results.firstOrNull { it.name.equals(name, ignoreCase = true) }
    }

    /**
     * Stream through the cache file to find multiple cards by name.
     * Single-pass algorithm: extracts each card object once, checks name against HashSet.
     * O(fileSize) instead of O(fileSize * numCards).
     */
    private suspend fun findCardsInFile(names: List<String>): List<Card> = withContext(ioDispatcher) {
        if (!cacheFile.exists()) return@withContext names.map { Card(name = it) }
        if (names.isEmpty()) return@withContext emptyList()

        // Pre-compute lowercase names for O(1) lookup
        val namesToFind = names.map { it.lowercase() }.toMutableSet()
        val foundCards = mutableMapOf<String, Card>()

        log("Searching for ${namesToFind.size} cards in cache file (single-pass)...")
        val startTime = System.currentTimeMillis()

        try {
            val inputStream = cacheFile.openInputStream()
            val buffer = ByteArray(256 * 1024) // 256KB buffer - good balance
            val objectBuilder = StringBuilder(8192) // Reusable builder for JSON objects
            var braceDepth = 0
            var inString = false
            var escapeNext = false
            var inArray = false // Track if we're inside the main array
            var cardsScanned = 0

            try {
                while (namesToFind.isNotEmpty()) {
                    val bytesRead = inputStream.read(buffer, 0, buffer.size)
                    if (bytesRead == -1) break

                    // Process bytes directly - avoid creating intermediate strings
                    for (i in 0 until bytesRead) {
                        val c = buffer[i].toInt().toChar()

                        // Handle escape sequences in strings
                        if (escapeNext) {
                            if (braceDepth > 0) objectBuilder.append(c)
                            escapeNext = false
                            continue
                        }

                        if (c == '\\' && inString) {
                            if (braceDepth > 0) objectBuilder.append(c)
                            escapeNext = true
                            continue
                        }

                        // Track string state (to ignore braces inside strings)
                        if (c == '"') {
                            inString = !inString
                            if (braceDepth > 0) objectBuilder.append(c)
                            continue
                        }

                        if (inString) {
                            if (braceDepth > 0) objectBuilder.append(c)
                            continue
                        }

                        // Track array start (the file is one big JSON array)
                        if (c == '[' && !inArray) {
                            inArray = true
                            continue
                        }
                        if (c == ']' && braceDepth == 0) {
                            inArray = false
                            continue
                        }

                        // Track object boundaries
                        when (c) {
                            '{' -> {
                                braceDepth++
                                objectBuilder.append(c)
                            }
                            '}' -> {
                                objectBuilder.append(c)
                                braceDepth--
                                if (braceDepth == 0 && objectBuilder.isNotEmpty()) {
                                    // Complete JSON object - check if it matches
                                    cardsScanned++
                                    val card = parseAndMatchCard(objectBuilder, namesToFind)
                                    if (card != null) {
                                        val key = card.name.lowercase()
                                        foundCards[key] = card
                                        namesToFind.remove(key)
                                        log("Found: ${card.name} (${foundCards.size}/${names.size})")
                                    }
                                    objectBuilder.clear()
                                }
                            }
                            else -> {
                                if (braceDepth > 0) objectBuilder.append(c)
                            }
                        }
                    }
                }
            } finally {
                inputStream.close()
            }

            val elapsed = System.currentTimeMillis() - startTime
            log("Search complete: found ${foundCards.size}/${names.size} cards in ${elapsed}ms (scanned $cardsScanned cards)")

        } catch (e: Exception) {
            logAlways("Error searching cards: ${e.message}")
            e.printStackTrace()
        }

        // Return cards in original order, with fallback placeholders for not found
        names.map { name ->
            foundCards[name.lowercase()] ?: Card(name = name)
        }
    }

    /**
     * Parse a JSON object and check if its name matches any we're looking for.
     * Only does full JSON parsing if quick name extraction matches.
     */
    private fun parseAndMatchCard(jsonBuilder: StringBuilder, namesToFind: Set<String>): Card? {
        // Quick extraction: find "name":"..." pattern without full parsing
        val nameKey = "\"name\":\""
        val nameIdx = jsonBuilder.indexOf(nameKey)
        if (nameIdx == -1) return null

        val nameStart = nameIdx + nameKey.length
        val nameEnd = jsonBuilder.indexOf("\"", nameStart)
        if (nameEnd == -1) return null

        val cardName = jsonBuilder.substring(nameStart, nameEnd)
        val cardNameLower = cardName.lowercase()

        // O(1) HashSet lookup - this is the key optimization
        if (cardNameLower !in namesToFind) return null

        // Only parse JSON for matching cards
        return try {
            val jsonStr = jsonBuilder.toString()
            val scryfallCard = json.decodeFromString<ScryfallCard>(jsonStr)
            scryfallCard.toCard()
        } catch (e: Exception) {
            log("Failed to parse card: ${e.message}")
            null
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

    /**
     * Get the number of unique cards in the cache.
     * Returns the count from loaded cardMap if available, otherwise from metadata.
     */
    fun getCardCount(): Int {
        // If cache is loaded in memory, return actual count
        cardMap?.let { return it.size }
        // Otherwise return count from metadata (may be 0 if not yet loaded)
        return getCacheMetadata()?.cardCount ?: 0
    }

    /**
     * Count cards in the cache file by streaming through it.
     * This counts occurrences of `"object":"card"` which appears in each card entry.
     * Uses minimal memory (~64KB buffer) regardless of file size.
     */
    suspend fun countCardsInFile(): Int = withContext(ioDispatcher) {
        if (!cacheFile.exists()) return@withContext 0

        var count = 0
        val searchPattern = "\"object\":\"card\"".encodeToByteArray()
        val buffer = ByteArray(64 * 1024)
        var carryOver = ByteArray(0)

        try {
            val inputStream = cacheFile.openInputStream()
            try {
                while (true) {
                    val bytesRead = inputStream.read(buffer, 0, buffer.size)
                    if (bytesRead == -1) break

                    // Combine carryover from previous chunk with current chunk
                    val searchBuffer = if (carryOver.isNotEmpty()) {
                        carryOver + buffer.copyOf(bytesRead)
                    } else {
                        buffer.copyOf(bytesRead)
                    }

                    // Count occurrences of the pattern
                    var searchStart = 0
                    while (true) {
                        val foundIndex = searchBuffer.indexOf(searchPattern, searchStart)
                        if (foundIndex == -1) break
                        count++
                        searchStart = foundIndex + searchPattern.size
                    }

                    // Keep last (pattern.size - 1) bytes as carryover for cross-boundary matches
                    val carrySize = (searchPattern.size - 1).coerceAtMost(searchBuffer.size)
                    carryOver = searchBuffer.copyOfRange(searchBuffer.size - carrySize, searchBuffer.size)
                }
            } finally {
                inputStream.close()
            }
        } catch (e: Exception) {
            logAlways("Error counting cards: ${e.message}")
        }

        count
    }

    /**
     * Helper function to find a byte array pattern within another byte array
     */
    private fun ByteArray.indexOf(pattern: ByteArray, startIndex: Int = 0): Int {
        if (pattern.isEmpty()) return startIndex
        if (startIndex + pattern.size > this.size) return -1

        outer@ for (i in startIndex..(this.size - pattern.size)) {
            for (j in pattern.indices) {
                if (this[i + j] != pattern[j]) continue@outer
            }
            return i
        }
        return -1
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
