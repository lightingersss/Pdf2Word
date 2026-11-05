package com.example.pdf2word.ocr

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.abs

fun isPureImage(bitmap: Bitmap): Boolean {
    if (bitmap.width < 20 || bitmap.height < 20) return true

    val sampleStep = maxOf(1, minOf(bitmap.width, bitmap.height) / 100)
    var sum = 0.0
    var sumSq = 0.0
    var count = 0
    var edgeCount = 0
    var prevLum = -1.0

    for (y in 0 until bitmap.height step sampleStep) {
        for (x in 0 until bitmap.width step sampleStep) {
            val pixel = bitmap.getPixel(x, y)
            val lum = (0.299 * Color.red(pixel) + 0.587 * Color.green(pixel) + 0.114 * Color.blue(pixel))
            sum += lum
            sumSq += lum * lum
            count++

            if (prevLum >= 0 && abs(lum - prevLum) > 40) edgeCount++
            prevLum = lum
        }
    }

    if (count == 0) return true
    val mean = sum / count
    val variance = sumSq / count - mean * mean
    val edgeRatio = edgeCount.toDouble() / count

    return variance < 800 || edgeRatio < 0.08
}
