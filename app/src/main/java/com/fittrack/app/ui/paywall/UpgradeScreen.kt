package com.fittrack.app.ui.paywall

import android.app.Activity
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fittrack.app.billing.ProFeature
import com.fittrack.app.billing.ProManager
import com.fittrack.app.ui.theme.Green

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpgradeScreen(
    proManager: ProManager,
    onBack: () -> Unit
) {
    val isPro by proManager.isPro.collectAsState()
    val context = LocalContext.current
    var selectedPlan by remember { mutableStateOf("yearly") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Upgrade to Pro") },
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
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        modifier = Modifier.size(72.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (isPro) "You're a Pro!" else "Unlock FitTrack Pro",
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isPro) "You have access to all features"
                        else "Get ML-powered insights, all programs, and more",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }

            if (isPro) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Green.copy(alpha = 0.1f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Green)
                            Text("Pro subscription active", style = MaterialTheme.typography.titleSmall)
                        }
                    }
                }
            }

            // Features list
            item {
                Text(
                    "What's included in Pro",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            val features = listOf(
                FeatureItem(Icons.Default.Psychology, ProFeature.ML_INSIGHTS.displayName, ProFeature.ML_INSIGHTS.description),
                FeatureItem(Icons.Default.FitnessCenter, ProFeature.ALL_PROGRAMS.displayName, ProFeature.ALL_PROGRAMS.description),
                FeatureItem(Icons.Default.EventNote, ProFeature.UNLIMITED_PLAN.displayName, ProFeature.UNLIMITED_PLAN.description),
                FeatureItem(Icons.Default.ShowChart, ProFeature.ADVANCED_CHARTS.displayName, ProFeature.ADVANCED_CHARTS.description),
                FeatureItem(Icons.Default.FileDownload, ProFeature.DATA_EXPORT.displayName, ProFeature.DATA_EXPORT.description)
            )

            items(features) { feature ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        feature.icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(feature.name, style = MaterialTheme.typography.titleSmall)
                        Text(
                            feature.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            // Pricing
            if (!isPro) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Choose your plan",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                // Yearly plan
                item {
                    val yearlyPrice = proManager.getYearlyPrice()
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { selectedPlan = "yearly" },
                        colors = if (selectedPlan == "yearly") {
                            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        } else {
                            CardDefaults.cardColors()
                        },
                        border = if (selectedPlan == "yearly") {
                            CardDefaults.outlinedCardBorder()
                        } else null
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = selectedPlan == "yearly",
                                        onClick = { selectedPlan = "yearly" }
                                    )
                                    Text("Yearly", style = MaterialTheme.typography.titleSmall)
                                    AssistChip(
                                        onClick = {},
                                        label = { Text("Save 50%", style = MaterialTheme.typography.labelSmall) }
                                    )
                                }
                                Text(
                                    text = "Best value",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(start = 48.dp)
                                )
                            }
                            Text(
                                text = yearlyPrice,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Monthly plan
                item {
                    val monthlyPrice = proManager.getMonthlyPrice()
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { selectedPlan = "monthly" },
                        colors = if (selectedPlan == "monthly") {
                            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        } else {
                            CardDefaults.cardColors()
                        },
                        border = if (selectedPlan == "monthly") {
                            CardDefaults.outlinedCardBorder()
                        } else null
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedPlan == "monthly",
                                    onClick = { selectedPlan = "monthly" }
                                )
                                Text("Monthly", style = MaterialTheme.typography.titleSmall)
                            }
                            Text(
                                text = monthlyPrice,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Subscribe button
                item {
                    Button(
                        onClick = {
                            val activity = context as? Activity ?: return@Button
                            if (selectedPlan == "yearly") {
                                proManager.purchaseYearly(activity)
                            } else {
                                proManager.purchaseMonthly(activity)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    ) {
                        Icon(Icons.Default.Star, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Subscribe Now",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }

                // Fine print
                item {
                    Text(
                        text = "Cancel anytime. Subscription auto-renews. Managed through Google Play.",
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Restore purchases
                item {
                    TextButton(
                        onClick = { proManager.initialize() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Restore Purchases")
                    }
                }
            }

            // Debug toggle - always visible at the bottom
            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "Debug: Toggle Pro",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                "For testing only",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                        Switch(
                            checked = isPro,
                            onCheckedChange = { proManager.debugTogglePro() }
                        )
                    }
                }
            }
        }
    }
}

private data class FeatureItem(
    val icon: ImageVector,
    val name: String,
    val description: String
)
