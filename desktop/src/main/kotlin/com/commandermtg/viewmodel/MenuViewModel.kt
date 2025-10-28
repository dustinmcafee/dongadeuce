package com.commandermtg.viewmodel

import com.commandermtg.api.ScryfallApi
import com.commandermtg.api.CardCache
import com.commandermtg.game.DeckParser
import com.commandermtg.models.Card
import com.commandermtg.models.Deck
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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
    val serverAddress: String = "",
    val serverPort: Int = 8080,
    val isLoading: Boolean = false,
    val loadingProgress: String = "",
    val loadingProgressPercent: Float = 0f,
    val error: String? = null,
    val currentScreen: Screen = Screen.Menu,
    val cacheAvailable: Boolean = false,
    val cacheCardCount: Int = 0,
    val cacheLastUpdated: Long? = null
)

sealed class Screen {
    object Menu : Screen()
    object HostLobby : Screen()
    object JoinLobby : Screen()
    object Game : Screen()
}

class MenuViewModel {
    private val _uiState = MutableStateFlow(MenuUiState())
    val uiState: StateFlow<MenuUiState> = _uiState.asStateFlow()

    private val viewModelScope = CoroutineScope(Dispatchers.IO)
    private val scryfallApi = ScryfallApi()
    private val cardCache = CardCache()

    init {
        // Check cache status on initialization
        updateCacheStatus()
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
     * Update player name
     */
    fun setPlayerName(name: String) {
        _uiState.update { it.copy(playerName = name) }
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
        if (_uiState.value.loadedDeck == null) {
            _uiState.update { it.copy(error = "Please load a deck first") }
            return
        }

        _uiState.update {
            it.copy(
                isHosting = true,
                currentScreen = Screen.HostLobby,
                error = null
            )
        }

        // TODO: Start network server
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
     * Set server address for joining
     */
    fun setServerAddress(address: String) {
        _uiState.update { it.copy(serverAddress = address) }
    }

    /**
     * Connect to a hosted game
     */
    fun connectToGame() {
        _uiState.update { it.copy(isLoading = true, error = null) }

        // TODO: Connect to network server
        // For now, just simulate connection
        _uiState.update {
            it.copy(
                isLoading = false,
                error = "Network connection not implemented yet"
            )
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
        _uiState.update {
            it.copy(
                currentScreen = Screen.Menu,
                isHosting = false,
                connectedPlayers = emptyList(),
                error = null
            )
        }
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
