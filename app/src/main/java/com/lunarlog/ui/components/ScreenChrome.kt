package com.lunarlog.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lunarlog.ui.theme.Spacing

/*
 * Shared screen chrome. Every screen's app bar, section header, content card and empty state comes
 * from here so the decisions — title weight, back-arrow contentDescription, card shape and container
 * colour, empty-state proportions — are made once instead of eight times.
 */

/**
 * Transparent container, tinting to `surfaceContainer` only once content scrolls under the bar.
 *
 * A bar painted in its own colour draws a seam across the top of every screen; leaving it
 * transparent lets the screen background run behind it uninterrupted. The scrolled colour is what
 * keeps the title legible once content slides beneath, and it only engages when the caller passes a
 * `scrollBehavior`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun lunarLogTopAppBarColors(): TopAppBarColors = TopAppBarDefaults.topAppBarColors(
    containerColor = Color.Transparent,
    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
    titleContentColor = MaterialTheme.colorScheme.onSurface,
    navigationIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
)

/**
 * The standard app bar for a push destination (Settings, Log List, Period Detail, Log History,
 * Log Period). Left-aligned title, optional back arrow, optional actions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LunarLogTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    onNavigateBack: (() -> Unit)? = null,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        modifier = modifier,
        navigationIcon = {
            if (onNavigateBack != null) {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        },
        actions = actions,
        colors = lunarLogTopAppBarColors(),
        scrollBehavior = scrollBehavior
    )
}

/**
 * The centre-aligned app bar, for top-level destinations whose title is more than a string — Home's
 * brand mark plus wordmark, Calendar's month-over-year stack. [navigationIcon] is a free slot rather
 * than a back arrow because those screens use it for something else (Calendar's previous-month
 * arrow); pass [LunarLogTopAppBar] a callback instead when you want the standard back affordance.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LunarLogCenterTopAppBar(
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    scrollBehavior: TopAppBarScrollBehavior? = null,
    title: @Composable () -> Unit
) {
    CenterAlignedTopAppBar(
        title = title,
        modifier = modifier,
        navigationIcon = navigationIcon,
        actions = actions,
        colors = lunarLogTopAppBarColors(),
        scrollBehavior = scrollBehavior
    )
}

/**
 * The "Security" / "Notifications" / "Weekly Digest" header above a group of settings or a block of
 * content. Owns the 8dp gap down to whatever it labels, so callers never re-type a Spacer.
 */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(bottom = Spacing.sm)
    )
}

/**
 * The app's one content card: `shapes.large`, the warm `surfaceContainer` tone, 16dp inner padding,
 * flat.
 *
 * Deliberately not M3's `Card`, which paints `surfaceContainerHighest` — a full two steps up the
 * ladder from the background, which is more separation than a page of stacked cards wants. Flat with
 * no border because the container tone already separates the card from the background; adding
 * elevation as well makes a scroll of stacked cards look noisy.
 *
 * [containerColor] exists for the handful of cards whose fill *is* the state — Home's fertility card
 * (`cycleColors.fertileContainer`), a marked period day (`primaryContainer`). Overriding it keeps
 * those on the same shape and inset as every other card instead of forcing a hand-rolled `Surface`;
 * [contentColor] follows from it automatically for any colour M3 knows a pairing for.
 */
@Composable
fun LunarLogCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(Spacing.cardPadding),
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    contentColor: Color = contentColorFor(containerColor),
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = MaterialTheme.shapes.large
    val container = containerColor
    val onContainer = contentColor
    if (onClick != null) {
        Surface(
            onClick = onClick,
            modifier = modifier,
            shape = shape,
            color = container,
            contentColor = onContainer
        ) {
            Column(modifier = Modifier.padding(contentPadding), content = content)
        }
    } else {
        Surface(
            modifier = modifier,
            shape = shape,
            color = container,
            contentColor = onContainer
        ) {
            Column(modifier = Modifier.padding(contentPadding), content = content)
        }
    }
}

/**
 * Centred icon + title + optional supporting line, for a screen or tab with nothing to show.
 *
 * Pads vertically only, so it inherits whatever horizontal gutter its parent already applies rather
 * than sitting at a deeper inset than the content it replaces.
 */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    description: String? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(Spacing.lg))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        if (description != null) {
            Spacer(Modifier.height(Spacing.sm))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * The one indeterminate loading treatment: a centred spinner in `primary`, filling whatever box it
 * is handed.
 *
 * Four screens each centred a bare `CircularProgressIndicator()`, which takes M3's default tint
 * rather than the app's — on a seeded theme that is a visibly different colour from everything
 * around it. Screens whose loaded layout has a known shape (Home, Log List, Settings) keep their
 * shimmer skeletons instead: those load on cold start, where the wait is longest and a skeleton
 * reads as the screen arriving rather than as the screen being absent. Use this one for the short,
 * shape-unknown waits.
 */
@Composable
fun LoadingState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}

/**
 * The one divider treatment: a hairline in `outlineVariant`, at full alpha.
 *
 * Screens had drifted to three different rules — bare `HorizontalDivider()`, `outlineVariant` at
 * 0.6 alpha, `onPrimaryContainer` at 0.2 — which read as three different weights of line in one
 * app. `outlineVariant` is already the low-emphasis outline token, so fading it further only makes
 * it disappear on the darker surfaces.
 */
@Composable
fun CardDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier,
        color = MaterialTheme.colorScheme.outlineVariant
    )
}
