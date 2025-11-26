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
}
