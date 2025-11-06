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
        Screen.Game -> GameScreen(
            loadedDeck = uiState.loadedDeck,
            hotseatDecks = if (uiState.hotseatMode) uiState.hotseatDecks else emptyMap(),
            playerCount = uiState.playerCount,
            isHotseatMode = uiState.hotseatMode
        )
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
                // Network mode: Single deck + Host/Join buttons
                Button(
                    onClick = { viewModel.startHosting() },
                    modifier = Modifier.width(200.dp),
                    enabled = uiState.loadedDeck != null && !uiState.isLoading
                ) {
                    Text("Host Game")
                }

                Button(
                    onClick = { viewModel.navigateToJoin() },
                    modifier = Modifier.width(200.dp),
                    enabled = uiState.loadedDeck != null && !uiState.isLoading
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
                    Text("Server: localhost:${uiState.serverPort}", style = MaterialTheme.typography.bodyMedium)

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Connected Players:", style = MaterialTheme.typography.labelLarge)
                    if (uiState.connectedPlayers.isEmpty()) {
                        Text("No players yet...", style = MaterialTheme.typography.bodySmall)
                    } else {
                        uiState.connectedPlayers.forEach { playerName ->
                            Text("• $playerName", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(
                    onClick = { viewModel.startGame() },
                    enabled = uiState.connectedPlayers.isNotEmpty()
                ) {
                    Text("Start Game")
                }

                OutlinedButton(onClick = { viewModel.returnToMenu() }) {
                    Text("Cancel")
                }
            }
        }
    }
}

@Composable
fun JoinLobbyScreen(viewModel: MenuViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    var serverAddress by remember { mutableStateOf("localhost") }

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
        }
    }
}
