package com.dustinmcafee.dongadeuce.settings

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * User settings data class for persistence
 */
@Serializable
data class UserSettingsData(
    val playerName: String = "Player 1",
    val serverAddress: String = "localhost",
    val serverPort: Int = 8080,
    val lastDeckDirectory: String? = null
)

/**
 * User settings manager that persists settings to disk
 * Uses platform-appropriate directory:
 * - Windows: %APPDATA%/DongADeuce or %USERPROFILE%/.commandermtg
 * - Linux/macOS: ~/.commandermtg
 */
class UserSettings(
    private val settingsDir: File = getDefaultSettingsDir()
) {
    companion object {
        private fun getDefaultSettingsDir(): File {
            val os = System.getProperty("os.name").lowercase()
            return if (os.contains("win")) {
                // Windows: prefer APPDATA, fallback to user home
                val appData = System.getenv("APPDATA")
                if (appData != null) {
                    File(appData, "DongADeuce")
                } else {
                    File(System.getProperty("user.home"), ".commandermtg")
                }
            } else {
                // Linux/macOS: use hidden folder in home
                File(System.getProperty("user.home"), ".commandermtg")
            }
        }
    }
    private val settingsFile = File(settingsDir, "settings.json")

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private var cachedSettings: UserSettingsData? = null

    init {
        settingsDir.mkdirs()
    }

    /**
     * Load settings from disk, or return defaults if not found
     */
    fun load(): UserSettingsData {
        cachedSettings?.let { return it }

        return try {
            if (settingsFile.exists()) {
                val content = settingsFile.readText()
                json.decodeFromString<UserSettingsData>(content).also {
                    cachedSettings = it
                }
            } else {
                UserSettingsData().also {
                    cachedSettings = it
                }
            }
        } catch (e: Exception) {
            // If settings file is corrupted, return defaults
            UserSettingsData().also {
                cachedSettings = it
            }
        }
    }

    /**
     * Save settings to disk
     */
    fun save(settings: UserSettingsData) {
        cachedSettings = settings
        try {
            settingsFile.writeText(json.encodeToString(settings))
        } catch (e: Exception) {
            // Silently fail on save errors - settings will work for session
        }
    }

    /**
     * Update a single setting and save
     */
    fun update(transform: (UserSettingsData) -> UserSettingsData) {
        val current = load()
        val updated = transform(current)
        save(updated)
    }

    /**
     * Get player name
     */
    fun getPlayerName(): String = load().playerName

    /**
     * Set player name and persist
     */
    fun setPlayerName(name: String) {
        update { it.copy(playerName = name) }
    }

    /**
     * Get server address
     */
    fun getServerAddress(): String = load().serverAddress

    /**
     * Set server address and persist
     */
    fun setServerAddress(address: String) {
        update { it.copy(serverAddress = address) }
    }

    /**
     * Get server port
     */
    fun getServerPort(): Int = load().serverPort

    /**
     * Set server port and persist
     */
    fun setServerPort(port: Int) {
        update { it.copy(serverPort = port.coerceIn(1024, 65535)) }
    }

    /**
     * Get last deck directory
     */
    fun getLastDeckDirectory(): String? = load().lastDeckDirectory

    /**
     * Set last deck directory and persist
     */
    fun setLastDeckDirectory(path: String?) {
        update { it.copy(lastDeckDirectory = path) }
    }
}
