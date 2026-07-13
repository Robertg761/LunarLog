package com.lunarlog.logic

import com.lunarlog.core.model.Cycle
import java.time.LocalDate
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartAnomalyDetectorTest {
    @Test
    fun `unusually long completed cycle is not hidden from anomaly detection`() {
        val cycles = listOf(
            Cycle(startDate = LocalDate.of(2026, 1, 1)),
            Cycle(startDate = LocalDate.of(2026, 1, 29)),
            Cycle(startDate = LocalDate.of(2026, 2, 26)),
            Cycle(startDate = LocalDate.of(2026, 5, 7))
        )

        val anomalies = SmartAnomalyDetector.detectAnomalies(cycles)

        assertTrue(anomalies.any { it.type == AnomalyType.IRREGULAR })
    }
}
