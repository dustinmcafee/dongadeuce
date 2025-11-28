package com.dustinmcafee.dongadeuce.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.dustinmcafee.dongadeuce.viewmodel.MenuViewModel
import com.dustinmcafee.dongadeuce.viewmodel.Screen
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
                loadedDeck = uiState.loadedDeck,
                hotseatDecks = if (uiState.hotseatMode) uiState.hotseatDecks else emptyMap(),
                playerCount = uiState.playerCount,
                isHotseatMode = uiState.hotseatMode,
                viewModel = gameViewModel,
                networkGameState = uiState.networkGameState,
                isPaused = uiState.isPaused,
                pauseReason = uiState.pauseReason,
                isHost = menuViewModel.isHost(),
                onResumeGame = { menuViewModel.resumeGame() },
                onReturnToMenu = { menuViewModel.returnToMenu() }
            )
        }
    }

    // Show error snackbar if there's an error
    if (uiState.error != null) {
        AlertDialog(
            onDismissRequest = { menuViewModel.clearError() },
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
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
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
                style = MaterialTheme.typography.headlineLarge
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
                        Text(uiState.loadingProgress, style = MaterialTheme.typography.bodyMedium)

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
                        Text("Deck Loaded", style = MaterialTheme.typography.labelMedium)
                        Text(uiState.loadedDeck.commander.name, style = MaterialTheme.typography.titleMedium)
                        Text("${uiState.loadedDeck.totalCards} cards", style = MaterialTheme.typography.bodySmall)
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
                    Text("Game Mode", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = !uiState.hotseatMode,
                            onClick = { viewModel.setHotseatMode(false) },
                            label = { Text("Network") }
                        )
                        FilterChip(
                            selected = uiState.hotseatMode,
                            onClick = { viewModel.setHotseatMode(true) },
                            label = { Text("Local Hotseat") }
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
                    Text("Player Count", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(2, 3, 4).forEach { count ->
                            FilterChip(
                                selected = uiState.playerCount == count,
                                onClick = { viewModel.setPlayerCount(count) },
                                label = { Text("$count Players") }
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
                    Text("Card Cache", style = MaterialTheme.typography.labelMedium)
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
                        if (uiState.cacheLastUpdated != null) {
                            val lastUpdated = java.text.SimpleDateFormat("MMM dd, yyyy").format(
                                java.util.Date(uiState.cacheLastUpdated)
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

                // Network mode: Single deck + Host/Join buttons
                Button(
                    onClick = { viewModel.startHosting() },
                    modifier = Modifier.width(200.dp),
                    enabled = uiState.loadedDeck != null && !uiState.isLoading && playerName.isNotBlank()
                ) {
                    Text("Host Game")
                }

                Button(
                    onClick = { viewModel.navigateToJoin() },
                    modifier = Modifier.width(200.dp),
                    enabled = uiState.loadedDeck != null && !uiState.isLoading && playerName.isNotBlank()
                ) {
                    Text("Join Game")
                }

                OutlinedButton(
                    onClick = {
                        // Open file chooser
                        val fileChooser = JFileChooser().apply {
                            fileFilter = FileNameExtensionFilter("Text files", "txt")
                        }
                        val result = fileChooser.showOpenDialog(null)
                        if (result == JFileChooser.APPROVE_OPTION) {
                            viewModel.loadDeck(fileChooser.selectedFile.absolutePath)
                        }
                    },
                    modifier = Modifier.width(200.dp),
                    enabled = !uiState.isLoading
                ) {
                    Text("Load Deck")
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
                            style = MaterialTheme.typography.titleSmall
                        )
                        if (deck != null) {
                            Text(
                                deck.commander.name,
                                style = MaterialTheme.typography.bodySmall
                            )
                        } else {
                            Text(
                                "No deck loaded",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            val fileChooser = JFileChooser().apply {
                                fileFilter = FileNameExtensionFilter("Text files", "txt")
                            }
                            val result = fileChooser.showOpenDialog(null)
                            if (result == JFileChooser.APPROVE_OPTION) {
                                viewModel.loadHotseatDeck(
                                    playerIndex,
                                    fileChooser.selectedFile.absolutePath
                                )
                            }
                        },
                        enabled = !uiState.isLoading
                    ) {
                        Text(if (deckLoaded) "Change" else "Load")
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

    Box(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Hosting Game", style = MaterialTheme.typography.headlineMedium)

            Spacer(modifier = Modifier.height(16.dp))

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
                                        if (player.isHost) "👑 ${player.name}" else "• ${player.name}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    if (player.isReady && !player.isHost) {
                                        Text(" ✓ Ready", style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary)
                                    }
                                }

                                // Kick button (not for host)
                                if (!player.isHost) {
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

            // Check if all non-host players are ready
            val allReady = lobbyState?.players?.filter { !it.isHost }?.all { it.isReady } ?: false
            val enoughPlayers = (lobbyState?.players?.size ?: 0) >= 2

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(
                    onClick = {
                        if (!viewModel.startNetworkGame()) {
                            // Show error if start failed
                        }
                    },
                    enabled = enoughPlayers && allReady
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

@Composable
fun JoinLobbyScreen(viewModel: MenuViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    var serverAddress by remember { mutableStateOf("localhost") }
    var serverPort by remember { mutableStateOf("8080") }
    val lobbyState = uiState.lobbyState
    val isConnected = uiState.connectionState is com.dustinmcafee.dongadeuce.network.ConnectionState.Connected

    // Find current player's ready status
    val currentPlayerId = (uiState.connectionState as? com.dustinmcafee.dongadeuce.network.ConnectionState.Connected)?.playerId
    val currentPlayer = lobbyState?.players?.find { it.id == currentPlayerId }
    val isReady = currentPlayer?.isReady ?: false

    Box(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Join Game", style = MaterialTheme.typography.headlineMedium)

            Spacer(modifier = Modifier.height(16.dp))

            if (!isConnected) {
                // Connection form
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
                    placeholder = { Text("8080") },
                    modifier = Modifier.width(300.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Button(
                        onClick = { viewModel.connectToGame() },
                        enabled = !uiState.isLoading
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
                Card(modifier = Modifier.width(400.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Connected to lobby", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))

                        Text("Players (${lobbyState?.players?.size ?: 0}/${lobbyState?.maxPlayers ?: 4}):", style = MaterialTheme.typography.labelLarge)

                        lobbyState?.players?.forEach { player ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    if (player.isHost) "👑 ${player.name}" else "• ${player.name}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                if (player.id == currentPlayerId) {
                                    Text(" (You)", style = MaterialTheme.typography.bodySmall)
                                }
                                if (player.isReady && !player.isHost) {
                                    Text(" ✓ Ready", style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Button(
                        onClick = { viewModel.setReady(!isReady) }
                    ) {
                        Text(if (isReady) "Not Ready" else "Ready!")
                    }

                    OutlinedButton(onClick = { viewModel.returnToMenu() }) {
                        Text("Leave")
                    }
                }

                Text(
                    if (isReady) "Waiting for host to start the game..." else "Click 'Ready!' when you're ready to play",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
