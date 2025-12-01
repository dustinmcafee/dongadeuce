package com.dustinmcafee.dongadeuce

import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dustinmcafee.dongadeuce.network.ConnectionState
import com.dustinmcafee.dongadeuce.ui.theme.DongAdeuceTheme
import com.dustinmcafee.dongadeuce.viewmodel.AndroidMenuViewModel
import com.dustinmcafee.dongadeuce.viewmodel.AndroidScreen
import com.dustinmcafee.dongadeuce.viewmodel.MenuUiState
import com.dustinmcafee.dongadeuce.viewmodel.Screen
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Main activity for the Commander MTG Android app.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DongAdeuceTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen()
                }
            }
        }
    }
}

@Composable
fun MainScreen(viewModel: AndroidMenuViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val currentScreen by viewModel.currentScreen.collectAsState()

    when (currentScreen) {
        AndroidScreen.Menu -> MenuScreen(viewModel = viewModel, uiState = uiState)
        AndroidScreen.HostLobby -> HostLobbyScreen(viewModel = viewModel, uiState = uiState)
        AndroidScreen.JoinLobby -> JoinLobbyScreen(viewModel = viewModel, uiState = uiState)
        AndroidScreen.Game -> GameScreen(viewModel = viewModel, uiState = uiState)
    }

    // Error dialog
    if (uiState.error != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            title = { Text("Error") },
            text = { Text(uiState.error ?: "") },
            confirmButton = {
                TextButton(onClick = { viewModel.clearError() }) {
                    Text("OK")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuScreen(
    viewModel: AndroidMenuViewModel,
    uiState: MenuUiState
) {
    val context = LocalContext.current
    var showSettingsDialog by remember { mutableStateOf(false) }

    // File picker for deck loading
    val deckFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.openInputStream(it)?.use { inputStream ->
                    InputStreamReader(inputStream).use { reader ->
                        val content = reader.readText()
                        viewModel.loadDeckFromContent(content)
                    }
                }
            } catch (e: Exception) {
                // Error handled by ViewModel
            }
        }
    }

    // File picker for hotseat deck loading
    var currentHotseatPlayer by remember { mutableStateOf(0) }
    val hotseatDeckPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.openInputStream(it)?.use { inputStream ->
                    InputStreamReader(inputStream).use { reader ->
                        val content = reader.readText()
                        viewModel.loadHotseatDeckFromContent(currentHotseatPlayer, content)
                    }
                }
            } catch (e: Exception) {
                // Error handled by ViewModel
            }
        }
    }

    // Settings Dialog
    if (showSettingsDialog) {
        SettingsDialog(
            playerName = uiState.playerName,
            serverAddress = uiState.serverAddress,
            serverPort = uiState.serverPort,
            onPlayerNameChange = { viewModel.setPlayerName(it) },
            onServerAddressChange = { viewModel.setServerAddress(it) },
            onServerPortChange = { viewModel.setServerPort(it) },
            onDismiss = { showSettingsDialog = false }
        )
    }

    // Commander Selection Dialog (shown when loading deck without explicit commander)
    if (uiState.pendingDeckData != null && uiState.commanderCandidates.isNotEmpty()) {
        com.dustinmcafee.dongadeuce.ui.CommanderSelectionDialog(
            deckData = uiState.pendingDeckData!!,
            candidates = uiState.commanderCandidates,
            playerIndex = uiState.pendingDeckPlayerIndex,
            onCommanderSelected = { commanderName ->
                viewModel.selectCommander(commanderName)
            },
            onDismiss = { viewModel.cancelCommanderSelection() }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Settings button
        IconButton(
            onClick = { showSettingsDialog = true },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings"
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Dong-A-Deuce",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Commander MTG",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Loading progress
            if (uiState.isLoading) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(uiState.loadingProgress, style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(8.dp))

                        if (uiState.loadingProgressPercent > 0) {
                            LinearProgressIndicator(
                                progress = uiState.loadingProgressPercent / 100f,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                "${"%.1f".format(uiState.loadingProgressPercent)}%",
                                style = MaterialTheme.typography.labelSmall
                            )
                        } else {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }

            // Loaded deck info
            if (uiState.loadedDeck != null && !uiState.isLoading) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val deck = uiState.loadedDeck
                        Text("Deck Loaded", style = MaterialTheme.typography.labelMedium)
                        Text(deck?.commander?.name ?: "", style = MaterialTheme.typography.titleMedium)
                        Text("${deck?.totalCards ?: 0} cards", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            // Game mode selector
            Card(
                modifier = Modifier.fillMaxWidth(),
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
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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

            // Player count selector
            Card(
                modifier = Modifier.fillMaxWidth(),
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
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(2, 3, 4).forEach { count ->
                            FilterChip(
                                selected = uiState.playerCount == count,
                                onClick = { viewModel.setPlayerCount(count) },
                                label = { Text("$count") }
                            )
                        }
                    }
                }
            }

            // Card cache status
            Card(
                modifier = Modifier.fillMaxWidth(),
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
                            "${uiState.cacheCardCount} cards cached",
                            style = MaterialTheme.typography.bodySmall
                        )
                    } else {
                        Text(
                            "No cache available",
                            style = MaterialTheme.typography.bodySmall
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

            Spacer(modifier = Modifier.height(8.dp))

            // Mode-specific UI
            if (uiState.hotseatMode) {
                // Hotseat mode: Load decks for each player
                for (playerIndex in 0 until uiState.playerCount) {
                    val deckLoaded = uiState.hotseatDecks.containsKey(playerIndex)
                    val deck = uiState.hotseatDecks[playerIndex]

                    Card(
                        modifier = Modifier.fillMaxWidth(),
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
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Player ${playerIndex + 1}", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    deck?.commander?.name ?: "No deck loaded",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        currentHotseatPlayer = playerIndex
                                        hotseatDeckPicker.launch("*/*")
                                    },
                                    enabled = !uiState.isLoading,
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Text(if (deckLoaded) "Change" else "Load")
                                }

                                OutlinedButton(
                                    onClick = {
                                        // Get clipboard content
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clipData = clipboard.primaryClip
                                        if (clipData != null && clipData.itemCount > 0) {
                                            val text = clipData.getItemAt(0).text?.toString()
                                            if (text != null && text.isNotBlank()) {
                                                viewModel.loadHotseatDeckFromContent(playerIndex, text)
                                            }
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

                Button(
                    onClick = { viewModel.startHotseatGame() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = uiState.hotseatDecks.size == uiState.playerCount && !uiState.isLoading
                ) {
                    Text("Start Hotseat Game")
                }
            } else {
                // Network mode
                var playerName by remember { mutableStateOf(uiState.playerName) }

                OutlinedTextField(
                    value = playerName,
                    onValueChange = {
                        playerName = it
                        viewModel.setPlayerName(it)
                    },
                    label = { Text("Your Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = { viewModel.startHosting() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = uiState.loadedDeck != null && !uiState.isLoading && playerName.isNotBlank()
                ) {
                    Text("Host Game")
                }

                Button(
                    onClick = { viewModel.navigateToJoin() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = uiState.loadedDeck != null && !uiState.isLoading && playerName.isNotBlank()
                ) {
                    Text("Join Game")
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = { deckFilePicker.launch("*/*") },
                        modifier = Modifier.weight(1f),
                        enabled = !uiState.isLoading
                    ) {
                        Text("Load Deck")
                    }

                    OutlinedButton(
                        onClick = {
                            // Get clipboard content
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clipData = clipboard.primaryClip
                            if (clipData != null && clipData.itemCount > 0) {
                                val text = clipData.getItemAt(0).text?.toString()
                                if (text != null && text.isNotBlank()) {
                                    viewModel.loadDeckFromContent(text)
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !uiState.isLoading
                    ) {
                        Text("Paste Deck")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "v${BuildConfig.VERSION_NAME}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SettingsDialog(
    playerName: String,
    serverAddress: String,
    serverPort: Int,
    onPlayerNameChange: (String) -> Unit,
    onServerAddressChange: (String) -> Unit,
    onServerPortChange: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(playerName) }
    var address by remember { mutableStateOf(serverAddress) }
    var port by remember { mutableStateOf(serverPort.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Settings") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        onPlayerNameChange(it)
                    },
                    label = { Text("Player Name") },
                    singleLine = true
                )

                OutlinedTextField(
                    value = address,
                    onValueChange = {
                        address = it
                        onServerAddressChange(it)
                    },
                    label = { Text("Default Server Address") },
                    singleLine = true
                )

                OutlinedTextField(
                    value = port,
                    onValueChange = {
                        port = it.filter { c -> c.isDigit() }
                        port.toIntOrNull()?.let(onServerPortChange)
                    },
                    label = { Text("Default Server Port") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}

@Composable
fun HostLobbyScreen(
    viewModel: AndroidMenuViewModel,
    uiState: MenuUiState
) {
    val lobbyState = uiState.lobbyState

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Hosting Game", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(16.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Waiting for players...", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))

                // Show server URL
                val serverUrl = "localhost:${uiState.serverPort}"
                Text("Server: $serverUrl", style = MaterialTheme.typography.bodyMedium)
                Text("Share this address with other players", style = MaterialTheme.typography.bodySmall)

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    "Connected Players (${lobbyState?.players?.size ?: 0}/${lobbyState?.maxPlayers ?: 4}):",
                    style = MaterialTheme.typography.labelLarge
                )

                lobbyState?.players?.forEach { player ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                if (player.isHost) "${player.name} (Host)" else player.name,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            if (player.isReady && !player.isHost) {
                                Text(
                                    " Ready",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        if (!player.isHost) {
                            TextButton(onClick = { viewModel.kickPlayer(player.id) }) {
                                Text("Kick", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        val allReady = lobbyState?.players?.filter { !it.isHost }?.all { it.isReady } ?: false
        val enoughPlayers = (lobbyState?.players?.size ?: 0) >= 2

        Button(
            onClick = { viewModel.startNetworkGame() },
            modifier = Modifier.fillMaxWidth(),
            enabled = enoughPlayers && allReady
        ) {
            Text("Start Game")
        }

        OutlinedButton(
            onClick = { viewModel.returnToMenu() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Cancel")
        }

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

@Composable
fun JoinLobbyScreen(
    viewModel: AndroidMenuViewModel,
    uiState: MenuUiState
) {
    var serverAddress by remember { mutableStateOf(uiState.serverAddress) }
    var serverPort by remember { mutableStateOf(uiState.serverPort.toString()) }

    val isConnected = uiState.connectionState is ConnectionState.Connected
    val currentPlayerId = (uiState.connectionState as? ConnectionState.Connected)?.playerId
    val currentPlayer = uiState.lobbyState?.players?.find { it.id == currentPlayerId }
    val isReady = currentPlayer?.isReady ?: false

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
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
                placeholder = { Text("IP address or hostname") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = serverPort,
                onValueChange = {
                    serverPort = it.filter { c -> c.isDigit() }
                    serverPort.toIntOrNull()?.let { viewModel.setServerPort(it) }
                },
                label = { Text("Port") },
                placeholder = { Text("8080") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { viewModel.connectToGame() },
                modifier = Modifier.fillMaxWidth(),
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

            OutlinedButton(
                onClick = { viewModel.returnToMenu() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cancel")
            }
        } else {
            // Connected - show lobby
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Connected to lobby", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        "Players (${uiState.lobbyState?.players?.size ?: 0}/${uiState.lobbyState?.maxPlayers ?: 4}):",
                        style = MaterialTheme.typography.labelLarge
                    )

                    uiState.lobbyState?.players?.forEach { player ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                if (player.isHost) "${player.name} (Host)" else player.name,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            if (player.id == currentPlayerId) {
                                Text(" (You)", style = MaterialTheme.typography.bodySmall)
                            }
                            if (player.isReady && !player.isHost) {
                                Text(
                                    " Ready",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { viewModel.setReady(!isReady) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isReady) "Not Ready" else "Ready!")
            }

            OutlinedButton(
                onClick = { viewModel.returnToMenu() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Leave")
            }

            Text(
                if (isReady) "Waiting for host to start the game..." else "Click 'Ready!' when you're ready to play",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun GameScreen(
    viewModel: AndroidMenuViewModel,
    uiState: MenuUiState
) {
    // Create GameViewModel with network client/server if in network mode
    val networkClient = viewModel.getGameClient()
    val networkServer = viewModel.getGameServer()
    val localPlayerId = viewModel.getLocalPlayerId()
    val gameViewModel = remember(networkClient, networkServer, localPlayerId) {
        com.dustinmcafee.dongadeuce.viewmodel.GameViewModel(
            networkClient = networkClient,
            networkServer = networkServer,
            localPlayerId = localPlayerId
        )
    }

    // Use the full-featured Android game screen
    com.dustinmcafee.dongadeuce.ui.AndroidGameScreen(
        menuViewModel = viewModel,
        uiState = uiState,
        gameViewModel = gameViewModel
    )
}
