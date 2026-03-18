package com.dustinmcafee.dongadeuce.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dustinmcafee.dongadeuce.network.GameClient
import com.dustinmcafee.dongadeuce.network.GameServer
import com.dustinmcafee.dongadeuce.service.DedicatedServerService
import com.dustinmcafee.dongadeuce.viewmodel.ServerMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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

    init {
        // Observe shared state screen transitions (for async game start in dedicated server mode)
        viewModelScope.launch {
            delegate.uiState.collect { state ->
                if (state.currentScreen == Screen.Game && _currentScreen.value != AndroidScreen.Game) {
                    _currentScreen.value = AndroidScreen.Game
                }
            }
        }
    }

    // Dedicated server state (from service companion)
    val dedicatedServerRunning: StateFlow<Boolean> = DedicatedServerService.isRunning
    val dedicatedServerPort: StateFlow<Int> = DedicatedServerService.serverPort
    val dedicatedServerGameCount: StateFlow<Int> = DedicatedServerService.activeGameCount
    val dedicatedServerIpAddress: StateFlow<String> = DedicatedServerService.serverIpAddress
    val dedicatedServerFingerprint: StateFlow<String?> = DedicatedServerService.serverFingerprint

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
     * Directly set a deck for a hotseat player (skip parsing, reuse existing deck object)
     */
    fun setHotseatDeckDirectly(playerIndex: Int, deck: com.dustinmcafee.dongadeuce.models.Deck) = delegate.setHotseatDeckDirectly(playerIndex, deck)

    /**
     * Select a commander for the pending deck
     */
    fun selectCommander(commanderName: String, partnerName: String? = null) = delegate.selectCommander(commanderName, partnerName)

    /**
     * Cancel commander selection
     */
    fun cancelCommanderSelection() = delegate.cancelCommanderSelection()

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
     * Navigate to dedicated server screen
     */
    fun navigateToDedicatedServer() {
        _currentScreen.value = AndroidScreen.DedicatedServer
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
        // In P2P mode, game starts synchronously — switch screen immediately
        // In dedicated mode, screen transition happens via the init collector when GameStarting arrives
        if (started && delegate.uiState.value.serverMode != ServerMode.DEDICATED) {
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
     * Set server mode (LAN or DEDICATED)
     */
    fun setServerMode(mode: ServerMode) = delegate.setServerMode(mode)

    /**
     * Set game code for joining a dedicated server game
     */
    fun setGameCode(code: String?) = delegate.setGameCode(code)

    /**
     * Create a new game room on a dedicated server via REST API
     */
    fun createGameOnServer(onCodeReceived: (String) -> Unit = {}) = delegate.createGameOnServer(onCodeReceived)

    fun setTlsEnabled(enabled: Boolean) = delegate.setTlsEnabled(enabled)
    fun acceptTofu() = delegate.acceptTofu()
    fun rejectTofu() = delegate.rejectTofu()

    /**
     * Start the dedicated server foreground service
     */
    fun startDedicatedServer(context: Context, port: Int, maxGames: Int, maxPlayers: Int, tlsEnabled: Boolean = false) {
        DedicatedServerService.startServer(context, port, maxGames, maxPlayers, tlsEnabled)
    }

    /**
     * Stop the dedicated server foreground service
     */
    fun stopDedicatedServer(context: Context) {
        DedicatedServerService.stopServer(context)
    }

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
    object DedicatedServer : AndroidScreen()
}
