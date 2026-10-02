package com.lunarlog.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay
import java.time.LocalDate

/** Samples the wall clock on resume and at most a minute after midnight/time-zone changes. */
@Composable
fun ObserveDateChanges(onDate: (LocalDate) -> Unit) {
    val callback = rememberUpdatedState(onDate)
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) callback.value(LocalDate.now())
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(owner) {
        while (true) {
            if (owner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) callback.value(LocalDate.now())
            delay(30_000)
        }
    }
}
