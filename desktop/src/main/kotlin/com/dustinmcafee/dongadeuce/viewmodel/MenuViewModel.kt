package com.dustinmcafee.dongadeuce.viewmodel

import com.dustinmcafee.dongadeuce.api.ScryfallApi
import com.dustinmcafee.dongadeuce.api.CardCache
import com.dustinmcafee.dongadeuce.game.DeckParser
import com.dustinmcafee.dongadeuce.models.Card
import com.dustinmcafee.dongadeuce.models.Deck
import com.dustinmcafee.dongadeuce.models.GameState
import com.dustinmcafee.dongadeuce.network.*
import com.dustinmcafee.dongadeuce.settings.UserSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

/**
 * UI state for the menu/lobby screen
 */
data class MenuUiState(
    val playerName: String = "Player 1",
    val playerCount: Int = 2, // Total players for hotseat: 2, 3, or 4
    val loadedDeck: Deck? = null, // Legacy single deck (for Host/Join modes)
    val hotseatMode: Boolean = false, // True = local hotseat, False = network game
    val hotseatDecks: Map<Int, Deck> = emptyMap(), // Map of player index (0-3) to their deck
    val isHosting: Boolean = false,
    val connectedPlayers: List<String> = emptyList(),
    val serverAddress: String = "localhost",
    val serverPort: Int = 8080,
    val isLoading: Boolean = false,
    val loadingProgress: String = "",
    val loadingProgressPercent: Float = 0f,
    val error: String? = null,
    val currentScreen: Screen = Screen.Menu,
    val cacheAvailable: Boolean = false,
    val cacheCardCount: Int = 0,
    val cacheLastUpdated: Long? = null,
    // Network state
    val lobbyState: GameMessage.LobbyState? = null,
    val connectionState: ConnectionState = ConnectionState.Disconnected,
    val networkGameState: GameState? = null,
    val isNetworkGameStarted: Boolean = false,
    val isPaused: Boolean = false,
    val pauseReason: String? = null,
    val serverUrl: String? = null // WebSocket URL for clients to connect
)

sealed class Screen {
    object Menu : Screen()
    object HostLobby : Screen()
    object JoinLobby : Screen()
    object Game : Screen()
}

class MenuViewModel {
    // User settings for persistence (exposed for settings dialog)
    val userSettings = UserSettings()

    private val _uiState = MutableStateFlow(MenuUiState())
    val uiState: StateFlow<MenuUiState> = _uiState.asStateFlow()

    // Use SupervisorJob so exceptions don't cancel the whole scope
    private val viewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val scryfallApi = ScryfallApi()
    private val cardCache = CardCache()

    // Network components
    private var gameServer: GameServer? = null
    private var gameClient: GameClient? = null

    // Expose for GameViewModel
    fun getGameServer(): GameServer? = gameServer
    fun getGameClient(): GameClient? = gameClient

    init {
        // Load persisted settings
        loadPersistedSettings()
        // Check cache status on initialization
        updateCacheStatus()
    }

    /**
     * Load persisted settings from disk
     */
    private fun loadPersistedSettings() {
        val settings = userSettings.load()
        _uiState.update {
            it.copy(
                playerName = settings.playerName,
                serverAddress = settings.serverAddress,
                serverPort = settings.serverPort
            )
        }
    }

    /**
     * Update cache status information
     */
    private fun updateCacheStatus() {
        viewModelScope.launch {
            val metadata = cardCache.getCacheMetadata()
            _uiState.update {
                it.copy(
                    cacheAvailable = cardCache.isCacheAvailable(),
                    cacheCardCount = metadata?.cardCount ?: 0,
                    cacheLastUpdated = metadata?.lastUpdated
                )
            }
        }
    }

    /**
     * Download and update the card cache
     */
    fun updateCardCache() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, loadingProgress = "Starting cache update...", loadingProgressPercent = 0f, error = null) }

            try {
                cardCache.updateCache { message, percent ->
                    // StateFlow.update is thread-safe and triggers recomposition
                    _uiState.update { it.copy(loadingProgress = message, loadingProgressPercent = percent) }
                }

                updateCacheStatus()

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        loadingProgress = "",
                        loadingProgressPercent = 0f,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        loadingProgress = "",
                        loadingProgressPercent = 0f,
                        error = "Failed to update cache: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Update player name and persist
     */
    fun setPlayerName(name: String) {
        _uiState.update { it.copy(playerName = name) }
        userSettings.setPlayerName(name)
    }

    /**
     * Set player count for hotseat games (2-4 players)
     */
    fun setPlayerCount(count: Int) {
        _uiState.update { it.copy(playerCount = count.coerceIn(2, 4)) }
    }

    /**
     * Toggle hotseat mode
     */
    fun setHotseatMode(enabled: Boolean) {
        _uiState.update {
            it.copy(
                hotseatMode = enabled,
                hotseatDecks = if (!enabled) emptyMap() else it.hotseatDecks
            )
        }
    }

    /**
     * Load a deck from file and fetch card data from Scryfall
     */
    fun loadDeck(filePath: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, loadingProgress = "Parsing deck file...", error = null) }

            try {
                // Parse the deck file (validation is now in DeckParser)
                val parsedDeck = try {
                    DeckParser.parseTextFile(filePath)
                } catch (e: IllegalArgumentException) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            loadingProgress = "",
                            error = "Invalid deck file: ${e.message}"
                        )
                    }
                    return@launch
                } catch (e: Exception) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            loadingProgress = "",
                            error = "Failed to read deck file: ${e.message}"
                        )
                    }
                    return@launch
                }

                _uiState.update { it.copy(loadingProgress = "Loading card data from cache...") }

                // Load from cache if available, otherwise fall back to API
                val commanderWithData = if (cardCache.isCacheAvailable()) {
                    cardCache.getCardByName(parsedDeck.commander.name) ?: parsedDeck.commander
                } else {
                    scryfallApi.getCardByName(parsedDeck.commander.name) ?: parsedDeck.commander
                }

                // Load card data
                val cardsWithData = if (cardCache.isCacheAvailable()) {
                    // Fast batch load from cache
                    _uiState.update { it.copy(loadingProgress = "Loading ${parsedDeck.cards.size} cards from cache...") }
                    cardCache.getCardsByNames(parsedDeck.cards.map { it.name })
                } else {
                    // Slow per-card API calls
                    val cards = mutableListOf<Card>()
                    parsedDeck.cards.forEachIndexed { index, card ->
                        val progress = "Fetching from Scryfall... (${index + 1}/${parsedDeck.cards.size})"
                        _uiState.update { it.copy(loadingProgress = progress) }
                        val cardWithData = scryfallApi.getCardByName(card.name) ?: card
                        cards.add(cardWithData)
                    }
                    cards
                }

                // Create deck with fetched data
                val deckWithData = Deck(
                    name = parsedDeck.name,
                    commander = commanderWithData,
                    cards = cardsWithData
                )

                _uiState.update {
                    it.copy(
                        loadedDeck = deckWithData,
                        isLoading = false,
                        loadingProgress = "",
                        error = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        loadingProgress = "",
                        error = "Failed to load deck: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Load a deck for a specific player in hotseat mode
     */
    fun loadHotseatDeck(playerIndex: Int, filePath: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, loadingProgress = "Parsing deck for Player ${playerIndex + 1}...", error = null) }

            try {
                // Parse the deck file
                val parsedDeck = try {
                    DeckParser.parseTextFile(filePath)
                } catch (e: IllegalArgumentException) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            loadingProgress = "",
                            error = "Invalid deck file for Player ${playerIndex + 1}: ${e.message}"
                        )
                    }
                    return@launch
                } catch (e: Exception) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            loadingProgress = "",
                            error = "Failed to read deck file for Player ${playerIndex + 1}: ${e.message}"
                        )
                    }
                    return@launch
                }

                _uiState.update { it.copy(loadingProgress = "Loading card data from cache...") }

                // Load from cache if available, otherwise fall back to API
                val commanderWithData = if (cardCache.isCacheAvailable()) {
                    cardCache.getCardByName(parsedDeck.commander.name) ?: parsedDeck.commander
                } else {
                    scryfallApi.getCardByName(parsedDeck.commander.name) ?: parsedDeck.commander
                }

                // Load card data
                val cardsWithData = if (cardCache.isCacheAvailable()) {
                    // Fast batch load from cache
                    _uiState.update { it.copy(loadingProgress = "Loading Player ${playerIndex + 1} cards from cache...") }
                    cardCache.getCardsByNames(parsedDeck.cards.map { it.name })
                } else {
                    // Slow per-card API calls
                    val cards = mutableListOf<Card>()
                    parsedDeck.cards.forEachIndexed { index, card ->
                        val progress = "Fetching Player ${playerIndex + 1} from Scryfall... (${index + 1}/${parsedDeck.cards.size})"
                        _uiState.update { it.copy(loadingProgress = progress) }
                        val cardWithData = scryfallApi.getCardByName(card.name) ?: card
                        cards.add(cardWithData)
                    }
                    cards
                }

                // Create deck with fetched data
                val deckWithData = Deck(
                    name = "Player ${playerIndex + 1} Deck",
                    commander = commanderWithData,
                    cards = cardsWithData
                )

                _uiState.update {
                    it.copy(
                        hotseatDecks = it.hotseatDecks + (playerIndex to deckWithData),
                        isLoading = false,
                        loadingProgress = "",
                        error = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        loadingProgress = "",
                        error = "Failed to load deck for Player ${playerIndex + 1}: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Start a local hotseat game
     */
    fun startHotseatGame() {
        val requiredDeckCount = _uiState.value.playerCount
        val loadedDeckCount = _uiState.value.hotseatDecks.size

        if (loadedDeckCount < requiredDeckCount) {
            _uiState.update {
                it.copy(error = "Please load decks for all $requiredDeckCount players (currently $loadedDeckCount loaded)")
            }
            return
        }

        _uiState.update { it.copy(currentScreen = Screen.Game) }
    }

    /**
     * Start hosting a game
     */
    fun startHosting() {
        val deck = _uiState.value.loadedDeck
        if (deck == null) {
            _uiState.update { it.copy(error = "Please load a deck first") }
            return
        }

        val port = _uiState.value.serverPort
        val playerName = _uiState.value.playerName

        // Create and start the server
        gameServer = GameServer(
            port = port,
            hostName = playerName,
            hostDeck = deck,
            maxPlayers = 4
        )

        val serverUrl = gameServer?.start()

        // Observe server state
        viewModelScope.launch {
            gameServer?.lobbyState?.collect { lobby ->
                _uiState.update { it.copy(lobbyState = lobby) }
            }
        }

        viewModelScope.launch {
            gameServer?.gameState?.collect { state ->
                _uiState.update { it.copy(networkGameState = state) }
            }
        }

        viewModelScope.launch {
            gameServer?.gameStarted?.collect { started ->
                _uiState.update { it.copy(isNetworkGameStarted = started) }
                if (started) {
                    _uiState.update { it.copy(currentScreen = Screen.Game) }
                }
            }
        }

        viewModelScope.launch {
            gameServer?.isPaused?.collect { paused ->
                _uiState.update { it.copy(isPaused = paused) }
            }
        }

        viewModelScope.launch {
            gameServer?.pauseReason?.collect { reason ->
                _uiState.update { it.copy(pauseReason = reason) }
            }
        }

        _uiState.update {
            it.copy(
                isHosting = true,
                currentScreen = Screen.HostLobby,
                serverUrl = serverUrl,
                error = null
            )
        }
    }

    /**
     * Navigate to join game screen
     */
    fun navigateToJoin() {
        if (_uiState.value.loadedDeck == null) {
            _uiState.update { it.copy(error = "Please load a deck first") }
            return
        }

        _uiState.update {
            it.copy(currentScreen = Screen.JoinLobby, error = null)
        }
    }

    /**
     * Set server address for joining and persist
     */
    fun setServerAddress(address: String) {
        _uiState.update { it.copy(serverAddress = address) }
        userSettings.setServerAddress(address)
    }

    /**
     * Connect to a hosted game
     */
    fun connectToGame() {
        val deck = _uiState.value.loadedDeck
        if (deck == null) {
            _uiState.update { it.copy(error = "Please load a deck first") }
            return
        }

        val address = _uiState.value.serverAddress
        val port = _uiState.value.serverPort
        val playerName = _uiState.value.playerName

        if (address.isBlank()) {
            _uiState.update { it.copy(error = "Please enter a server address") }
            return
        }

        _uiState.update { it.copy(isLoading = true, error = null) }

        // Create client
        gameClient = GameClient()

        // Observe client state
        viewModelScope.launch {
            gameClient?.connectionState?.collect { state ->
                _uiState.update { it.copy(connectionState = state) }
                when (state) {
                    is ConnectionState.Connected -> {
                        _uiState.update { it.copy(isLoading = false) }
                    }
                    is ConnectionState.Error -> {
                        _uiState.update { it.copy(isLoading = false, error = state.message) }
                    }
                    else -> {}
                }
            }
        }

        viewModelScope.launch {
            gameClient?.lobbyState?.collect { lobby ->
                _uiState.update { it.copy(lobbyState = lobby) }
            }
        }

        viewModelScope.launch {
            gameClient?.gameState?.collect { state ->
                _uiState.update { it.copy(networkGameState = state) }
            }
        }

        viewModelScope.launch {
            gameClient?.gameStarted?.collect { started ->
                _uiState.update { it.copy(isNetworkGameStarted = started) }
                if (started) {
                    _uiState.update { it.copy(currentScreen = Screen.Game) }
                }
            }
        }

        viewModelScope.launch {
            gameClient?.isPaused?.collect { paused ->
                _uiState.update { it.copy(isPaused = paused) }
            }
        }

        viewModelScope.launch {
            gameClient?.pauseReason?.collect { reason ->
                _uiState.update { it.copy(pauseReason = reason) }
            }
        }

        viewModelScope.launch {
            gameClient?.error?.collect { error ->
                if (error != null) {
                    _uiState.update { it.copy(error = error) }
                }
            }
        }

        // Connect to server
        viewModelScope.launch {
            val success = gameClient?.connect(address, port, playerName, deck) ?: false
            if (!success) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to connect to server"
                    )
                }
            }
        }
    }

    /**
     * Start the game
     */
    fun startGame() {
        _uiState.update { it.copy(currentScreen = Screen.Game) }
    }

    /**
     * Return to main menu
     */
    fun returnToMenu() {
        // Clean up network resources
        gameServer?.stop()
        gameServer = null
        gameClient?.disconnect()
        gameClient = null

        _uiState.update {
            it.copy(
                currentScreen = Screen.Menu,
                isHosting = false,
                connectedPlayers = emptyList(),
                lobbyState = null,
                connectionState = ConnectionState.Disconnected,
                networkGameState = null,
                isNetworkGameStarted = false,
                isPaused = false,
                pauseReason = null,
                serverUrl = null,
                error = null
            )
        }
    }

    /**
     * Start network game (host only)
     */
    fun startNetworkGame(): Boolean {
        return gameServer?.startGame() ?: false
    }

    /**
     * Kick a player from the lobby (host only)
     */
    fun kickPlayer(playerId: String) {
        viewModelScope.launch {
            gameServer?.kickPlayer(playerId)
        }
    }

    /**
     * Set ready status (client only)
     */
    fun setReady(ready: Boolean) {
        viewModelScope.launch {
            gameClient?.setReady(ready)
        }
    }

    /**
     * Resume a paused network game (host only)
     */
    fun resumeGame() {
        viewModelScope.launch {
            gameServer?.resumeGame()
        }
    }

    /**
     * Set server port and persist
     */
    fun setServerPort(port: Int) {
        val validPort = port.coerceIn(1024, 65535)
        _uiState.update { it.copy(serverPort = validPort) }
        userSettings.setServerPort(validPort)
    }

    /**
     * Check if we're the host
     */
    fun isHost(): Boolean = gameServer != null

    /**
     * Get the local player ID
     */
    fun getLocalPlayerId(): String? {
        return gameServer?.getHostId() ?: (uiState.value.connectionState as? ConnectionState.Connected)?.playerId
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    /**
     * Add a player to the lobby (for host)
     */
    fun addPlayer(playerName: String) {
        _uiState.update {
            it.copy(connectedPlayers = it.connectedPlayers + playerName)
        }
    }

    /**
     * Remove a player from the lobby (for host)
     */
    fun removePlayer(playerName: String) {
        _uiState.update {
            it.copy(connectedPlayers = it.connectedPlayers.filter { p -> p != playerName })
        }
    }
}
