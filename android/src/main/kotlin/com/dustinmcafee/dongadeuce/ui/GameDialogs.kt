@file:OptIn(ExperimentalMaterial3Api::class)

package com.dustinmcafee.dongadeuce.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dustinmcafee.dongadeuce.models.CardInstance
import com.dustinmcafee.dongadeuce.models.GameEvent
import com.dustinmcafee.dongadeuce.models.Player
import com.dustinmcafee.dongadeuce.models.Zone
import com.dustinmcafee.dongadeuce.models.toDisplayString
import com.dustinmcafee.dongadeuce.viewmodel.GameViewModel
import kotlin.random.Random

/**
 * Dialog for viewing card details
 */
@Composable
fun CardDetailsDialog(
    cardInstance: CardInstance,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(cardInstance.card.name) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                // Card image
                CardImage(
                    imageUrl = cardInstance.card.imageUri,
                    contentDescription = cardInstance.card.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Card info
                cardInstance.card.type?.let {
                    Text("Type: $it", style = MaterialTheme.typography.bodyMedium)
                }
                cardInstance.card.manaCost?.let {
                    Text("Mana Cost: $it", style = MaterialTheme.typography.bodyMedium)
                }
                if (cardInstance.card.power != null && cardInstance.card.toughness != null) {
                    val currentP = (cardInstance.card.power?.toIntOrNull() ?: 0) + cardInstance.powerModifier
                    val currentT = (cardInstance.card.toughness?.toIntOrNull() ?: 0) + cardInstance.toughnessModifier
                    Text("P/T: $currentP/$currentT", style = MaterialTheme.typography.bodyMedium)
                }
                if (cardInstance.counters.isNotEmpty()) {
                    Text("Counters: ${cardInstance.counters.entries.joinToString { "${it.key}: ${it.value}" }}",
                        style = MaterialTheme.typography.bodyMedium)
                }
                if (!cardInstance.annotation.isNullOrBlank()) {
                    Text("Annotation: ${cardInstance.annotation}", style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

/**
 * Dialog for setting life total
 */
@Composable
fun SetLifeDialog(
    playerName: String,
    currentLife: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var lifeValue by remember { mutableStateOf(currentLife.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Life - $playerName") },
        text = {
            Column {
                OutlinedTextField(
                    value = lifeValue,
                    onValueChange = { lifeValue = it.filter { c -> c.isDigit() || c == '-' } },
                    label = { Text("Life Total") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Quick adjustment buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    listOf(-10, -5, -1, 1, 5, 10).forEach { delta ->
                        FilledTonalButton(
                            onClick = {
                                val newValue = (lifeValue.toIntOrNull() ?: currentLife) + delta
                                lifeValue = newValue.toString()
                            },
                            modifier = Modifier.weight(1f).padding(horizontal = 2.dp)
                        ) {
                            Text(if (delta > 0) "+$delta" else delta.toString())
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                lifeValue.toIntOrNull()?.let { onConfirm(it) }
                onDismiss()
            }) {
                Text("Set")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Dialog for managing card counters
 */
@Composable
fun CounterDialog(
    cardName: String,
    counterType: String,
    currentCount: Int,
    onDismiss: () -> Unit,
    onSet: (Int) -> Unit,
    onAdd: (Int) -> Unit,
    onSubtract: (Int) -> Unit
) {
    var inputValue by remember { mutableStateOf(currentCount.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("$counterType Counters") },
        text = {
            Column {
                Text("$cardName", style = MaterialTheme.typography.bodyMedium)

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { onSubtract(1); onDismiss() }) {
                        Icon(Icons.Default.KeyboardArrowDown, "Remove")
                    }

                    OutlinedTextField(
                        value = inputValue,
                        onValueChange = { inputValue = it.filter { c -> c.isDigit() } },
                        modifier = Modifier.width(100.dp),
                        textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
                        singleLine = true
                    )

                    IconButton(onClick = { onAdd(1); onDismiss() }) {
                        Icon(Icons.Default.Add, "Add")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    listOf(-5, -1, 1, 5).forEach { delta ->
                        OutlinedButton(
                            onClick = {
                                if (delta > 0) onAdd(delta) else onSubtract(-delta)
                                onDismiss()
                            }
                        ) {
                            Text(if (delta > 0) "+$delta" else delta.toString())
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                inputValue.toIntOrNull()?.let { onSet(it) }
                onDismiss()
            }) {
                Text("Set")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Dialog for modifying power/toughness
 */
@Composable
fun PowerToughnessDialog(
    cardName: String,
    basePower: String?,
    baseToughness: String?,
    currentPowerMod: Int,
    currentToughnessMod: Int,
    onDismiss: () -> Unit,
    onModifyPower: (Int) -> Unit,
    onModifyToughness: (Int) -> Unit,
    onModifyBoth: (Int) -> Unit,
    onSetPT: (Int?, Int?) -> Unit,
    onReset: () -> Unit,
    onFlowP: () -> Unit,
    onFlowT: () -> Unit
) {
    val baseP = basePower?.toIntOrNull() ?: 0
    val baseT = baseToughness?.toIntOrNull() ?: 0
    val currentP = baseP + currentPowerMod
    val currentT = baseT + currentToughnessMod

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Power/Toughness - $cardName") },
        text = {
            Column {
                Text(
                    "Current: $currentP/$currentT (Base: $basePower/$baseToughness)",
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Power row
                Text("Power", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    listOf(-1, 1).forEach { delta ->
                        OutlinedButton(onClick = { onModifyPower(delta); onDismiss() }) {
                            Text(if (delta > 0) "+$delta/+0" else "$delta/+0")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Toughness row
                Text("Toughness", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    listOf(-1, 1).forEach { delta ->
                        OutlinedButton(onClick = { onModifyToughness(delta); onDismiss() }) {
                            Text(if (delta > 0) "+0/+$delta" else "+0/$delta")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Both row
                Text("Both", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    listOf(-1, 1).forEach { delta ->
                        OutlinedButton(onClick = { onModifyBoth(delta); onDismiss() }) {
                            Text(if (delta > 0) "+$delta/+$delta" else "$delta/$delta")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Flow buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    OutlinedButton(onClick = { onFlowP(); onDismiss() }) {
                        Text("Flow P (+1/-1)")
                    }
                    OutlinedButton(onClick = { onFlowT(); onDismiss() }) {
                        Text("Flow T (-1/+1)")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { onReset(); onDismiss() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Reset to Base")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

/**
 * Dialog for setting card annotation
 */
@Composable
fun AnnotationDialog(
    cardName: String,
    currentAnnotation: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var annotation by remember { mutableStateOf(currentAnnotation) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Annotation - $cardName") },
        text = {
            OutlinedTextField(
                value = annotation,
                onValueChange = { annotation = it },
                label = { Text("Annotation") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3
            )
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(annotation)
                onDismiss()
            }) {
                Text("Set")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Dialog for rolling dice
 */
@Composable
fun DieRollerDialog(
    playerName: String,
    onDismiss: () -> Unit,
    onRollLogged: (String, Int, Int) -> Unit
) {
    var lastRoll by remember { mutableStateOf<Pair<String, Int>?>(null) }
    var numberOfDice by remember { mutableStateOf(1) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Die Roller - $playerName") },
        text = {
            Column {
                // Number of dice
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { if (numberOfDice > 1) numberOfDice-- }) {
                        Icon(Icons.Default.KeyboardArrowDown, "Less")
                    }
                    Text("$numberOfDice dice", style = MaterialTheme.typography.titleMedium)
                    IconButton(onClick = { numberOfDice++ }) {
                        Icon(Icons.Default.Add, "More")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Die type buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    listOf(4, 6, 8, 10, 12, 20).forEach { sides ->
                        OutlinedButton(
                            onClick = {
                                val result = (1..numberOfDice).sumOf { Random.nextInt(1, sides + 1) }
                                lastRoll = "d$sides" to result
                                onRollLogged("d$sides", result, numberOfDice)
                            },
                            modifier = Modifier.padding(2.dp)
                        ) {
                            Text("d$sides")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Coin flip
                Button(
                    onClick = {
                        val result = if (Random.nextBoolean()) 1 else 0
                        lastRoll = "Coin" to result
                        onRollLogged("Coin", result, 1)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Flip Coin")
                }

                // Last roll result
                lastRoll?.let { (type, result) ->
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Text(
                            text = if (type == "Coin") {
                                if (result == 1) "Heads!" else "Tails!"
                            } else {
                                "Rolled $type: $result"
                            },
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.headlineMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

/**
 * Dialog for player counters (poison, energy, experience, etc.)
 */
@Composable
fun PlayerCountersDialog(
    player: Player,
    onDismiss: () -> Unit,
    onAddCounter: (String, Int) -> Unit,
    onRemoveCounter: (String, Int) -> Unit,
    onSetCounter: (String, Int) -> Unit
) {
    val counterTypes = listOf(
        "poison" to "Poison",
        "energy" to "Energy",
        "experience" to "Experience",
        "rad" to "Rad",
        "ticket" to "Ticket"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Player Counters - ${player.name}") },
        text = {
            Column {
                counterTypes.forEach { (type, displayName) ->
                    val count = player.getCounter(type)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(displayName, modifier = Modifier.weight(1f))

                        IconButton(onClick = { onRemoveCounter(type, 1) }) {
                            Icon(Icons.Default.KeyboardArrowDown, "Remove")
                        }

                        Text(
                            count.toString(),
                            modifier = Modifier.width(40.dp),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.titleMedium
                        )

                        IconButton(onClick = { onAddCounter(type, 1) }) {
                            Icon(Icons.Default.Add, "Add")
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

/**
 * Dialog for creating tokens
 */
@Composable
fun TokenCreationDialog(
    viewModel: GameViewModel,
    onDismiss: () -> Unit,
    onCreateToken: (String, String, String, String, String, String?, Int) -> Unit
) {
    var tokenName by remember { mutableStateOf("") }
    var tokenType by remember { mutableStateOf("Creature") }
    var power by remember { mutableStateOf("1") }
    var toughness by remember { mutableStateOf("1") }
    var color by remember { mutableStateOf("Colorless") }
    var quantity by remember { mutableStateOf("1") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Token") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = tokenName,
                    onValueChange = { tokenName = it },
                    label = { Text("Token Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = tokenType,
                    onValueChange = { tokenType = it },
                    label = { Text("Type") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = power,
                        onValueChange = { power = it.filter { c -> c.isDigit() } },
                        label = { Text("Power") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = toughness,
                        onValueChange = { toughness = it.filter { c -> c.isDigit() } },
                        label = { Text("Toughness") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Color selector
                Text("Color", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    listOf("White", "Blue", "Black", "Red", "Green", "Colorless").forEach { c ->
                        FilterChip(
                            selected = color == c,
                            onClick = { color = c },
                            label = { Text(c.take(1)) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it.filter { c -> c.isDigit() } },
                    label = { Text("Quantity") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (tokenName.isNotBlank()) {
                    onCreateToken(
                        tokenName,
                        tokenType,
                        power,
                        toughness,
                        color,
                        null,
                        quantity.toIntOrNull() ?: 1
                    )
                }
            }) {
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

/**
 * Dialog for library position selection
 */
@Composable
fun LibraryPositionDialog(
    cardName: String,
    librarySize: Int,
    onDismiss: () -> Unit,
    onToTop: () -> Unit,
    onToBottom: () -> Unit,
    onToPositionFromTop: (Int) -> Unit,
    onToPositionFromBottom: (Int) -> Unit
) {
    var positionFromTop by remember { mutableStateOf("1") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Library Position") },
        text = {
            Column {
                Text("Move \"$cardName\" to library", style = MaterialTheme.typography.bodyMedium)
                Text("Library size: $librarySize cards", style = MaterialTheme.typography.bodySmall)

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(onClick = { onToTop(); onDismiss() }) {
                        Text("Top")
                    }
                    Button(onClick = { onToBottom(); onDismiss() }) {
                        Text("Bottom")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = positionFromTop,
                    onValueChange = { positionFromTop = it.filter { c -> c.isDigit() } },
                    label = { Text("Position from top") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        positionFromTop.toIntOrNull()?.let { pos ->
                            if (pos in 1..librarySize) {
                                onToPositionFromTop(pos - 1) // 0-indexed
                                onDismiss()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Set Position")
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Dialog for viewing graveyard
 */
@Composable
fun GraveyardDialog(
    cards: List<CardInstance>,
    playerName: String,
    onDismiss: () -> Unit,
    onReturnToHand: (CardInstance) -> Unit,
    onReturnToBattlefield: (CardInstance) -> Unit
) {
    ZoneViewerDialog(
        title = "Graveyard - $playerName",
        cards = cards,
        onDismiss = onDismiss,
        onCardAction = { card, action ->
            when (action) {
                "hand" -> onReturnToHand(card)
                "battlefield" -> onReturnToBattlefield(card)
            }
        },
        actionButtons = listOf("hand" to "To Hand", "battlefield" to "To Battlefield")
    )
}

/**
 * Dialog for viewing exile zone
 */
@Composable
fun ExileDialog(
    cards: List<CardInstance>,
    playerName: String,
    onDismiss: () -> Unit,
    onReturnToHand: (CardInstance) -> Unit,
    onReturnToBattlefield: (CardInstance) -> Unit
) {
    ZoneViewerDialog(
        title = "Exile - $playerName",
        cards = cards,
        onDismiss = onDismiss,
        onCardAction = { card, action ->
            when (action) {
                "hand" -> onReturnToHand(card)
                "battlefield" -> onReturnToBattlefield(card)
            }
        },
        actionButtons = listOf("hand" to "To Hand", "battlefield" to "To Battlefield")
    )
}

/**
 * Generic zone viewer dialog
 */
@Composable
private fun ZoneViewerDialog(
    title: String,
    cards: List<CardInstance>,
    onDismiss: () -> Unit,
    onCardAction: (CardInstance, String) -> Unit,
    actionButtons: List<Pair<String, String>>
) {
    var selectedCard by remember { mutableStateOf<CardInstance?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(modifier = Modifier.heightIn(max = 400.dp)) {
                if (cards.isEmpty()) {
                    Text("Empty", style = MaterialTheme.typography.bodyMedium)
                } else {
                    Text("${cards.size} cards", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn {
                        items(cards) { card ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                                    .clickable { selectedCard = card },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (selectedCard == card)
                                        MaterialTheme.colorScheme.primaryContainer
                                    else
                                        MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CardImageThumbnail(
                                        imageUrl = card.card.imageUri,
                                        contentDescription = card.card.name
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(card.card.name, style = MaterialTheme.typography.bodyMedium)
                                        card.card.type?.let {
                                            Text(it, style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Action buttons for selected card
                    selectedCard?.let { card ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            actionButtons.forEach { (action, label) ->
                                OutlinedButton(onClick = {
                                    onCardAction(card, action)
                                    selectedCard = null
                                }) {
                                    Text(label)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

/**
 * Dialog for library search
 */
@Composable
fun LibrarySearchDialog(
    cards: List<CardInstance>,
    playerName: String,
    onDismiss: () -> Unit,
    onToHand: (CardInstance) -> Unit,
    onToBattlefield: (CardInstance) -> Unit,
    onToTop: (CardInstance) -> Unit,
    onToBottom: (CardInstance) -> Unit,
    onShuffle: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCard by remember { mutableStateOf<CardInstance?>(null) }

    val filteredCards = cards.filter {
        searchQuery.isEmpty() ||
        it.card.name.contains(searchQuery, ignoreCase = true) ||
        it.card.type?.contains(searchQuery, ignoreCase = true) == true
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Library - $playerName (${cards.size} cards)") },
        text = {
            Column(modifier = Modifier.heightIn(max = 450.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(filteredCards) { card ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                                .clickable { selectedCard = card },
                            colors = CardDefaults.cardColors(
                                containerColor = if (selectedCard == card)
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CardImageThumbnail(
                                    imageUrl = card.card.imageUri,
                                    contentDescription = card.card.name
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(card.card.name, style = MaterialTheme.typography.bodyMedium)
                                    card.card.type?.let {
                                        Text(it, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }
                }

                selectedCard?.let { card ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            OutlinedButton(onClick = { onToHand(card); selectedCard = null }) {
                                Text("Hand")
                            }
                            OutlinedButton(onClick = { onToBattlefield(card); selectedCard = null }) {
                                Text("Battlefield")
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            OutlinedButton(onClick = { onToTop(card); selectedCard = null }) {
                                Text("Top")
                            }
                            OutlinedButton(onClick = { onToBottom(card); selectedCard = null }) {
                                Text("Bottom")
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(onClick = { onShuffle(); onDismiss() }, modifier = Modifier.fillMaxWidth()) {
                    Text("Shuffle Library")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

/**
 * Game log dialog
 */
@Composable
fun GameLogDialog(
    gameLog: List<GameEvent>,
    players: List<Player>,
    onDismiss: () -> Unit,
    onSendMessage: (String) -> Unit
) {
    var message by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Game Log") },
        text = {
            Column(modifier = Modifier.heightIn(max = 400.dp)) {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(gameLog.reversed().size) { index ->
                        val entry = gameLog.reversed()[index]
                        Text(
                            text = entry.toDisplayString(),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = message,
                        onValueChange = { message = it },
                        label = { Text("Chat") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (message.isNotBlank()) {
                                onSendMessage(message)
                                message = ""
                            }
                        }
                    ) {
                        Text("Send")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
