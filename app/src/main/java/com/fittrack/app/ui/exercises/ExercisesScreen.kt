package com.fittrack.app.ui.exercises

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fittrack.app.billing.FreeLimits
import com.fittrack.app.billing.ProManager
import com.fittrack.app.data.entity.Exercise
import com.fittrack.app.ui.components.CustomExerciseSheet
import com.fittrack.app.data.entity.MuscleGroup
import com.fittrack.app.di.AppModule
import com.fittrack.app.domain.model.ExerciseGuideData
import com.fittrack.app.data.repository.ExerciseImageRepository
import com.fittrack.app.data.repository.OpenExerciseDb
import androidx.compose.ui.platform.LocalContext
import com.fittrack.app.ui.components.ExerciseImageGallery
import com.fittrack.app.ui.components.IconBadge
import com.fittrack.app.ui.components.MuscleGroupChipRow
import com.fittrack.app.ui.components.ScreenGutter
import com.fittrack.app.ui.components.ScreenHeader
import com.fittrack.app.ui.components.Tile
import com.fittrack.app.ui.components.fitTextFieldColors

@Composable
fun ExercisesScreen(
    appModule: AppModule,
    proManager: ProManager,
    onUpgrade: () -> Unit,
) {
    val viewModel: ExercisesViewModel = viewModel(
        factory = ExercisesViewModel.Factory(appModule.exerciseRepository)
    )

    val selectedMuscleGroup by viewModel.selectedMuscleGroup.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val exercises by viewModel.exercises.collectAsState()
    val groupedExercises by viewModel.groupedExercises.collectAsState()
    val customCount by viewModel.customCount.collectAsState()
    val isPro by proManager.isPro.collectAsState()

    val showGrouped = selectedMuscleGroup == null && searchQuery.isBlank()

    // null = sheet closed; Exercise(id = 0) = creating; otherwise editing.
    var editing by remember { mutableStateOf<Exercise?>(null) }
    var creating by remember { mutableStateOf(false) }
    val limitReached = !isPro && customCount >= FreeLimits.CUSTOM_EXERCISES

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(viewModel) {
        viewModel.messages.collect { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            ScreenHeader(
                title = "Exercises",
                eyebrow = if (exercises.size == 1) "1 exercise" else "${exercises.size} exercises",
                actions = {
                    IconButton(onClick = { creating = true }) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "New custom exercise",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        }
    ) { padding ->
        // Horizontal gutter is applied per item (not via contentPadding) so the
        // muscle-group pill row can scroll edge to edge.
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Search bar
            item {
                TextField(
                    value = searchQuery,
                    onValueChange = { viewModel.search(it) },
                    placeholder = { Text("Search exercises") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = ScreenGutter),
                    singleLine = true,
                    shape = MaterialTheme.shapes.small,
                    colors = fitTextFieldColors(),
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.search("") }) {
                                Icon(
                                    Icons.Default.Clear,
                                    contentDescription = "Clear",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                )
            }

            // Muscle group filter ("All" pill first, then each group)
            item {
                MuscleGroupChipRow(
                    selectedGroup = selectedMuscleGroup,
                    onGroupSelected = { viewModel.selectMuscleGroup(it) }
                )
            }

            if (showGrouped) {
                // Grouped by muscle
                val sortedGroups = groupedExercises.entries.sortedBy { it.key.displayName }
                sortedGroups.forEach { (group, groupExercises) ->
                    item {
                        MuscleGroupHeader(group, groupExercises.size)
                    }
                    items(groupExercises.sortedBy { it.name }) { exercise ->
                        ExerciseDetailCard(exercise, onEdit = { editing = exercise })
                    }
                }
            } else {
                // Flat filtered list
                items(exercises.sortedBy { it.name }) { exercise ->
                    ExerciseDetailCard(exercise, onEdit = { editing = exercise })
                }
            }
        }
    }

    if (creating || editing != null) {
        CustomExerciseSheet(
            initial = editing,
            limitReached = limitReached,
            onSave = { viewModel.saveCustom(it); creating = false; editing = null },
            onDismiss = { creating = false; editing = null },
            onUpgrade = { creating = false; editing = null; onUpgrade() },
            onDelete = editing?.let { ex -> { viewModel.deleteCustom(ex); editing = null } },
        )
    }
}

@Composable
private fun MuscleGroupHeader(group: MuscleGroup, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = ScreenGutter)
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconBadge(
            icon = Icons.Default.FitnessCenter,
            tint = MaterialTheme.colorScheme.primary,
            size = 36.dp
        )
        Text(
            text = group.displayName,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f)
        )
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                text = "$count",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ExerciseDetailCard(exercise: Exercise, onEdit: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "chevronRotation"
    )

    Tile(
        modifier = Modifier.padding(horizontal = ScreenGutter),
        onClick = { expanded = !expanded }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = exercise.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = exercise.muscleGroup.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (exercise.isCustom) {
                        Text(
                            text = "CUSTOM",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.16f))
                                .padding(horizontal = 6.dp, vertical = 1.dp)
                        )
                    }
                }
            }
            if (exercise.isCustom) {
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit exercise",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Icon(
                Icons.Default.ExpandMore,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.rotate(chevronRotation)
            )
        }

        AnimatedVisibility(visible = expanded) {
            val context = LocalContext.current
            // Hand-written guides win; dataset entries (the imported catalog)
            // use their own step-by-step text and skip the generic cues.
            val curated = remember(exercise.name) { ExerciseGuideData.hasGuide(exercise.name) }
            val guide = ExerciseGuideData.getGuide(exercise.name)
            val datasetSteps = remember(exercise.name) {
                if (curated) null
                else OpenExerciseDb.getInstructions(context, exercise.name)
                    ?.split("\n\n")?.map { it.trim() }?.filter { it.isNotEmpty() }
            }
            val steps = datasetSteps ?: guide.steps
            val hasImages = remember(exercise.name) {
                ExerciseImageRepository.getExerciseImages(exercise.name).isNotEmpty()
            }
            val muted = MaterialTheme.colorScheme.onSurfaceVariant

            Column(modifier = Modifier.padding(top = 12.dp)) {
                // Hairline between header and detail
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )
                Spacer(modifier = Modifier.height(12.dp))

                if (hasImages) {
                    ExerciseImageGallery(exerciseName = exercise.name)
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Muscles
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Circle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(12.dp))
                    Text("Primary:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Text(exercise.muscleGroup.displayName, style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Circle, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(12.dp))
                    Text("Secondary:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Text(
                        exercise.secondaryMuscleGroup?.displayName ?: "None",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (exercise.secondaryMuscleGroup != null) MaterialTheme.colorScheme.onSurface else muted
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = muted, modifier = Modifier.size(12.dp))
                    Text("Type:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Text(if (exercise.secondaryMuscleGroup != null) "Compound" else "Isolation", style = MaterialTheme.typography.bodyMedium)
                }

                Spacer(modifier = Modifier.height(12.dp))

                // How to perform
                Text("How to Perform", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                steps.forEachIndexed { i, step ->
                    InstructionStepRow(number = i + 1, text = step)
                }

                if (curated || datasetSteps == null) {
                Spacer(modifier = Modifier.height(8.dp))

                // Breathing
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Air, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(14.dp))
                    Text("Breathing:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
                Text(guide.breathingCue, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 20.dp))

                Spacer(modifier = Modifier.height(8.dp))

                // Tempo
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Timer, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(14.dp))
                    Text("Tempo:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Text(guide.tempo, style = MaterialTheme.typography.bodySmall)
                    if (guide.tempo.contains("-")) {
                        Text("(eccentric-pause-concentric)", style = MaterialTheme.typography.bodySmall, color = muted)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Common mistakes
                Text("Common Mistakes", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(4.dp))
                guide.commonMistakes.forEach { mistake ->
                    Row(modifier = Modifier.padding(vertical = 1.dp)) {
                        Text("  ✗  ", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                        Text(mistake, style = MaterialTheme.typography.bodySmall)
                    }
                }
                }
            }
        }
    }
}

/** Numbered instruction row: 22dp tinted circle with the step number, then the step text. */
@Composable
private fun InstructionStepRow(number: Int, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$number",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f)
        )
    }
}
