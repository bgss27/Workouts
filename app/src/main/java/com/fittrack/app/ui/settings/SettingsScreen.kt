package com.fittrack.app.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fittrack.app.billing.ProManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    proManager: ProManager,
    onNavigateToUpgrade: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModel.Factory(context, proManager)
    )

    val profile by viewModel.profile.collectAsState()
    val isPro by proManager.isPro.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Settings") })
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Profile header
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Column {
                            Text(
                                text = profile.name.ifEmpty { "Set up your profile" },
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = if (isPro) "Pro Member" else "Free Plan",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isPro) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }

            // Profile section
            item {
                Text("Profile", style = MaterialTheme.typography.titleSmall)
            }

            item {
                OutlinedTextField(
                    value = profile.name,
                    onValueChange = { viewModel.updateProfile(profile.copy(name = it)) },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) }
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = profile.age,
                        onValueChange = { viewModel.updateProfile(profile.copy(age = it.filter { c -> c.isDigit() })) },
                        label = { Text("Age") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = profile.weightKg,
                        onValueChange = { viewModel.updateProfile(profile.copy(weightKg = it.filter { c -> c.isDigit() || c == '.' })) },
                        label = { Text("Weight (${profile.weightUnit})") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    OutlinedTextField(
                        value = profile.heightCm,
                        onValueChange = { viewModel.updateProfile(profile.copy(heightCm = it.filter { c -> c.isDigit() })) },
                        label = { Text("Height (cm)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            }

            // Gender dropdown
            item {
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = profile.gender,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Gender") },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        listOf("Male", "Female", "Not specified").forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    viewModel.updateProfile(profile.copy(gender = option))
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Fitness settings
            item {
                Text("Fitness Preferences", style = MaterialTheme.typography.titleSmall)
            }

            // Fitness goal dropdown
            item {
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = profile.fitnessGoal,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Fitness Goal") },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        leadingIcon = { Icon(Icons.Default.FitnessCenter, contentDescription = null) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        listOf("Build Muscle", "Lose Weight", "Gain Strength", "Improve Endurance", "General Fitness").forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    viewModel.updateProfile(profile.copy(fitnessGoal = option))
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Experience level dropdown
            item {
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = profile.experienceLevel,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Experience Level") },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        leadingIcon = { Icon(Icons.Default.TrendingUp, contentDescription = null) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        listOf("Beginner", "Intermediate", "Advanced").forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    viewModel.updateProfile(profile.copy(experienceLevel = option))
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Weight unit toggle
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Weight Unit", style = MaterialTheme.typography.bodyLarge)
                        Text("Used for logging sets", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = profile.weightUnit == "kg",
                            onClick = { viewModel.updateProfile(profile.copy(weightUnit = "kg")) },
                            label = { Text("kg") }
                        )
                        FilterChip(
                            selected = profile.weightUnit == "lbs",
                            onClick = { viewModel.updateProfile(profile.copy(weightUnit = "lbs")) },
                            label = { Text("lbs") }
                        )
                    }
                }
            }

            // Subscription section
            item {
                Text("Subscription", style = MaterialTheme.typography.titleSmall)
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToUpgrade() },
                    colors = if (isPro) {
                        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    } else {
                        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            tint = if (isPro) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isPro) "Pro Member" else "Upgrade to Pro",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = if (isPro) "All features unlocked"
                                else "ML insights, all programs, and more",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
            }

            // App info
            item {
                Text("About", style = MaterialTheme.typography.titleSmall)
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Version", style = MaterialTheme.typography.bodyMedium)
                        Text("1.0", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Data Storage", style = MaterialTheme.typography.bodyMedium)
                        Text("On-device only", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Exercise images provided by wger Workout Manager (wger.de) under CC BY-SA license.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}
