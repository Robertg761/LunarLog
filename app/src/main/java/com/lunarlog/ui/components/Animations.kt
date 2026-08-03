package com.lunarlog.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * A full-screen overlay that plays a success checkmark animation.
 *
 * The disc is `primary`, not the stock material green it used to hardcode: this plays over the app's
 * own screens, and a saturated green landing in the middle of a pink theme read as a system dialog
 * rather than as LunarLog confirming a save. A scrim underneath keeps the content behind it from
 * competing with the check.
 *
 * @param onAnimationFinished Callback when the animation completes (approx 1.5s).
 */
@Composable
fun SuccessOverlay(onAnimationFinished: () -> Unit) {
    var startAnimation by remember { mutableStateOf(false) }

    val discColor = MaterialTheme.colorScheme.primary
    val checkColor = MaterialTheme.colorScheme.onPrimary
    val scrimColor = MaterialTheme.colorScheme.scrim

    val scale by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )

    val alpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(300),
        label = "alpha"
    )

    LaunchedEffect(Unit) {
        startAnimation = true
        delay(1500) // Show for 1.5 seconds
        onAnimationFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { this.alpha = alpha }
            .background(scrimColor.copy(alpha = 0.32f)),
        contentAlignment = Alignment.Center
    ) {
        // Background Circle
        Canvas(modifier = Modifier.size(120.dp).graphicsLayer {
            scaleX = scale
            scaleY = scale
        }) {
            drawCircle(color = discColor)
        }

        // Check Icon
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = "Success",
            tint = checkColor,
            modifier = Modifier
                .size(64.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
        )
    }
}

/**
 * A confetti explosion effect.
 *
 * @param modifier Modifier for the container.
 * @param durationMillis How long the particles last.
 * @param colors The palette particles are drawn from. Defaults to the theme's accent roles, so a
 *   user who has picked a seed colour gets confetti in their own palette rather than the fixed
 *   amber/blue/pink/green set this used to hardcode.
 */
@Composable
fun ConfettiExplosion(
    modifier: Modifier = Modifier,
    durationMillis: Int = 1000,
    colors: List<Color> = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary
    )
) {
    val particles = remember(colors) { List(50) { ConfettiParticle(color = colors.random()) } }
    val anim = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        anim.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis, easing = LinearEasing)
        )
    }

    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2, size.height / 2)
        
        particles.forEach { particle ->
            val progress = anim.value
            val currentRadius = particle.maxRadius * progress
            val x = center.x + currentRadius * cos(particle.angle)
            val y = center.y + currentRadius * sin(particle.angle) + (progress * progress * 500f) // Add gravity

            val particleAlpha = (1f - progress).coerceIn(0f, 1f)

            drawCircle(
                color = particle.color.copy(alpha = particleAlpha),
                radius = particle.size * (1f - progress), // Shrink as they fly
                center = Offset(x.toFloat(), y.toFloat())
            )
        }
    }
}

private data class ConfettiParticle(
    val color: Color,
    val angle: Double = Random.nextDouble() * 2 * Math.PI,
    val maxRadius: Float = Random.nextFloat() * 300f + 100f,
    val size: Float = Random.nextFloat() * 10f + 5f
)

/**
 * A modifier that shakes the content horizontally.
 * @param enabled Whether the shake is active.
 */
@Composable
fun rememberShakeController(): ShakeController {
    return remember { ShakeController() }
}

class ShakeController {
    var shakeTrigger by mutableLongStateOf(0L)
        private set

    fun shake() {
        shakeTrigger = System.currentTimeMillis()
    }
}

@Composable
fun Modifier.shake(controller: ShakeController): Modifier {
    val offsetX = remember { Animatable(0f) }

    LaunchedEffect(controller.shakeTrigger) {
        if (controller.shakeTrigger != 0L) {
            offsetX.snapTo(0f)
            offsetX.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = 400
                    0f at 0
                    (-20f) at 50
                    20f at 100
                    (-20f) at 150
                    20f at 200
                    (-10f) at 250
                    10f at 300
                    0f at 400
                }
            )
        }
    }

    return this.graphicsLayer {
        translationX = offsetX.value
    }
}
