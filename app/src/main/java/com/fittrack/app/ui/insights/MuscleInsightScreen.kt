package com.fittrack.app.ui.insights

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fittrack.app.di.AppModule
import com.fittrack.app.domain.ml.*
import com.fittrack.app.ui.components.EmptyState
import com.fittrack.app.ui.components.MuscleGroupChipRow
import com.fittrack.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MuscleInsightScreen(
    appModule: AppModule,
    onBack: () -> Unit
) {
    val viewModel: MuscleInsightViewModel = viewModel(
        factory = MuscleInsightViewModel.Factory(appModule.mlAnalysisEngine)
    )

    val allInsights by viewModel.allInsights.collectAsState()
    val selectedInsight by viewModel.selectedInsight.collectAsState()
    val selectedMuscleGroup by viewModel.selectedMuscleGroup.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ML Insights") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            item {
                MuscleGroupChipRow(
                    selectedGroup = selectedMuscleGroup,
                    onGroupSelected = { viewModel.selectMuscleGroup(it) }
                )
            }

            if (isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Running ML analysis...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            } else if (selectedInsight != null) {
                // Detailed view for one muscle group
                val insight = selectedInsight!!
                item { InsightHeader(insight) }
                item { ScoreCard(insight) }
                item { TrendCard(insight) }
                item { RecoveryCard(insight) }

                if (insight.recommendations.isNotEmpty()) {
                    item {
                        Text(
                            "ML Recommendations",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    items(insight.recommendations) { rec ->
                        RecommendationCard(rec)
                    }
                }

                if (insight.exerciseInsights.isNotEmpty()) {
                    item {
                        Text(
                            "Per-Exercise Analysis",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    items(insight.exerciseInsights) { exInsight ->
                        ExerciseInsightCard(exInsight)
                    }
                }
            } else if (allInsights.isNotEmpty()) {
                // Overview of all muscle groups
                item {
                    Text(
                        "Muscle Group Scores",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                items(allInsights) { insight ->
                    OverviewCard(insight) { viewModel.selectMuscleGroup(insight.muscleGroup) }
                }
            } else {
                item {
                    EmptyState(
                        icon = Icons.Default.Psychology,
                        title = "No data for ML analysis",
                        subtitle = "Complete a few workouts to get ML-powered insights on your training"
                    )
                }
            }
        }
    }
}

@Composable
private fun InsightHeader(insight: MuscleInsight) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = insight.muscleGroup.displayName,
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = {}, label = { Text(insight.trainingPhase.label) })
                AssistChip(onClick = {}, label = { Text(insight.recoveryStatus.label) })
            }
        }
    }
}

@Composable
private fun ScoreCard(insight: MuscleInsight) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Overall Score", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ScoreItem("Score", "${insight.overallScore.toInt()}/100", scoreColor(insight.overallScore))
                ScoreItem("Balance", "${insight.balanceScore.toInt()}/100", scoreColor(insight.balanceScore))
                ScoreItem("Fatigue", "${insight.fatigueScore.toInt()}%", fatigueColor(insight.fatigueScore))
                ScoreItem("Sets/Wk", String.format("%.0f", insight.weeklyVolumeSets), MaterialTheme.colorScheme.primary)
            }
            if (insight.isPlateaued) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = Orange.copy(alpha = 0.15f))
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.TrendingFlat, contentDescription = null, tint = Orange, modifier = Modifier.size(18.dp))
                        Text(
                            "Plateau detected (${insight.plateauDurationSessions} sessions)",
                            style = MaterialTheme.typography.bodySmall,
                            color = Orange
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ScoreItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, color = color)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
    }
}

@Composable
private fun TrendCard(insight: MuscleInsight) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Trend Analysis", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(8.dp))

            TrendRow("Strength", insight.strengthTrend, insight.strengthRegression)
            Spacer(modifier = Modifier.height(4.dp))
            TrendRow("Volume", insight.volumeTrend, insight.volumeRegression)
        }
    }
}

@Composable
private fun TrendRow(label: String, trend: TrendDirection, regression: LinearRegressionResult?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when (trend) {
                    TrendDirection.IMPROVING, TrendDirection.SLIGHTLY_IMPROVING -> Icons.Default.TrendingUp
                    TrendDirection.DECLINING, TrendDirection.SLIGHTLY_DECLINING -> Icons.Default.TrendingDown
                    else -> Icons.Default.TrendingFlat
                },
                contentDescription = null,
                tint = trendColor(trend),
                modifier = Modifier.size(20.dp)
            )
            Text(label, style = MaterialTheme.typography.bodyMedium)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                trend.label,
                style = MaterialTheme.typography.labelMedium,
                color = trendColor(trend)
            )
            regression?.let {
                Text(
                    "R²=${String.format("%.2f", it.rSquared)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
private fun RecoveryCard(insight: MuscleInsight) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Recovery & Phase", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Recovery", style = MaterialTheme.typography.bodyMedium)
                Text(
                    insight.recoveryStatus.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = fatigueColor(insight.fatigueScore)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { (1f - insight.fatigueScore.toFloat() / 100f).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
                color = fatigueColor(insight.fatigueScore),
            )

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Training Phase", style = MaterialTheme.typography.bodyMedium)
                Text(insight.trainingPhase.label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
            Text(
                insight.trainingPhase.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun RecommendationCard(rec: MlRecommendation) {
    val (icon, color) = when (rec.type) {
        RecommendationType.INCREASE_WEIGHT -> Icons.Default.TrendingUp to Green
        RecommendationType.INCREASE_VOLUME -> Icons.Default.Add to Blue40
        RecommendationType.INCREASE_FREQUENCY -> Icons.Default.Repeat to Blue40
        RecommendationType.DELOAD -> Icons.Default.TrendingDown to Orange
        RecommendationType.CHANGE_EXERCISE -> Icons.Default.SwapHoriz to Teal40
        RecommendationType.PERIODIZATION_SHIFT -> Icons.Default.Autorenew to Blue40
        RecommendationType.MUSCLE_BALANCE -> Icons.Default.Balance to Orange
        RecommendationType.RECOVERY -> Icons.Default.Hotel to Red
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(rec.title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                    Text(
                        "${(rec.confidence * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    rec.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun ExerciseInsightCard(insight: ExerciseInsight) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(insight.exerciseName, style = MaterialTheme.typography.titleSmall)
                Icon(
                    when (insight.strengthTrend) {
                        TrendDirection.IMPROVING, TrendDirection.SLIGHTLY_IMPROVING -> Icons.Default.TrendingUp
                        TrendDirection.DECLINING, TrendDirection.SLIGHTLY_DECLINING -> Icons.Default.TrendingDown
                        else -> Icons.Default.TrendingFlat
                    },
                    contentDescription = null,
                    tint = trendColor(insight.strengthTrend),
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Est. 1RM: ${String.format("%.1f", insight.estimated1RM)}kg", style = MaterialTheme.typography.bodySmall)
                Text("Best: ${String.format("%.1f", insight.bestWeight)}kg", style = MaterialTheme.typography.bodySmall)
            }
            if (insight.rSquared > 0.2 && insight.predicted1RMNextSession > insight.estimated1RM) {
                Text(
                    "Predicted next: ${String.format("%.1f", insight.predicted1RMNextSession)}kg (${(insight.rSquared * 100).toInt()}% confidence)",
                    style = MaterialTheme.typography.labelSmall,
                    color = Green
                )
            }
            if (insight.isPlateaued) {
                Text(
                    "Plateaued - consider changing variation",
                    style = MaterialTheme.typography.labelSmall,
                    color = Orange
                )
            }
            Text(
                "${insight.sessionCount} sessions logged",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
private fun OverviewCard(insight: MuscleInsight, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(insight.muscleGroup.displayName, style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        insight.strengthTrend.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = trendColor(insight.strengthTrend)
                    )
                    Text(
                        "${String.format("%.0f", insight.weeklyVolumeSets)} sets/wk",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    if (insight.isPlateaued) {
                        Text("Plateaued", style = MaterialTheme.typography.labelSmall, color = Orange)
                    }
                }
                if (insight.recommendations.isNotEmpty()) {
                    Text(
                        "${insight.recommendations.size} recommendation${if (insight.recommendations.size != 1) "s" else ""}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "${insight.overallScore.toInt()}",
                    style = MaterialTheme.typography.headlineSmall,
                    color = scoreColor(insight.overallScore)
                )
                Text("Score", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }
        }
    }
}

@Composable
private fun trendColor(trend: TrendDirection): Color = when (trend) {
    TrendDirection.IMPROVING -> Green
    TrendDirection.SLIGHTLY_IMPROVING -> Green.copy(alpha = 0.7f)
    TrendDirection.FLAT, TrendDirection.NO_CLEAR_TREND -> Orange
    TrendDirection.SLIGHTLY_DECLINING -> Red.copy(alpha = 0.7f)
    TrendDirection.DECLINING -> Red
    TrendDirection.INSUFFICIENT_DATA -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
}

@Composable
private fun scoreColor(score: Double): Color = when {
    score >= 70 -> Green
    score >= 40 -> Orange
    else -> Red
}

@Composable
private fun fatigueColor(fatigue: Double): Color = when {
    fatigue < 25 -> Green
    fatigue < 50 -> Orange
    else -> Red
}
