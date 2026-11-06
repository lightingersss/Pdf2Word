package com.example.pdf2word.ocr

import android.content.Context
import android.graphics.Bitmap
import com.rmtheis.tess_two.TessBaseAPI
import java.io.File
import java.io.FileOutputStream

class OCRManager(private val context: Context) {

    private var tessApi: TessBaseAPI? = null

    fun init() {
        val tessDataDir = File(context.filesDir, "tessdata")
        if (!tessDataDir.exists()) tessDataDir.mkdirs()
        copyAssetIfNotExists("tessdata/chi_sim.traineddata", File(tessDataDir, "chi_sim.traineddata"))
        copyAssetIfNotExists("tessdata/eng.traineddata", File(tessDataDir, "eng.traineddata"))

        tessApi = TessBaseAPI().apply {
            init(context.filesDir.absolutePath, "chi_sim+eng")
        }
    }

    private fun copyAssetIfNotExists(assetPath: String, target: File) {
        if (target.exists()) return
        context.assets.open(assetPath).use { input ->
            FileOutputStream(target).use { output -> input.copyTo(output) }
        }
    }

    fun recognizeText(bitmap: Bitmap): String {
        val api = tessApi ?: error("OCRManager 未 init")
        api.setImage(bitmap)
        return api.getUTF8Text()?.trim().orEmpty()
    }

    fun release() {
        tessApi?.end()
        tessApi = null
    }
}
