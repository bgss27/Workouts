package com.fittrack.app.ui.workout

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Share
import com.fittrack.app.share.ShareSummary
import com.fittrack.app.ui.components.PrimaryButton
import com.fittrack.app.ui.components.StatValue
import com.fittrack.app.ui.components.TonalButton
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.fittrack.app.data.WeightUnits
import com.fittrack.app.domain.analysis.PRResult
import kotlinx.coroutines.delay
import nl.dionsegijn.konfetti.compose.KonfettiView
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import java.util.concurrent.TimeUnit

/**
 * Full-screen wrap-up shown after every saved workout: confetti, the
 * session's headline stats, PR rows when any were hit, and Share / Continue.
 */
@Composable
fun PostWorkoutCelebration(
    prs: List<PRResult>,
    summary: ShareSummary?,
    displayUnit: String,
    onShare: () -> Unit,
    onDismiss: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(Unit) {
        // Two quick LongPress pulses give a more "celebration-y" feel than one.
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        delay(120)
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            // Confetti layer
            val party = remember {
                Party(
                    speed = 0f,
                    maxSpeed = 30f,
                    damping = 0.9f,
                    spread = 360,
                    colors = listOf(
                        0xFFFCE18A.toInt(),
                        0xFFFF726D.toInt(),
                        0xFFF4306D.toInt(),
                        0xFFB48DEF.toInt(),
                    ),
                    position = Position.Relative(0.5, 0.3),
                    emitter = Emitter(duration = 250, TimeUnit.MILLISECONDS).max(120),
                )
            }
            KonfettiView(
                modifier = Modifier.fillMaxSize(),
                parties = listOf(party),
            )

            // Pulse-on-entry scale animation for the card itself.
            var entered by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) { entered = true }
            val scale by animateFloatAsState(
                targetValue = if (entered) 1f else 0.85f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "celebrationCardScale",
            )

            Surface(
                modifier = Modifier
                    .padding(32.dp)
                    .scale(scale),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                shadowElevation = 12.dp,
            ) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 24.dp, vertical = 28.dp)
                        .widthIn(max = 360.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        imageVector = if (prs.isNotEmpty()) Icons.Default.EmojiEvents else Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = if (prs.isNotEmpty()) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(72.dp),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = when {
                            prs.size == 1 -> "New Personal Record!"
                            prs.size > 1 -> "${prs.size} New PRs!"
                            else -> "Workout complete"
                        },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                    if (summary != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                        ) {
                            StatValue(value = "${summary.durationMinutes}", label = "min", align = Alignment.CenterHorizontally)
                            StatValue(value = "${summary.setCount}", label = "sets", align = Alignment.CenterHorizontally)
                            StatValue(value = summary.volumeText, label = "volume", align = Alignment.CenterHorizontally)
                        }
                        if (summary.streakWeeks > 0) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "\uD83D\uDD25 ${summary.streakWeeks}-week streak",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))

                    prs.take(3).forEach { pr ->
                        PrRow(pr = pr, displayUnit = displayUnit)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    if (prs.size > 3) {
                        Text(
                            text = "+${prs.size - 3} more",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    if (summary != null) {
                        TonalButton(text = "Share", icon = Icons.Default.Share, onClick = onShare)
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                    PrimaryButton(text = "Continue", onClick = onDismiss)
                }
            }
        }
    }
}

@Composable
private fun PrRow(pr: PRResult, displayUnit: String) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = Icons.Default.TrendingUp,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = pr.exerciseName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "Previous: ${WeightUnits.format(pr.previousBest1RM, displayUnit)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = WeightUnits.format(pr.newBest1RM, displayUnit),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "+${WeightUnits.format(pr.delta, displayUnit)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Suppress("unused")
private val Transparent = Color.Transparent
