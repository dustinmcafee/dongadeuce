package com.dustinmcafee.dongadeuce.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dustinmcafee.dongadeuce.models.GamePhase
import com.dustinmcafee.dongadeuce.models.Player

@Composable
fun TurnIndicator(
    activePlayer: Player,
    currentPhase: GamePhase,
    turnNumber: Int,
    onNextPhase: () -> Unit,
    onPassTurn: () -> Unit,
    onUntapAll: () -> Unit,
    onRollDice: () -> Unit,
    isActivePlayerLocal: Boolean = true,
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
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Turn and player info - compact
            Text(
                text = "T$turnNumber • ${activePlayer.name}",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            // Current phase highlighted
            Surface(
                color = MaterialTheme.colorScheme.secondary,
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = currentPhase.displayName,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondary,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }

            // Compact phase indicator
            CompactPhaseIndicator(currentPhase = currentPhase, onNextPhase = onNextPhase)

            // Control buttons - more compact
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                SmallButton(
                    text = "Untap",
                    onClick = onUntapAll,
                    modifier = Modifier.weight(1f)
                )
                SmallButton(
                    text = "Dice",
                    onClick = onRollDice,
                    modifier = Modifier.weight(1f),
                    outlined = true
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                SmallButton(
                    text = "→ Phase",
                    onClick = onNextPhase,
                    modifier = Modifier.weight(1f),
                    outlined = true
                )
                SmallButton(
                    text = "→ Turn",
                    onClick = onPassTurn,
                    modifier = Modifier.weight(1f),
                    enabled = isActivePlayerLocal
                )
            }
        }
    }
}

@Composable
private fun SmallButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    outlined: Boolean = false,
    enabled: Boolean = true
) {
    if (outlined) {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier.height(32.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
            enabled = enabled
        ) {
            Text(text, style = MaterialTheme.typography.labelSmall)
        }
    } else {
        Button(
            onClick = onClick,
            modifier = modifier.height(32.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
            enabled = enabled
        ) {
            Text(text, style = MaterialTheme.typography.labelSmall)
        }
    }
}

// Extension property for readable phase names
private val GamePhase.displayName: String
    get() = when (this) {
        GamePhase.UNTAP -> "Untap"
        GamePhase.UPKEEP -> "Upkeep"
        GamePhase.DRAW -> "Draw"
        GamePhase.MAIN_1 -> "Main 1"
        GamePhase.COMBAT_BEGIN -> "Begin Combat"
        GamePhase.COMBAT_DECLARE_ATTACKERS -> "Attackers"
        GamePhase.COMBAT_DECLARE_BLOCKERS -> "Blockers"
        GamePhase.COMBAT_DAMAGE -> "Damage"
        GamePhase.COMBAT_END -> "End Combat"
        GamePhase.MAIN_2 -> "Main 2"
        GamePhase.END -> "End Step"
        GamePhase.CLEANUP -> "Cleanup"
    }

// Abbreviations for compact display
private val GamePhase.abbrev: String
    get() = when (this) {
        GamePhase.UNTAP -> "U"
        GamePhase.UPKEEP -> "Up"
        GamePhase.DRAW -> "D"
        GamePhase.MAIN_1 -> "M1"
        GamePhase.COMBAT_BEGIN -> "BC"
        GamePhase.COMBAT_DECLARE_ATTACKERS -> "DA"
        GamePhase.COMBAT_DECLARE_BLOCKERS -> "DB"
        GamePhase.COMBAT_DAMAGE -> "CD"
        GamePhase.COMBAT_END -> "EC"
        GamePhase.MAIN_2 -> "M2"
        GamePhase.END -> "E"
        GamePhase.CLEANUP -> "C"
    }

@Composable
fun CompactPhaseIndicator(
    currentPhase: GamePhase,
    onNextPhase: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Split into rows for compact display
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        // Row 1: Beginning phases + Main 1
        Row(
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            listOf(GamePhase.UNTAP, GamePhase.UPKEEP, GamePhase.DRAW, GamePhase.MAIN_1).forEach { phase ->
                CompactPhaseChip(
                    phase = phase,
                    isActive = currentPhase == phase,
                    onClick = onNextPhase,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Row 2: Combat phases
        Row(
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            listOf(
                GamePhase.COMBAT_BEGIN,
                GamePhase.COMBAT_DECLARE_ATTACKERS,
                GamePhase.COMBAT_DECLARE_BLOCKERS,
                GamePhase.COMBAT_DAMAGE,
                GamePhase.COMBAT_END
            ).forEach { phase ->
                CompactPhaseChip(
                    phase = phase,
                    isActive = currentPhase == phase,
                    onClick = onNextPhase,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Row 3: Main 2 + End phases
        Row(
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            listOf(GamePhase.MAIN_2, GamePhase.END, GamePhase.CLEANUP).forEach { phase ->
                CompactPhaseChip(
                    phase = phase,
                    isActive = currentPhase == phase,
                    onClick = onNextPhase,
                    modifier = Modifier.weight(1f)
                )
            }
            // Spacer to maintain alignment
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun CompactPhaseChip(
    phase: GamePhase,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isActive) {
        MaterialTheme.colorScheme.secondary
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
    }

    val textColor = if (isActive) {
        MaterialTheme.colorScheme.onSecondary
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
    }

    Surface(
        modifier = modifier
            .height(20.dp)
            .clickable(onClick = onClick),
        color = backgroundColor,
        shape = RoundedCornerShape(3.dp),
        border = if (isActive) {
            BorderStroke(1.dp, MaterialTheme.colorScheme.secondary)
        } else null
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = phase.abbrev,
                style = MaterialTheme.typography.labelSmall,
                color = textColor,
                textAlign = TextAlign.Center
            )
        }
    }
}

// Legacy PhaseIndicator for backwards compatibility if needed elsewhere
@Composable
fun PhaseIndicator(
    currentPhase: GamePhase,
    modifier: Modifier = Modifier
) {
    CompactPhaseIndicator(currentPhase = currentPhase, onNextPhase = {}, modifier = modifier)
}
