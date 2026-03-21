package com.dustinmcafee.dongadeuce

import android.Manifest
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
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
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dustinmcafee.dongadeuce.network.ConnectionState
import com.dustinmcafee.dongadeuce.ui.theme.DongAdeuceTheme
import com.dustinmcafee.dongadeuce.viewmodel.AndroidMenuViewModel
import com.dustinmcafee.dongadeuce.viewmodel.AndroidScreen
import com.dustinmcafee.dongadeuce.viewmodel.MenuUiState
import com.dustinmcafee.dongadeuce.viewmodel.Screen
import com.dustinmcafee.dongadeuce.viewmodel.ServerMode
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
        AndroidScreen.DedicatedServer -> DedicatedServerScreen(viewModel = viewModel)
    }

    // TOFU certificate verification dialog
    if (uiState.tofuPrompt != null) {
        val prompt = uiState.tofuPrompt!!
        AlertDialog(
            onDismissRequest = { viewModel.rejectTofu() },
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
                TextButton(onClick = { viewModel.acceptTofu() }) {
                    Text("Trust")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.rejectTofu() }) {
                    Text("Reject")
                }
            }
        )
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
            tlsEnabled = uiState.tlsEnabled,
            onPlayerNameChange = { viewModel.setPlayerName(it) },
            onServerAddressChange = { viewModel.setServerAddress(it) },
            onServerPortChange = { viewModel.setServerPort(it) },
            onTlsEnabledChange = { viewModel.setTlsEnabled(it) },
            onDismiss = { showSettingsDialog = false }
        )
    }

    // Commander Selection Dialog (shown when loading deck without explicit commander)
    if (uiState.pendingDeckData != null && uiState.commanderCandidates.isNotEmpty()) {
        com.dustinmcafee.dongadeuce.ui.CommanderSelectionDialog(
            deckData = uiState.pendingDeckData!!,
            candidates = uiState.commanderCandidates,
            playerIndex = uiState.pendingDeckPlayerIndex,
            onCommanderSelected = { commanderName, partnerName ->
                viewModel.selectCommander(commanderName, partnerName)
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
                            selected = uiState.hotseatMode,
                            onClick = { viewModel.setHotseatMode(true) },
                            label = { Text("Hotseat") }
                        )
                        FilterChip(
                            selected = !uiState.hotseatMode && uiState.serverMode == ServerMode.LAN,
                            onClick = {
                                viewModel.setHotseatMode(false)
                                viewModel.setServerMode(ServerMode.LAN)
                            },
                            label = { Text("P2P") }
                        )
                        FilterChip(
                            selected = !uiState.hotseatMode && uiState.serverMode == ServerMode.DEDICATED,
                            onClick = {
                                viewModel.setHotseatMode(false)
                                viewModel.setServerMode(ServerMode.DEDICATED)
                            },
                            label = { Text("Dedicated") }
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
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(2, 3, 4, 5, 6).forEach { count ->
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

                if (uiState.serverMode == ServerMode.LAN) {
                    // P2P panel
                    Button(
                        onClick = { viewModel.startHosting() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isLoading && playerName.isNotBlank()
                    ) {
                        Text("Host Game")
                    }

                    Button(
                        onClick = { viewModel.navigateToJoin() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isLoading && playerName.isNotBlank()
                    ) {
                        Text("Join Game")
                    }
                } else {
                    // Dedicated panel
                    Button(
                        onClick = { viewModel.navigateToDedicatedCreate() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isLoading && playerName.isNotBlank()
                    ) {
                        Text("Create Game")
                    }

                    Button(
                        onClick = { viewModel.navigateToJoin() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isLoading && playerName.isNotBlank()
                    ) {
                        Text("Join Game")
                    }

                    OutlinedButton(
                        onClick = { viewModel.navigateToDedicatedServer() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Host Dedicated Server")
                    }
                }

                // Dev Test button — debug builds only
                if (BuildConfig.DEBUG) {
                    var devTestLoading by remember { mutableStateOf(false) }
                    Button(
                        onClick = {
                            if (devTestLoading) return@Button
                            devTestLoading = true
                            // Load Zedruu deck from assets for both players, start hotseat
                            try {
                                val deckContent = context.assets.open("Zedruu.cod").bufferedReader().readText()
                                viewModel.setHotseatMode(true)
                                MainScope().launch {
                                    // Load Player 1's deck
                                    viewModel.loadHotseatDeckFromContent(0, deckContent)
                                    while (viewModel.uiState.value.isLoading ||
                                        (!viewModel.uiState.value.hotseatDecks.containsKey(0) &&
                                         viewModel.uiState.value.pendingDeckData == null)) {
                                        delay(100)
                                    }
                                    if (viewModel.uiState.value.pendingDeckData != null) {
                                        viewModel.selectCommander("Zedruu the Greathearted")
                                        while (viewModel.uiState.value.isLoading ||
                                            viewModel.uiState.value.pendingDeckData != null ||
                                            !viewModel.uiState.value.hotseatDecks.containsKey(0)) {
                                            delay(100)
                                        }
                                    }

                                    // Reuse Player 1's loaded deck for Player 2 (same cards, already fetched)
                                    val p1Deck = viewModel.uiState.value.hotseatDecks[0]
                                    if (p1Deck != null) {
                                        viewModel.setHotseatDeckDirectly(1, p1Deck)
                                    } else {
                                        // Fallback: load again
                                        viewModel.loadHotseatDeckFromContent(1, deckContent)
                                        while (viewModel.uiState.value.isLoading ||
                                            (!viewModel.uiState.value.hotseatDecks.containsKey(1) &&
                                             viewModel.uiState.value.pendingDeckData == null)) {
                                            delay(100)
                                        }
                                        if (viewModel.uiState.value.pendingDeckData != null) {
                                            viewModel.selectCommander("Zedruu the Greathearted")
                                            while (viewModel.uiState.value.isLoading ||
                                                !viewModel.uiState.value.hotseatDecks.containsKey(1)) {
                                                delay(100)
                                            }
                                        }
                                    }

                                    viewModel.startHotseatGame()
                                    devTestLoading = false
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("DevTest", "Failed to load dev test game", e)
                                devTestLoading = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !devTestLoading && !uiState.isLoading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF6200EA)
                        )
                    ) {
                        if (devTestLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text("Dev Test — 2P Hotseat (Zedruu)")
                    }
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
    tlsEnabled: Boolean,
    onPlayerNameChange: (String) -> Unit,
    onServerAddressChange: (String) -> Unit,
    onServerPortChange: (Int) -> Unit,
    onTlsEnabledChange: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(playerName) }
    var address by remember { mutableStateOf(serverAddress) }
    var port by remember { mutableStateOf(serverPort.toString()) }
    var tls by remember { mutableStateOf(tlsEnabled) }

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

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = tls,
                        onCheckedChange = {
                            tls = it
                            onTlsEnabledChange(it)
                        }
                    )
                    Text("Encrypt connections (TLS)", style = MaterialTheme.typography.bodyMedium)
                }

                var trustedCount by remember {
                    mutableStateOf(com.dustinmcafee.dongadeuce.tls.TrustedServersStore().load().servers.size)
                }
                OutlinedButton(
                    onClick = {
                        val store = com.dustinmcafee.dongadeuce.tls.TrustedServersStore()
                        val servers = store.load().servers
                        servers.forEach { store.removeServer(it.host, it.port) }
                        trustedCount = 0
                    },
                    enabled = trustedCount > 0
                ) {
                    Text("Clear Trusted Servers ($trustedCount)")
                }
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
    val context = LocalContext.current
    val lobbyState = uiState.lobbyState

    // File picker for lobby deck loading
    val lobbyDeckPicker = rememberLauncherForActivityResult(
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
            } catch (_: Exception) {}
        }
    }

    // Commander Selection Dialog
    if (uiState.pendingDeckData != null && uiState.commanderCandidates.isNotEmpty()) {
        com.dustinmcafee.dongadeuce.ui.CommanderSelectionDialog(
            deckData = uiState.pendingDeckData!!,
            candidates = uiState.commanderCandidates,
            playerIndex = uiState.pendingDeckPlayerIndex,
            onCommanderSelected = { commanderName, partnerName ->
                viewModel.selectCommander(commanderName, partnerName)
            },
            onDismiss = { viewModel.cancelCommanderSelection() }
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
        Text("Hosting Game", style = MaterialTheme.typography.headlineMedium)

        // Show game code if available (dedicated server mode)
        if (uiState.gameCode != null) {
            Text(
                "Game Code: ${uiState.gameCode}",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Deck status card
        Card(
            modifier = Modifier.fillMaxWidth(),
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
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = { lobbyDeckPicker.launch("*/*") },
                        modifier = Modifier.weight(1f),
                        enabled = !uiState.isLoading
                    ) {
                        Text(if (uiState.loadedDeck != null) "Change Deck" else "Load Deck")
                    }
                    OutlinedButton(
                        onClick = {
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
        }

        // Loading indicator
        if (uiState.isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Text(uiState.loadingProgress, style = MaterialTheme.typography.bodySmall)
        }

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
                                if (player.isAdmin || player.isHost) "${player.name} (Host)" else player.name,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            if (!player.hasDeck) {
                                Text(
                                    " (no deck)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                            if (player.isReady && !player.isAdmin && !player.isHost) {
                                Text(
                                    " Ready",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        if (!player.isAdmin && !player.isHost) {
                            TextButton(onClick = { viewModel.kickPlayer(player.id) }) {
                                Text("Kick", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        val allReady = lobbyState?.players?.filter { !it.isAdmin && !it.isHost }?.all { it.isReady } ?: false
        val allHaveDecks = lobbyState?.players?.all { it.hasDeck } ?: false
        val enoughPlayers = (lobbyState?.players?.size ?: 0) >= 2

        Button(
            onClick = { viewModel.startNetworkGame() },
            modifier = Modifier.fillMaxWidth(),
            enabled = enoughPlayers && allReady && allHaveDecks
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JoinLobbyScreen(
    viewModel: AndroidMenuViewModel,
    uiState: MenuUiState
) {
    val context = LocalContext.current
    var serverAddress by remember { mutableStateOf(uiState.serverAddress) }
    var serverPort by remember { mutableStateOf(uiState.serverPort.toString()) }
    var gameCode by remember { mutableStateOf(uiState.gameCode ?: "") }
    val isDedicated = uiState.serverMode == ServerMode.DEDICATED

    val isConnected = uiState.connectionState is ConnectionState.Connected
    val currentPlayerId = (uiState.connectionState as? ConnectionState.Connected)?.playerId
    val currentPlayer = uiState.lobbyState?.players?.find { it.id == currentPlayerId }
    val isReady = currentPlayer?.isReady ?: false

    // File picker for lobby deck loading
    val lobbyDeckPicker = rememberLauncherForActivityResult(
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
            } catch (_: Exception) {}
        }
    }

    // Sync game code from viewmodel (e.g. after Create Game)
    LaunchedEffect(uiState.gameCode) {
        if (uiState.gameCode != null && uiState.gameCode != gameCode) {
            gameCode = uiState.gameCode!!
        }
    }

    // Commander Selection Dialog
    if (uiState.pendingDeckData != null && uiState.commanderCandidates.isNotEmpty()) {
        com.dustinmcafee.dongadeuce.ui.CommanderSelectionDialog(
            deckData = uiState.pendingDeckData!!,
            candidates = uiState.commanderCandidates,
            playerIndex = uiState.pendingDeckPlayerIndex,
            onCommanderSelected = { commanderName, partnerName ->
                viewModel.selectCommander(commanderName, partnerName)
            },
            onDismiss = { viewModel.cancelCommanderSelection() }
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
        Text(
            if (uiState.dedicatedCreateMode) "Create Game" else "Join Game",
            style = MaterialTheme.typography.headlineMedium
        )

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
                placeholder = { Text(if (isDedicated) "9090" else "8080") },
                modifier = Modifier.fillMaxWidth()
            )

            // Game code field (dedicated join only — not shown in create mode)
            if (isDedicated && !uiState.dedicatedCreateMode) {
                OutlinedTextField(
                    value = gameCode,
                    onValueChange = {
                        gameCode = it.uppercase()
                        viewModel.setGameCode(it.uppercase().ifBlank { null })
                    },
                    label = { Text("Game Code") },
                    placeholder = { Text("e.g. ABC123") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    if (uiState.dedicatedCreateMode) {
                        viewModel.hostDedicatedGame()
                    } else {
                        viewModel.connectToGame()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading && (!isDedicated || uiState.dedicatedCreateMode || gameCode.isNotBlank())
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(if (uiState.dedicatedCreateMode) "Create & Join" else "Connect")
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

            // Show game code if available
            if (uiState.gameCode != null) {
                Text(
                    "Game Code: ${uiState.gameCode}",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Deck status + load/paste buttons
            Card(
                modifier = Modifier.fillMaxWidth(),
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
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(
                            onClick = { lobbyDeckPicker.launch("*/*") },
                            modifier = Modifier.weight(1f),
                            enabled = !uiState.isLoading
                        ) {
                            Text(if (uiState.loadedDeck != null) "Change Deck" else "Load Deck")
                        }
                        OutlinedButton(
                            onClick = {
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
            }

            // Loading indicator
            if (uiState.isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text(uiState.loadingProgress, style = MaterialTheme.typography.bodySmall)
            }

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
                                if (player.isAdmin || player.isHost) "${player.name} (Host)" else player.name,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            if (player.id == currentPlayerId) {
                                Text(" (You)", style = MaterialTheme.typography.bodySmall)
                            }
                            if (!player.hasDeck) {
                                Text(
                                    " (no deck)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                            if (player.isReady && !player.isAdmin && !player.isHost) {
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

            // Check if current player is admin (first player / game creator)
            val isAdmin = uiState.lobbyState?.players?.find { it.id == currentPlayerId }?.isAdmin ?: false
            val allNonAdminReady = uiState.lobbyState?.players?.filter { !it.isAdmin }?.all { it.isReady } ?: false
            val allHaveDecks = uiState.lobbyState?.players?.all { it.hasDeck } ?: false
            val enoughPlayers = (uiState.lobbyState?.players?.size ?: 0) >= 2

            if (isAdmin && isDedicated) {
                Button(
                    onClick = { viewModel.startNetworkGame() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = enoughPlayers && allNonAdminReady && allHaveDecks
                ) {
                    Text("Start Game")
                }
            } else {
                Button(
                    onClick = { viewModel.setReady(!isReady) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = uiState.loadedDeck != null || isReady
                ) {
                    Text(if (isReady) "Not Ready" else "Ready!")
                }
            }

            OutlinedButton(
                onClick = { viewModel.returnToMenu() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Leave")
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

@Composable
fun DedicatedServerScreen(viewModel: AndroidMenuViewModel) {
    val context = LocalContext.current
    val isRunning by viewModel.dedicatedServerRunning.collectAsState()
    val serverPort by viewModel.dedicatedServerPort.collectAsState()
    val gameCount by viewModel.dedicatedServerGameCount.collectAsState()
    val ipAddress by viewModel.dedicatedServerIpAddress.collectAsState()

    val fingerprint by viewModel.dedicatedServerFingerprint.collectAsState()

    var port by remember { mutableStateOf("9090") }
    var maxGames by remember { mutableStateOf("100") }
    var maxPlayers by remember { mutableStateOf("6") }
    var tlsEnabled by remember { mutableStateOf(false) }

    // Notification permission launcher (API 33+)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.startDedicatedServer(
                context,
                port.toIntOrNull() ?: 9090,
                maxGames.toIntOrNull() ?: 100,
                maxPlayers.toIntOrNull() ?: 6,
                tlsEnabled
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Dedicated Server", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(8.dp))

        // Status card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (isRunning)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    if (isRunning) "Server Running" else "Server Stopped",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                if (isRunning) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("IP: $ipAddress", style = MaterialTheme.typography.bodyMedium)
                    Text("Port: $serverPort", style = MaterialTheme.typography.bodyMedium)
                    Text("Active Games: $gameCount", style = MaterialTheme.typography.bodyMedium)
                    if (fingerprint != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("TLS Fingerprint:", style = MaterialTheme.typography.labelSmall)
                        Text(
                            fingerprint ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    val scheme = if (fingerprint != null) "wss" else "ws"
                    Text(
                        "Connect via: $scheme://$ipAddress:$serverPort/game/{code}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Configuration (only when not running)
        if (!isRunning) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Configuration", style = MaterialTheme.typography.labelLarge)

                    OutlinedTextField(
                        value = port,
                        onValueChange = { port = it.filter { c -> c.isDigit() } },
                        label = { Text("Port") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = maxGames,
                        onValueChange = { maxGames = it.filter { c -> c.isDigit() } },
                        label = { Text("Max Games") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = maxPlayers,
                        onValueChange = { maxPlayers = it.filter { c -> c.isDigit() } },
                        label = { Text("Max Players Per Game") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = tlsEnabled,
                            onCheckedChange = { tlsEnabled = it }
                        )
                        Text("Enable TLS encryption", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Start/Stop button
        Button(
            onClick = {
                if (isRunning) {
                    viewModel.stopDedicatedServer(context)
                } else {
                    // Check notification permission on API 33+
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED
                    ) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        viewModel.startDedicatedServer(
                            context,
                            port.toIntOrNull() ?: 9090,
                            maxGames.toIntOrNull() ?: 100,
                            maxPlayers.toIntOrNull() ?: 6,
                            tlsEnabled
                        )
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = if (isRunning)
                ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            else
                ButtonDefaults.buttonColors()
        ) {
            Text(if (isRunning) "Stop Server" else "Start Server")
        }

        OutlinedButton(
            onClick = { viewModel.returnToMenu() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back to Menu")
        }
    }
}

@Composable
fun GameScreen(
    viewModel: AndroidMenuViewModel,
    uiState: MenuUiState
) {
    val context = LocalContext.current

    // Start game session foreground service to keep alive when backgrounded
    DisposableEffect(Unit) {
        val mode = if (uiState.hotseatMode) "Hotseat" else "Network"
        val server = if (!uiState.hotseatMode) "${uiState.serverAddress}:${uiState.serverPort}" else ""
        com.dustinmcafee.dongadeuce.service.GameSessionService.start(context, mode, server)
        onDispose {
            com.dustinmcafee.dongadeuce.service.GameSessionService.stop(context)
        }
    }

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
