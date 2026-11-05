package com.example.pdf2word.util

import android.content.Context
import android.graphics.Bitmap
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.io.MemoryUsageSetting
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.rendering.PDFRenderer
import java.io.File

object PdfRenderUtils {

    fun init(context: Context) {
        PDFBoxResourceLoader.init(context.applicationContext)
    }

    fun renderPdfToBitmaps(
        context: Context,
        pdfPath: String,
        dpi: Int = 300
    ): List<Bitmap> {
        val pdfFile = File(pdfPath)
        require(pdfFile.exists()) { "PDF 不存在：$pdfPath" }

        val memSetting = MemoryUsageSetting.setupMixed(8L * 1024 * 1024)
            .setTempDir(context.cacheDir)

        val bitmaps = mutableListOf<Bitmap>()
        PDDocument.load(pdfFile, memSetting).use { document ->
            val renderer = PDFRenderer(document)
            val pageCount = document.numberOfPages
            for (i in 0 until pageCount) {
                val bitmap = renderer.renderImageWithDPI(i, dpi.toFloat())
                bitmaps.add(bitmap)
            }
        }
        return bitmaps
    }
}
