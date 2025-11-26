package com.lunarlog.ui.analysis

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import java.io.OutputStream
import java.time.LocalDate

object ReportGenerator {

    fun generatePdf(
        outputStream: OutputStream,
        cycleHistory: List<Pair<LocalDate, Int>>,
        symptomCounts: Map<String, Int>,
        moodCounts: Map<String, Int>
    ) {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas
        val paint = Paint()

        // Title
        paint.textSize = 24f
        paint.color = Color.BLACK
        paint.isFakeBoldText = true
        canvas.drawText("LunarLog Health Report", 50f, 50f, paint)

        // Date
        paint.textSize = 12f
        paint.isFakeBoldText = false
        canvas.drawText("Generated on: ${LocalDate.now()}", 50f, 70f, paint)

        // Cycle History Section
        var yPos = 120f
        paint.textSize = 18f
        paint.isFakeBoldText = true
        canvas.drawText("Cycle History (Last 6 Months)", 50f, yPos, paint)
        
        yPos += 30f
        paint.textSize = 14f
        paint.isFakeBoldText = false
        
        if (cycleHistory.isEmpty()) {
            canvas.drawText("No completed cycles recorded.", 50f, yPos, paint)
            yPos += 20f
        } else {
            cycleHistory.forEach { (date, length) ->
                canvas.drawText("Start: $date  -  Length: $length days", 50f, yPos, paint)
                yPos += 20f
            }
        }

        // Symptom Frequency
        yPos += 30f
        paint.textSize = 18f
        paint.isFakeBoldText = true
        canvas.drawText("Symptom Frequency", 50f, yPos, paint)

        yPos += 30f
        paint.textSize = 14f
        paint.isFakeBoldText = false
        if (symptomCounts.isEmpty()) {
             canvas.drawText("No symptoms recorded.", 50f, yPos, paint)
             yPos += 20f
        } else {
            symptomCounts.forEach { (symptom, count) ->
                canvas.drawText("$symptom: $count times", 50f, yPos, paint)
                yPos += 20f
            }
        }

        // Mood Frequency
        yPos += 30f
        paint.textSize = 18f
        paint.isFakeBoldText = true
        canvas.drawText("Mood Frequency", 50f, yPos, paint)

        yPos += 30f
        paint.textSize = 14f
        paint.isFakeBoldText = false
        if (moodCounts.isEmpty()) {
             canvas.drawText("No moods recorded.", 50f, yPos, paint)
             yPos += 20f
        } else {
            moodCounts.forEach { (mood, count) ->
                canvas.drawText("$mood: $count times", 50f, yPos, paint)
                yPos += 20f
            }
        }

        pdfDocument.finishPage(page)

        try {
            pdfDocument.writeTo(outputStream)
        } finally {
            pdfDocument.close()
            outputStream.close()
        }
    }

    fun generateCsv(
        outputStream: OutputStream,
        cycleHistory: List<Pair<LocalDate, Int>>
    ) {
        outputStream.use { output ->
            val header = "Date,Type,Value\n"
            output.write(header.toByteArray())
            
            cycleHistory.forEach { (date, length) ->
                val line = "$date,CycleLength,$length\n"
                output.write(line.toByteArray())
            }
        }
    }
}
