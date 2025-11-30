@file:OptIn(ExperimentalMaterial3Api::class)

package com.dustinmcafee.dongadeuce.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
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
import com.dustinmcafee.dongadeuce.models.GameConstants
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
                    "Current: $currentP/$currentT (Base: ${basePower ?: "?"} / ${baseToughness ?: "?"})",
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

                Spacer(modifier = Modifier.height(16.dp))

                // Set specific P/T
                Text("Set Specific P/T", style = MaterialTheme.typography.labelMedium)
                var setP by remember { mutableStateOf(currentP.toString()) }
                var setT by remember { mutableStateOf(currentT.toString()) }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = setP,
                        onValueChange = { if (it.isEmpty() || it == "-" || it.toIntOrNull() != null) setP = it },
                        label = { Text("P") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = setT,
                        onValueChange = { if (it.isEmpty() || it == "-" || it.toIntOrNull() != null) setT = it },
                        label = { Text("T") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
                Button(
                    onClick = {
                        val p = setP.toIntOrNull()
                        val t = setT.toIntOrNull()
                        if (p != null && t != null) {
                            onSetPT(p, t)
                            onDismiss()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = setP.toIntOrNull() != null && setT.toIntOrNull() != null
                ) {
                    Text("Set P/T")
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
    var customSides by remember { mutableStateOf("100") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Die Roller - $playerName") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
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

                // Standard die type buttons
                Text("Standard Dice", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
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

                // Custom die
                Text("Custom Die", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = customSides,
                        onValueChange = { customSides = it.filter { c -> c.isDigit() } },
                        label = { Text("Sides") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    Button(
                        onClick = {
                            val sides = customSides.toIntOrNull() ?: 100
                            if (sides > 0) {
                                val result = (1..numberOfDice).sumOf { Random.nextInt(1, sides + 1) }
                                lastRoll = "d$sides" to result
                                onRollLogged("d$sides", result, numberOfDice)
                            }
                        }
                    ) {
                        Text("Roll d$customSides")
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
    onSetCounter: (String, Int) -> Unit = { _, _ -> }
) {
    val counterTypes = listOf(
        "poison" to "Poison",
        "energy" to "Energy",
        "experience" to "Experience",
        "rad" to "Rad",
        "ticket" to "Ticket"
    )

    var editingCounter by remember { mutableStateOf<String?>(null) }
    var editValue by remember { mutableStateOf("") }

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

                        if (editingCounter == type) {
                            OutlinedTextField(
                                value = editValue,
                                onValueChange = { editValue = it.filter { c -> c.isDigit() } },
                                modifier = Modifier.width(60.dp),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                            IconButton(onClick = {
                                editValue.toIntOrNull()?.let { onSetCounter(type, it) }
                                editingCounter = null
                            }) {
                                Icon(Icons.Default.Check, "Set")
                            }
                        } else {
                            Text(
                                count.toString(),
                                modifier = Modifier
                                    .width(40.dp)
                                    .clickable {
                                        editValue = count.toString()
                                        editingCounter = type
                                    },
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }

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
    onToPositionFromTop: (Int) -> Unit
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
                        items(cards, key = { it.instanceId }) { card ->
                            val isSelected = selectedCard?.instanceId == card.instanceId
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                                    .clickable { selectedCard = if (isSelected) null else card },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected)
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
 * Dialog showing all library actions (context menu equivalent)
 */
@Composable
fun LibraryActionsDialog(
    librarySize: Int,
    topCard: CardInstance?,
    canSeeTopCard: Boolean,
    onDismiss: () -> Unit,
    onDraw: () -> Unit,
    onDrawMultiple: (Int) -> Unit,
    onSearch: () -> Unit,
    onShuffle: () -> Unit,
    onMill: (Int) -> Unit,
    onScry: (Int) -> Unit,
    onPlayTopCard: () -> Unit,
    onRevealTop: () -> Unit,
    onLookAtTop: () -> Unit,
    onViewTopCard: (CardInstance) -> Unit,
    onSendTopTo: (Int, Zone) -> Unit = { _, _ -> },
    onSendBottomTo: (Int, Zone) -> Unit = { _, _ -> },
    onViewTopN: (Int) -> Unit = { _ -> },
    onViewBottomN: (Int) -> Unit = { _ -> },
    onRevealTopN: (Int) -> Unit = { _ -> },
    onRevealBottomN: (Int) -> Unit = { _ -> },
    onShuffleTopN: (Int) -> Unit = { _ -> },
    onShuffleBottomN: (Int) -> Unit = { _ -> },
    onViewLibrary: () -> Unit = {}
) {
    var drawCount by remember { mutableStateOf("7") }
    var millCount by remember { mutableStateOf("1") }
    var scryCount by remember { mutableStateOf("1") }
    var sendTopCount by remember { mutableStateOf("1") }
    var sendBottomCount by remember { mutableStateOf("1") }
    var viewCount by remember { mutableStateOf("5") }
    var revealCount by remember { mutableStateOf("1") }
    var shufflePartialCount by remember { mutableStateOf("5") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Library ($librarySize cards)") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Drawing
                Text("Drawing", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(onClick = { onDraw(); onDismiss() }, modifier = Modifier.weight(1f)) {
                        Text("Draw 1")
                    }
                    OutlinedTextField(
                        value = drawCount,
                        onValueChange = { drawCount = it.filter { c -> c.isDigit() } },
                        modifier = Modifier.width(60.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedButton(
                        onClick = {
                            drawCount.toIntOrNull()?.let { onDrawMultiple(it) }
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Draw X")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Top card actions
                Text("Top Card", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                if (canSeeTopCard && topCard != null) {
                    OutlinedButton(
                        onClick = { onViewTopCard(topCard); onDismiss() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("View Top Card: ${topCard.card.name}")
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(onClick = { onPlayTopCard(); onDismiss() }, modifier = Modifier.weight(1f)) {
                        Text("Play Top")
                    }
                }

                // Scry actions
                Text("Scry", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(onClick = { onScry(1); onDismiss() }, modifier = Modifier.weight(1f)) {
                        Text("Scry 1")
                    }
                    OutlinedTextField(
                        value = scryCount,
                        onValueChange = { scryCount = it.filter { c -> c.isDigit() } },
                        modifier = Modifier.width(60.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedButton(
                        onClick = {
                            scryCount.toIntOrNull()?.let { onScry(it) }
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Scry X")
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(onClick = { onLookAtTop(); onDismiss() }, modifier = Modifier.weight(1f)) {
                        Text("Look at Top")
                    }
                    OutlinedButton(onClick = { onRevealTop(); onDismiss() }, modifier = Modifier.weight(1f)) {
                        Text("Reveal Top")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // View Top/Bottom N (private peek)
                Text("View Cards (Private)", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = viewCount,
                        onValueChange = { viewCount = it.filter { c -> c.isDigit() } },
                        modifier = Modifier.width(60.dp),
                        singleLine = true,
                        label = { Text("N") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedButton(
                        onClick = {
                            viewCount.toIntOrNull()?.let { onViewTopN(it) }
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("View Top N")
                    }
                    OutlinedButton(
                        onClick = {
                            viewCount.toIntOrNull()?.let { onViewBottomN(it) }
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("View Bottom N")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Reveal Top/Bottom N (public)
                Text("Reveal Cards (All See)", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = revealCount,
                        onValueChange = { revealCount = it.filter { c -> c.isDigit() } },
                        modifier = Modifier.width(60.dp),
                        singleLine = true,
                        label = { Text("N") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedButton(
                        onClick = {
                            revealCount.toIntOrNull()?.let { onRevealTopN(it) }
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Reveal Top N")
                    }
                    OutlinedButton(
                        onClick = {
                            revealCount.toIntOrNull()?.let { onRevealBottomN(it) }
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Reveal Bottom N")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Shuffle Top/Bottom N
                Text("Shuffle Partial", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = shufflePartialCount,
                        onValueChange = { shufflePartialCount = it.filter { c -> c.isDigit() } },
                        modifier = Modifier.width(60.dp),
                        singleLine = true,
                        label = { Text("N") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedButton(
                        onClick = {
                            shufflePartialCount.toIntOrNull()?.let { onShuffleTopN(it) }
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Shuffle Top N")
                    }
                    OutlinedButton(
                        onClick = {
                            shufflePartialCount.toIntOrNull()?.let { onShuffleBottomN(it) }
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Shuffle Bottom N")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Mill actions
                Text("Mill", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(onClick = { onMill(1); onDismiss() }, modifier = Modifier.weight(1f)) {
                        Text("Mill 1")
                    }
                    OutlinedTextField(
                        value = millCount,
                        onValueChange = { millCount = it.filter { c -> c.isDigit() } },
                        modifier = Modifier.width(60.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedButton(
                        onClick = {
                            millCount.toIntOrNull()?.let { onMill(it) }
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Mill X")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Send top X cards to...
                Text("Send Top X To", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = sendTopCount,
                        onValueChange = { sendTopCount = it.filter { c -> c.isDigit() } },
                        modifier = Modifier.width(60.dp),
                        singleLine = true,
                        label = { Text("X") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    Text("cards to:", style = MaterialTheme.typography.bodySmall)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            sendTopCount.toIntOrNull()?.let { onSendTopTo(it, Zone.HAND) }
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Hand", style = MaterialTheme.typography.labelSmall)
                    }
                    OutlinedButton(
                        onClick = {
                            sendTopCount.toIntOrNull()?.let { onSendTopTo(it, Zone.BATTLEFIELD) }
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Play", style = MaterialTheme.typography.labelSmall)
                    }
                    OutlinedButton(
                        onClick = {
                            sendTopCount.toIntOrNull()?.let { onSendTopTo(it, Zone.GRAVEYARD) }
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("GY", style = MaterialTheme.typography.labelSmall)
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            sendTopCount.toIntOrNull()?.let { onSendTopTo(it, Zone.EXILE) }
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Exile", style = MaterialTheme.typography.labelSmall)
                    }
                    OutlinedButton(
                        onClick = {
                            sendTopCount.toIntOrNull()?.let { onSendTopTo(it, Zone.COMMAND_ZONE) }
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("CZ", style = MaterialTheme.typography.labelSmall)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Send bottom X cards to...
                Text("Send Bottom X To", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = sendBottomCount,
                        onValueChange = { sendBottomCount = it.filter { c -> c.isDigit() } },
                        modifier = Modifier.width(60.dp),
                        singleLine = true,
                        label = { Text("X") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    Text("cards to:", style = MaterialTheme.typography.bodySmall)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            sendBottomCount.toIntOrNull()?.let { onSendBottomTo(it, Zone.HAND) }
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Hand", style = MaterialTheme.typography.labelSmall)
                    }
                    OutlinedButton(
                        onClick = {
                            sendBottomCount.toIntOrNull()?.let { onSendBottomTo(it, Zone.BATTLEFIELD) }
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Play", style = MaterialTheme.typography.labelSmall)
                    }
                    OutlinedButton(
                        onClick = {
                            sendBottomCount.toIntOrNull()?.let { onSendBottomTo(it, Zone.GRAVEYARD) }
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("GY", style = MaterialTheme.typography.labelSmall)
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            sendBottomCount.toIntOrNull()?.let { onSendBottomTo(it, Zone.EXILE) }
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Exile", style = MaterialTheme.typography.labelSmall)
                    }
                    OutlinedButton(
                        onClick = {
                            sendBottomCount.toIntOrNull()?.let { onSendBottomTo(it, Zone.COMMAND_ZONE) }
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("CZ", style = MaterialTheme.typography.labelSmall)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Other actions
                Text("Other", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(onClick = { onSearch(); onDismiss() }, modifier = Modifier.weight(1f)) {
                        Text("Search")
                    }
                    OutlinedButton(onClick = { onShuffle(); onDismiss() }, modifier = Modifier.weight(1f)) {
                        Text("Shuffle")
                    }
                }

                // View Library button
                Button(
                    onClick = { onViewLibrary(); onDismiss() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = librarySize > 0
                ) {
                    Text("View Library")
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
 * Card type categories for column-based display in Hand dialog (matches Desktop)
 */
private enum class HandCardTypeCategory(val displayName: String) {
    CREATURE("Creatures"),
    PLANESWALKER("Planeswalkers"),
    INSTANT("Instants"),
    SORCERY("Sorceries"),
    ENCHANTMENT("Enchantments"),
    ARTIFACT("Artifacts"),
    LAND("Lands"),
    OTHER("Other")
}

private fun getHandCardCategory(typeLine: String?): HandCardTypeCategory {
    val type = typeLine?.lowercase() ?: ""
    return when {
        type.contains("creature") -> HandCardTypeCategory.CREATURE
        type.contains("planeswalker") -> HandCardTypeCategory.PLANESWALKER
        type.contains("instant") -> HandCardTypeCategory.INSTANT
        type.contains("sorcery") -> HandCardTypeCategory.SORCERY
        type.contains("enchantment") -> HandCardTypeCategory.ENCHANTMENT
        type.contains("artifact") -> HandCardTypeCategory.ARTIFACT
        type.contains("land") -> HandCardTypeCategory.LAND
        else -> HandCardTypeCategory.OTHER
    }
}

/**
 * Enhanced Hand View Dialog with column-based layout by card type (matches Desktop/Cockatrice style)
 */
@Composable
fun HandViewDialog(
    cards: List<CardInstance>,
    playerName: String,
    onDismiss: () -> Unit,
    onPlayCard: (CardInstance) -> Unit,
    onDiscardCard: (CardInstance) -> Unit,
    onCardDetails: (CardInstance) -> Unit
) {
    var selectedCard by remember { mutableStateOf<CardInstance?>(null) }

    // Group cards by type category and sort alphabetically within each group
    val cardsByCategory = remember(cards) {
        cards
            .groupBy { getHandCardCategory(it.card.type) }
            .mapValues { (_, cardList) -> cardList.sortedBy { it.card.name.lowercase() } }
    }

    // Get non-empty categories in display order
    val activeCategories = remember(cardsByCategory) {
        HandCardTypeCategory.entries.filter { cardsByCategory[it]?.isNotEmpty() == true }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Hand - $playerName (${cards.size} cards)") },
        text = {
            Column(modifier = Modifier.heightIn(max = 500.dp)) {
                if (cards.isEmpty()) {
                    Text("No cards in hand", style = MaterialTheme.typography.bodyMedium)
                } else {
                    // Horizontal scrollable row of columns (one per card type)
                    val horizontalScrollState = rememberScrollState()

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .horizontalScroll(horizontalScrollState)
                    ) {
                        activeCategories.forEach { category ->
                            val categoryCards = cardsByCategory[category] ?: emptyList()

                            // Column for this card type - fillMaxHeight to use available space
                            Card(
                                modifier = Modifier
                                    .width(160.dp)
                                    .fillMaxHeight(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    // Column header
                                    Text(
                                        "${category.displayName} (${categoryCards.size})",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Divider(modifier = Modifier.padding(vertical = 2.dp))

                                    // Vertically scrollable list of cards
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .verticalScroll(rememberScrollState()),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        categoryCards.forEach { cardInstance ->
                                            val isSelected = selectedCard?.instanceId == cardInstance.instanceId
                                            Card(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { selectedCard = if (isSelected) null else cardInstance },
                                                colors = CardDefaults.cardColors(
                                                    containerColor = if (isSelected)
                                                        MaterialTheme.colorScheme.primaryContainer
                                                    else
                                                        MaterialTheme.colorScheme.secondaryContainer
                                                )
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(4.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    CardImage(
                                                        imageUrl = cardInstance.card.imageUri,
                                                        contentDescription = cardInstance.card.name,
                                                        modifier = Modifier.size(width = 30.dp, height = 42.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = cardInstance.card.name,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            maxLines = 2
                                                        )
                                                        cardInstance.card.manaCost?.let {
                                                            Text(
                                                                text = it,
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Action buttons for selected card
                    selectedCard?.let { card ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Selected: ${card.card.name}", style = MaterialTheme.typography.labelMedium)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    onPlayCard(card)
                                    selectedCard = null
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Play", style = MaterialTheme.typography.labelSmall)
                            }
                            OutlinedButton(
                                onClick = {
                                    onDiscardCard(card)
                                    selectedCard = null
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Discard", style = MaterialTheme.typography.labelSmall)
                            }
                            OutlinedButton(
                                onClick = { onCardDetails(card) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Details", style = MaterialTheme.typography.labelSmall)
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
 * Read-only dialog for viewing opponent's zones (graveyard, exile)
 */
@Composable
fun OpponentZoneDialog(
    cards: List<CardInstance>,
    playerName: String,
    zoneName: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("$zoneName - $playerName") },
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
                                    .padding(vertical = 2.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
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
 * Dialog for scrying - view and reorder top X cards of library
 */
@Composable
fun ScryDialog(
    cards: List<CardInstance>,
    onDismiss: () -> Unit,
    onReorder: (List<String>) -> Unit, // Card instance IDs in new order (top to bottom)
    onToHand: (CardInstance) -> Unit,
    onToBattlefield: (CardInstance) -> Unit,
    onToBottom: (CardInstance) -> Unit,
    onToGraveyard: (CardInstance) -> Unit
) {
    // Capture initial cards once - don't refresh when library changes
    val initialCards = remember { cards.reversed() } // Reversed so top card is first
    var orderedCards by remember { mutableStateOf(initialCards) }
    var selectedCard by remember { mutableStateOf<CardInstance?>(null) }
    var draggedIndex by remember { mutableStateOf(-1) }

    AlertDialog(
        onDismissRequest = {
            // Apply reorder on dismiss
            onReorder(orderedCards.map { it.instanceId })
            onDismiss()
        },
        title = { Text("Scry ${orderedCards.size}") },
        text = {
            Column(modifier = Modifier.heightIn(max = 500.dp)) {
                Text(
                    "Drag cards to reorder. Top card will be drawn first.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                Text(
                    "Tap to select, then use actions below.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Card list with reordering
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(orderedCards.size) { index ->
                        val card = orderedCards[index]
                        val isSelected = selectedCard?.instanceId == card.instanceId
                        val isDragging = draggedIndex == index

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                                .border(
                                    width = if (isDragging) 2.dp else 0.dp,
                                    color = if (isDragging) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedCard = if (isSelected) null else card },
                            colors = CardDefaults.cardColors(
                                containerColor = when {
                                    isSelected -> MaterialTheme.colorScheme.primaryContainer
                                    index == 0 -> MaterialTheme.colorScheme.tertiaryContainer
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                }
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Position indicator
                                Text(
                                    text = "${index + 1}",
                                    modifier = Modifier.width(24.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    textAlign = TextAlign.Center
                                )

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

                                // Move up/down buttons
                                Column {
                                    if (index > 0) {
                                        IconButton(
                                            onClick = {
                                                val newList = orderedCards.toMutableList()
                                                val item = newList.removeAt(index)
                                                newList.add(index - 1, item)
                                                orderedCards = newList
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Text("▲", style = MaterialTheme.typography.labelSmall)
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.size(24.dp))
                                    }
                                    if (index < orderedCards.size - 1) {
                                        IconButton(
                                            onClick = {
                                                val newList = orderedCards.toMutableList()
                                                val item = newList.removeAt(index)
                                                newList.add(index + 1, item)
                                                orderedCards = newList
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Text("▼", style = MaterialTheme.typography.labelSmall)
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.size(24.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                // Action buttons for selected card
                selectedCard?.let { card ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Selected: ${card.card.name}",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    orderedCards = orderedCards.filter { it.instanceId != card.instanceId }
                                    onToHand(card)
                                    selectedCard = null
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Hand", style = MaterialTheme.typography.labelSmall)
                            }
                            OutlinedButton(
                                onClick = {
                                    orderedCards = orderedCards.filter { it.instanceId != card.instanceId }
                                    onToBattlefield(card)
                                    selectedCard = null
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Battlefield", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    orderedCards = orderedCards.filter { it.instanceId != card.instanceId }
                                    onToBottom(card)
                                    selectedCard = null
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Bottom", style = MaterialTheme.typography.labelSmall)
                            }
                            OutlinedButton(
                                onClick = {
                                    orderedCards = orderedCards.filter { it.instanceId != card.instanceId }
                                    onToGraveyard(card)
                                    selectedCard = null
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Graveyard", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }

                // Quick actions
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            // Put all on bottom in reverse order
                            orderedCards.forEach { onToBottom(it) }
                            orderedCards = emptyList()
                        },
                        modifier = Modifier.weight(1f),
                        enabled = orderedCards.isNotEmpty()
                    ) {
                        Text("All Bottom", style = MaterialTheme.typography.labelSmall)
                    }
                    OutlinedButton(
                        onClick = {
                            // Reverse order (bottom becomes top)
                            orderedCards = orderedCards.reversed()
                        },
                        modifier = Modifier.weight(1f),
                        enabled = orderedCards.size > 1
                    ) {
                        Text("Reverse", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                // Apply reorder and close
                onReorder(orderedCards.map { it.instanceId })
                onDismiss()
            }) {
                Text("Done")
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
 * Commander damage tracking dialog
 */
@Composable
fun CommanderDamageDialog(
    players: List<Player>,
    commanders: List<CardInstance>,
    onDismiss: () -> Unit,
    onDamageChange: (playerId: String, commanderId: String, newDamage: Int) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Commander Damage Tracking") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (commanders.isEmpty()) {
                    Text(
                        "No commanders on the battlefield or in command zone",
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    commanders.forEach { commander ->
                        CommanderDamageSection(
                            commander = commander,
                            players = players,
                            onDamageChange = onDamageChange
                        )
                    }
                }

                // Loss condition info
                Text(
                    text = "Note: A player loses if they take 21 or more combat damage from a single commander.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun CommanderDamageSection(
    commander: CardInstance,
    players: List<Player>,
    onDamageChange: (playerId: String, commanderId: String, newDamage: Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Commander header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Commander thumbnail
                CardImageThumbnail(
                    imageUrl = commander.card.imageUri,
                    contentDescription = commander.card.name
                )

                // Commander name and owner
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = commander.card.name,
                        style = MaterialTheme.typography.titleMedium
                    )
                    val owner = players.find { it.id == commander.ownerId }
                    if (owner != null) {
                        Text(
                            text = "Owned by ${owner.name}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            Divider()

            // Damage to each player
            players.forEach { player ->
                val damage = player.commanderDamage[commander.instanceId] ?: 0
                val isLethal = damage >= GameConstants.COMMANDER_DAMAGE_THRESHOLD

                CommanderDamageCounter(
                    playerName = player.name,
                    damage = damage,
                    isLethal = isLethal,
                    onIncrement = {
                        onDamageChange(player.id, commander.instanceId, damage + 1)
                    },
                    onDecrement = {
                        if (damage > 0) {
                            onDamageChange(player.id, commander.instanceId, damage - 1)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun CommanderDamageCounter(
    playerName: String,
    damage: Int,
    isLethal: Boolean,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isLethal) {
                    Modifier
                        .background(
                            MaterialTheme.colorScheme.errorContainer,
                            RoundedCornerShape(8.dp)
                        )
                        .border(
                            2.dp,
                            MaterialTheme.colorScheme.error,
                            RoundedCornerShape(8.dp)
                        )
                        .padding(8.dp)
                } else {
                    Modifier.padding(8.dp)
                }
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Player name
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = playerName,
                style = MaterialTheme.typography.bodyLarge
            )
            if (isLethal) {
                Text(
                    text = "LETHAL DAMAGE!",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        // Damage controls
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onDecrement,
                enabled = damage > 0
            ) {
                Text("-", style = MaterialTheme.typography.titleLarge)
            }

            Text(
                text = damage.toString(),
                style = MaterialTheme.typography.headlineSmall,
                color = if (isLethal) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                modifier = Modifier.widthIn(min = 40.dp)
            )

            IconButton(onClick = onIncrement) {
                Text("+", style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}

/**
 * Command Zone viewing dialog
 */
@Composable
fun CommandZoneDialog(
    cards: List<CardInstance>,
    playerName: String,
    onDismiss: () -> Unit,
    onCast: (CardInstance) -> Unit,
    onToHand: (CardInstance) -> Unit
) {
    ZoneViewerDialog(
        title = "Command Zone - $playerName",
        cards = cards,
        onDismiss = onDismiss,
        onCardAction = { card, action ->
            when (action) {
                "cast" -> onCast(card)
                "hand" -> onToHand(card)
            }
        },
        actionButtons = listOf("cast" to "Cast", "hand" to "To Hand")
    )
}

/**
 * Card type categories for column-based display (matches Desktop)
 */
private enum class PeekCardTypeCategory(val displayName: String) {
    CREATURE("Creatures"),
    PLANESWALKER("Planeswalkers"),
    INSTANT("Instants"),
    SORCERY("Sorceries"),
    ENCHANTMENT("Enchantments"),
    ARTIFACT("Artifacts"),
    LAND("Lands"),
    OTHER("Other")
}

private fun getPeekCardCategory(typeLine: String?): PeekCardTypeCategory {
    val type = typeLine?.lowercase() ?: ""
    return when {
        type.contains("creature") -> PeekCardTypeCategory.CREATURE
        type.contains("planeswalker") -> PeekCardTypeCategory.PLANESWALKER
        type.contains("instant") -> PeekCardTypeCategory.INSTANT
        type.contains("sorcery") -> PeekCardTypeCategory.SORCERY
        type.contains("enchantment") -> PeekCardTypeCategory.ENCHANTMENT
        type.contains("artifact") -> PeekCardTypeCategory.ARTIFACT
        type.contains("land") -> PeekCardTypeCategory.LAND
        else -> PeekCardTypeCategory.OTHER
    }
}

/**
 * Enhanced Library Peek Dialog with column-based layout by card type (matches Desktop/Cockatrice style)
 */
@Composable
fun LibraryPeekDialog(
    cards: List<CardInstance>,
    title: String,
    onDismiss: () -> Unit,
    onToHand: (CardInstance) -> Unit,
    onToBattlefield: (CardInstance) -> Unit,
    onToGraveyard: (CardInstance) -> Unit,
    onToExile: (CardInstance) -> Unit,
    onToTop: (CardInstance) -> Unit,
    onToBottom: (CardInstance) -> Unit
) {
    // Capture initial cards once - don't refresh when library changes
    val initialCards = remember { cards }
    var displayedCards by remember { mutableStateOf(initialCards) }
    var selectedCard by remember { mutableStateOf<CardInstance?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    // Filter cards based on search query
    val filteredCards = remember(displayedCards, searchQuery) {
        if (searchQuery.isBlank()) {
            displayedCards
        } else {
            val query = searchQuery.lowercase()
            displayedCards.filter { card ->
                card.card.name.lowercase().contains(query) ||
                card.card.type?.lowercase()?.contains(query) == true
            }
        }
    }

    // Group cards by type category and sort alphabetically within each group
    val cardsByCategory = remember(filteredCards) {
        filteredCards
            .groupBy { getPeekCardCategory(it.card.type) }
            .mapValues { (_, cardList) -> cardList.sortedBy { it.card.name.lowercase() } }
    }

    // Get non-empty categories in display order
    val activeCategories = remember(cardsByCategory) {
        PeekCardTypeCategory.entries.filter { cardsByCategory[it]?.isNotEmpty() == true }
    }

    // Helper to remove card from display and call action
    fun removeAndAction(card: CardInstance, action: (CardInstance) -> Unit) {
        displayedCards = displayedCards.filter { it.instanceId != card.instanceId }
        selectedCard = null
        action(card)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(modifier = Modifier.heightIn(max = 500.dp)) {
                // Search bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search by name or type") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                if (searchQuery.isNotBlank()) {
                    Text(
                        "Showing ${filteredCards.size} of ${displayedCards.size} cards",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                if (filteredCards.isEmpty()) {
                    Text(
                        if (displayedCards.isEmpty()) "No cards" else "No matching cards",
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    Text("${filteredCards.size} cards", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(4.dp))

                    // Horizontal scrollable row of columns (one per card type)
                    val horizontalScrollState = rememberScrollState()

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .horizontalScroll(horizontalScrollState)
                    ) {
                        activeCategories.forEach { category ->
                            val categoryCards = cardsByCategory[category] ?: emptyList()

                            // Column for this card type - fillMaxHeight to use available space
                            Card(
                                modifier = Modifier
                                    .width(160.dp)
                                    .fillMaxHeight(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    // Column header
                                    Text(
                                        "${category.displayName} (${categoryCards.size})",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Divider(modifier = Modifier.padding(vertical = 2.dp))

                                    // Vertically scrollable list of cards
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .verticalScroll(rememberScrollState()),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        categoryCards.forEach { cardInstance ->
                                            val isSelected = selectedCard?.instanceId == cardInstance.instanceId
                                            Card(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { selectedCard = if (isSelected) null else cardInstance },
                                                colors = CardDefaults.cardColors(
                                                    containerColor = if (isSelected)
                                                        MaterialTheme.colorScheme.primaryContainer
                                                    else
                                                        MaterialTheme.colorScheme.secondaryContainer
                                                )
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(4.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    CardImage(
                                                        imageUrl = cardInstance.card.imageUri,
                                                        contentDescription = cardInstance.card.name,
                                                        modifier = Modifier.size(width = 30.dp, height = 42.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = cardInstance.card.name,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            maxLines = 2
                                                        )
                                                        cardInstance.card.manaCost?.let {
                                                            Text(
                                                                text = it,
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Action buttons for selected card
                    selectedCard?.let { card ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Selected: ${card.card.name}", style = MaterialTheme.typography.labelMedium)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { removeAndAction(card, onToHand) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Hand", style = MaterialTheme.typography.labelSmall)
                                }
                                OutlinedButton(
                                    onClick = { removeAndAction(card, onToBattlefield) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Play", style = MaterialTheme.typography.labelSmall)
                                }
                                OutlinedButton(
                                    onClick = { removeAndAction(card, onToGraveyard) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("GY", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { removeAndAction(card, onToExile) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Exile", style = MaterialTheme.typography.labelSmall)
                                }
                                OutlinedButton(
                                    onClick = { removeAndAction(card, onToTop) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Top", style = MaterialTheme.typography.labelSmall)
                                }
                                OutlinedButton(
                                    onClick = { removeAndAction(card, onToBottom) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Bottom", style = MaterialTheme.typography.labelSmall)
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
 * Generic number input dialog for operations requiring a numeric value
 */
@Composable
fun NumberInputDialog(
    title: String,
    defaultValue: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var value by remember { mutableStateOf(defaultValue.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it.filter { c -> c.isDigit() || c == '-' } },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    label = { Text("Enter number") }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                value.toIntOrNull()?.let { onConfirm(it) }
                onDismiss()
            }) {
                Text("OK")
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
 * Game log dialog
 */
@Composable
fun GameLogDialog(
    gameLog: List<GameEvent>,
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

/**
 * Settings dialog for configuring player settings.
 * Android version - simplified compared to Desktop (no file chooser).
 */
@Composable
fun SettingsDialog(
    currentPlayerName: String,
    currentServerAddress: String,
    currentServerPort: Int,
    onPlayerNameChange: (String) -> Unit,
    onServerAddressChange: (String) -> Unit,
    onServerPortChange: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var playerName by remember { mutableStateOf(currentPlayerName) }
    var serverAddress by remember { mutableStateOf(currentServerAddress) }
    var serverPort by remember { mutableStateOf(currentServerPort.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Settings") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Player Settings Section
                Text(
                    "Player",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = playerName,
                    onValueChange = { playerName = it },
                    label = { Text("Player Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Divider()

                // Network Settings Section
                Text(
                    "Network",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = serverAddress,
                    onValueChange = { serverAddress = it },
                    label = { Text("Default Server Address") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = serverPort,
                    onValueChange = { newValue ->
                        serverPort = newValue.filter { it.isDigit() }
                    },
                    label = { Text("Default Server Port") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = { Text("Valid range: 1024-65535") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onPlayerNameChange(playerName)
                    onServerAddressChange(serverAddress)
                    val port = serverPort.toIntOrNull()?.coerceIn(1024, 65535) ?: 8080
                    onServerPortChange(port)
                    onDismiss()
                }
            ) {
                Text("Save")
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
 * Stack Until Found dialog - searches library from top until a card matching the search is found.
 */
@Composable
fun StackUntilFoundDialog(
    libraryCards: List<CardInstance>,
    onDismiss: () -> Unit,
    onMoveToGraveyard: (CardInstance) -> Unit,
    onMoveToBottom: (CardInstance) -> Unit
) {
    var searchText by remember { mutableStateOf("") }
    var hasSearched by remember { mutableStateOf(false) }
    var results by remember { mutableStateOf<List<CardInstance>>(emptyList()) }
    var matchFound by remember { mutableStateOf<CardInstance?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (hasSearched) "Stack Until Found - Results" else "Stack Until Found") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
            ) {
                if (!hasSearched) {
                    // Stage 1: Enter search term
                    Text(
                        "Enter a card name or type to search for. Cards will be revealed from the top of your library until a match is found.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = searchText,
                        onValueChange = { searchText = it },
                        label = { Text("Search (name or type)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    // Stage 2: Show results
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        if (matchFound != null) {
                            Text(
                                "Found: ${matchFound?.card?.name}",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        } else {
                            Text(
                                "No match found in library",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }

                        if (results.isNotEmpty()) {
                            Text(
                                "Revealed ${results.size} card(s):",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            results.forEach { card ->
                                Text(
                                    "• ${card.card.name}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Choose where to put the revealed cards:",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        } else if (matchFound != null) {
                            Text(
                                "Match was on top of library (no cards revealed)",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (!hasSearched) {
                Button(
                    onClick = {
                        if (searchText.isNotBlank()) {
                            // Library is stored with bottom at index 0, top at end
                            val searchLower = searchText.lowercase()
                            val revealed = mutableListOf<CardInstance>()
                            var match: CardInstance? = null

                            // Search from top (end of list) to bottom
                            for (card in libraryCards.reversed()) {
                                val nameMatches = card.card.name.lowercase().contains(searchLower)
                                val typeMatches = card.card.type?.lowercase()?.contains(searchLower) == true
                                if (nameMatches || typeMatches) {
                                    match = card
                                    break
                                } else {
                                    revealed.add(card)
                                }
                            }

                            results = revealed
                            matchFound = match
                            hasSearched = true
                        }
                    },
                    enabled = searchText.isNotBlank()
                ) {
                    Text("Search")
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            results.forEach { card -> onMoveToGraveyard(card) }
                            onDismiss()
                        },
                        enabled = results.isNotEmpty()
                    ) {
                        Text("To Graveyard")
                    }
                    Button(
                        onClick = {
                            results.forEach { card -> onMoveToBottom(card) }
                            onDismiss()
                        },
                        enabled = results.isNotEmpty()
                    ) {
                        Text("To Bottom")
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (hasSearched) "Close" else "Cancel")
            }
        }
    )
}

/**
 * Sideboard dialog - view and manage sideboard cards
 */
@Composable
fun SideboardDialog(
    sideboardCards: List<CardInstance>,
    onDismiss: () -> Unit,
    onToHand: (CardInstance) -> Unit,
    onToBattlefield: (CardInstance) -> Unit,
    onToGraveyard: (CardInstance) -> Unit
) {
    var selectedCard by remember { mutableStateOf<CardInstance?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sideboard (${sideboardCards.size} cards)") },
        text = {
            Column(modifier = Modifier.heightIn(max = 500.dp)) {
                if (sideboardCards.isEmpty()) {
                    Text("No cards in sideboard", style = MaterialTheme.typography.bodyMedium)
                } else {
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(sideboardCards, key = { it.instanceId }) { card ->
                            val isSelected = selectedCard?.instanceId == card.instanceId
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                                    .clickable { selectedCard = if (isSelected) null else card },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected)
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
                                        card.card.manaCost?.let {
                                            Text(it, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
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
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    onToHand(card)
                                    selectedCard = null
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Hand", style = MaterialTheme.typography.bodySmall)
                            }
                            OutlinedButton(
                                onClick = {
                                    onToBattlefield(card)
                                    selectedCard = null
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Play", style = MaterialTheme.typography.bodySmall)
                            }
                            OutlinedButton(
                                onClick = {
                                    onToGraveyard(card)
                                    selectedCard = null
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Grave", style = MaterialTheme.typography.bodySmall)
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
 * Read-only dialog showing cards that have been revealed to you by another player.
 */
@Composable
fun RevealedCardsDialog(
    cards: List<CardInstance>,
    revealingPlayerName: String,
    title: String = "revealed cards",
    onDismiss: () -> Unit,
    onViewDetails: (CardInstance) -> Unit = {}
) {
    var selectedCard by remember { mutableStateOf<CardInstance?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("$revealingPlayerName $title") },
        text = {
            Column(modifier = Modifier.heightIn(max = 500.dp)) {
                Text(
                    "${cards.size} cards",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (cards.isEmpty()) {
                    Text("No cards", style = MaterialTheme.typography.bodyMedium)
                } else {
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(cards, key = { it.instanceId }) { card ->
                            val isSelected = selectedCard?.instanceId == card.instanceId
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                                    .clickable { selectedCard = if (isSelected) null else card },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected)
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
                                        card.card.manaCost?.let {
                                            Text(it, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // View details button for selected card
                    selectedCard?.let { card ->
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { onViewDetails(card) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("View Details: ${card.card.name}")
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
