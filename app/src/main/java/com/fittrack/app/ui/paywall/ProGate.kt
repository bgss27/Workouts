package com.fittrack.app.ui.paywall

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fittrack.app.billing.ProFeature
import com.fittrack.app.billing.ProManager

/**
 * Wraps content that requires Pro.
 * Shows the content if Pro, otherwise shows a locked state with upgrade prompt.
 */
@Composable
fun ProGate(
    proManager: ProManager,
    feature: ProFeature,
    onUpgradeClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val isPro by proManager.isPro.collectAsState()

    if (isPro) {
        content()
    } else {
        ProLockedOverlay(
            feature = feature,
            onUpgradeClick = onUpgradeClick,
            modifier = modifier
        )
    }
}

@Composable
fun ProLockedOverlay(
    feature: ProFeature,
    onUpgradeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Lock,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = feature.displayName,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = feature.description,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onUpgradeClick) {
            Icon(
                Icons.Default.Star,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Upgrade to Pro")
        }
    }
}

/**
 * A small inline badge for locked features in lists.
 */
@Composable
fun ProBadge(modifier: Modifier = Modifier) {
    AssistChip(
        onClick = {},
        label = {
            Text("PRO", style = MaterialTheme.typography.labelSmall)
        },
        leadingIcon = {
            Icon(
                Icons.Default.Star,
                contentDescription = null,
                modifier = Modifier.size(14.dp)
            )
        },
        modifier = modifier
    )
}
