package com.dustinmcafee.dongadeuce.viewmodel

import com.dustinmcafee.dongadeuce.api.ScryfallApi
import com.dustinmcafee.dongadeuce.api.CardCache
import com.dustinmcafee.dongadeuce.game.DeckFormat
import com.dustinmcafee.dongadeuce.game.DeckParser
import com.dustinmcafee.dongadeuce.game.DeckParseResult
import com.dustinmcafee.dongadeuce.game.ParsedDeckData
import com.dustinmcafee.dongadeuce.models.Card
import com.dustinmcafee.dongadeuce.models.Deck
import com.dustinmcafee.dongadeuce.models.GameState
import com.dustinmcafee.dongadeuce.network.*
import com.dustinmcafee.dongadeuce.platform.ioDispatcher
import com.dustinmcafee.dongadeuce.settings.UserSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
    val serverUrl: String? = null, // WebSocket URL for clients to connect
    // Commander selection state
    val pendingDeckData: ParsedDeckData? = null, // Deck waiting for commander selection
    val pendingDeckPlayerIndex: Int? = null, // Player index for hotseat (null for single deck)
    val commanderCandidates: List<Card> = emptyList() // Cards that can be selected as commander
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
    private val viewModelScope = CoroutineScope(SupervisorJob() + ioDispatcher)
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
     * Supports multiple formats: .cod (Cockatrice), .dec, .dek, .txt, .mwDeck
     */
    fun loadDeck(filePath: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, loadingProgress = "Parsing deck file...", error = null) }

            try {
                // Parse the deck file with automatic format detection
                when (val parseResult = DeckParser.parseFile(filePath)) {
                    is DeckParseResult.Complete -> {
                        // Deck has commander - load card data
                        finishLoadingDeck(parseResult.deck, null)
                    }
                    is DeckParseResult.NeedsCommanderSelection -> {
                        // Need to select commander - load card data first
                        _uiState.update { it.copy(loadingProgress = "Loading card data...") }
                        val candidates = loadCommanderCandidates(parseResult.data)

                        if (candidates.isEmpty()) {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    loadingProgress = "",
                                    error = "No cards found in deck. Cannot select commander."
                                )
                            }
                            return@launch
                        }

                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                loadingProgress = "",
                                pendingDeckData = parseResult.data,
                                pendingDeckPlayerIndex = null,
                                commanderCandidates = candidates
                            )
                        }
                    }
                    is DeckParseResult.Error -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                loadingProgress = "",
                                error = parseResult.message
                            )
                        }
                    }
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
     * Load commander candidates from parsed deck data
     * Checks both mainboard and sideboard (commander is often in sideboard in Cockatrice)
     * Returns ALL cards with their data - UI can filter for legendaries
     */
    private suspend fun loadCommanderCandidates(data: ParsedDeckData): List<Card> {
        // Check both mainboard and sideboard for commander candidates
        val cardNames = data.allCardNamesIncludingSideboard
        val sideboardNames = data.sideboardCardNames.toSet()

        // Load card data for all cards using batch method
        val candidates = if (cardCache.isCacheAvailable()) {
            cardCache.getCardsByNames(cardNames)
        } else {
            scryfallApi.getCardsByNames(cardNames)
        }

        // Sort with sideboard cards first (as they're more likely to be the commander in Cockatrice)
        // Then legendary creatures, then planeswalkers, then others alphabetically
        return candidates.sortedWith(
            compareByDescending<Card> { it.name in sideboardNames }
                .thenByDescending { it.isLegendary && it.canBeCommander }
                .thenBy { it.name }
        )
    }

    /**
     * Select a commander for the pending deck
     * Reuses card data already loaded for commander candidates
     */
    fun selectCommander(commanderName: String) {
        val pendingData = _uiState.value.pendingDeckData ?: return
        val playerIndex = _uiState.value.pendingDeckPlayerIndex
        val loadedCards = _uiState.value.commanderCandidates

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, loadingProgress = "Building deck with commander...") }

            try {
                val parsedDeck = pendingData.toDeck(commanderName)

                // Reuse already-loaded card data from commander candidates
                val cardDataMap = loadedCards.associateBy { it.name.lowercase() }

                val commanderWithData = cardDataMap[commanderName.lowercase()] ?: parsedDeck.commander
                val cardsWithData = parsedDeck.cards.map { card ->
                    cardDataMap[card.name.lowercase()] ?: card
                }
                val sideboardWithData = parsedDeck.sideboard.map { card ->
                    cardDataMap[card.name.lowercase()] ?: card
                }

                val deckWithData = Deck(
                    name = parsedDeck.name,
                    commander = commanderWithData,
                    cards = cardsWithData,
                    sideboard = sideboardWithData
                )

                if (playerIndex != null) {
                    // Hotseat mode - add to hotseat decks
                    _uiState.update {
                        it.copy(
                            hotseatDecks = it.hotseatDecks + (playerIndex to deckWithData),
                            isLoading = false,
                            loadingProgress = "",
                            pendingDeckData = null,
                            pendingDeckPlayerIndex = null,
                            commanderCandidates = emptyList(),
                            error = null
                        )
                    }
                } else {
                    // Single deck mode
                    _uiState.update {
                        it.copy(
                            loadedDeck = deckWithData,
                            isLoading = false,
                            loadingProgress = "",
                            pendingDeckData = null,
                            pendingDeckPlayerIndex = null,
                            commanderCandidates = emptyList(),
                            error = null
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        loadingProgress = "",
                        error = "Failed to create deck: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Cancel commander selection
     */
    fun cancelCommanderSelection() {
        _uiState.update {
            it.copy(
                pendingDeckData = null,
                pendingDeckPlayerIndex = null,
                commanderCandidates = emptyList()
            )
        }
    }

    /**
     * Finish loading a deck with card data
     */
    private suspend fun finishLoadingDeck(parsedDeck: Deck, playerIndex: Int?) {
        _uiState.update { it.copy(loadingProgress = "Loading card data from cache...") }

        // Collect all unique card names for a single batch load
        val allCardNames = (listOf(parsedDeck.commander.name) +
                parsedDeck.cards.map { it.name } +
                parsedDeck.sideboard.map { it.name }).distinct()

        // Load all card data in one batch
        val cardDataMap = if (cardCache.isCacheAvailable()) {
            _uiState.update { it.copy(loadingProgress = "Loading ${allCardNames.size} cards from cache...") }
            cardCache.getCardsByNames(allCardNames).associateBy { it.name.lowercase() }
        } else {
            // Slow per-card API calls (only when no cache)
            val cards = mutableMapOf<String, Card>()
            allCardNames.forEachIndexed { index, name ->
                val progress = "Fetching from Scryfall... (${index + 1}/${allCardNames.size})"
                _uiState.update { it.copy(loadingProgress = progress) }
                val cardWithData = scryfallApi.getCardByName(name)
                if (cardWithData != null) {
                    cards[name.lowercase()] = cardWithData
                }
            }
            cards
        }

        // Map loaded data back to deck structure
        val commanderWithData = cardDataMap[parsedDeck.commander.name.lowercase()] ?: parsedDeck.commander
        val cardsWithData = parsedDeck.cards.map { card ->
            cardDataMap[card.name.lowercase()] ?: card
        }
        val sideboardWithData = parsedDeck.sideboard.map { card ->
            cardDataMap[card.name.lowercase()] ?: card
        }

        // Create deck with fetched data
        val deckWithData = Deck(
            name = parsedDeck.name,
            commander = commanderWithData,
            cards = cardsWithData,
            sideboard = sideboardWithData
        )

        if (playerIndex != null) {
            // Hotseat mode
            _uiState.update {
                it.copy(
                    hotseatDecks = it.hotseatDecks + (playerIndex to deckWithData),
                    isLoading = false,
                    loadingProgress = "",
                    pendingDeckData = null,
                    pendingDeckPlayerIndex = null,
                    commanderCandidates = emptyList(),
                    error = null
                )
            }
        } else {
            // Single deck mode
            _uiState.update {
                it.copy(
                    loadedDeck = deckWithData,
                    isLoading = false,
                    loadingProgress = "",
                    pendingDeckData = null,
                    pendingDeckPlayerIndex = null,
                    commanderCandidates = emptyList(),
                    error = null
                )
            }
        }
    }

    /**
     * Load a deck from text content (useful for clipboard paste or Android content URI)
     * Supports plain text and Cockatrice XML formats with commander selection
     */
    fun loadDeckFromContent(content: String, deckName: String = "Pasted Deck") {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, loadingProgress = "Parsing deck...", error = null) }

            try {
                // Detect format based on content (XML starts with < or <?xml)
                val format = if (content.trimStart().startsWith("<")) {
                    DeckFormat.COCKATRICE_XML
                } else {
                    DeckFormat.PLAIN_TEXT
                }

                when (val parseResult = DeckParser.parseContent(content, format, deckName)) {
                    is DeckParseResult.Complete -> {
                        // Deck has commander - load card data
                        finishLoadingDeck(parseResult.deck, null)
                    }
                    is DeckParseResult.NeedsCommanderSelection -> {
                        // Need to select commander - load card data first
                        _uiState.update { it.copy(loadingProgress = "Loading card data...") }
                        val candidates = loadCommanderCandidates(parseResult.data)

                        if (candidates.isEmpty()) {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    loadingProgress = "",
                                    error = "No cards found in deck. Cannot select commander."
                                )
                            }
                            return@launch
                        }

                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                loadingProgress = "",
                                pendingDeckData = parseResult.data,
                                pendingDeckPlayerIndex = null,
                                commanderCandidates = candidates
                            )
                        }
                    }
                    is DeckParseResult.Error -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                loadingProgress = "",
                                error = parseResult.message
                            )
                        }
                    }
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
     * Load a hotseat deck from text content (useful for clipboard paste)
     * Supports plain text and Cockatrice XML formats with commander selection
     */
    fun loadHotseatDeckFromContent(playerIndex: Int, content: String, deckName: String = "Player ${playerIndex + 1} Deck") {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, loadingProgress = "Parsing deck for Player ${playerIndex + 1}...", error = null) }

            try {
                // Detect format based on content (XML starts with < or <?xml)
                val format = if (content.trimStart().startsWith("<")) {
                    DeckFormat.COCKATRICE_XML
                } else {
                    DeckFormat.PLAIN_TEXT
                }

                when (val parseResult = DeckParser.parseContent(content, format, deckName)) {
                    is DeckParseResult.Complete -> {
                        // Deck has commander - load card data
                        finishLoadingDeck(parseResult.deck, playerIndex)
                    }
                    is DeckParseResult.NeedsCommanderSelection -> {
                        // Need to select commander - load card data first
                        _uiState.update { it.copy(loadingProgress = "Loading card data...") }
                        val candidates = loadCommanderCandidates(parseResult.data)

                        if (candidates.isEmpty()) {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    loadingProgress = "",
                                    error = "No cards found in deck for Player ${playerIndex + 1}."
                                )
                            }
                            return@launch
                        }

                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                loadingProgress = "",
                                pendingDeckData = parseResult.data,
                                pendingDeckPlayerIndex = playerIndex,
                                commanderCandidates = candidates
                            )
                        }
                    }
                    is DeckParseResult.Error -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                loadingProgress = "",
                                error = "Failed to load deck for Player ${playerIndex + 1}: ${parseResult.message}"
                            )
                        }
                    }
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
     * Load a deck for a specific player in hotseat mode
     * Supports multiple formats: .cod (Cockatrice), .dec, .dek, .txt, .mwDeck
     */
    fun loadHotseatDeck(playerIndex: Int, filePath: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, loadingProgress = "Parsing deck for Player ${playerIndex + 1}...", error = null) }

            try {
                // Parse the deck file with automatic format detection
                when (val parseResult = DeckParser.parseFile(filePath)) {
                    is DeckParseResult.Complete -> {
                        // Deck has commander - load card data
                        finishLoadingDeck(parseResult.deck, playerIndex)
                    }
                    is DeckParseResult.NeedsCommanderSelection -> {
                        // Need to select commander - load card data first
                        _uiState.update { it.copy(loadingProgress = "Loading card data...") }
                        val candidates = loadCommanderCandidates(parseResult.data)

                        if (candidates.isEmpty()) {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    loadingProgress = "",
                                    error = "No cards found in deck for Player ${playerIndex + 1}."
                                )
                            }
                            return@launch
                        }

                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                loadingProgress = "",
                                pendingDeckData = parseResult.data,
                                pendingDeckPlayerIndex = playerIndex,
                                commanderCandidates = candidates
                            )
                        }
                    }
                    is DeckParseResult.Error -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                loadingProgress = "",
                                error = "Failed to load deck for Player ${playerIndex + 1}: ${parseResult.message}"
                            )
                        }
                    }
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
