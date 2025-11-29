package com.dustinmcafee.dongadeuce.viewmodel

import androidx.lifecycle.ViewModel
import com.dustinmcafee.dongadeuce.network.GameClient
import com.dustinmcafee.dongadeuce.network.GameServer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Android-specific wrapper around the shared MenuViewModel.
 * Provides Android lifecycle management and content URI handling.
 */
class AndroidMenuViewModel : ViewModel() {
    // Delegate to the shared MenuViewModel
    private val delegate = MenuViewModel()

    // Expose the shared UI state
    val uiState: StateFlow<MenuUiState> = delegate.uiState

    // Expose user settings for access
    val userSettings get() = delegate.userSettings

    // Track current screen for Android navigation
    private val _currentScreen = MutableStateFlow<AndroidScreen>(AndroidScreen.Menu)
    val currentScreen: StateFlow<AndroidScreen> = _currentScreen.asStateFlow()

    // Delegate all methods to shared MenuViewModel
    fun setPlayerName(name: String) = delegate.setPlayerName(name)
    fun setPlayerCount(count: Int) = delegate.setPlayerCount(count)
    fun setHotseatMode(enabled: Boolean) = delegate.setHotseatMode(enabled)
    fun setServerAddress(address: String) = delegate.setServerAddress(address)
    fun setServerPort(port: Int) = delegate.setServerPort(port)
    fun updateCardCache() = delegate.updateCardCache()
    fun clearError() = delegate.clearError()

    fun getGameServer(): GameServer? = delegate.getGameServer()
    fun getGameClient(): GameClient? = delegate.getGameClient()
    fun isHost(): Boolean = delegate.isHost()
    fun getLocalPlayerId(): String? = delegate.getLocalPlayerId()

    /**
     * Load deck from text content (for Android file picker results)
     */
    fun loadDeckFromContent(content: String) = delegate.loadDeckFromContent(content)

    /**
     * Load hotseat deck from text content
     */
    fun loadHotseatDeckFromContent(playerIndex: Int, content: String) = delegate.loadHotseatDeckFromContent(playerIndex, content)

    /**
     * Load deck from file path
     */
    fun loadDeck(filePath: String) = delegate.loadDeck(filePath)

    /**
     * Load hotseat deck from file path
     */
    fun loadHotseatDeck(playerIndex: Int, filePath: String) = delegate.loadHotseatDeck(playerIndex, filePath)

    /**
     * Start hosting a game
     */
    fun startHosting() {
        delegate.startHosting()
        _currentScreen.value = AndroidScreen.HostLobby
    }

    /**
     * Navigate to join screen
     */
    fun navigateToJoin() {
        delegate.navigateToJoin()
        _currentScreen.value = AndroidScreen.JoinLobby
    }

    /**
     * Connect to a hosted game
     */
    fun connectToGame() {
        delegate.connectToGame()
    }

    /**
     * Start a hotseat game
     */
    fun startHotseatGame() {
        delegate.startHotseatGame()
        _currentScreen.value = AndroidScreen.Game
    }

    /**
     * Start network game (host only)
     */
    fun startNetworkGame(): Boolean {
        val started = delegate.startNetworkGame()
        if (started) {
            _currentScreen.value = AndroidScreen.Game
        }
        return started
    }

    /**
     * Set ready status (client only)
     */
    fun setReady(ready: Boolean) = delegate.setReady(ready)

    /**
     * Kick a player from the lobby (host only)
     */
    fun kickPlayer(playerId: String) = delegate.kickPlayer(playerId)

    /**
     * Resume a paused game (host only)
     */
    fun resumeGame() = delegate.resumeGame()

    /**
     * Return to main menu
     */
    fun returnToMenu() {
        delegate.returnToMenu()
        _currentScreen.value = AndroidScreen.Menu
    }

    override fun onCleared() {
        super.onCleared()
        // Clean up delegate resources
        delegate.returnToMenu()
    }
}

/**
 * Android navigation screens
 */
sealed class AndroidScreen {
    object Menu : AndroidScreen()
    object HostLobby : AndroidScreen()
    object JoinLobby : AndroidScreen()
    object Game : AndroidScreen()
}
