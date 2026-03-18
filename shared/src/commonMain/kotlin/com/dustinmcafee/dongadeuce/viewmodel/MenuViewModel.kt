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
import com.dustinmcafee.dongadeuce.platform.createHttpClientEngine
import com.dustinmcafee.dongadeuce.platform.createTlsHttpClientEngine
import com.dustinmcafee.dongadeuce.platform.ioDispatcher
import com.dustinmcafee.dongadeuce.settings.UserSettings
import com.dustinmcafee.dongadeuce.tls.TofuVerifier
import com.dustinmcafee.dongadeuce.tls.TrustDecision
import com.dustinmcafee.dongadeuce.tls.TrustedServersStore
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * UI state for the menu/lobby screen
 */
/**
 * Server mode for network games
 */
enum class ServerMode {
    LAN,       // P2P / LAN: host runs embedded server
    DEDICATED  // Dedicated server: connect to remote server
}

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
    // Server mode
    val serverMode: ServerMode = ServerMode.LAN,
    val gameCode: String? = null, // Game code for dedicated server mode
    val availableGames: List<String> = emptyList(), // Available games from dedicated server
    // Commander selection state
    val pendingDeckData: ParsedDeckData? = null, // Deck waiting for commander selection
    val pendingDeckPlayerIndex: Int? = null, // Player index for hotseat (null for single deck)
    val commanderCandidates: List<Card> = emptyList(), // Cards that can be selected as commander
    // TLS
    val tlsEnabled: Boolean = false,
    val serverFingerprint: String? = null, // Shown when hosting with TLS
    val tofuPrompt: TofuPromptData? = null // Shown when connecting to untrusted server
)

data class TofuPromptData(
    val host: String,
    val port: Int,
    val fingerprint: String
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
                serverPort = settings.serverPort,
                serverMode = if (settings.serverMode == "DEDICATED") ServerMode.DEDICATED else ServerMode.LAN,
                tlsEnabled = settings.tlsEnabled
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
     * Set player count for hotseat games (2-6 players)
     */
    fun setPlayerCount(count: Int) {
        _uiState.update { it.copy(playerCount = count.coerceIn(2, 6)) }
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
                        // Validate deck has exactly 100 cards before commander selection
                        val data = parseResult.data
                        val totalCards = data.mainboardSize + data.sideboardSize
                        if (totalCards != 100) {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    loadingProgress = "",
                                    error = "Commander decks must have exactly 100 cards. This deck has $totalCards cards."
                                )
                            }
                            return@launch
                        }

                        // Need to select commander - load card data first
                        _uiState.update { it.copy(loadingProgress = "Loading card data...") }
                        val candidates = loadCommanderCandidates(data)

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
                                pendingDeckData = data,
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
    fun selectCommander(commanderName: String, partnerName: String? = null) {
        val pendingData = _uiState.value.pendingDeckData ?: return
        val playerIndex = _uiState.value.pendingDeckPlayerIndex
        val loadedCards = _uiState.value.commanderCandidates

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, loadingProgress = "Building deck with commander...") }

            try {
                val parsedDeck = pendingData.toDeck(commanderName, partnerName)

                // Reuse already-loaded card data from commander candidates
                val cardDataMap = loadedCards.associateBy { it.name.lowercase() }

                val commanderWithData = cardDataMap[commanderName.lowercase()] ?: parsedDeck.commander
                val partnerWithData = partnerName?.let { cardDataMap[it.lowercase()] ?: parsedDeck.partnerCommander }
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
                    sideboard = sideboardWithData,
                    partnerCommander = partnerWithData
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
                        // Validate deck has exactly 100 cards before commander selection
                        val data = parseResult.data
                        val totalCards = data.mainboardSize + data.sideboardSize
                        if (totalCards != 100) {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    loadingProgress = "",
                                    error = "Commander decks must have exactly 100 cards. This deck has $totalCards cards."
                                )
                            }
                            return@launch
                        }

                        // Need to select commander - load card data first
                        _uiState.update { it.copy(loadingProgress = "Loading card data...") }
                        val candidates = loadCommanderCandidates(data)

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
                                pendingDeckData = data,
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
                        // Validate deck has exactly 100 cards before commander selection
                        val data = parseResult.data
                        val totalCards = data.mainboardSize + data.sideboardSize
                        if (totalCards != 100) {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    loadingProgress = "",
                                    error = "Commander decks must have exactly 100 cards. Player ${playerIndex + 1}'s deck has $totalCards cards."
                                )
                            }
                            return@launch
                        }

                        // Need to select commander - load card data first
                        _uiState.update { it.copy(loadingProgress = "Loading card data...") }
                        val candidates = loadCommanderCandidates(data)

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
                                pendingDeckData = data,
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
                        // Validate deck has exactly 100 cards before commander selection
                        val data = parseResult.data
                        val totalCards = data.mainboardSize + data.sideboardSize
                        if (totalCards != 100) {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    loadingProgress = "",
                                    error = "Commander decks must have exactly 100 cards. Player ${playerIndex + 1}'s deck has $totalCards cards."
                                )
                            }
                            return@launch
                        }

                        // Need to select commander - load card data first
                        _uiState.update { it.copy(loadingProgress = "Loading card data...") }
                        val candidates = loadCommanderCandidates(data)

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
                                pendingDeckData = data,
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
    /**
     * Directly set a deck for a hotseat player (skip parsing, reuse existing deck object)
     */
    fun setHotseatDeckDirectly(playerIndex: Int, deck: Deck) {
        _uiState.update {
            it.copy(hotseatDecks = it.hotseatDecks + (playerIndex to deck))
        }
    }

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
     * Start hosting a LAN game.
     * Creates an embedded server, then connects to it as a regular GameClient.
     */
    fun startHosting() {
        val deck = _uiState.value.loadedDeck
        if (deck == null) {
            _uiState.update { it.copy(error = "Please load a deck first") }
            return
        }

        val port = _uiState.value.serverPort
        val playerName = _uiState.value.playerName

        // 1. Create and start the embedded server (no host deck/name coupling)
        gameServer = GameServer(
            port = port,
            maxPlayers = 4
        )

        val serverUrl = gameServer?.start()

        _uiState.update {
            it.copy(
                isHosting = true,
                currentScreen = Screen.HostLobby,
                serverUrl = serverUrl,
                error = null
            )
        }

        // 2. Connect to own server as a regular client
        connectAsClient("localhost", port, playerName, deck, gameCode = null)
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
     * Connect to a hosted game (P2P or dedicated server)
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

        connectAsClient(address, port, playerName, deck, gameCode = _uiState.value.gameCode)
    }

    /**
     * Shared connection flow: create a GameClient, observe its state, and connect.
     * Used by both startHosting() (to localhost) and connectToGame() (to remote).
     */
    private fun connectAsClient(host: String, port: Int, playerName: String, deck: Deck, gameCode: String?) {
        // Create client (reuse if already created, e.g. for host reconnect)
        if (gameClient == null) {
            gameClient = GameClient()
        }

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
        val useTls = _uiState.value.tlsEnabled
        viewModelScope.launch {
            val success = gameClient?.connect(
                host, port, playerName, deck, gameCode,
                useTls = useTls,
                tofuVerifier = if (useTls) tofuVerifier else null,
                trustedServersStore = if (useTls) trustedServersStore else null
            ) ?: false
            if (!success && _uiState.value.currentScreen != Screen.Menu) {
                // Only show error if we're still on the connect screen (not user-initiated disconnect)
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
                gameCode = null,
                availableGames = emptyList(),
                error = null,
                serverFingerprint = null,
                tofuPrompt = null
            )
        }
    }

    /**
     * Start network game (host or admin on dedicated server)
     */
    fun startNetworkGame(): Boolean {
        // P2P mode: start via local server
        if (gameServer != null) {
            return gameServer?.startGame() ?: false
        }
        // Dedicated server mode: send StartGame via client
        if (_uiState.value.serverMode == ServerMode.DEDICATED && gameClient != null) {
            viewModelScope.launch {
                gameClient?.requestStartGame()
            }
            return true
        }
        return false
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
     * Set server mode (LAN or DEDICATED) and persist
     */
    fun setServerMode(mode: ServerMode) {
        _uiState.update { it.copy(serverMode = mode) }
        userSettings.setServerMode(mode.name)
    }

    /**
     * Set game code for joining a dedicated server game
     */
    fun setGameCode(code: String?) {
        _uiState.update { it.copy(gameCode = code) }
    }

    /**
     * Toggle TLS encryption for connections and persist.
     */
    fun setTlsEnabled(enabled: Boolean) {
        _uiState.update { it.copy(tlsEnabled = enabled) }
        userSettings.setTlsEnabled(enabled)
    }

    // TOFU deferred — suspends GameClient.connect() until user responds
    private var tofuResponseDeferred: CompletableDeferred<TrustDecision>? = null

    private val trustedServersStore = TrustedServersStore()

    private val tofuVerifier: TofuVerifier = { host, port, fingerprint ->
        val result = CompletableDeferred<TrustDecision>()
        tofuResponseDeferred = result
        _uiState.update { it.copy(tofuPrompt = TofuPromptData(host, port, fingerprint)) }
        result.await()
    }

    /**
     * Accept the TOFU fingerprint prompt.
     */
    fun acceptTofu() {
        tofuResponseDeferred?.complete(TrustDecision.ACCEPT)
        tofuResponseDeferred = null
        _uiState.update { it.copy(tofuPrompt = null) }
    }

    /**
     * Reject the TOFU fingerprint prompt.
     */
    fun rejectTofu() {
        tofuResponseDeferred?.complete(TrustDecision.REJECT)
        tofuResponseDeferred = null
        _uiState.update { it.copy(tofuPrompt = null) }
    }

    /**
     * Create a new game room on a dedicated server via REST API.
     * On success, sets the game code so the user can connect.
     * @param onCodeReceived callback with the game code for UI updates
     */
    fun createGameOnServer(onCodeReceived: (String) -> Unit = {}) {
        val state = _uiState.value
        val address = state.serverAddress
        val port = state.serverPort
        val useTls = state.tlsEnabled

        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            try {
                val engine = if (useTls) {
                    // Use trusted fingerprint if we have one, otherwise trust-all for initial create
                    val fp = trustedServersStore.getTrustedFingerprint(address, port)
                    createTlsHttpClientEngine(fp)
                } else {
                    createHttpClientEngine()
                }
                val scheme = if (useTls) "https" else "http"
                val client = HttpClient(engine)
                val response = client.post("$scheme://$address:$port/api/games")
                client.close()

                if (response.status == HttpStatusCode.Created) {
                    val body = response.bodyAsText()
                    val json = Json { ignoreUnknownKeys = true }
                    val code = json.parseToJsonElement(body).jsonObject["code"]?.jsonPrimitive?.content
                    if (code != null) {
                        _uiState.update { it.copy(gameCode = code, isLoading = false) }
                        onCodeReceived(code)
                    } else {
                        _uiState.update { it.copy(isLoading = false, error = "Server returned no game code") }
                    }
                } else {
                    val body = response.bodyAsText()
                    _uiState.update { it.copy(isLoading = false, error = "Failed to create game: $body") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "Failed to reach server: ${e.message}") }
            }
        }
    }

    /**
     * Check if we're the host
     */
    fun isHost(): Boolean = gameServer != null

    /**
     * Get the local player ID (always from GameClient now)
     */
    fun getLocalPlayerId(): String? {
        return (uiState.value.connectionState as? ConnectionState.Connected)?.playerId
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
