package com.lunarlog.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lunarlog.ui.analysis.ReportGenerator
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class ReportDeviceTest {
    @Test fun longNamesProduceReadableMultiPagePdf() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val file = File(context.getExternalFilesDir(null), "multipage-verification.pdf")
        val symptoms = (1..50).associate { "Observation $it " + "longcustomword".repeat(10) to it }
        file.outputStream().use { ReportGenerator.generatePdf(it, emptyList(), symptoms, mapOf("Calm" to 2)) }
        PdfRenderer(ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)).use { pdf ->
            assertTrue("Long wrapped names should span several pages", pdf.pageCount >= 3)
            pdf.openPage(1).use { page ->
                val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                bitmap.eraseColor(android.graphics.Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                File(context.getExternalFilesDir(null), "report-page-2.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                bitmap.recycle()
            }
        }
    }
}
