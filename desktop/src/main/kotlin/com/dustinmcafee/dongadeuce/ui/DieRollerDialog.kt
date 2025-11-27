package com.dustinmcafee.dongadeuce.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.random.Random

/**
 * Represents a die roll result
 */
data class DieRollResult(
    val dieType: String,
    val numberOfDice: Int,
    val rolls: List<Int>,
    val total: Int,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Dialog for rolling dice
 */
@Composable
fun DieRollerDialog(
    playerName: String,
    onDismiss: () -> Unit,
    onRollLogged: ((dieType: String, result: Int, numberOfDice: Int) -> Unit)? = null
) {
    var rollHistory by remember { mutableStateOf<List<DieRollResult>>(emptyList()) }
    var numberOfDice by remember { mutableStateOf("1") }
    var customSides by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Standard dice types
    val standardDice = listOf(
        "D4" to 4,
        "D6" to 6,
        "D8" to 8,
        "D10" to 10,
        "D12" to 12,
        "D20" to 20,
        "D100" to 100
    )

    fun rollDie(sides: Int, dieType: String) {
        val count = numberOfDice.toIntOrNull()?.coerceIn(1, 100) ?: 1
        val rolls = List(count) { Random.nextInt(1, sides + 1) }
        val result = DieRollResult(
            dieType = dieType,
            numberOfDice = count,
            rolls = rolls,
            total = rolls.sum()
        )
        rollHistory = listOf(result) + rollHistory.take(49) // Keep last 50 rolls

        // Log the roll to the game log
        onRollLogged?.invoke(dieType, result.total, count)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("$playerName - Dice Roller") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(min = 400.dp, max = 600.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Number of dice input
                OutlinedTextField(
                    value = numberOfDice,
                    onValueChange = {
                        if (it.isEmpty() || (it.toIntOrNull() != null && it.toIntOrNull()!! in 1..100)) {
                            numberOfDice = it
                        }
                    },
                    label = { Text("Number of Dice") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Standard dice buttons - row 1
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    standardDice.take(4).forEach { (name, sides) ->
                        DieButton(
                            name = name,
                            modifier = Modifier.weight(1f),
                            onClick = { rollDie(sides, name) }
                        )
                    }
                }

                // Standard dice buttons - row 2
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    standardDice.drop(4).forEach { (name, sides) ->
                        DieButton(
                            name = name,
                            modifier = Modifier.weight(1f),
                            onClick = { rollDie(sides, name) }
                        )
                    }
                }

                Divider()

                // Custom die
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = customSides,
                        onValueChange = {
                            if (it.isEmpty() || it.toIntOrNull() != null) {
                                customSides = it
                            }
                        },
                        label = { Text("Custom Sides") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )

                    Button(
                        onClick = {
                            val sides = customSides.toIntOrNull()
                            if (sides != null && sides > 0) {
                                rollDie(sides, "D$sides")
                            }
                        },
                        enabled = (customSides.toIntOrNull() ?: 0) > 0
                    ) {
                        Text("Roll")
                    }
                }

                Divider()

                // Quick actions row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            // Coin flip (D2)
                            val result = if (Random.nextBoolean()) "Heads" else "Tails"
                            val numResult = if (result == "Heads") 1 else 2
                            val coinResult = DieRollResult(
                                dieType = "Coin",
                                numberOfDice = 1,
                                rolls = listOf(numResult),
                                total = numResult
                            )
                            rollHistory = listOf(coinResult) + rollHistory.take(49)

                            // Log coin flip to game log
                            onRollLogged?.invoke("Coin ($result)", numResult, 1)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Flip Coin")
                    }

                    OutlinedButton(
                        onClick = { rollHistory = emptyList() },
                        modifier = Modifier.weight(1f),
                        enabled = rollHistory.isNotEmpty()
                    ) {
                        Text("Clear History")
                    }
                }

                Divider()

                // Roll history
                Text(
                    "Roll History",
                    style = MaterialTheme.typography.titleSmall
                )

                if (rollHistory.isEmpty()) {
                    Text(
                        "No rolls yet. Click a die button to roll!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(rollHistory) { result ->
                            RollResultCard(result)
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun DieButton(
    name: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        contentPadding = PaddingValues(horizontal = 8.dp)
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun RollResultCard(result: DieRollResult) {
    val isHighlight = result == result // Always true, but we can use this for animation later

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(8.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = if (result.dieType == "Coin") {
                        "Coin Flip"
                    } else if (result.numberOfDice == 1) {
                        result.dieType
                    } else {
                        "${result.numberOfDice}${result.dieType}"
                    },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                if (result.dieType == "Coin") {
                    Text(
                        text = if (result.rolls.first() == 1) "Heads" else "Tails",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else if (result.numberOfDice > 1) {
                    Text(
                        text = result.rolls.joinToString(" + "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (result.dieType != "Coin") {
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(
                        text = "${result.total}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}
