package com.fittrack.app.ui.paywall

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fittrack.app.BuildConfig
import com.fittrack.app.billing.FeatureGate
import com.fittrack.app.billing.ProManager
import com.fittrack.app.ui.theme.FitTrackTheme
import java.text.DateFormat
import java.util.Date

private enum class Tier { ANNUAL, LIFETIME, MONTHLY }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpgradeScreen(
    proManager: ProManager,
    onBack: () -> Unit
) {
    val isPro by proManager.isPro.collectAsState()
    val founderActive = proManager.founderLifetimePriceActive
    val founderEndsAt = proManager.founderLifetimeEndsAtMillis
    val yearlyTrialDuration = proManager.getYearlyTrialDuration()
    val context = LocalContext.current
    var selectedTier by remember { mutableStateOf(Tier.ANNUAL) }

    val founderEndsLabel = remember(founderEndsAt) {
        founderEndsAt?.let { DateFormat.getDateInstance(DateFormat.LONG).format(Date(it)) }
    }

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
            item { UpgradeHeader(isPro = isPro) }

            if (isPro) {
                item { ActiveSubscriptionCard() }
            }

            item {
                Text("What's included in Pro", style = MaterialTheme.typography.titleMedium)
            }

            items(featureItems()) { feature -> FeatureRow(feature) }

            if (!isPro) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Choose your plan", style = MaterialTheme.typography.titleMedium)
                }

                item {
                    TierCard(
                        title = "Annual",
                        price = proManager.getYearlyPrice(),
                        pricePeriod = " / year",
                        subtitle = if (yearlyTrialDuration != null)
                            "$yearlyTrialDuration free, then auto-renews"
                        else "Best for committed lifters",
                        badge = "Most Popular",
                        selected = selectedTier == Tier.ANNUAL,
                        onClick = { selectedTier = Tier.ANNUAL }
                    )
                }

                item {
                    TierCard(
                        title = "Lifetime",
                        price = proManager.getLifetimePrice(),
                        pricePeriod = " once",
                        subtitle = when {
                            founderActive && founderEndsLabel != null ->
                                "Founder pricing — through $founderEndsLabel"
                            founderActive -> "Founder pricing — limited time"
                            else -> "One-time payment, lifetime access"
                        },
                        badge = "Best Value",
                        emphasizeBadge = founderActive,
                        selected = selectedTier == Tier.LIFETIME,
                        onClick = { selectedTier = Tier.LIFETIME }
                    )
                }

                item {
                    TierCard(
                        title = "Monthly",
                        price = proManager.getMonthlyPrice(),
                        pricePeriod = " / month",
                        subtitle = "Cancel anytime",
                        badge = null,
                        selected = selectedTier == Tier.MONTHLY,
                        onClick = { selectedTier = Tier.MONTHLY }
                    )
                }

                item {
                    Button(
                        onClick = {
                            val activity = context as? Activity ?: return@Button
                            when (selectedTier) {
                                Tier.ANNUAL -> proManager.purchaseYearly(activity)
                                Tier.LIFETIME -> proManager.purchaseLifetime(activity)
                                Tier.MONTHLY -> proManager.purchaseMonthly(activity)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    ) {
                        Icon(Icons.Default.Star, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = ctaLabel(selectedTier, yearlyTrialDuration),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }

                item {
                    Text(
                        text = finePrint(selectedTier, yearlyTrialDuration, proManager.getYearlyPrice()),
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    TextButton(
                        onClick = { proManager.restorePurchases() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Restore Purchases")
                    }
                }
            }

            if (BuildConfig.DEBUG) {
                item { DebugProToggle(isPro = isPro, onToggle = { proManager.debugTogglePro() }) }
            }
        }
    }
}

@Composable
private fun UpgradeHeader(isPro: Boolean) {
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

@Composable
private fun ActiveSubscriptionCard() {
    val success = FitTrackTheme.colors.success
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = success.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = success)
            Text("Pro entitlement active", style = MaterialTheme.typography.titleSmall)
        }
    }
}

@Composable
private fun FeatureRow(feature: FeatureItem) {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TierCard(
    title: String,
    price: String,
    pricePeriod: String,
    subtitle: String,
    badge: String?,
    selected: Boolean,
    onClick: () -> Unit,
    emphasizeBadge: Boolean = false,
) {
    val borderColor =
        if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.outlineVariant
    val containerColor =
        if (selected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surface

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            ),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                RadioButton(selected = selected, onClick = onClick)
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(title, style = MaterialTheme.typography.titleSmall)
                        if (badge != null) TierBadge(text = badge, accent = emphasizeBadge)
                    }
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = price,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = pricePeriod.trim(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
private fun TierBadge(text: String, accent: Boolean) {
    val bg =
        if (accent) MaterialTheme.colorScheme.tertiary
        else MaterialTheme.colorScheme.primary
    val fg =
        if (accent) MaterialTheme.colorScheme.onTertiary
        else MaterialTheme.colorScheme.onPrimary
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            color = fg,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun DebugProToggle(isPro: Boolean, onToggle: () -> Unit) {
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
                Text("Debug: Toggle Pro", style = MaterialTheme.typography.titleSmall)
                Text(
                    "Debug build only",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
            Switch(checked = isPro, onCheckedChange = { onToggle() })
        }
    }
}

private fun ctaLabel(tier: Tier, trialDuration: String?): String = when (tier) {
    Tier.LIFETIME -> "Get Lifetime Access"
    Tier.ANNUAL -> if (trialDuration != null) "Start $trialDuration Free Trial" else "Subscribe Now"
    Tier.MONTHLY -> "Subscribe Now"
}

private fun finePrint(tier: Tier, trialDuration: String?, yearlyPrice: String): String = when {
    tier == Tier.LIFETIME ->
        "One-time purchase. No subscription. Managed through Google Play."
    tier == Tier.ANNUAL && trialDuration != null ->
        "Free for $trialDuration, then $yearlyPrice. Cancel anytime in Google Play subscriptions."
    else ->
        "Cancel anytime. Subscription auto-renews. Managed through Google Play."
}

private data class FeatureItem(
    val icon: ImageVector,
    val name: String,
    val description: String
)

// Only features that are actually live ship in the upgrade screen.
// Add a new entry here when (and only when) the corresponding gate is implemented.
// Avoid listing aspirational features — Apple Guideline 5.0 / 2.3.1 risk on iOS.
private fun featureItems(): List<FeatureItem> = listOf(
    FeatureItem(Icons.Default.Psychology, FeatureGate.INSIGHTS.displayName, FeatureGate.INSIGHTS.description),
    FeatureItem(Icons.Default.ShowChart, FeatureGate.FULL_HISTORY.displayName, FeatureGate.FULL_HISTORY.description),
    FeatureItem(Icons.Default.Sync, FeatureGate.INTEGRATIONS.displayName, FeatureGate.INTEGRATIONS.description),
    FeatureItem(Icons.Default.FileDownload, FeatureGate.EXPORT.displayName, FeatureGate.EXPORT.description),
)
