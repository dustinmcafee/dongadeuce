package com.dustinmcafee.dongadeuce.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dustinmcafee.dongadeuce.viewmodel.GameViewModel
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.ExperimentalMaterial3Api

/**
 * Dialog for creating tokens
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TokenCreationDialog(
    viewModel: GameViewModel,
    onDismiss: () -> Unit,
    onCreateToken: (tokenName: String, tokenType: String, power: String?, toughness: String?, color: String, imageUri: String?, quantity: Int) -> Unit
) {
    var tokenName by remember { mutableStateOf("") }
    var tokenType by remember { mutableStateOf("Creature Token") }
    var power by remember { mutableStateOf("") }
    var toughness by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf("Colorless") }
    var tokenImageUri by remember { mutableStateOf<String?>(null) }
    var quantity by remember { mutableStateOf("1") }
    var searchQuery by remember { mutableStateOf("") }

    val colors = listOf("Colorless", "White", "Blue", "Black", "Red", "Green", "Multicolor")
    val uiState by viewModel.uiState.collectAsState()
    val searchResults = uiState.tokenSearchResults
    val isSearching = uiState.isSearchingTokens

    // Clear search on dismiss
    DisposableEffect(Unit) {
        onDispose {
            viewModel.clearTokenSearch()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Token") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(600.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Search field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        viewModel.searchTokens(it)
                    },
                    label = { Text("Search Scryfall Tokens") },
                    placeholder = { Text("e.g., Goblin, Soldier, Treasure") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    trailingIcon = {
                        if (isSearching) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        }
                    }
                )

                // Search results
                if (searchResults.isNotEmpty()) {
                    Text("Search Results (tap to use):", style = MaterialTheme.typography.labelMedium)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                                .padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            searchResults.forEach { card ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            // Auto-fill form with selected token
                                            tokenName = card.name
                                            tokenType = card.type ?: "Creature Token"
                                            power = card.power ?: ""
                                            toughness = card.toughness ?: ""
                                            tokenImageUri = card.imageUri
                                            selectedColor = when {
                                                card.colors.isEmpty() -> "Colorless"
                                                card.colors.size > 1 -> "Multicolor"
                                                card.colors.contains("W") -> "White"
                                                card.colors.contains("U") -> "Blue"
                                                card.colors.contains("B") -> "Black"
                                                card.colors.contains("R") -> "Red"
                                                card.colors.contains("G") -> "Green"
                                                else -> "Colorless"
                                            }
                                            searchQuery = ""
                                            viewModel.clearTokenSearch()
                                        },
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(8.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(card.name, style = MaterialTheme.typography.bodyMedium)
                                            Text(
                                                "${card.type ?: "Token"} ${if (card.power != null && card.toughness != null) "${card.power}/${card.toughness}" else ""}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Divider()

                Text("Or create custom token:", style = MaterialTheme.typography.labelMedium)

                // Token Name
                OutlinedTextField(
                    value = tokenName,
                    onValueChange = { tokenName = it },
                    label = { Text("Token Name") },
                    placeholder = { Text("e.g., Goblin, Soldier, Treasure") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Token Type
                OutlinedTextField(
                    value = tokenType,
                    onValueChange = { tokenType = it },
                    label = { Text("Type") },
                    placeholder = { Text("e.g., Creature — Goblin, Artifact") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Power/Toughness Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = power,
                        onValueChange = { power = it },
                        label = { Text("Power") },
                        placeholder = { Text("1") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = toughness,
                        onValueChange = { toughness = it },
                        label = { Text("Toughness") },
                        placeholder = { Text("1") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                // Color Dropdown
                Text("Color", style = MaterialTheme.typography.labelMedium)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    colors.forEach { color ->
                        FilterChip(
                            selected = selectedColor == color,
                            onClick = { selectedColor = color },
                            label = { Text(color) }
                        )
                    }
                }

                // Quantity
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { if (it.all { char -> char.isDigit() }) quantity = it },
                    label = { Text("Quantity") },
                    placeholder = { Text("1") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val name = tokenName.trim().ifBlank { "Token" }
                    val type = tokenType.trim().ifBlank { "Token" }
                    val pow = power.trim().ifBlank { null }
                    val tough = toughness.trim().ifBlank { null }
                    val qty = quantity.toIntOrNull()?.coerceAtLeast(1) ?: 1
                    val colorValue = if (selectedColor == "Colorless") "" else selectedColor

                    onCreateToken(name, type, pow, tough, colorValue, tokenImageUri, qty)
                    onDismiss()
                },
                enabled = tokenName.isNotBlank()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
