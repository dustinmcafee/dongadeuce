package com.commandermtg.viewmodel

import com.commandermtg.game.DeckParser
import com.commandermtg.models.Deck
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.File

/**
 * UI state for the menu/lobby screen
 */
data class MenuUiState(
    val playerName: String = "Player 1",
    val loadedDeck: Deck? = null,
    val isHosting: Boolean = false,
    val connectedPlayers: List<String> = emptyList(),
    val serverAddress: String = "",
    val serverPort: Int = 8080,
    val isLoading: Boolean = false,
    val error: String? = null,
    val currentScreen: Screen = Screen.Menu
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

    /**
     * Update player name
     */
    fun setPlayerName(name: String) {
        _uiState.update { it.copy(playerName = name) }
    }

    /**
     * Load a deck from file
     */
    fun loadDeck(filePath: String) {
        _uiState.update { it.copy(isLoading = true, error = null) }

        try {
            val file = File(filePath)
            if (!file.exists()) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "File not found: $filePath"
                    )
                }
                return
            }

            val deck = DeckParser.parseCockatriceFile(file)

            _uiState.update {
                it.copy(
                    loadedDeck = deck,
                    isLoading = false,
                    error = null
                )
            }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    error = "Failed to load deck: ${e.message}"
                )
            }
        }
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
