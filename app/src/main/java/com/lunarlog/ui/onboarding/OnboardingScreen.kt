package com.lunarlog.ui.onboarding

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lunarlog.ui.theme.Spacing
import com.lunarlog.ui.util.MediumDate
import com.lunarlog.ui.util.toPickerLocalDate
import com.lunarlog.ui.util.toPickerMillis
import java.time.LocalDate

/**
 * Reproduces the old platform dialog's `datePicker.maxDate = System.currentTimeMillis()`: a period
 * cannot have started in the future. There is no lower bound, as before.
 */
@OptIn(ExperimentalMaterial3Api::class)
private object PastOrPresentDates : SelectableDates {
    override fun isSelectableDate(utcTimeMillis: Long): Boolean =
        utcTimeMillis <= LocalDate.now().toPickerMillis()

    override fun isSelectableYear(year: Int): Boolean = year <= LocalDate.now().year
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    onOnboardingComplete: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val onboardingState by viewModel.onboardingState.collectAsState()
    val isLoading = onboardingState is OnboardingViewModel.OnboardingState.Saving
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var showDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(onboardingState) {
        if (onboardingState is OnboardingViewModel.OnboardingState.Success) {
            onOnboardingComplete()
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        // 1. Ambient background. Draws itself; never invalidates the content above it.
        AnimatedAuroraBackground()

        // 2. Content. `heightIn(min = ...)` sits inside the scroll modifier, so the column is at
        // least a screen tall (and therefore vertically centred) on roomy devices, and grows past
        // the viewport into a real scroll on short ones or at large font scales.
        val viewportHeight = maxHeight
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .heightIn(min = viewportHeight)
                .padding(
                    horizontal = Spacing.xl,
                    vertical = Spacing.xxl
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "LunarLog",
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(Spacing.md))
            Text(
                text = "Your Cycle. Your Rhythm.\nYour Privacy.",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(Spacing.xxl))

            // Opaque `surfaceContainer` on `shapes.large` — the same tokens LunarLogCard uses, so
            // onboarding introduces the app with the card treatment the rest of it keeps using.
            // It was `surface` at 70% alpha, the only translucent panel in the app: with an animated
            // aurora drifting behind it, the effective contrast of the text on top changed frame to
            // frame, so there was no ratio to measure. The aurora still frames the card.
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(Spacing.xl),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Let's get started",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    Text(
                        text = "When did your last period start?",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (onboardingState is OnboardingViewModel.OnboardingState.Error) {
                        Spacer(modifier = Modifier.height(Spacing.sm))
                        Text(
                            text = (onboardingState as OnboardingViewModel.OnboardingState.Error).message,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    Spacer(modifier = Modifier.height(Spacing.lg))

                    DateSelectorButton(
                        date = selectedDate,
                        onClick = { showDatePicker = true }
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.lg))

            // The CTA slot keeps its height while saving, so the card above it does not jump when
            // the button is swapped for the spinner.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = CtaMinHeight),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                } else {
                    PrimaryCTAButton(
                        text = "Begin Journey",
                        onClick = { viewModel.completeOnboarding(selectedDate) }
                    )
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.toPickerMillis(),
            selectableDates = PastOrPresentDates
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        selectedDate = millis.toPickerLocalDate()
                    }
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

/**
 * Symmetric ease-in-out. Deliberate: with [RepeatMode.Reverse] a linear curve flips velocity
 * instantly at each endpoint and the drift reads as a glitch. (Shimmer.kt is linear because it
 * restarts rather than reverses — do not "fix" that one to match this.)
 */
private val AuroraEasing = CubicBezierEasing(0.4f, 0f, 0.6f, 1f)

/**
 * Two soft blobs drifting behind the onboarding content.
 *
 * Cost notes, because this runs forever on the first screen a new user sees:
 * - one animated float drives everything; the second blob is its phase inverse.
 * - the radial shaders are [remember]ed and drawn at a constant size, so each is compiled once
 *   instead of being rebuilt from animated values every frame.
 * - motion lives entirely in [graphicsLayer] blocks, which read the animation in the layer phase.
 *   Nothing here recomposes, and nothing above it redraws.
 * - the base fill and the lightening veil are two draws in the parent's [drawWithContent] rather
 *   than extra composables and compositing passes.
 */
@Composable
fun AnimatedAuroraBackground(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "aurora")
    val drift: State<Float> = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 40_000, easing = AuroraEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "aurora_drift"
    )

    val baseColor = MaterialTheme.colorScheme.background
    val veilColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.2f)
    val warm = MaterialTheme.colorScheme.primaryContainer
    val cool = MaterialTheme.colorScheme.secondaryContainer

    val warmBrush = remember(warm) {
        Brush.radialGradient(listOf(warm.copy(alpha = 0.8f), Color.Transparent))
    }
    val coolBrush = remember(cool) {
        Brush.radialGradient(listOf(cool.copy(alpha = 0.7f), Color.Transparent))
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawWithContent {
                drawRect(baseColor)
                drawContent()
                drawRect(veilColor)
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val t = drift.value
                    translationX = size.width * (-0.28f + 0.10f * t)
                    translationY = size.height * (-0.30f + 0.06f * t)
                    val s = 1.90f + 0.12f * t
                    scaleX = s
                    scaleY = s
                }
                .background(warmBrush)
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val t = 1f - drift.value
                    translationX = size.width * (0.28f - 0.10f * t)
                    translationY = size.height * (0.30f - 0.07f * t)
                    val s = 1.80f + 0.15f * t
                    scaleX = s
                    scaleY = s
                }
                .background(coolBrush)
        )
    }
}

@Composable
fun DateSelectorButton(date: LocalDate, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.lg),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(Spacing.md))
                Text(
                    text = date.format(MediumDate),
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Minimum touch height of the primary call to action, shared with the slot that reserves it. */
private val CtaMinHeight = 56.dp

@Composable
fun PrimaryCTAButton(text: String, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(targetValue = if (isPressed) 0.95f else 1f, label = "btn_scale")

    Button(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = CtaMinHeight)
            .scale(scale),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            // Was hardcoded white, which sat at 1.69:1 on the dark theme's pink primary.
            contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        shape = CircleShape,
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp, pressedElevation = 2.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium
        )
    }
}
