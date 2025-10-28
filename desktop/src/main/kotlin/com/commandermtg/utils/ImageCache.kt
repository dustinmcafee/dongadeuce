package com.commandermtg.utils

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

/**
 * Manages caching of card images downloaded from Scryfall
 */
object ImageCache {
    private var client: HttpClient? = null

    private fun getClient(): HttpClient {
        if (client == null) {
            client = HttpClient()
        }
        return client!!
    }

    // Cache directory in user's home directory
    private val cacheDir: File by lazy {
        val userHome = System.getProperty("user.home")
        val dir = File(userHome, ".commandermtg/image_cache")
        dir.mkdirs()
        dir
    }

    /**
     * Get cached image file path, downloading if not already cached
     * @param imageUrl URL of the image to cache
     * @return File pointing to the cached image, or null if download fails
     */
    suspend fun getCachedImage(imageUrl: String?): File? {
        if (imageUrl == null || imageUrl.isEmpty()) {
            return null
        }

        return withContext(Dispatchers.IO) {
            try {
                // Generate filename from URL hash
                val filename = generateFilename(imageUrl)
                val cachedFile = File(cacheDir, filename)

                // Return cached file if it exists
                if (cachedFile.exists()) {
                    return@withContext cachedFile
                }

                // Download image
                val response = getClient().get(imageUrl)
                if (response.status == HttpStatusCode.OK) {
                    val imageBytes = response.readBytes()
                    cachedFile.writeBytes(imageBytes)
                    cachedFile
                } else {
                    null
                }
            } catch (e: Exception) {
                println("Error caching image from $imageUrl: ${e.message}")
                null
            }
        }
    }

    /**
     * Generate a unique filename for a given URL
     */
    private fun generateFilename(url: String): String {
        val hash = MessageDigest.getInstance("SHA-256")
            .digest(url.toByteArray())
            .joinToString("") { "%02x".format(it) }

        // Extract file extension from URL
        val extension = url.substringAfterLast('.', "jpg")
            .substringBefore('?') // Remove query parameters

        return "$hash.$extension"
    }

    /**
     * Get the cache directory
     */
    fun getCacheDirectory(): File = cacheDir

    /**
     * Clear all cached images
     */
    fun clearCache() {
        cacheDir.listFiles()?.forEach { it.delete() }
    }

    /**
     * Get cache size in bytes
     */
    fun getCacheSize(): Long {
        return cacheDir.listFiles()?.sumOf { it.length() } ?: 0L
    }

    /**
     * Close the HTTP client and release resources
     * Should be called when the application shuts down
     */
    fun close() {
        client?.close()
        client = null
    }
}
