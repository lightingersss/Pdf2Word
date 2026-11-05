package com.example.pdf2word

import android.app.Application
import com.example.pdf2word.ocr.OCRManager
import com.example.pdf2word.util.PdfRenderUtils

class Pdf2WordApp : Application() {

    override fun onCreate() {
        super.onCreate()
        PdfRenderUtils.init(this)
        ocrManager = OCRManager(this).also { it.init() }
    }

    companion object {
        lateinit var ocrManager: OCRManager
            private set
    }
}
