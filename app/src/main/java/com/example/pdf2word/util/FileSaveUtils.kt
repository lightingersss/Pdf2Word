package com.example.pdf2word.util

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File

fun saveFileToDownloads(
    context: Context,
    file: File,
    mimeType: String = "application/octet-stream"
): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        saveViaMediaStore(context, file, mimeType)
    } else {
        saveViaLegacy(context, file)
    }
}

private fun saveViaMediaStore(context: Context, file: File, mimeType: String): Boolean {
    val values = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, file.name)
        put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
    }
    val uri = context.contentResolver.insert(
        MediaStore.Downloads.EXTERNAL_CONTENT_URI, values
    ) ?: return false
    return try {
        context.contentResolver.openOutputStream(uri)?.use { out ->
            file.inputStream().use { it.copyTo(out) }
        } ?: return false
        true
    } catch (e: Exception) {
        false
    }
}

@Suppress("DEPRECATION")
private fun saveViaLegacy(context: Context, file: File): Boolean {
    val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
    if (!downloads.exists()) downloads.mkdirs()
    val target = File(downloads, file.name)
    return try {
        file.copyTo(target, overwrite = true)
        true
    } catch (e: Exception) {
        false
    }
}
