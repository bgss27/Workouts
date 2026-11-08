package com.fittrack.app.ui.routines

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOff
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.ui.text.font.FontWeight
import com.fittrack.app.data.relation.RoutineWithExercises
import com.fittrack.app.ui.components.fitTextFieldColors
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fittrack.app.di.AppModule
import com.fittrack.app.ui.components.DetailHeader
import com.fittrack.app.ui.components.EmptyState
import com.fittrack.app.ui.components.MuscleGroupChipRow
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun RoutineListScreen(
    appModule: AppModule,
    onRoutineClick: (Long) -> Unit,
    onBack: () -> Unit
) {
    val viewModel: RoutinesViewModel = viewModel(
        factory = RoutinesViewModel.Factory(
            appModule.routineSuggestionEngine,
            appModule.exerciseRepository,
            appModule.userPlanRepository,
            appModule.routineRepository,
        )
    )

    val folders by viewModel.folders.collectAsState()
    // Long-pressed routine awaiting a folder choice; folder being renamed.
    var moveTarget by remember { mutableStateOf<RoutineWithExercises?>(null) }
    var renameTarget by remember { mutableStateOf<String?>(null) }
    var collapsedFolders by rememberSaveable { mutableStateOf(setOf<String>()) }

    val selectedMuscleGroup by viewModel.selectedMuscleGroup.collectAsState()
    val selectedDays by viewModel.selectedDaysPerWeek.collectAsState()
    val routines by viewModel.filteredRoutines.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.addedToast.collectLatest { message ->
            snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Short)
        }
    }

    Scaffold(
        topBar = {
            DetailHeader(title = "Workout Routines", onBack = onBack)
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            // Days per week filter
            item {
                Column {
                    Text(
                        text = "Days per Week",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(start = 16.dp, top = 8.dp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Row(
                        modifier = Modifier
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedDays == null,
                            onClick = { viewModel.selectDaysPerWeek(null) },
                            label = { Text("All") }
                        )
                        FilterChip(
                            selected = selectedDays == 3,
                            onClick = { viewModel.selectDaysPerWeek(if (selectedDays == 3) null else 3) },
                            label = { Text("3-Day") },
                            leadingIcon = if (selectedDays == 3) {
                                { Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp)) }
                            } else null
                        )
                        FilterChip(
                            selected = selectedDays == 5,
                            onClick = { viewModel.selectDaysPerWeek(if (selectedDays == 5) null else 5) },
                            label = { Text("5-Day") },
                            leadingIcon = if (selectedDays == 5) {
                                { Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp)) }
                            } else null
                        )
                        FilterChip(
                            selected = selectedDays == 0,
                            onClick = { viewModel.selectDaysPerWeek(if (selectedDays == 0) null else 0) },
                            label = { Text("Single Day") }
                        )
                    }
                }
            }

            // Muscle group filter
            item {
                MuscleGroupChipRow(
                    selectedGroup = selectedMuscleGroup,
                    onGroupSelected = { viewModel.selectMuscleGroup(it) }
                )
            }

            if (routines.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Default.List,
                        title = "No routines found",
                        subtitle = "Try selecting a different filter"
                    )
                }
            } else {
                // Group routines by program
                val programRoutines = routines.filter { it.routine.programName != null }
                    .groupBy { it.routine.programName!! }
                val standaloneRoutines = routines.filter { it.routine.programName == null }

                // Show program groups
                programRoutines.forEach { (programName, programDays) ->
                    val sortedDays = programDays.sortedBy { it.routine.dayOrder }
                    val daysCount = sortedDays.firstOrNull()?.routine?.daysPerWeek ?: 0
                    val difficulty = sortedDays.firstOrNull()?.routine?.difficulty ?: "intermediate"

                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = programName,
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            AssistChip(
                                                onClick = {},
                                                label = { Text("${daysCount}x/week") }
                                            )
                                            AssistChip(
                                                onClick = {},
                                                label = { Text(difficulty.replaceFirstChar { it.uppercase() }) }
                                            )
                                        }
                                    }
                                    FilledTonalButton(
                                        onClick = { viewModel.addProgramToPlan(programName) }
                                    ) {
                                        Icon(
                                            Icons.Default.EventNote,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Add to Plan")
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${sortedDays.size} workouts in this program",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }

                    items(sortedDays) { routineWithExercises ->
                        val routine = routineWithExercises.routine
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 32.dp, end = 16.dp, top = 2.dp, bottom = 2.dp)
                                .clickable { onRoutineClick(routine.id) }
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = routine.name,
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = routine.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${routineWithExercises.exercises.size} exercises",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                // Standalone routines: unfiled first, then one collapsible
                // section per folder. Long-press a routine to move it.
                if (standaloneRoutines.isNotEmpty()) {
                    item {
                        Text(
                            text = "Single Day Routines",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }

                    val unfiled = standaloneRoutines.filter { it.routine.folder.isNullOrBlank() }
                    val byFolder = standaloneRoutines
                        .filter { !it.routine.folder.isNullOrBlank() }
                        .groupBy { it.routine.folder!! }
                        .toSortedMap(String.CASE_INSENSITIVE_ORDER)

                    items(unfiled, key = { "r${it.routine.id}" }) { rwe ->
                        StandaloneRoutineCard(
                            routineWithExercises = rwe,
                            onClick = { onRoutineClick(rwe.routine.id) },
                            onLongClick = { moveTarget = rwe },
                            onAddToPlan = { viewModel.addStandaloneRoutineToPlan(rwe.routine.id, rwe.routine.name) },
                        )
                    }

                    byFolder.forEach { (folder, members) ->
                        val collapsed = folder in collapsedFolders
                        item(key = "f$folder") {
                            FolderHeader(
                                name = folder,
                                count = members.size,
                                collapsed = collapsed,
                                onToggle = {
                                    collapsedFolders = if (collapsed) collapsedFolders - folder else collapsedFolders + folder
                                },
                                onRename = { renameTarget = folder },
                            )
                        }
                        if (!collapsed) {
                            items(members, key = { "r${it.routine.id}" }) { rwe ->
                                StandaloneRoutineCard(
                                    routineWithExercises = rwe,
                                    indent = true,
                                    onClick = { onRoutineClick(rwe.routine.id) },
                                    onLongClick = { moveTarget = rwe },
                                    onAddToPlan = { viewModel.addStandaloneRoutineToPlan(rwe.routine.id, rwe.routine.name) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    moveTarget?.let { target ->
        MoveToFolderSheet(
            routineName = target.routine.name,
            currentFolder = target.routine.folder,
            folders = folders,
            onSelect = { folder ->
                viewModel.moveToFolder(target.routine.id, folder)
                moveTarget = null
            },
            onDismiss = { moveTarget = null },
        )
    }

    renameTarget?.let { old ->
        RenameFolderDialog(
            current = old,
            onRename = { viewModel.renameFolder(old, it); renameTarget = null },
            onDismiss = { renameTarget = null },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun StandaloneRoutineCard(
    routineWithExercises: RoutineWithExercises,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onAddToPlan: () -> Unit,
    indent: Boolean = false,
) {
    val routine = routineWithExercises.routine
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = if (indent) 32.dp else 16.dp, end = 16.dp, top = 4.dp, bottom = 4.dp)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = routine.name,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = onAddToPlan) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Add to My Plan",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    AssistChip(
                        onClick = {},
                        label = {
                            Text(
                                routine.difficulty.replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = routine.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${routineWithExercises.exercises.size} exercises",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

/** Collapsible folder header with a rename action in its overflow menu. */
@Composable
private fun FolderHeader(
    name: String,
    count: Int,
    collapsed: Boolean,
    onToggle: () -> Unit,
    onRename: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "$count",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Icon(
            if (collapsed) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
            contentDescription = if (collapsed) "Expand" else "Collapse",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Box {
            IconButton(onClick = { menuOpen = true }, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.MoreVert, contentDescription = "Folder options", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text("Rename folder") },
                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                    onClick = { menuOpen = false; onRename() },
                )
            }
        }
    }
}

/** Existing folders, a new-folder field, and "Remove from folder". */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MoveToFolderSheet(
    routineName: String,
    currentFolder: String?,
    folders: List<String>,
    onSelect: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    var newName by remember { mutableStateOf("") }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text("Move to folder", style = MaterialTheme.typography.titleMedium)
            Text(
                routineName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(8.dp))
            folders.forEach { folder ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(folder) }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                    Text(folder, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    if (folder == currentFolder) {
                        Text("Current", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TextField(
                    value = newName,
                    onValueChange = { newName = it },
                    placeholder = { Text("New folder") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.small,
                    colors = fitTextFieldColors(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    modifier = Modifier.weight(1f),
                )
                FilledTonalButton(
                    onClick = { onSelect(newName.trim()) },
                    enabled = newName.isNotBlank(),
                ) { Text("Create") }
            }
            if (currentFolder != null) {
                TextButton(onClick = { onSelect(null) }) {
                    Icon(Icons.Default.FolderOff, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Remove from folder")
                }
            }
        }
    }
}

@Composable
private fun RenameFolderDialog(current: String, onRename: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(current) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename folder") },
        text = {
            TextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                shape = MaterialTheme.shapes.small,
                colors = fitTextFieldColors(),
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = { onRename(name.trim()) }, enabled = name.isNotBlank()) { Text("Rename") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
