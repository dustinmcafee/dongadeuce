package com.commandermtg.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.commandermtg.models.GamePhase
import com.commandermtg.models.Player

@Composable
fun TurnIndicator(
    activePlayer: Player,
    currentPhase: GamePhase,
    turnNumber: Int,
    onNextPhase: () -> Unit,
    onPassTurn: () -> Unit,
    onUntapAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Turn and player info
            Text(
                text = "Turn $turnNumber - ${activePlayer.name}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            // Current phase with all phases listed
            PhaseIndicator(currentPhase = currentPhase)

            // Control buttons
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = onUntapAll,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Untap All")
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = onNextPhase,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Next Phase")
                    }

                    Button(
                        onClick = onPassTurn,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Pass Turn")
                    }
                }
            }
        }
    }
}

@Composable
fun PhaseIndicator(
    currentPhase: GamePhase,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "Phase:",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )

        // Group phases by category
        val mainPhases = listOf(
            GamePhase.UNTAP to "Untap",
            GamePhase.UPKEEP to "Upkeep",
            GamePhase.DRAW to "Draw"
        )

        val combatPhases = listOf(
            GamePhase.COMBAT_BEGIN to "Begin Combat",
            GamePhase.COMBAT_DECLARE_ATTACKERS to "Declare Attackers",
            GamePhase.COMBAT_DECLARE_BLOCKERS to "Declare Blockers",
            GamePhase.COMBAT_DAMAGE to "Combat Damage",
            GamePhase.COMBAT_END to "End Combat"
        )

        val endPhases = listOf(
            GamePhase.END to "End Step",
            GamePhase.CLEANUP to "Cleanup"
        )

        // Beginning Phase
        PhaseGroup("Beginning", mainPhases, currentPhase)

        // Main Phase 1
        PhaseChip(
            label = "Main Phase 1",
            isActive = currentPhase == GamePhase.MAIN_1
        )

        // Combat Phase
        PhaseGroup("Combat", combatPhases, currentPhase)

        // Main Phase 2
        PhaseChip(
            label = "Main Phase 2",
            isActive = currentPhase == GamePhase.MAIN_2
        )

        // Ending Phase
        PhaseGroup("Ending", endPhases, currentPhase)
    }
}

@Composable
fun PhaseGroup(
    groupName: String,
    phases: List<Pair<GamePhase, String>>,
    currentPhase: GamePhase
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = groupName,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f)
        )
        phases.forEach { (phase, label) ->
            PhaseChip(
                label = label,
                isActive = currentPhase == phase,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

@Composable
fun PhaseChip(
    label: String,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isActive) {
        MaterialTheme.colorScheme.secondary
    } else {
        MaterialTheme.colorScheme.surface
    }

    val textColor = if (isActive) {
        MaterialTheme.colorScheme.onSecondary
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(28.dp),
        color = backgroundColor,
        shape = RoundedCornerShape(4.dp),
        border = if (isActive) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.secondary)
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = textColor
            )
        }
    }
}
