package com.fittrack.app.ui.progress

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fittrack.app.billing.ProManager
import com.fittrack.app.data.WeightUnits
import com.fittrack.app.di.AppModule
import com.fittrack.app.ui.components.EmptyState
import com.fittrack.app.ui.components.MuscleGroupChipRow
import com.fittrack.app.ui.components.ProgressChart
import com.fittrack.app.ui.components.Sparkline

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressScreen(
    appModule: AppModule,
    isPro: Boolean,
    onUpgrade: () -> Unit,
    onBack: () -> Unit
) {
    val viewModel: ProgressViewModel = viewModel(
        factory = ProgressViewModel.Factory(
            appModule.exerciseRepository,
            appModule.progressAnalyzer
        )
    )

    val selectedMuscleGroup by viewModel.selectedMuscleGroup.collectAsState()
    val exercises by viewModel.exercises.collectAsState()
    val selectedExercise by viewModel.selectedExercise.collectAsState()
    val progressData by viewModel.progressData.collectAsState()
    val sparklineData by viewModel.sparklineData.collectAsState()

    val context = LocalContext.current
    val unit = remember { WeightUnits.preferred(context) }

    val historyCutoff = remember {
        System.currentTimeMillis() - ProManager.FREE_HISTORY_DAYS * 24L * 60 * 60 * 1000
    }
    val displayedData = if (isPro) progressData else progressData.filter { it.date >= historyCutoff }
    val isClamped = !isPro && progressData.size > displayedData.size

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Progress") },
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

            // Chart area
            if (selectedExercise != null && displayedData.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Column {
                            ProgressChart(
                                dataPoints = displayedData,
                                label = "Estimated 1RM ($unit) - ${selectedExercise!!.name}",
                                valueSelector = { WeightUnits.fromKg(it.estimated1RM, unit) }
                            )
                            HorizontalDivider()
                            ProgressChart(
                                dataPoints = displayedData,
                                label = "Total Volume ($unit)",
                                valueSelector = { WeightUnits.fromKg(it.totalVolume, unit) }
                            )
                        }
                    }
                }
                if (isClamped) {
                    item {
                        FullHistoryUpgradeCta(
                            hiddenCount = progressData.size - displayedData.size,
                            unit = "data points",
                            onUpgrade = onUpgrade
                        )
                    }
                }
            } else if (selectedExercise != null) {
                item {
                    EmptyState(
                        icon = Icons.Default.ShowChart,
                        title = "No data yet",
                        subtitle = "Complete some workouts with ${selectedExercise!!.name} to see your progress",
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            // Exercise list
            item {
                Text(
                    text = "Select an exercise to view progress",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            items(exercises) { exercise ->
                val spark = sparklineData[exercise.id]
                ListItem(
                    headlineContent = { Text(exercise.name) },
                    supportingContent = {
                        Text(
                            buildString {
                                append(exercise.muscleGroup.displayName)
                                exercise.secondaryMuscleGroup?.let { append(" / ${it.displayName}") }
                            }
                        )
                    },
                    trailingContent = if (spark != null) {
                        {
                            Sparkline(
                                values = spark.map { WeightUnits.fromKg(it, unit) },
                                modifier = Modifier
                                    .width(56.dp)
                                    .height(24.dp),
                            )
                        }
                    } else null,
                    modifier = Modifier.clickable { viewModel.selectExercise(exercise) },
                    colors = if (selectedExercise?.id == exercise.id) {
                        ListItemDefaults.colors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    } else {
                        ListItemDefaults.colors()
                    }
                )
            }
        }
    }
}

@Composable
private fun FullHistoryUpgradeCta(
    hiddenCount: Int,
    unit: String,
    onUpgrade: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Showing the last ${ProManager.FREE_HISTORY_DAYS} days",
                style = MaterialTheme.typography.titleSmall
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "$hiddenCount older $unit are hidden. Unlock full history with Pro.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = onUpgrade) {
                Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Unlock with Pro")
            }
        }
    }
}
