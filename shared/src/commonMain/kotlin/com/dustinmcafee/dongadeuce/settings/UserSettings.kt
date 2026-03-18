package com.dustinmcafee.dongadeuce.settings

import com.dustinmcafee.dongadeuce.platform.FileHandle
import com.dustinmcafee.dongadeuce.platform.getAppDataDirectory
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * User settings data class for persistence
 */
@Serializable
data class UserSettingsData(
    val playerName: String = "Player 1",
    val serverAddress: String = "localhost",
    val serverPort: Int = 8080,
    val lastDeckDirectory: String? = null,
    val uiScale: Float = 1.0f,  // UI scale factor (0.75, 1.0, 1.25, 1.5)
    val serverMode: String = "LAN",  // "LAN" or "DEDICATED"
    val tlsEnabled: Boolean = false
)

/**
 * User settings manager that persists settings to disk
 * Uses platform-appropriate directory:
 * - Windows: %APPDATA%/DongADeuce or %USERPROFILE%/.commandermtg
 * - Linux/macOS: ~/.commandermtg
 * - Android: Context.filesDir
 */
class UserSettings(
    private val settingsDir: FileHandle = getAppDataDirectory()
) {
    private val settingsFile: FileHandle = settingsDir.child("settings.json")

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

    /**
     * Get UI scale factor
     */
    fun getUiScale(): Float = load().uiScale

    /**
     * Set UI scale factor and persist
     */
    fun setUiScale(scale: Float) {
        update { it.copy(uiScale = scale.coerceIn(0.5f, 2.0f)) }
    }

    /**
     * Get server mode ("LAN" or "DEDICATED")
     */
    fun getServerMode(): String = load().serverMode

    /**
     * Set server mode and persist
     */
    fun setServerMode(mode: String) {
        update { it.copy(serverMode = mode) }
    }

    /**
     * Get TLS enabled setting
     */
    fun getTlsEnabled(): Boolean = load().tlsEnabled

    /**
     * Set TLS enabled and persist
     */
    fun setTlsEnabled(enabled: Boolean) {
        update { it.copy(tlsEnabled = enabled) }
    }
}
