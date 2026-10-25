package com.fittrack.app.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fittrack.app.billing.ProManager
import com.fittrack.app.data.ThemePrefs
import com.fittrack.app.data.health.HealthConnectAvailability
import com.fittrack.app.di.AppModule
import com.fittrack.app.ui.components.ListRow
import com.fittrack.app.ui.components.Overline
import com.fittrack.app.ui.components.PillChip
import com.fittrack.app.ui.components.RowDivider
import com.fittrack.app.ui.components.ScreenGutter
import com.fittrack.app.ui.components.ScreenHeader
import com.fittrack.app.ui.components.Tile
import com.fittrack.app.ui.components.fitTextFieldColors
import com.fittrack.app.ui.theme.ThemeMode
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    appModule: AppModule,
    proManager: ProManager,
    onNavigateToUpgrade: () -> Unit,
    onNavigateToBodyMetrics: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModel.Factory(context, proManager)
    )

    val profile by viewModel.profile.collectAsState()
    val isPro by proManager.isPro.collectAsState()
    val themeMode by ThemePrefs.mode.collectAsState()

    val healthConnect = appModule.healthConnectService
    val hcEnabled by healthConnect.enabled.collectAsState()
    val hcGranted by healthConnect.permissionsGranted.collectAsState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) { healthConnect.refreshPermissions() }

    val healthConnectLauncher = rememberLauncherForActivityResult(
        contract = healthConnect.permissionRequestContract()
    ) { granted ->
        scope.launch {
            healthConnect.refreshPermissions()
            if (granted.containsAll(healthConnect.permissions)) {
                healthConnect.setEnabled(true)
            }
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }

    val csvExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val csv = appModule.csvExporter.export()
            val ok = runCatching {
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    stream.write(csv.toByteArray(Charsets.UTF_8))
                }
            }.isSuccess
            snackbarHostState.showSnackbar(
                if (ok) "Workouts exported." else "Export failed.",
                duration = SnackbarDuration.Short,
            )
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { ScreenHeader(title = "Settings") },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = ScreenGutter, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Profile hero
            item {
                ProfileHero(
                    name = profile.name,
                    isPro = isPro,
                    onUpgrade = onNavigateToUpgrade,
                )
            }

            // Profile
            item {
                SettingsGroup(label = "Profile") {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        TextField(
                            value = profile.name,
                            onValueChange = { viewModel.updateProfile(profile.copy(name = it)) },
                            placeholder = { Text("Name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = MaterialTheme.shapes.small,
                            colors = fitTextFieldColors(),
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            },
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TextField(
                                value = profile.age,
                                onValueChange = { viewModel.updateProfile(profile.copy(age = it.filter { c -> c.isDigit() })) },
                                placeholder = { Text("Age") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = MaterialTheme.shapes.small,
                                colors = fitTextFieldColors(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                            TextField(
                                value = profile.weightKg,
                                onValueChange = { viewModel.updateProfile(profile.copy(weightKg = it.filter { c -> c.isDigit() || c == '.' })) },
                                placeholder = { Text("Wt ${profile.weightUnit}", maxLines = 1) },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = MaterialTheme.shapes.small,
                                colors = fitTextFieldColors(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                            )
                            TextField(
                                value = profile.heightCm,
                                onValueChange = { viewModel.updateProfile(profile.copy(heightCm = it.filter { c -> c.isDigit() })) },
                                placeholder = { Text("Ht cm", maxLines = 1) },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = MaterialTheme.shapes.small,
                                colors = fitTextFieldColors(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                        }
                    }
                    RowDivider()
                    DropdownRow(
                        title = "Gender",
                        value = profile.gender,
                        icon = Icons.Default.Wc,
                        options = listOf("Male", "Female", "Not specified"),
                        onSelect = { viewModel.updateProfile(profile.copy(gender = it)) },
                    )
                    RowDivider()
                    // Body metrics (Pro) — the weight field above is a one-off
                    // profile value; this is the dated history with trends.
                    ListRow(
                        title = "Body Metrics",
                        subtitle = "Weight, body fat, and measurements over time",
                        icon = Icons.Default.MonitorWeight,
                        onClick = onNavigateToBodyMetrics,
                        trailing = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                if (!isPro) {
                                    Icon(
                                        Icons.Default.Lock,
                                        contentDescription = "Pro feature",
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.tertiary,
                                    )
                                }
                                SmallChevron()
                            }
                        },
                    )
                }
            }

            // Fitness preferences
            item {
                SettingsGroup(label = "Fitness preferences") {
                    DropdownRow(
                        title = "Fitness goal",
                        value = profile.fitnessGoal,
                        icon = Icons.Default.FitnessCenter,
                        options = listOf("Build Muscle", "Lose Weight", "Gain Strength", "Improve Endurance", "General Fitness"),
                        onSelect = { viewModel.updateProfile(profile.copy(fitnessGoal = it)) },
                    )
                    RowDivider()
                    DropdownRow(
                        title = "Experience level",
                        value = profile.experienceLevel,
                        icon = Icons.Default.TrendingUp,
                        options = listOf("Beginner", "Intermediate", "Advanced"),
                        onSelect = { viewModel.updateProfile(profile.copy(experienceLevel = it)) },
                    )
                }
            }

            // Units
            item {
                SettingsGroup(label = "Units") {
                    ListRow(
                        title = "Weight unit",
                        subtitle = "Used for logging sets",
                        icon = Icons.Default.Scale,
                        trailing = {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf("kg", "lbs").forEach { u ->
                                    PillChip(
                                        label = u,
                                        selected = profile.weightUnit == u,
                                        onClick = { viewModel.updateProfile(profile.copy(weightUnit = u)) },
                                    )
                                }
                            }
                        },
                    )
                }
            }

            // Appearance
            item {
                SettingsGroup(label = "Appearance") {
                    ListRow(
                        title = "Theme",
                        subtitle = when (themeMode) {
                            ThemeMode.SYSTEM -> "Follows your device setting"
                            ThemeMode.DARK -> "Always dark"
                            ThemeMode.LIGHT -> "Always light"
                        },
                        icon = Icons.Default.Palette,
                        iconTint = MaterialTheme.colorScheme.secondary,
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        listOf(
                            ThemeMode.SYSTEM to "System",
                            ThemeMode.DARK to "Dark",
                            ThemeMode.LIGHT to "Light",
                        ).forEach { (mode, label) ->
                            PillChip(
                                label = label,
                                selected = themeMode == mode,
                                onClick = { ThemePrefs.set(context, mode) },
                            )
                        }
                    }
                }
            }

            // Subscription
            item {
                SettingsGroup(label = "Subscription") {
                    ListRow(
                        title = if (isPro) "Pro Member" else "Upgrade to Pro",
                        subtitle = if (isPro) "All features unlocked"
                        else "ML insights, all programs, and more",
                        icon = Icons.Default.Star,
                        iconTint = if (isPro) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.tertiary,
                        onClick = onNavigateToUpgrade,
                        trailing = { SmallChevron() },
                    )
                }
            }

            // Integrations
            if (healthConnect.availability != HealthConnectAvailability.UNSUPPORTED) {
                item {
                    SettingsGroup(label = "Integrations") {
                        HealthConnectRow(
                            isPro = isPro,
                            availability = healthConnect.availability,
                            granted = hcGranted,
                            enabled = hcEnabled,
                            onUpgrade = onNavigateToUpgrade,
                            onConnect = { healthConnectLauncher.launch(healthConnect.permissions) },
                            onToggle = { healthConnect.setEnabled(it) },
                            onInstall = {
                                val uri = Uri.parse("market://details?id=com.google.android.apps.healthdata")
                                runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
                            },
                        )
                    }
                }
            }

            // Data
            item {
                SettingsGroup(label = "Data") {
                    CsvExportRow(
                        isPro = isPro,
                        onUpgrade = onNavigateToUpgrade,
                        onExport = {
                            val name = "fittrack-workouts-${System.currentTimeMillis()}.csv"
                            csvExportLauncher.launch(name)
                        },
                    )
                }
            }

            // About
            item {
                SettingsGroup(label = "About") {
                    ListRow(
                        title = "Version",
                        icon = Icons.Default.Info,
                        iconTint = MaterialTheme.colorScheme.secondary,
                        trailing = { TrailingValue("1.0") },
                    )
                    RowDivider()
                    ListRow(
                        title = "Data storage",
                        icon = Icons.Default.Storage,
                        iconTint = MaterialTheme.colorScheme.secondary,
                        trailing = { TrailingValue("On-device only") },
                    )
                    RowDivider()
                    Text(
                        text = "Exercise images from free-exercise-db (github.com/yuhonas/free-exercise-db). Open source.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

/** Overline label above a zero-padding [Tile] that stacks [ListRow]s. */
@Composable
private fun SettingsGroup(
    label: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column {
        Overline(text = label)
        Tile(contentPadding = PaddingValues(0.dp), content = content)
    }
}

@Composable
private fun ProfileHero(
    name: String,
    isPro: Boolean,
    onUpgrade: () -> Unit,
) {
    Tile {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                val initial = name.trim().firstOrNull()?.uppercaseChar()
                if (initial != null) {
                    Text(
                        text = initial.toString(),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                } else {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = name.ifEmpty { "Set up your profile" },
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (isPro) {
                    Text(
                        text = "Pro Member",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            text = "Free plan",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        PillChip(label = "Upgrade", selected = true, onClick = onUpgrade)
                    }
                }
            }
        }
    }
}

/**
 * A [ListRow] showing the current [value] as its subtitle; tapping opens a
 * [DropdownMenu] of [options].
 */
@Composable
private fun DropdownRow(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    options: List<String>,
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxWidth()) {
        ListRow(
            title = title,
            subtitle = value,
            icon = icon,
            onClick = { expanded = true },
            trailing = { SmallChevron() },
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun SmallChevron() {
    Icon(
        Icons.Default.ChevronRight,
        contentDescription = null,
        modifier = Modifier.size(20.dp),
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun TrailingValue(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun CsvExportRow(
    isPro: Boolean,
    onUpgrade: () -> Unit,
    onExport: () -> Unit,
) {
    ListRow(
        title = "Export workouts as CSV",
        subtitle = if (isPro) "Open in Sheets, Excel, or any spreadsheet app"
        else "Pro · Open in Sheets, Excel, or any spreadsheet app",
        icon = Icons.Default.Download,
        trailing = {
            if (isPro) {
                PillChip(label = "Export", selected = true, onClick = onExport)
            } else {
                PillChip(label = "Upgrade", selected = false, onClick = onUpgrade)
            }
        },
    )
}

@Composable
private fun HealthConnectRow(
    isPro: Boolean,
    availability: HealthConnectAvailability,
    granted: Boolean,
    enabled: Boolean,
    onUpgrade: () -> Unit,
    onConnect: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onInstall: () -> Unit,
) {
    val subtitle = when {
        !isPro -> "Pro · Sync workouts to the system Health store"
        availability == HealthConnectAvailability.NOT_INSTALLED ->
            "Install Health Connect to enable sync"
        !granted -> "Grant access to write workouts"
        enabled -> "Connected — finished workouts will sync"
        else -> "Connected — sync is paused"
    }
    ListRow(
        title = "Health Connect",
        subtitle = subtitle,
        icon = Icons.Default.Sync,
        iconTint = MaterialTheme.colorScheme.secondary,
        trailing = {
            when {
                !isPro -> PillChip(label = "Upgrade", selected = false, onClick = onUpgrade)
                availability == HealthConnectAvailability.NOT_INSTALLED ->
                    PillChip(label = "Install", selected = false, onClick = onInstall)
                !granted -> PillChip(label = "Connect", selected = true, onClick = onConnect)
                else -> Switch(checked = enabled, onCheckedChange = onToggle)
            }
        },
    )
}
