package com.dustinmcafee.dongadeuce.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.dustinmcafee.dongadeuce.viewmodel.MenuViewModel
import com.dustinmcafee.dongadeuce.viewmodel.Screen
import com.dustinmcafee.dongadeuce.viewmodel.ServerMode
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

@Composable
fun MainScreen(
    menuViewModel: MenuViewModel = remember { MenuViewModel() }
) {
    val uiState by menuViewModel.uiState.collectAsState()

    when (uiState.currentScreen) {
        Screen.Menu -> MenuScreen(
            viewModel = menuViewModel,
            uiState = uiState
        )
        Screen.HostLobby -> HostLobbyScreen(viewModel = menuViewModel)
        Screen.JoinLobby -> JoinLobbyScreen(viewModel = menuViewModel)
        Screen.Game -> {
            // Create GameViewModel with network client/server if in network mode
            val networkClient = menuViewModel.getGameClient()
            val networkServer = menuViewModel.getGameServer()
            val localPlayerId = menuViewModel.getLocalPlayerId()
            val gameViewModel = remember(networkClient, networkServer, localPlayerId) {
                com.dustinmcafee.dongadeuce.viewmodel.GameViewModel(
                    networkClient = networkClient,
                    networkServer = networkServer,
                    localPlayerId = localPlayerId
                )
            }

            GameScreen(
                hotseatDecks = if (uiState.hotseatMode) uiState.hotseatDecks else emptyMap(),
                playerCount = uiState.playerCount,
                isHotseatMode = uiState.hotseatMode,
                viewModel = gameViewModel,
                isPaused = uiState.isPaused,
                pauseReason = uiState.pauseReason,
                isAdmin = menuViewModel.isHost(),
                onResumeGame = { menuViewModel.resumeGame() },
                onReturnToMenu = { menuViewModel.returnToMenu() }
            )
        }
    }

    // TOFU certificate verification dialog
    if (uiState.tofuPrompt != null) {
        val prompt = uiState.tofuPrompt!!
        AlertDialog(
            onDismissRequest = { menuViewModel.rejectTofu() },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurface,
            title = { Text("Unknown Server Certificate") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Connecting to ${prompt.host}:${prompt.port}")
                    Text("The server presented a certificate you haven't seen before.")
                    Text("Fingerprint (SHA-256):", style = MaterialTheme.typography.labelMedium)
                    Text(
                        prompt.fingerprint,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                    Text("Verify this matches the fingerprint shown on the server.")
                }
            },
            confirmButton = {
                TextButton(onClick = { menuViewModel.acceptTofu() }) {
                    Text("Trust")
                }
            },
            dismissButton = {
                TextButton(onClick = { menuViewModel.rejectTofu() }) {
                    Text("Reject")
                }
            }
        )
    }

    // Show error dialog
    if (uiState.error != null) {
        AlertDialog(
            onDismissRequest = { menuViewModel.clearError() },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurface,
            title = { Text("Error") },
            text = { Text(uiState.error ?: "") },
            confirmButton = {
                TextButton(onClick = { menuViewModel.clearError() }) {
                    Text("OK")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuScreen(
    viewModel: MenuViewModel,
    uiState: com.dustinmcafee.dongadeuce.viewmodel.MenuUiState
) {
    var showSettingsDialog by remember { mutableStateOf(false) }

    // Settings Dialog
    if (showSettingsDialog) {
        SettingsDialog(
            userSettings = viewModel.userSettings,
            currentPlayerName = uiState.playerName,
            currentServerAddress = uiState.serverAddress,
            currentServerPort = uiState.serverPort,
            onPlayerNameChange = { viewModel.setPlayerName(it) },
            onServerAddressChange = { viewModel.setServerAddress(it) },
            onServerPortChange = { viewModel.setServerPort(it) },
            onDismiss = { showSettingsDialog = false }
        )
    }

    // Commander Selection Dialog (shown when loading deck without explicit commander)
    if (uiState.pendingDeckData != null && uiState.commanderCandidates.isNotEmpty()) {
        CommanderSelectionDialog(
            deckData = uiState.pendingDeckData!!,
            candidates = uiState.commanderCandidates,
            playerIndex = uiState.pendingDeckPlayerIndex,
            onCommanderSelected = { commanderName, partnerName ->
                viewModel.selectCommander(commanderName, partnerName)
            },
            onDismiss = { viewModel.cancelCommanderSelection() }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Settings button in top-right corner
        IconButton(
            onClick = { showSettingsDialog = true },
            modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings",
                tint = MaterialTheme.colorScheme.onBackground
            )
        }

        // Main content centered and scrollable
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Logo
            Image(
                painter = painterResource("dongadeuce_logo.png"),
                contentDescription = "Dong-A-Deuce Logo",
                modifier = Modifier.size(128.dp)
            )

            Text(
                text = "Dong-A-Deuce",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Show loading progress
            if (uiState.isLoading) {
                Card(
                    modifier = Modifier.width(300.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(uiState.loadingProgress, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)

                        Spacer(modifier = Modifier.height(8.dp))

                        if (uiState.loadingProgressPercent > 0) {
                            // Show determinate progress bar with percentage
                            LinearProgressIndicator(
                                progress = uiState.loadingProgressPercent / 100f,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(4.dp))

                            // Show helpful message during connection phase or initial download
                            if (uiState.loadingProgress.contains("Connecting") ||
                                (uiState.loadingProgressPercent <= 5f && uiState.loadingProgress.contains("Downloaded"))) {
                                Text(
                                    "${"%.1f".format(uiState.loadingProgressPercent)}% - This may take 1-2 minutes...",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            } else {
                                Text(
                                    "${"%.1f".format(uiState.loadingProgressPercent)}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        } else {
                            // Show indeterminate progress bar
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
            // Show loaded deck info
            else if (uiState.loadedDeck != null) {
                Card(
                    modifier = Modifier.width(300.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val deck = uiState.loadedDeck
                        Text("Deck Loaded", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text(deck?.commander?.name ?: "", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text("${deck?.totalCards ?: 0} cards", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Game mode selector
            Card(
                modifier = Modifier.width(300.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Game Mode", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = uiState.hotseatMode,
                            onClick = { viewModel.setHotseatMode(true) },
                            label = { Text("Local Hotseat", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        )
                        FilterChip(
                            selected = !uiState.hotseatMode && uiState.serverMode == ServerMode.LAN,
                            onClick = {
                                viewModel.setHotseatMode(false)
                                viewModel.setServerMode(ServerMode.LAN)
                            },
                            label = { Text("Network - P2P", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        )
                        FilterChip(
                            selected = !uiState.hotseatMode && uiState.serverMode == ServerMode.DEDICATED,
                            onClick = {
                                viewModel.setHotseatMode(false)
                                viewModel.setServerMode(ServerMode.DEDICATED)
                            },
                            label = { Text("Network - Dedicated", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Player count selector
            Card(
                modifier = Modifier.width(300.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Player Count", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(2, 3, 4, 5, 6).forEach { count ->
                            FilterChip(
                                selected = uiState.playerCount == count,
                                onClick = { viewModel.setPlayerCount(count) },
                                label = { Text("$count", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Card cache status and update
            Card(
                modifier = Modifier.width(300.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Card Cache", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))

                    if (uiState.cacheAvailable) {
                        Text(
                            if (uiState.cacheCardCount > 0) {
                                "${uiState.cacheCardCount} cards cached"
                            } else {
                                "Cache available"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        val cacheTimestamp = uiState.cacheLastUpdated
                        if (cacheTimestamp != null) {
                            val lastUpdated = java.text.SimpleDateFormat("MMM dd, yyyy").format(
                                java.util.Date(cacheTimestamp)
                            )
                            Text(
                                "Last updated: $lastUpdated",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Text(
                            "No cache available",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = { viewModel.updateCardCache() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isLoading
                    ) {
                        Text(if (uiState.cacheAvailable) "Update Cache" else "Download Cache")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Show different UI based on mode
            if (uiState.hotseatMode) {
                // Hotseat mode: Load decks for each player
                HotseatDeckLoader(
                    viewModel = viewModel,
                    uiState = uiState
                )
            } else {
                // Network mode: Player name input
                var playerName by remember { mutableStateOf(uiState.playerName) }
                OutlinedTextField(
                    value = playerName,
                    onValueChange = {
                        playerName = it
                        viewModel.setPlayerName(it)
                    },
                    label = { Text("Your Name") },
                    placeholder = { Text("Enter your name") },
                    singleLine = true,
                    modifier = Modifier.width(200.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (uiState.serverMode == ServerMode.LAN) {
                    // P2P panel
                    Button(
                        onClick = { viewModel.startHosting() },
                        modifier = Modifier.width(200.dp),
                        enabled = !uiState.isLoading && playerName.isNotBlank()
                    ) {
                        Text("Host Game")
                    }

                    Button(
                        onClick = { viewModel.navigateToJoin() },
                        modifier = Modifier.width(200.dp),
                        enabled = !uiState.isLoading && playerName.isNotBlank()
                    ) {
                        Text("Join Game")
                    }
                } else {
                    // Dedicated panel
                    Button(
                        onClick = { viewModel.hostDedicatedGame() },
                        modifier = Modifier.width(200.dp),
                        enabled = !uiState.isLoading && playerName.isNotBlank()
                    ) {
                        Text("Host Game")
                    }

                    Button(
                        onClick = { viewModel.navigateToJoin() },
                        modifier = Modifier.width(200.dp),
                        enabled = !uiState.isLoading && playerName.isNotBlank()
                    ) {
                        Text("Join Game")
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            // Open file chooser with default directory if set
                            val defaultDir = viewModel.userSettings.getLastDeckDirectory()
                            val fileChooser = JFileChooser().apply {
                                fileFilter = FileNameExtensionFilter("Deck files (*.txt, *.dec, *.dek, *.cod, *.mwDeck)", "txt", "dec", "dek", "cod", "mwDeck")
                                if (!defaultDir.isNullOrBlank()) {
                                    currentDirectory = File(defaultDir)
                                }
                            }
                            val result = fileChooser.showOpenDialog(null)
                            if (result == JFileChooser.APPROVE_OPTION) {
                                // Save the directory for next time
                                viewModel.userSettings.setLastDeckDirectory(
                                    fileChooser.selectedFile.parentFile.absolutePath
                                )
                                viewModel.loadDeck(fileChooser.selectedFile.absolutePath)
                            }
                        },
                        enabled = !uiState.isLoading
                    ) {
                        Text("Load Deck")
                    }

                    OutlinedButton(
                        onClick = {
                            // Get clipboard content
                            try {
                                val clipboard = Toolkit.getDefaultToolkit().systemClipboard
                                val clipboardData = clipboard.getData(DataFlavor.stringFlavor) as? String
                                if (clipboardData != null && clipboardData.isNotBlank()) {
                                    viewModel.loadDeckFromContent(clipboardData)
                                }
                            } catch (e: Exception) {
                                // Clipboard may be empty or unavailable
                            }
                        },
                        enabled = !uiState.isLoading
                    ) {
                        Text("Paste Deck")
                    }
                }
            }
        }
    }
}

@Composable
fun HotseatDeckLoader(
    viewModel: MenuViewModel,
    uiState: com.dustinmcafee.dongadeuce.viewmodel.MenuUiState
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Show deck loading UI for each player
        for (playerIndex in 0 until uiState.playerCount) {
            val deckLoaded = uiState.hotseatDecks.containsKey(playerIndex)
            val deck = uiState.hotseatDecks[playerIndex]

            Card(
                modifier = Modifier.width(300.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (deckLoaded)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            "Player ${playerIndex + 1}",
                            style = MaterialTheme.typography.titleSmall,
                            color = if (deckLoaded) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (deck != null) {
                            Text(
                                deck.commander.name,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        } else {
                            Text(
                                "No deck loaded",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        OutlinedButton(
                            onClick = {
                                // Open file chooser with default directory if set
                                val defaultDir = viewModel.userSettings.getLastDeckDirectory()
                                val fileChooser = JFileChooser().apply {
                                    fileFilter = FileNameExtensionFilter("Deck files (*.txt, *.dec, *.dek, *.cod, *.mwDeck)", "txt", "dec", "dek", "cod", "mwDeck")
                                    if (!defaultDir.isNullOrBlank()) {
                                        currentDirectory = File(defaultDir)
                                    }
                                }
                                val result = fileChooser.showOpenDialog(null)
                                if (result == JFileChooser.APPROVE_OPTION) {
                                    // Save the directory for next time
                                    viewModel.userSettings.setLastDeckDirectory(
                                        fileChooser.selectedFile.parentFile.absolutePath
                                    )
                                    viewModel.loadHotseatDeck(
                                        playerIndex,
                                        fileChooser.selectedFile.absolutePath
                                    )
                                }
                            },
                            enabled = !uiState.isLoading,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(if (deckLoaded) "Change" else "Load")
                        }

                        OutlinedButton(
                            onClick = {
                                // Get clipboard content
                                try {
                                    val clipboard = Toolkit.getDefaultToolkit().systemClipboard
                                    val clipboardData = clipboard.getData(DataFlavor.stringFlavor) as? String
                                    if (clipboardData != null && clipboardData.isNotBlank()) {
                                        viewModel.loadHotseatDeckFromContent(playerIndex, clipboardData)
                                    }
                                } catch (e: Exception) {
                                    // Clipboard may be empty or unavailable
                                }
                            },
                            enabled = !uiState.isLoading,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text("Paste")
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Start game button
        Button(
            onClick = { viewModel.startHotseatGame() },
            modifier = Modifier.width(200.dp),
            enabled = uiState.hotseatDecks.size == uiState.playerCount && !uiState.isLoading
        ) {
            Text("Start Game")
        }
    }
}

@Composable
fun HostLobbyScreen(viewModel: MenuViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val lobbyState = uiState.lobbyState

    // Commander Selection Dialog
    if (uiState.pendingDeckData != null && uiState.commanderCandidates.isNotEmpty()) {
        CommanderSelectionDialog(
            deckData = uiState.pendingDeckData!!,
            candidates = uiState.commanderCandidates,
            playerIndex = uiState.pendingDeckPlayerIndex,
            onCommanderSelected = { commanderName, partnerName ->
                viewModel.selectCommander(commanderName, partnerName)
            },
            onDismiss = { viewModel.cancelCommanderSelection() }
        )
    }

    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Hosting Game", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)

            Spacer(modifier = Modifier.height(16.dp))

            // Deck status card
            Card(
                modifier = Modifier.width(400.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (uiState.loadedDeck != null)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        if (uiState.loadedDeck != null) "Deck: ${uiState.loadedDeck!!.commander.name}"
                        else "No deck loaded",
                        style = MaterialTheme.typography.titleSmall,
                        color = if (uiState.loadedDeck != null) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                val defaultDir = viewModel.userSettings.getLastDeckDirectory()
                                val fileChooser = JFileChooser().apply {
                                    fileFilter = FileNameExtensionFilter("Deck files (*.txt, *.dec, *.dek, *.cod, *.mwDeck)", "txt", "dec", "dek", "cod", "mwDeck")
                                    if (!defaultDir.isNullOrBlank()) {
                                        currentDirectory = File(defaultDir)
                                    }
                                }
                                val result = fileChooser.showOpenDialog(null)
                                if (result == JFileChooser.APPROVE_OPTION) {
                                    viewModel.userSettings.setLastDeckDirectory(
                                        fileChooser.selectedFile.parentFile.absolutePath
                                    )
                                    viewModel.loadDeck(fileChooser.selectedFile.absolutePath)
                                }
                            },
                            enabled = !uiState.isLoading
                        ) {
                            Text(if (uiState.loadedDeck != null) "Change Deck" else "Load Deck")
                        }
                        OutlinedButton(
                            onClick = {
                                try {
                                    val clipboard = Toolkit.getDefaultToolkit().systemClipboard
                                    val clipboardData = clipboard.getData(DataFlavor.stringFlavor) as? String
                                    if (clipboardData != null && clipboardData.isNotBlank()) {
                                        viewModel.loadDeckFromContent(clipboardData)
                                    }
                                } catch (_: Exception) {}
                            },
                            enabled = !uiState.isLoading
                        ) {
                            Text("Paste Deck")
                        }
                    }
                }
            }

            // Loading indicator
            if (uiState.isLoading) {
                LinearProgressIndicator(modifier = Modifier.width(400.dp))
                Text(uiState.loadingProgress, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground)
            }

            Card(modifier = Modifier.width(400.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Waiting for players...", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))

                    // Show server URL
                    val serverUrl = uiState.serverUrl ?: "localhost:${uiState.serverPort}"
                    Text("Server: $serverUrl", style = MaterialTheme.typography.bodyMedium)
                    Text("Share this address with other players", style = MaterialTheme.typography.bodySmall)

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Connected Players (${lobbyState?.players?.size ?: 0}/${lobbyState?.maxPlayers ?: 4}):", style = MaterialTheme.typography.labelLarge)

                    if (lobbyState?.players.isNullOrEmpty()) {
                        Text("No players yet...", style = MaterialTheme.typography.bodySmall)
                    } else {
                        lobbyState?.players?.forEach { player ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        if (player.isAdmin || player.isHost) "👑 ${player.name}" else "• ${player.name}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    if (!player.hasDeck) {
                                        Text(" (no deck)", style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.error)
                                    }
                                    if (player.isReady && !player.isAdmin && !player.isHost) {
                                        Text(" ✓ Ready", style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary)
                                    }
                                }

                                // Kick button (not for host)
                                if (!player.isAdmin && !player.isHost) {
                                    TextButton(
                                        onClick = { viewModel.kickPlayer(player.id) }
                                    ) {
                                        Text("Kick", color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Check if all non-host players are ready and all have decks
            val allReady = lobbyState?.players?.filter { !it.isAdmin && !it.isHost }?.all { it.isReady } ?: false
            val allHaveDecks = lobbyState?.players?.all { it.hasDeck } ?: false
            val enoughPlayers = (lobbyState?.players?.size ?: 0) >= 2

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(
                    onClick = {
                        if (!viewModel.startNetworkGame()) {
                            // Show error if start failed
                        }
                    },
                    enabled = enoughPlayers && allReady && allHaveDecks
                ) {
                    Text("Start Game")
                }

                OutlinedButton(onClick = { viewModel.returnToMenu() }) {
                    Text("Cancel")
                }
            }

            // Show hint if waiting for players
            if (!enoughPlayers) {
                Text(
                    "Need at least 2 players to start",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else if (!allHaveDecks) {
                Text(
                    "Waiting for all players to load a deck...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else if (!allReady) {
                Text(
                    "Waiting for all players to be ready...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JoinLobbyScreen(viewModel: MenuViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    var serverAddress by remember { mutableStateOf(uiState.serverAddress) }
    var serverPort by remember { mutableStateOf(uiState.serverPort.toString()) }
    var gameCode by remember { mutableStateOf(uiState.gameCode ?: "") }
    val isDedicated = uiState.serverMode == ServerMode.DEDICATED
    val lobbyState = uiState.lobbyState
    val isConnected = uiState.connectionState is com.dustinmcafee.dongadeuce.network.ConnectionState.Connected

    // Find current player's ready status
    val currentPlayerId = (uiState.connectionState as? com.dustinmcafee.dongadeuce.network.ConnectionState.Connected)?.playerId
    val currentPlayer = lobbyState?.players?.find { it.id == currentPlayerId }
    val isReady = currentPlayer?.isReady ?: false

    // Sync game code from viewmodel (e.g. after Create Game)
    LaunchedEffect(uiState.gameCode) {
        if (uiState.gameCode != null && uiState.gameCode != gameCode) {
            gameCode = uiState.gameCode!!
        }
    }

    // Commander Selection Dialog
    if (uiState.pendingDeckData != null && uiState.commanderCandidates.isNotEmpty()) {
        CommanderSelectionDialog(
            deckData = uiState.pendingDeckData!!,
            candidates = uiState.commanderCandidates,
            playerIndex = uiState.pendingDeckPlayerIndex,
            onCommanderSelected = { commanderName, partnerName ->
                viewModel.selectCommander(commanderName, partnerName)
            },
            onDismiss = { viewModel.cancelCommanderSelection() }
        )
    }

    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Join Game", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)

            Spacer(modifier = Modifier.height(16.dp))

            if (!isConnected) {
                // Connection form — server mode already set from menu
                OutlinedTextField(
                    value = serverAddress,
                    onValueChange = {
                        serverAddress = it
                        viewModel.setServerAddress(it)
                    },
                    label = { Text("Server Address") },
                    placeholder = { Text("localhost or IP address") },
                    modifier = Modifier.width(300.dp)
                )

                OutlinedTextField(
                    value = serverPort,
                    onValueChange = {
                        serverPort = it.filter { c -> c.isDigit() }
                        serverPort.toIntOrNull()?.let { port ->
                            viewModel.setServerPort(port)
                        }
                    },
                    label = { Text("Port") },
                    placeholder = { Text(if (isDedicated) "9090" else "8080") },
                    modifier = Modifier.width(300.dp)
                )

                // Game code field (dedicated server only)
                if (isDedicated) {
                    OutlinedTextField(
                        value = gameCode,
                        onValueChange = {
                            gameCode = it.uppercase()
                            viewModel.setGameCode(it.uppercase().ifBlank { null })
                        },
                        label = { Text("Game Code") },
                        placeholder = { Text("e.g. ABC123") },
                        modifier = Modifier.width(300.dp)
                    )

                    OutlinedButton(
                        onClick = {
                            viewModel.createGameOnServer { code ->
                                gameCode = code
                            }
                        },
                        enabled = !uiState.isLoading
                    ) {
                        Text("Create New Game")
                    }
                }

                // TLS toggle
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Checkbox(
                        checked = uiState.tlsEnabled,
                        onCheckedChange = { viewModel.setTlsEnabled(it) }
                    )
                    Text("Encrypt connection (TLS)", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Button(
                        onClick = { viewModel.connectToGame() },
                        enabled = !uiState.isLoading && (!isDedicated || gameCode.isNotBlank())
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Connect")
                        }
                    }

                    OutlinedButton(onClick = { viewModel.returnToMenu() }) {
                        Text("Cancel")
                    }
                }
            } else {
                // Connected - show lobby

                // Deck status + load/paste buttons
                Card(
                    modifier = Modifier.width(400.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (uiState.loadedDeck != null)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            if (uiState.loadedDeck != null) "Deck: ${uiState.loadedDeck!!.commander.name}"
                            else "No deck loaded",
                            style = MaterialTheme.typography.titleSmall,
                            color = if (uiState.loadedDeck != null) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    val defaultDir = viewModel.userSettings.getLastDeckDirectory()
                                    val fileChooser = JFileChooser().apply {
                                        fileFilter = FileNameExtensionFilter("Deck files (*.txt, *.dec, *.dek, *.cod, *.mwDeck)", "txt", "dec", "dek", "cod", "mwDeck")
                                        if (!defaultDir.isNullOrBlank()) {
                                            currentDirectory = File(defaultDir)
                                        }
                                    }
                                    val result = fileChooser.showOpenDialog(null)
                                    if (result == JFileChooser.APPROVE_OPTION) {
                                        viewModel.userSettings.setLastDeckDirectory(
                                            fileChooser.selectedFile.parentFile.absolutePath
                                        )
                                        viewModel.loadDeck(fileChooser.selectedFile.absolutePath)
                                    }
                                },
                                enabled = !uiState.isLoading
                            ) {
                                Text(if (uiState.loadedDeck != null) "Change Deck" else "Load Deck")
                            }
                            OutlinedButton(
                                onClick = {
                                    try {
                                        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
                                        val clipboardData = clipboard.getData(DataFlavor.stringFlavor) as? String
                                        if (clipboardData != null && clipboardData.isNotBlank()) {
                                            viewModel.loadDeckFromContent(clipboardData)
                                        }
                                    } catch (_: Exception) {}
                                },
                                enabled = !uiState.isLoading
                            ) {
                                Text("Paste Deck")
                            }
                        }
                    }
                }

                // Loading indicator
                if (uiState.isLoading) {
                    LinearProgressIndicator(modifier = Modifier.width(400.dp))
                    Text(uiState.loadingProgress, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground)
                }

                Card(
                    modifier = Modifier.width(400.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Connected to lobby", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(8.dp))

                        Text("Players (${lobbyState?.players?.size ?: 0}/${lobbyState?.maxPlayers ?: 4}):", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)

                        lobbyState?.players?.forEach { player ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    if (player.isAdmin || player.isHost) "👑 ${player.name}" else "• ${player.name}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (player.id == currentPlayerId) {
                                    Text(" (You)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                if (!player.hasDeck) {
                                    Text(" (no deck)", style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error)
                                }
                                if (player.isReady && !player.isAdmin && !player.isHost) {
                                    Text(" ✓ Ready", style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Check if current player is admin (first player / game creator)
                val isAdmin = lobbyState?.players?.find { it.id == currentPlayerId }?.isAdmin ?: false
                val allNonAdminReady = lobbyState?.players?.filter { !it.isAdmin }?.all { it.isReady } ?: false
                val allHaveDecks = lobbyState?.players?.all { it.hasDeck } ?: false
                val enoughPlayers = (lobbyState?.players?.size ?: 0) >= 2

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    if (isAdmin && isDedicated) {
                        Button(
                            onClick = { viewModel.startNetworkGame() },
                            enabled = enoughPlayers && allNonAdminReady && allHaveDecks
                        ) {
                            Text("Start Game")
                        }
                    } else {
                        Button(
                            onClick = { viewModel.setReady(!isReady) },
                            enabled = uiState.loadedDeck != null || isReady
                        ) {
                            Text(if (isReady) "Not Ready" else "Ready!")
                        }
                    }

                    OutlinedButton(onClick = { viewModel.returnToMenu() }) {
                        Text("Leave")
                    }
                }

                Text(
                    when {
                        uiState.loadedDeck == null && !isAdmin -> "Load a deck to ready up"
                        isAdmin && !enoughPlayers -> "Need at least 2 players to start"
                        isAdmin && !allHaveDecks -> "Waiting for all players to load a deck..."
                        isAdmin && !allNonAdminReady -> "Waiting for all players to be ready..."
                        isReady -> "Waiting for host to start the game..."
                        else -> "Click 'Ready!' when you're ready to play"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
