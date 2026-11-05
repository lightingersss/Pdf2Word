package com.example.pdf2word.net

import android.graphics.Bitmap
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.ByteArrayOutputStream

class TencentOcrClient(
    private val endpoint: String = "https://ocr.tencentcloudapi.com",
    private val region: String = "ap-guangzhou"
) {
    private val client = OkHttpClient()

    suspend fun recognizeTable(bitmap: Bitmap): JSONObject = withContext(Dispatchers.IO) {
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, baos)
        val imageBase64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)

        val payload = JSONObject().apply {
            put("ImageBase64", imageBase64)
        }.toString()

        val request = Request.Builder()
            .url(endpoint)
            .post(payload.toRequestBody("application/json; charset=utf-8".toMediaType()))
            .header("Content-Type", "application/json; charset=utf-8")
            .header("Host", "ocr.tencentcloudapi.com")
            .header("X-TC-Action", "RecognizeTableAccurateOCR")
            .header("X-TC-Version", "2018-11-19")
            .header("X-TC-Region", region)
            .header("X-TC-Timestamp", (System.currentTimeMillis() / 1000).toString())
            .header("Authorization", getAuthorization())
            .build()

        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw IllegalStateException("OCR HTTP ${response.code}: $body")
            JSONObject(body)
        }
    }

    private fun getAuthorization(): String {
        return "TC3-HMAC-SHA256 Credential=.../..., SignedHeaders=content-type;host, Signature=..."
    }
}
