package com.lunarlog.ui.analysis

import android.graphics.Typeface
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.cartesian.marker.rememberDefaultCartesianMarker
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import com.patrykandpatrick.vico.compose.common.component.rememberTextComponent
import com.patrykandpatrick.vico.compose.common.component.shapeComponent
import com.patrykandpatrick.vico.compose.common.fill
import com.patrykandpatrick.vico.compose.common.insets
import com.patrykandpatrick.vico.core.cartesian.marker.CartesianMarker
import com.patrykandpatrick.vico.core.common.LayeredComponent
import com.patrykandpatrick.vico.core.common.shape.CorneredShape

@Composable
fun rememberMarker(): CartesianMarker {
    val labelBackground = shapeComponent(
        fill = fill(MaterialTheme.colorScheme.surfaceContainer),
        shape = CorneredShape.Pill,
    )
    val label = rememberTextComponent(
        color = MaterialTheme.colorScheme.onSurface,
        typeface = Typeface.DEFAULT,
        padding = insets(8.dp, 4.dp),
        background = labelBackground,
    )

    val surfaceColor = MaterialTheme.colorScheme.surface

    val guideline = rememberLineComponent(
        fill = fill(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)),
        thickness = 2.dp,
    )

    return rememberDefaultCartesianMarker(
        label = label,
        indicator = { color ->
            LayeredComponent(
                shapeComponent(fill(surfaceColor), CorneredShape.Pill),
                LayeredComponent(
                    shapeComponent(fill(color.copy(alpha = 1f)), CorneredShape.Pill),
                    shapeComponent(fill(surfaceColor), CorneredShape.Pill),
                    insets(2.dp),
                ),
                insets(4.dp),
            )
        },
        indicatorSize = 12.dp,
        guideline = guideline,
    )
}
