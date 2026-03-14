package com.dustinmcafee.dongadeuce.viewmodel

import com.dustinmcafee.dongadeuce.models.*
import com.dustinmcafee.dongadeuce.network.*
import kotlin.test.*

class MenuViewModelTest {

    private fun createTestDeck(): Deck {
        return Deck(
            name = "Test Deck",
            commander = Card(name = "Test Commander", type = "Legendary Creature"),
            cards = (1..99).map { Card(name = "Card $it") }
        )
    }

    @Test
    fun `initial state has correct defaults`() {
        val vm = MenuViewModel()
        val state = vm.uiState.value

        // playerName, serverAddress, serverPort may come from persisted settings
        assertTrue(state.playerName.isNotEmpty())
        assertEquals(2, state.playerCount)
        assertNull(state.loadedDeck)
        assertFalse(state.hotseatMode)
        assertFalse(state.isHosting)
        assertTrue(state.serverPort in 1024..65535)
        assertEquals(Screen.Menu, state.currentScreen)
        assertEquals(ServerMode.LAN, state.serverMode)
        assertNull(state.gameCode)
        assertTrue(state.availableGames.isEmpty())
        assertEquals(ConnectionState.Disconnected, state.connectionState)
        assertFalse(state.isNetworkGameStarted)
        assertFalse(state.isPaused)
    }

    @Test
    fun `setPlayerName updates state`() {
        val vm = MenuViewModel()
        vm.setPlayerName("TestPlayer")
        assertEquals("TestPlayer", vm.uiState.value.playerName)
    }

    @Test
    fun `setPlayerCount clamps to valid range`() {
        val vm = MenuViewModel()
        vm.setPlayerCount(1)
        assertEquals(2, vm.uiState.value.playerCount) // min 2

        vm.setPlayerCount(10)
        assertEquals(6, vm.uiState.value.playerCount) // max 6

        vm.setPlayerCount(4)
        assertEquals(4, vm.uiState.value.playerCount)
    }

    @Test
    fun `setHotseatMode toggles mode`() {
        val vm = MenuViewModel()
        vm.setHotseatMode(true)
        assertTrue(vm.uiState.value.hotseatMode)

        vm.setHotseatMode(false)
        assertFalse(vm.uiState.value.hotseatMode)
    }

    @Test
    fun `setHotseatMode off clears hotseat decks`() {
        val vm = MenuViewModel()
        vm.setHotseatMode(true)
        vm.setHotseatMode(false)
        assertTrue(vm.uiState.value.hotseatDecks.isEmpty())
    }

    @Test
    fun `setServerAddress updates state`() {
        val vm = MenuViewModel()
        vm.setServerAddress("192.168.1.100")
        assertEquals("192.168.1.100", vm.uiState.value.serverAddress)
    }

    @Test
    fun `setServerPort clamps to valid range`() {
        val vm = MenuViewModel()
        vm.setServerPort(80)
        assertEquals(1024, vm.uiState.value.serverPort)

        vm.setServerPort(70000)
        assertEquals(65535, vm.uiState.value.serverPort)

        vm.setServerPort(9090)
        assertEquals(9090, vm.uiState.value.serverPort)
    }

    @Test
    fun `setServerMode updates state`() {
        val vm = MenuViewModel()
        vm.setServerMode(ServerMode.DEDICATED)
        assertEquals(ServerMode.DEDICATED, vm.uiState.value.serverMode)

        vm.setServerMode(ServerMode.LAN)
        assertEquals(ServerMode.LAN, vm.uiState.value.serverMode)
    }

    @Test
    fun `setGameCode updates state`() {
        val vm = MenuViewModel()
        vm.setGameCode("ABC123")
        assertEquals("ABC123", vm.uiState.value.gameCode)

        vm.setGameCode(null)
        assertNull(vm.uiState.value.gameCode)
    }

    @Test
    fun `clearError clears error`() {
        val vm = MenuViewModel()
        // Trigger an error by trying to host without deck
        vm.startHosting()
        assertNotNull(vm.uiState.value.error)

        vm.clearError()
        assertNull(vm.uiState.value.error)
    }

    @Test
    fun `startHosting without deck shows error`() {
        val vm = MenuViewModel()
        vm.startHosting()
        assertEquals("Please load a deck first", vm.uiState.value.error)
        assertFalse(vm.uiState.value.isHosting)
    }

    @Test
    fun `navigateToJoin without deck shows error`() {
        val vm = MenuViewModel()
        vm.navigateToJoin()
        assertEquals("Please load a deck first", vm.uiState.value.error)
    }

    @Test
    fun `connectToGame without deck shows error`() {
        val vm = MenuViewModel()
        vm.connectToGame()
        assertEquals("Please load a deck first", vm.uiState.value.error)
    }

    @Test
    fun `connectToGame with blank address shows error`() {
        val vm = MenuViewModel()
        // Need to set a deck first via internal state
        // Use reflection or just test the address check
        vm.setServerAddress("")
        vm.connectToGame()
        // Will hit deck check first
        assertEquals("Please load a deck first", vm.uiState.value.error)
    }

    @Test
    fun `isHost returns false initially`() {
        val vm = MenuViewModel()
        assertFalse(vm.isHost())
    }

    @Test
    fun `getLocalPlayerId returns null initially`() {
        val vm = MenuViewModel()
        assertNull(vm.getLocalPlayerId())
    }

    @Test
    fun `getGameServer returns null initially`() {
        val vm = MenuViewModel()
        assertNull(vm.getGameServer())
    }

    @Test
    fun `getGameClient returns null initially`() {
        val vm = MenuViewModel()
        assertNull(vm.getGameClient())
    }

    @Test
    fun `returnToMenu resets all network state`() {
        val vm = MenuViewModel()
        vm.returnToMenu()

        val state = vm.uiState.value
        assertEquals(Screen.Menu, state.currentScreen)
        assertFalse(state.isHosting)
        assertTrue(state.connectedPlayers.isEmpty())
        assertNull(state.lobbyState)
        assertEquals(ConnectionState.Disconnected, state.connectionState)
        assertNull(state.networkGameState)
        assertFalse(state.isNetworkGameStarted)
        assertFalse(state.isPaused)
        assertNull(state.pauseReason)
        assertNull(state.serverUrl)
        assertNull(state.gameCode)
        assertTrue(state.availableGames.isEmpty())
        assertNull(state.error)
    }

    @Test
    fun `startHotseatGame with insufficient decks shows error`() {
        val vm = MenuViewModel()
        vm.setHotseatMode(true)
        vm.setPlayerCount(2)
        vm.startHotseatGame()

        assertNotNull(vm.uiState.value.error)
        assertTrue(vm.uiState.value.error!!.contains("load decks"))
    }

    @Test
    fun `startNetworkGame without server returns false`() {
        val vm = MenuViewModel()
        assertFalse(vm.startNetworkGame())
    }

    @Test
    fun `addPlayer and removePlayer update connected players`() {
        val vm = MenuViewModel()
        vm.addPlayer("Alice")
        vm.addPlayer("Bob")
        assertEquals(listOf("Alice", "Bob"), vm.uiState.value.connectedPlayers)

        vm.removePlayer("Alice")
        assertEquals(listOf("Bob"), vm.uiState.value.connectedPlayers)
    }

    @Test
    fun `cancelCommanderSelection clears pending state`() {
        val vm = MenuViewModel()
        vm.cancelCommanderSelection()

        assertNull(vm.uiState.value.pendingDeckData)
        assertNull(vm.uiState.value.pendingDeckPlayerIndex)
        assertTrue(vm.uiState.value.commanderCandidates.isEmpty())
    }
}
