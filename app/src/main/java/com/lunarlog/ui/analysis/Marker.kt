package com.lunarlog.ui.analysis

import android.graphics.Typeface
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.component.lineComponent
import com.patrykandpatrick.vico.compose.component.overlayingComponent
import com.patrykandpatrick.vico.compose.component.shapeComponent
import com.patrykandpatrick.vico.compose.component.textComponent
import com.patrykandpatrick.vico.compose.dimensions.dimensionsOf
import com.patrykandpatrick.vico.core.component.marker.MarkerComponent
import com.patrykandpatrick.vico.core.component.shape.Shapes
import com.patrykandpatrick.vico.core.component.shape.cornered.Corner
import com.patrykandpatrick.vico.core.component.shape.cornered.MarkerCorneredShape
import com.patrykandpatrick.vico.core.extension.copyColor
import com.patrykandpatrick.vico.core.marker.Marker

@Composable
fun rememberMarker(): Marker {
    val labelBackgroundColor = MaterialTheme.colorScheme.surfaceContainer
    val labelBackground = shapeComponent(shape = Shapes.pillShape, color = labelBackgroundColor)
    val label = textComponent(
        background = labelBackground,
        lineCount = 1,
        padding = dimensionsOf(8.dp, 4.dp),
        typeface = Typeface.DEFAULT,
        color = MaterialTheme.colorScheme.onSurface,
    )
    
    val indicatorInner = shapeComponent(Shapes.pillShape, MaterialTheme.colorScheme.surface)
    val indicatorCenter = shapeComponent(Shapes.pillShape, MaterialTheme.colorScheme.primary)
    val indicatorOuter = shapeComponent(Shapes.pillShape, MaterialTheme.colorScheme.surface)
    
    val indicator = overlayingComponent(
        outer = indicatorOuter,
        inner = overlayingComponent(
            outer = indicatorCenter,
            inner = indicatorInner,
            innerPaddingAll = 2.dp,
        ),
        innerPaddingAll = 4.dp,
    )
    
    val guideline = lineComponent(
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
        2.dp,
    )
    
    return remember(label, indicator, guideline) {
        object : MarkerComponent(label, indicator, guideline) {
            init {
                indicatorSizeDp = 12f
                onApplyEntryColor = { entryColor ->
                    indicatorCenter.color = entryColor.copyColor(alpha = 255)
                }
            }
        }
    }
}
