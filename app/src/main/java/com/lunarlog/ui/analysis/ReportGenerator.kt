package com.lunarlog.ui.analysis

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.lunarlog.core.model.Cycle
import com.lunarlog.core.model.DailyLog
import com.lunarlog.data.LogEntry
import java.io.OutputStream
import java.time.Instant
import java.time.LocalDate

object ReportGenerator {

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 50f
    private const val BOTTOM_MARGIN = 55f

    fun generatePdf(
        outputStream: OutputStream,
        cycleHistory: List<Pair<LocalDate, Int>>,
        symptomCounts: Map<String, Int>,
        moodCounts: Map<String, Int>
    ) {
        val document = PdfDocument()
        val writer = PdfReportWriter(document)

        try {
            writer.startPage()
            writer.drawLine("LunarLog Health Report", textSize = 24f, bold = true, spacingAfter = 10f)
            writer.drawLine("Generated on: ${LocalDate.now()}", textSize = 12f, spacingAfter = 14f)
            writer.drawWrapped(
                "For wellness tracking only. LunarLog is not a medical device and does not diagnose, treat, cure, or prevent any medical condition. Consult a healthcare professional for medical advice, diagnosis, or treatment.",
                textSize = 10f,
                spacingAfter = 20f
            )

            writer.drawSection("Cycle History (Last 6 Months)")
            if (cycleHistory.isEmpty()) {
                writer.drawLine("No completed cycles recorded.")
            } else {
                cycleHistory.forEach { (date, length) ->
                    writer.drawLine("Start: $date  -  Cycle length: $length days")
                }
            }

            writer.drawSection("Symptom Frequency")
            if (symptomCounts.isEmpty()) {
                writer.drawLine("No symptoms recorded.")
            } else {
                symptomCounts.forEach { (symptom, count) ->
                    writer.drawWrapped("$symptom: $count ${if (count == 1) "time" else "times"}")
                }
            }

            writer.drawSection("Mood Frequency")
            if (moodCounts.isEmpty()) {
                writer.drawLine("No moods recorded.")
            } else {
                moodCounts.forEach { (mood, count) ->
                    writer.drawWrapped("$mood: $count ${if (count == 1) "time" else "times"}")
                }
            }

            writer.finish()
            outputStream.use(document::writeTo)
        } finally {
            writer.finishIfNeeded()
            document.close()
        }
    }

    fun generateCsv(
        outputStream: OutputStream,
        periods: List<Cycle>,
        dailyLogs: List<DailyLog>,
        logEntries: List<LogEntry>
    ) {
        outputStream.bufferedWriter().use { writer ->
            writer.appendCsvRow("RecordType", "Date", "Time", "Field", "Value", "Details")

            periods.sortedBy { it.startDate }.forEach { period ->
                writer.appendCsvRow(
                    "PERIOD",
                    period.startDate.toString(),
                    "",
                    "Period end",
                    period.endDate?.toString() ?: "Ongoing",
                    ""
                )
            }

            dailyLogs.sortedBy { it.date }.forEach { log ->
                writer.appendDailyValue(log.date, "Flow level", log.flowLevel.takeIf { it > 0 })
                writer.appendDailyValue(log.date, "Mood", log.mood.takeIf { it.isNotEmpty() }?.joinToString(" | "))
                writer.appendDailyValue(log.date, "Symptoms", log.symptoms.takeIf { it.isNotEmpty() }?.joinToString(" | "))
                writer.appendDailyValue(log.date, "Water intake", log.waterIntake.takeIf { it > 0 })
                writer.appendDailyValue(log.date, "Sleep hours", log.sleepHours.takeIf { it > 0f })
                writer.appendDailyValue(log.date, "Sleep quality", log.sleepQuality.takeIf { it > 0 })
                writer.appendDailyValue(log.date, "Sex drive", log.sexDrive.takeIf { it > 0 })
                writer.appendDailyValue(log.date, "Notes", log.notes.takeIf { it.isNotBlank() })
                writer.appendDailyValue(log.date, "Temperature", log.temperature)
                writer.appendDailyValue(log.date, "Cervical mucus", log.cervicalMucus.takeIf { it > 0 })
            }

            logEntries.sortedWith(compareBy<LogEntry> { it.date }.thenBy { it.time }).forEach { entry ->
                writer.appendCsvRow(
                    "LOG_ENTRY",
                    LocalDate.ofEpochDay(entry.date).toString(),
                    Instant.ofEpochMilli(entry.time).toString(),
                    entry.type.name,
                    entry.value,
                    entry.details.orEmpty()
                )
            }
        }
    }

    private fun java.io.Writer.appendDailyValue(date: LocalDate, field: String, value: Any?) {
        if (value == null) return
        appendCsvRow("DAILY_SUMMARY", date.toString(), "", field, value.toString(), "")
    }

    private fun java.io.Writer.appendCsvRow(vararg values: String) {
        append(values.joinToString(",") { value ->
            val spreadsheetSafeValue = if (
                value.firstOrNull() in setOf('=', '+', '-', '@', '\t', '\r')
            ) {
                "'$value"
            } else {
                value
            }
            "\"${spreadsheetSafeValue.replace("\"", "\"\"")}\""
        })
        append('\n')
    }

    private class PdfReportWriter(private val document: PdfDocument) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK }
        private var page: PdfDocument.Page? = null
        private var canvas: Canvas? = null
        private var pageNumber = 0
        private var y = MARGIN

        fun startPage() {
            finishIfNeeded()
            pageNumber += 1
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            page = document.startPage(pageInfo)
            canvas = page?.canvas
            y = MARGIN
        }

        fun drawSection(title: String) {
            ensureSpace(48f)
            y += 20f
            drawLine(title, textSize = 18f, bold = true, spacingAfter = 12f)
        }

        fun drawLine(
            text: String,
            textSize: Float = 14f,
            bold: Boolean = false,
            spacingAfter: Float = 8f
        ) {
            val lineHeight = textSize + spacingAfter
            ensureSpace(lineHeight)
            configurePaint(textSize, bold)
            canvas?.drawText(text, MARGIN, y, paint)
            y += lineHeight
        }

        fun drawWrapped(
            text: String,
            textSize: Float = 14f,
            bold: Boolean = false,
            spacingAfter: Float = 8f
        ) {
            configurePaint(textSize, bold)
            val maxWidth = PAGE_WIDTH - (MARGIN * 2)
            val lines = wrapText(text, maxWidth)
            val lineHeight = textSize + 5f
            lines.forEach { line ->
                ensureSpace(lineHeight)
                canvas?.drawText(line, MARGIN, y, paint)
                y += lineHeight
            }
            y += spacingAfter
        }

        fun finish() {
            finishIfNeeded()
        }

        fun finishIfNeeded() {
            val currentPage = page ?: return
            configurePaint(9f, false)
            currentPage.canvas.drawText("Page $pageNumber", PAGE_WIDTH - MARGIN - 38f, PAGE_HEIGHT - 25f, paint)
            document.finishPage(currentPage)
            page = null
            canvas = null
        }

        private fun ensureSpace(requiredHeight: Float) {
            if (page == null) startPage()
            if (y + requiredHeight > PAGE_HEIGHT - BOTTOM_MARGIN) {
                startPage()
            }
        }

        private fun configurePaint(textSize: Float, bold: Boolean) {
            paint.textSize = textSize
            paint.isFakeBoldText = bold
        }

        private fun wrapText(text: String, maxWidth: Float): List<String> {
            if (text.isBlank()) return listOf("")
            val lines = mutableListOf<String>()
            var current = StringBuilder()
            text.split(Regex("\\s+")).forEach { word ->
                val candidate = if (current.isEmpty()) word else "$current $word"
                if (paint.measureText(candidate) <= maxWidth) {
                    current = StringBuilder(candidate)
                } else {
                    if (current.isNotEmpty()) lines += current.toString()
                    current = StringBuilder(word)
                }
            }
            if (current.isNotEmpty()) lines += current.toString()
            return lines
        }
    }
}
