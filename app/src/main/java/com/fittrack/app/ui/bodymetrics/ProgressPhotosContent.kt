package com.fittrack.app.ui.bodymetrics

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.fittrack.app.data.entity.PhotoPose
import com.fittrack.app.data.entity.ProgressPhoto
import com.fittrack.app.data.repository.ProgressPhotoRepository
import com.fittrack.app.ui.components.EmptyState
import com.fittrack.app.ui.components.PillChip
import com.fittrack.app.ui.components.PillChipRow
import com.fittrack.app.ui.components.PrimaryButton
import com.fittrack.app.ui.components.ScreenGutter
import com.fittrack.app.ui.components.TonalButton
import com.fittrack.app.ui.components.fitTextFieldColors
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class ProgressPhotosViewModel(private val repository: ProgressPhotoRepository) : ViewModel() {
    /** Newest first. */
    val photos: StateFlow<List<ProgressPhoto>> = repository.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    fun fileFor(photo: ProgressPhoto): File = repository.fileFor(photo)

    fun add(uri: Uri, takenAt: Long, pose: PhotoPose, notes: String?) {
        viewModelScope.launch {
            val id = repository.import(uri, takenAt, pose, notes)
            if (id == null) _messages.emit("Couldn't read that photo")
        }
    }

    fun update(photo: ProgressPhoto) {
        viewModelScope.launch { repository.update(photo) }
    }

    fun delete(photo: ProgressPhoto) {
        viewModelScope.launch { repository.delete(photo) }
    }

    class Factory(private val repository: ProgressPhotoRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = ProgressPhotosViewModel(repository) as T
    }
}

/**
 * The Photos tab of Body Metrics: a month-grouped grid, camera / library
 * import, a swipeable viewer and a two-photo side-by-side compare.
 */
@Composable
fun ProgressPhotosContent(
    repository: ProgressPhotoRepository,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState? = null,
) {
    val context = LocalContext.current
    val viewModel: ProgressPhotosViewModel = viewModel(factory = ProgressPhotosViewModel.Factory(repository))
    val photos by viewModel.photos.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { snackbarHostState?.showSnackbar(it) }
    }

    // A freshly picked/captured image waiting for date, pose and notes.
    var pendingUri by remember { mutableStateOf<Uri?>(null) }
    var editing by remember { mutableStateOf<ProgressPhoto?>(null) }
    var viewing by remember { mutableStateOf<ProgressPhoto?>(null) }
    var pendingDelete by remember { mutableStateOf<ProgressPhoto?>(null) }
    var compareMode by rememberSaveable { mutableStateOf(false) }
    var compareIds by remember { mutableStateOf(listOf<Long>()) }
    var showCompare by remember { mutableStateOf(false) }

    val pickLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) pendingUri = uri
    }
    var cameraUri by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok) pendingUri = cameraUri
    }
    fun takePhoto() {
        val uri = captureUri(context)
        cameraUri = uri
        runCatching { cameraLauncher.launch(uri) }
            .onFailure { viewModel.viewModelScope.launch { snackbarHostState?.showSnackbar("No camera app available") } }
    }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = ScreenGutter, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TonalButton(text = "Take photo", icon = Icons.Default.PhotoCamera, onClick = { takePhoto() }, modifier = Modifier.weight(1f))
            TonalButton(
                text = "Library",
                icon = Icons.Default.PhotoLibrary,
                onClick = { pickLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                modifier = Modifier.weight(1f),
            )
        }
        if (photos.size >= 2) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = ScreenGutter),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    if (compareMode) "Tap two photos to compare" else "${photos.size} photos",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { compareMode = !compareMode; compareIds = emptyList() }) {
                    Icon(Icons.Default.Compare, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (compareMode) "Done" else "Compare")
                }
            }
        }

        if (photos.isEmpty()) {
            EmptyState(
                icon = Icons.Default.PhotoCamera,
                title = "No progress photos yet",
                subtitle = "Take a photo in the same spot and light each month. Photos are stored only on this device and are never uploaded.",
            )
            return@Column
        }

        val zone = remember { ZoneId.systemDefault() }
        val monthFmt = remember { DateTimeFormatter.ofPattern("MMMM yyyy") }
        val groups = remember(photos) {
            photos.groupBy { Instant.ofEpochMilli(it.takenAt).atZone(zone).toLocalDate().withDayOfMonth(1) }
                .toSortedMap(compareByDescending { it })
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = ScreenGutter, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            groups.forEach { (month, items) ->
                item(span = { GridItemSpan(maxLineSpan) }, key = "h-$month") {
                    Text(
                        month.format(monthFmt),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
                    )
                }
                items(items, key = { it.id }) { photo ->
                    PhotoTile(
                        photo = photo,
                        file = viewModel.fileFor(photo),
                        selected = photo.id in compareIds,
                        onClick = {
                            if (compareMode) {
                                compareIds = when {
                                    photo.id in compareIds -> compareIds - photo.id
                                    compareIds.size < 2 -> compareIds + photo.id
                                    else -> listOf(compareIds.last(), photo.id)
                                }
                                if (compareIds.size == 2) showCompare = true
                            } else {
                                viewing = photo
                            }
                        },
                    )
                }
            }
            item(span = { GridItemSpan(maxLineSpan) }) { Spacer(Modifier.height(24.dp)) }
        }
    }

    // Details for a new photo.
    pendingUri?.let { uri ->
        PhotoDetailsSheet(
            title = "New progress photo",
            initialDate = LocalDate.now(),
            initialPose = PhotoPose.FRONT,
            initialNotes = "",
            onDismiss = { pendingUri = null },
            onSave = { date, pose, notes ->
                val millis = date.atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                viewModel.add(uri, millis, pose, notes)
                pendingUri = null
            },
        )
    }

    // Edit an existing photo's details.
    editing?.let { photo ->
        PhotoDetailsSheet(
            title = "Edit photo",
            initialDate = Instant.ofEpochMilli(photo.takenAt).atZone(ZoneId.systemDefault()).toLocalDate(),
            initialPose = photo.pose,
            initialNotes = photo.notes.orEmpty(),
            onDismiss = { editing = null },
            onSave = { date, pose, notes ->
                val millis = date.atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                viewModel.update(photo.copy(takenAt = millis, pose = pose, notes = notes))
                editing = null
                viewing = null
            },
        )
    }

    viewing?.let { start ->
        PhotoViewer(
            photos = photos,
            startId = start.id,
            fileFor = viewModel::fileFor,
            onDismiss = { viewing = null },
            onEdit = { editing = it },
            onDelete = { pendingDelete = it },
        )
    }

    pendingDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete photo?") },
            text = { Text("The ${formatDay(target.takenAt)} photo is removed from this device. This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(target)
                    pendingDelete = null
                    viewing = null
                    compareIds = compareIds - target.id
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Cancel") } },
        )
    }

    if (showCompare && compareIds.size == 2) {
        val pair = compareIds.mapNotNull { id -> photos.firstOrNull { it.id == id } }.sortedBy { it.takenAt }
        if (pair.size == 2) {
            CompareDialog(
                before = pair[0],
                after = pair[1],
                fileFor = viewModel::fileFor,
                onDismiss = { showCompare = false },
            )
        } else {
            showCompare = false
        }
    }
}

private fun captureUri(context: Context): Uri {
    val dir = File(context.cacheDir, "camera").apply { mkdirs() }
    val file = File(dir, "capture_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

private fun formatDay(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDate()
        .format(DateTimeFormatter.ofPattern("MMM d, yyyy"))

@Composable
private fun PhotoTile(photo: ProgressPhoto, file: File, selected: Boolean, onClick: () -> Unit) {
    val shape = MaterialTheme.shapes.small
    Box(
        modifier = Modifier
            .aspectRatio(3f / 4f)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .then(if (selected) Modifier.border(3.dp, MaterialTheme.colorScheme.primary, shape) else Modifier)
            .clickable(onClick = onClick),
    ) {
        AsyncImage(
            model = file,
            contentDescription = "${photo.pose.displayName} progress photo, ${formatDay(photo.takenAt)}",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.45f))
                .padding(horizontal = 6.dp, vertical = 4.dp),
        ) {
            Text(
                Instant.ofEpochMilli(photo.takenAt).atZone(ZoneId.systemDefault()).toLocalDate()
                    .format(DateTimeFormatter.ofPattern("MMM d")),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Text(
            photo.pose.displayName.take(1),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
                .padding(horizontal = 6.dp, vertical = 1.dp),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PhotoDetailsSheet(
    title: String,
    initialDate: LocalDate,
    initialPose: PhotoPose,
    initialNotes: String,
    onDismiss: () -> Unit,
    onSave: (LocalDate, PhotoPose, String?) -> Unit,
) {
    var date by remember { mutableStateOf(initialDate) }
    var pose by remember { mutableStateOf(initialPose) }
    var notes by remember { mutableStateOf(initialNotes) }
    var showDatePicker by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = ScreenGutter)
                .padding(bottom = 24.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .clickable { showDatePicker = true }
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(Icons.Default.CalendarToday, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Text(date.format(DateTimeFormatter.ofPattern("EEE, MMM d, yyyy")), style = MaterialTheme.typography.bodyLarge)
            }
            Text("Pose", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            PillChipRow(horizontalPadding = 0.dp) {
                PhotoPose.entries.forEach { p ->
                    PillChip(label = p.displayName, selected = pose == p, onClick = { pose = p })
                }
            }
            TextField(
                value = notes,
                onValueChange = { notes = it },
                placeholder = { Text("Notes (optional)") },
                shape = MaterialTheme.shapes.small,
                colors = fitTextFieldColors(),
                maxLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(4.dp))
            PrimaryButton(text = "Save", onClick = { onSave(date, pose, notes.trim().takeIf { it.isNotEmpty() }) })
            TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) { Text("Cancel") }
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                    utcTimeMillis <= LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
            },
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { ms ->
                        date = Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate()
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } },
        ) { DatePicker(state = pickerState) }
    }
}

@Composable
private fun PhotoViewer(
    photos: List<ProgressPhoto>,
    startId: Long,
    fileFor: (ProgressPhoto) -> File,
    onDismiss: () -> Unit,
    onEdit: (ProgressPhoto) -> Unit,
    onDelete: (ProgressPhoto) -> Unit,
) {
    val start = photos.indexOfFirst { it.id == startId }.coerceAtLeast(0)
    val pagerState = rememberPagerState(initialPage = start) { photos.size }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                val photo = photos[page]
                AsyncImage(
                    model = fileFor(photo),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            val current = photos.getOrNull(pagerState.currentPage)
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White) }
                Spacer(Modifier.weight(1f))
                if (current != null) {
                    IconButton(onClick = { onEdit(current) }) { Icon(Icons.Default.Edit, contentDescription = "Edit details", tint = Color.White) }
                    IconButton(onClick = { onDelete(current) }) { Icon(Icons.Default.Delete, contentDescription = "Delete photo", tint = Color.White) }
                }
            }
            if (current != null) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.55f))
                        .navigationBarsPadding()
                        .padding(16.dp),
                ) {
                    Text(
                        "${formatDay(current.takenAt)} · ${current.pose.displayName}",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White,
                    )
                    current.notes?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f))
                    }
                    Text(
                        "${pagerState.currentPage + 1} of ${photos.size}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.6f),
                    )
                }
            }
        }
    }
}

@Composable
private fun CompareDialog(
    before: ProgressPhoto,
    after: ProgressPhoto,
    fileFor: (ProgressPhoto) -> File,
    onDismiss: () -> Unit,
) {
    val zone = ZoneId.systemDefault()
    val days = ChronoUnit.DAYS.between(
        Instant.ofEpochMilli(before.takenAt).atZone(zone).toLocalDate(),
        Instant.ofEpochMilli(after.takenAt).atZone(zone).toLocalDate(),
    )
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White) }
                Text(
                    "$days ${if (days == 1L) "day" else "days"} apart",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                )
            }
            Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
                listOf(before, after).forEach { photo ->
                    Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        AsyncImage(
                            model = fileFor(photo),
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxWidth().weight(1f),
                        )
                        Text(
                            "${formatDay(photo.takenAt)} · ${photo.pose.displayName}",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            modifier = Modifier.padding(8.dp).navigationBarsPadding(),
                        )
                    }
                }
            }
        }
    }
}
