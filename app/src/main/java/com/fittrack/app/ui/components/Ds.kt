package com.fittrack.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

// ---------------------------------------------------------------------------
// FitTrack design system
//
// Every screen is built from these pieces so the app reads as one product:
//   ScreenHeader / DetailHeader  — large titles, no app-bar chrome
//   Tile                         — the card: container-low fill + hairline border
//   IconBadge                    — tinted rounded square behind an icon
//   StatTile / StatValue         — numerals-first stat blocks
//   PillChip / PillChipRow       — filter pills, no outlines
//   PrimaryButton / TonalButton  — 56dp CTAs
//   ListRow                      — icon badge + title/subtitle + trailing
//   fitTextFieldColors()         — filled, borderless inputs
// ---------------------------------------------------------------------------

/** Standard horizontal gutter for screen content. */
val ScreenGutter = 20.dp

/**
 * Large-title header for tab screens. Replaces TopAppBar: no bar background,
 * title sits on the canvas. [eyebrow] is a small muted line above the title
 * (e.g. the date or a greeting); [actions] render on the trailing side.
 */
@Composable
fun ScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    eyebrow: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = ScreenGutter, end = 8.dp, top = 12.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            if (eyebrow != null) {
                Text(
                    text = eyebrow,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, content = actions)
    }
}

/**
 * Compact header for pushed screens: back chevron, title, trailing actions.
 */
@Composable
fun DetailHeader(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 4.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.weight(1f),
        )
        Row(verticalAlignment = Alignment.CenterVertically, content = actions)
    }
}

/**
 * The card. Flat container-low fill with a hairline border so tiles read on
 * both the near-black and the light canvas without elevation shadows.
 */
@Composable
fun Tile(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    borderColor: Color = MaterialTheme.colorScheme.outlineVariant,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = MaterialTheme.shapes.medium
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(containerColor)
            .border(BorderStroke(1.dp, borderColor), shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(contentPadding),
        content = content,
    )
}

/** Tinted rounded square behind an icon. [tint] defaults to primary. */
@Composable
fun IconBadge(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary,
    size: androidx.compose.ui.unit.Dp = 40.dp,
    contentDescription: String? = null,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.3f))
            .background(tint.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(size * 0.55f),
        )
    }
}

/** Big numeral with a small caps label beneath. */
@Composable
fun StatValue(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    align: Alignment.Horizontal = Alignment.Start,
) {
    Column(modifier = modifier, horizontalAlignment = align) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            color = valueColor,
            maxLines = 1,
        )
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = if (align == Alignment.CenterHorizontally) TextAlign.Center else TextAlign.Start,
        )
    }
}

/** A [Tile] holding a single [StatValue], for 2- or 3-up grids. */
@Composable
fun StatTile(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    tint: Color = MaterialTheme.colorScheme.primary,
) {
    Tile(modifier = modifier, contentPadding = PaddingValues(14.dp)) {
        if (icon != null) {
            IconBadge(icon = icon, tint = tint, size = 32.dp)
            Spacer(modifier = Modifier.height(10.dp))
        }
        StatValue(value = value, label = label)
    }
}

/** Filter pill. Selected = primary fill; unselected = container-high fill, no outline. */
@Composable
fun PillChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
) {
    val bg = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh
    val fg = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (leadingIcon != null) {
            Icon(leadingIcon, contentDescription = null, tint = fg, modifier = Modifier.size(16.dp))
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = fg,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
        )
    }
}

/** Horizontally scrolling row of [PillChip]s with the screen gutter. */
@Composable
fun PillChipRow(
    modifier: Modifier = Modifier,
    horizontalPadding: androidx.compose.ui.unit.Dp = ScreenGutter,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = horizontalPadding, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        content = content,
    )
}

/** Full-width 56dp primary call to action. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().height(56.dp),
        shape = MaterialTheme.shapes.small,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

/** Full-width 56dp secondary action on a container-high fill. */
@Composable
fun TonalButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().height(56.dp),
        shape = MaterialTheme.shapes.small,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

/**
 * Settings-style row: icon badge, title, optional subtitle, trailing slot
 * (defaults to a chevron when [onClick] is set). Stack several inside a
 * [Tile] with [RowDivider] between them.
 */
@Composable
fun ListRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (icon != null) IconBadge(icon = icon, tint = iconTint, size = 36.dp)
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        when {
            trailing != null -> trailing()
            onClick != null -> Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Hairline divider for rows inside a [Tile]. */
@Composable
fun RowDivider(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outlineVariant)
    )
}

/**
 * Filled, borderless text field colors. Use with `TextField` (not Outlined)
 * and `shape = MaterialTheme.shapes.small`.
 */
@Composable
fun fitTextFieldColors(): TextFieldColors = TextFieldDefaults.colors(
    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    errorContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
    disabledIndicatorColor = Color.Transparent,
    errorIndicatorColor = Color.Transparent,
    focusedLabelColor = MaterialTheme.colorScheme.primary,
    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    cursorColor = MaterialTheme.colorScheme.primary,
)

/** Small uppercase overline for grouping rows ("PROFILE", "PREFERENCES"). */
@Composable
fun Overline(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        // Meant to sit inside an already gutter-padded list; only a small inset.
        modifier = modifier.padding(horizontal = 4.dp, vertical = 6.dp),
    )
}

/** Provides [color] as the default content color for [content]. */
@Composable
fun WithContentColor(color: Color, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalContentColor provides color, content = content)
}
