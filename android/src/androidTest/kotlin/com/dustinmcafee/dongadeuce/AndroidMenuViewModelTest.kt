package com.dustinmcafee.dongadeuce

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dustinmcafee.dongadeuce.network.ConnectionState
import com.dustinmcafee.dongadeuce.viewmodel.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Android instrumented tests for AndroidMenuViewModel.
 * Validates that the Android-specific ViewModel wrapper correctly
 * delegates to the shared MenuViewModel.
 */
@RunWith(AndroidJUnit4::class)
class AndroidMenuViewModelTest {

    @Test
    fun initialState_hasDefaults() {
        val vm = AndroidMenuViewModel()
        val state = vm.uiState.value
        assertTrue(state.playerName.isNotEmpty())
        assertEquals(2, state.playerCount)
        assertNull(state.loadedDeck)
        assertFalse(state.hotseatMode)
        assertFalse(state.isHosting)
        assertEquals(ServerMode.LAN, state.serverMode)
        assertNull(state.gameCode)
        assertEquals(ConnectionState.Disconnected, state.connectionState)
    }

    @Test
    fun setPlayerName_delegatesToShared() {
        val vm = AndroidMenuViewModel()
        vm.setPlayerName("TestAndroid")
        assertEquals("TestAndroid", vm.uiState.value.playerName)
    }

    @Test
    fun setPlayerCount_clampsRange() {
        val vm = AndroidMenuViewModel()
        vm.setPlayerCount(1)
        assertEquals(2, vm.uiState.value.playerCount)
        vm.setPlayerCount(10)
        assertEquals(6, vm.uiState.value.playerCount)
    }

    @Test
    fun setHotseatMode_toggles() {
        val vm = AndroidMenuViewModel()
        vm.setHotseatMode(true)
        assertTrue(vm.uiState.value.hotseatMode)
        vm.setHotseatMode(false)
        assertFalse(vm.uiState.value.hotseatMode)
    }

    @Test
    fun setServerAddress_updates() {
        val vm = AndroidMenuViewModel()
        vm.setServerAddress("192.168.1.50")
        assertEquals("192.168.1.50", vm.uiState.value.serverAddress)
    }

    @Test
    fun setServerPort_clampsRange() {
        val vm = AndroidMenuViewModel()
        vm.setServerPort(80)
        assertEquals(1024, vm.uiState.value.serverPort)
        vm.setServerPort(9090)
        assertEquals(9090, vm.uiState.value.serverPort)
    }

    @Test
    fun setServerMode_updates() {
        val vm = AndroidMenuViewModel()
        vm.setServerMode(ServerMode.DEDICATED)
        assertEquals(ServerMode.DEDICATED, vm.uiState.value.serverMode)
    }

    @Test
    fun setGameCode_updates() {
        val vm = AndroidMenuViewModel()
        vm.setGameCode("XYZ789")
        assertEquals("XYZ789", vm.uiState.value.gameCode)
        vm.setGameCode(null)
        assertNull(vm.uiState.value.gameCode)
    }

    @Test
    fun clearError_clearsError() {
        val vm = AndroidMenuViewModel()
        vm.setServerAddress("") // blank address
        vm.connectToGame() // triggers "Please enter a server address" error
        assertNotNull(vm.uiState.value.error)
        vm.clearError()
        assertNull(vm.uiState.value.error)
    }

    @Test
    fun startHosting_withoutDeck_proceedsToLobby() {
        val vm = AndroidMenuViewModel()
        vm.startHosting()
        // Deck is no longer required — hosting proceeds (may fail on port, but no deck error)
        assertNotEquals("Please load a deck first", vm.uiState.value.error)
    }

    @Test
    fun isHost_initiallyFalse() {
        val vm = AndroidMenuViewModel()
        assertFalse(vm.isHost())
    }

    @Test
    fun getLocalPlayerId_initiallyNull() {
        val vm = AndroidMenuViewModel()
        assertNull(vm.getLocalPlayerId())
    }

    @Test
    fun getGameServer_initiallyNull() {
        val vm = AndroidMenuViewModel()
        assertNull(vm.getGameServer())
    }

    @Test
    fun getGameClient_initiallyNull() {
        val vm = AndroidMenuViewModel()
        assertNull(vm.getGameClient())
    }

    @Test
    fun returnToMenu_resetsState() {
        val vm = AndroidMenuViewModel()
        vm.returnToMenu()
        val state = vm.uiState.value
        assertFalse(state.isHosting)
        assertNull(state.lobbyState)
        assertEquals(ConnectionState.Disconnected, state.connectionState)
        assertNull(state.networkGameState)
        assertFalse(state.isNetworkGameStarted)
        assertNull(state.gameCode)
        assertNull(state.error)
    }

    @Test
    fun currentScreen_initiallyMenu() {
        val vm = AndroidMenuViewModel()
        assertEquals(AndroidScreen.Menu, vm.currentScreen.value)
    }

    @Test
    fun cancelCommanderSelection_clearsState() {
        val vm = AndroidMenuViewModel()
        vm.cancelCommanderSelection()
        assertNull(vm.uiState.value.pendingDeckData)
        assertTrue(vm.uiState.value.commanderCandidates.isEmpty())
    }

    @Test
    fun startNetworkGame_withoutServer_returnsFalse() {
        val vm = AndroidMenuViewModel()
        assertFalse(vm.startNetworkGame())
    }

    @Test
    fun startHotseatGame_withoutDecks_showsError() {
        val vm = AndroidMenuViewModel()
        vm.setHotseatMode(true)
        vm.setPlayerCount(2)
        vm.startHotseatGame()
        assertNotNull(vm.uiState.value.error)
    }
}
