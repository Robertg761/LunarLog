package com.lunarlog.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The app's 4dp spacing scale.
 *
 * Values were chosen from what the screens already use most (8dp and 16dp dominate, then 24 and 12),
 * so adopting these tokens is a rename for almost every call site rather than a visual move. Anything
 * off this scale — the 3/6/10/18/20/28dp one-offs scattered through Calendar, Settings and the update
 * sheet — should snap to the nearest token here.
 *
 * Not exposed through a CompositionLocal: spacing does not vary by theme, and a plain object keeps
 * the import a single line for call sites (`Spacing.lg`).
 */
@Immutable
object Spacing {

    /** 4dp — icon-to-glyph nudges, chip internals. */
    val xs: Dp = 4.dp

    /** 8dp — the default gap between two adjacent things (icon to label, row to row). */
    val sm: Dp = 8.dp

    /** 12dp — a slightly opened-up list rhythm, e.g. detail screens with taller rows. */
    val md: Dp = 12.dp

    /** 16dp — the app's base inset and the standard gap between grouped blocks. */
    val lg: Dp = 16.dp

    /** 24dp — generous padding for hero surfaces and sheets. */
    val xl: Dp = 24.dp

    /** 32dp — separation between unrelated groups. */
    val xxl: Dp = 32.dp

    /** Horizontal inset for every screen's content. Use on the Scaffold body, LazyColumn
     *  contentPadding or top-level Column — never on a child that a parent already inset. */
    val screenHorizontal: Dp = 16.dp

    /** Top/bottom inset for screen content that is not FAB-bearing (see [fabClearance]). */
    val screenVertical: Dp = 16.dp

    /** Inner padding of a content card. This is `LunarLogCard`'s default; pass it explicitly only
     *  when hand-rolling a Surface that has to line up with the cards around it. */
    val cardPadding: Dp = 16.dp

    /** Gap between two settings-style sections (i.e. below one section's card, above the next
     *  section's header). The 8dp between a header and its own card lives in `SectionHeader`. */
    val sectionGap: Dp = 32.dp

    /** Gap between sibling items in a list — the `verticalArrangement = Arrangement.spacedBy(...)`
     *  value for LazyColumns of cards or rows. */
    val itemGap: Dp = 8.dp

    /** Bottom content padding for any scroll container on a screen with a FAB, so the last row
     *  clears it. 56dp FAB + 16dp Scaffold margin + 16dp breathing room. */
    val fabClearance: Dp = 88.dp

    /** Horizontal (and bottom) content inset inside a ModalBottomSheet. M3's recommended sheet
     *  gutter; the app currently runs five different ones. */
    val sheetHorizontal: Dp = 24.dp

    /** The 48dp accessibility floor for anything tappable — use as `heightIn(min = ...)` on rows
     *  that are clickable but whose content alone would be shorter. Three screens each declared
     *  their own `MinTouchTarget` / `MinRowHeight` constant at this value. */
    val minTouchTarget: Dp = 48.dp
}
