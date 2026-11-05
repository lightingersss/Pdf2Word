package com.example.pdf2word

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.pdf2word.net.TencentOcrClient
import com.example.pdf2word.ocr.isPureImage
import com.example.pdf2word.util.PdfRenderUtils
import com.example.pdf2word.util.saveFileToDownloads
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.poi.util.Units
import org.apache.poi.xwpf.usermodel.BreakType
import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

sealed class UiState {
    object Idle : UiState()
    data class Loading(val msg: String) : UiState()
    data class Success(val outputFile: File) : UiState()
    data class Error(val msg: String) : UiState()
}

sealed class PageContent {
    data class Text(val text: String) : PageContent()
    data class Image(val bitmap: Bitmap) : PageContent()
    data class OcrJson(val json: JSONObject) : PageContent()
}

class ConversionViewModel(app: Application) : AndroidViewModel(app) {

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val ocrManager = Pdf2WordApp.ocrManager
    private val tencentClient = TencentOcrClient()

    fun convert(pdfPath: String, outputFileName: String) {
        viewModelScope.launch {
            val app = getApplication<Application>()
            var bitmaps: List<Bitmap> = emptyList()
            try {
                _uiState.value = UiState.Loading("正在渲染 PDF...")
                bitmaps = withContext(Dispatchers.IO) {
                    PdfRenderUtils.renderPdfToBitmaps(app, pdfPath)
                }

                _uiState.value = UiState.Loading("正在识别内容（共 ${bitmaps.size} 页）...")

                val pages = withContext(Dispatchers.IO) {
                    bitmaps.map { bmp ->
                        if (isPureImage(bmp)) {
                            PageContent.Image(bmp)
                        } else {
                            val localText = try {
                                ocrManager.recognizeText(bmp)
                            } catch (e: Exception) {
                                ""
                            }

                            if (localText.length > 20) {
                                PageContent.Text(localText)
                            } else {
                                try {
                                    val json = tencentClient.recognizeTable(bmp)
                                    PageContent.OcrJson(json)
                                } catch (e: Exception) {
                                    PageContent.Text(localText.ifBlank { "[无法识别的内容]" })
                                }
                            }
                        }
                    }
                }

                _uiState.value = UiState.Loading("正在生成 Word...")
                val cacheFile = File(app.cacheDir, outputFileName)
                withContext(Dispatchers.IO) {
                    generateWord(cacheFile, pages)
                }

                _uiState.value = UiState.Loading("正在保存到 Downloads...")
                val saved = withContext(Dispatchers.IO) {
                    saveFileToDownloads(
                        app, cacheFile,
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                    )
                }

                _uiState.value = if (saved) {
                    UiState.Success(cacheFile)
                } else {
                    UiState.Error("保存到 Downloads 失败")
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error("转换失败：${e.message}")
            } finally {
                bitmaps.forEach { if (!it.isRecycled) it.recycle() }
            }
        }
    }

    private fun generateWord(outputFile: File, pages: List<PageContent>) {
        XWPFDocument().use { document ->
            for ((idx, page) in pages.withIndex()) {
                when (page) {
                    is PageContent.Text -> {
                        val p = document.createParagraph()
                        p.createRun().setText(page.text)
                    }
                    is PageContent.Image -> {
                        val p = document.createParagraph()
                        val run = p.createRun()
                        val baos = ByteArrayOutputStream()
                        page.bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
                        val width = Units.toEMU(6.0 * 72.0 * 0.9)
                        run.addPicture(
                            baos.toByteArray().inputStream(),
                            XWPFDocument.PICTURE_TYPE_PNG,
                            "img_$idx.png",
                            width,
                            Units.toEMU(200.0)
                        )
                    }
                    is PageContent.OcrJson -> {
                        val p = document.createParagraph()
                        p.createRun().setText(extractOcrText(page.json))
                    }
                }
                document.createParagraph().createRun().addBreak(BreakType.PAGE)
            }
            FileOutputStream(outputFile).use { document.write(it) }
        }
    }

    private fun extractOcrText(json: JSONObject): String {
        val response = json.optJSONObject("Response")
        val array = response?.optJSONArray("TextDetections")
            ?: json.optJSONArray("TextDetections")
            ?: response?.optJSONArray("TableDetections")
            ?: json.optJSONArray("TableDetections")
            ?: return "[无文字]"
        val sb = StringBuilder()
        for (i in 0 until array.length()) {
            val d = array.getJSONObject(i)
            val t = d.optString("DetectedText", d.optString("Text", ""))
            if (t.isNotBlank()) sb.append(t).append(' ')
        }
        return sb.toString().ifBlank { "[无文字]" }
    }
}
