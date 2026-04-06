package com.fittrack.app.ui.suggestions

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fittrack.app.di.AppModule
import com.fittrack.app.ui.components.EmptyState
import com.fittrack.app.ui.components.MuscleGroupChipRow
import com.fittrack.app.ui.components.SuggestionCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuggestionsScreen(
    appModule: AppModule,
    onBack: () -> Unit
) {
    val viewModel: SuggestionsViewModel = viewModel(
        factory = SuggestionsViewModel.Factory(
            appModule.progressAnalyzer,
            appModule.routineSuggestionEngine
        )
    )

    val selectedMuscleGroup by viewModel.selectedMuscleGroup.collectAsState()
    val suggestions by viewModel.suggestions.collectAsState()
    val exerciseSuggestions by viewModel.exerciseSuggestions.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Improvement Tips") },
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
                        contentAlignment = androidx.compose.ui.Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            } else {
                // Progress-based suggestions
                if (suggestions.isNotEmpty()) {
                    item {
                        Text(
                            text = "Based on Your Progress",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    items(suggestions) { suggestion ->
                        SuggestionCard(
                            suggestion = suggestion,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }

                // Exercise suggestions for selected muscle group
                if (exerciseSuggestions.isNotEmpty()) {
                    item {
                        Text(
                            text = "Recommended Exercises",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    items(exerciseSuggestions) { suggestion ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = suggestion.exercise.name,
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = "${suggestion.suggestedSets} sets x ${suggestion.suggestedReps} reps",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = suggestion.reason,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }

                if (suggestions.isEmpty() && exerciseSuggestions.isEmpty()) {
                    item {
                        EmptyState(
                            icon = Icons.Default.Lightbulb,
                            title = "No suggestions yet",
                            subtitle = if (selectedMuscleGroup != null) {
                                "Complete a few workouts targeting ${selectedMuscleGroup!!.displayName} to get personalized suggestions"
                            } else {
                                "Complete at least 3 sessions of an exercise to get improvement suggestions"
                            }
                        )
                    }
                }
            }
        }
    }
}
