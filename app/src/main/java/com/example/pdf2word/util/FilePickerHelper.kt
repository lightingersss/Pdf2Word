package com.example.pdf2word.util

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun rememberPdfPicker(
    onPicked: (File) -> Unit,
    onError: (String) -> Unit
): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            try {
                val file = copyUriToPrivateStorage(context, uri, "imported.pdf")
                onPicked(file)
            } catch (e: Exception) {
                onError("复制 PDF 失败：${e.message}")
            }
        }
    }
    return { launcher.launch(arrayOf("application/pdf")) }
}

suspend fun copyUriToPrivateStorage(
    context: Context,
    uri: Uri,
    fileName: String
): File = withContext(Dispatchers.IO) {
    val target = File(context.filesDir, fileName)
    context.contentResolver.openInputStream(uri)?.use { input ->
        target.outputStream().use { output ->
            input.copyTo(output)
        }
    } ?: throw IllegalStateException("无法打开输入流")
    target
}
