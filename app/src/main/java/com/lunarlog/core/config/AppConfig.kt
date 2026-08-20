package com.lunarlog.core.config

object AppConfig {
    const val DEFAULT_CYCLE_LENGTH = 28
    const val DEFAULT_LUTEAL_PHASE_LENGTH = 14
    
    // Fertile Window (relative to Ovulation)
    const val FERTILE_WINDOW_OFFSET_START = 5L // days before
    const val FERTILE_WINDOW_OFFSET_END = 1L // days after

    // UI/Flow
    const val FLOW_SUBSCRIPTION_TIMEOUT = 5000L

    // Prediction
    const val AVERAGE_PERIOD_LENGTH_DEFAULT = 5

    // Sanity bounds for a single period's length in days. Recorded periods outside this range
    // are kept but never shape the period-length average, and a "start period" that lands more
    // than MAX_PERIOD_LENGTH_DAYS after an open period's start is treated as a new period
    // rather than a continuation.
    const val MIN_PERIOD_LENGTH_DAYS = 2
    const val MAX_PERIOD_LENGTH_DAYS = 10
}
