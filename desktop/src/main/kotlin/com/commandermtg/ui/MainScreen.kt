package com.commandermtg.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.commandermtg.viewmodel.MenuViewModel
import com.commandermtg.viewmodel.Screen
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
            loadedDeckName = uiState.loadedDeck?.commander?.name
        )
        Screen.HostLobby -> HostLobbyScreen(viewModel = menuViewModel)
        Screen.JoinLobby -> JoinLobbyScreen(viewModel = menuViewModel)
        Screen.Game -> GameScreen()
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

@Composable
fun MenuScreen(
    viewModel: MenuViewModel,
    loadedDeckName: String?
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Commander MTG",
                style = MaterialTheme.typography.headlineLarge
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Show loaded deck info
            if (loadedDeckName != null) {
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
                        Text(loadedDeckName, style = MaterialTheme.typography.titleMedium)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { viewModel.startHosting() },
                modifier = Modifier.width(200.dp),
                enabled = loadedDeckName != null
            ) {
                Text("Host Game")
            }

            Button(
                onClick = { viewModel.navigateToJoin() },
                modifier = Modifier.width(200.dp),
                enabled = loadedDeckName != null
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
                modifier = Modifier.width(200.dp)
            ) {
                Text("Load Deck")
            }
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
