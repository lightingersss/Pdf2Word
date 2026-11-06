package com.example.pdf2word.export

import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

fun buildWordFromOcrCoordinates(
    outputFile: File,
    ocrJson: JSONObject,
    pageWidthPx: Int,
    columnCount: Int = 3
) {
    val detections = ocrJson.optJSONArray("TextDetections")
        ?: ocrJson.optJSONArray("TableDetections")
        ?: JSONArray()

    data class Item(val text: String, val x: Double, val y: Double, val w: Double, val h: Double)

    val items = mutableListOf<Item>()
    for (i in 0 until detections.length()) {
        val d = detections.getJSONObject(i)
        val polygon = d.optJSONObject("ItemPolygon") ?: d.optJSONObject("Polygon") ?: continue
        val text = d.optString("DetectedText", "")
        if (text.isBlank()) continue
        val x = polygon.optDouble("X", polygon.optDouble("Left", 0.0))
        val y = polygon.optDouble("Y", polygon.optDouble("Top", 0.0))
        val w = polygon.optDouble("Width", 0.0)
        val h = polygon.optDouble("Height", 0.0)
        items.add(Item(text, x, y, w, h))
    }

    if (items.isEmpty()) return

    val sorted = items.sortedBy { it.y }
    val rows = mutableListOf<MutableList<Item>>()
    var currentRow = mutableListOf<Item>()
    var currentRowY = sorted.first().y
    val rowThreshold = sorted.first().h.coerceAtLeast(10.0) * 0.8

    for (item in sorted) {
        if (item.y - currentRowY > rowThreshold && currentRow.isNotEmpty()) {
            rows.add(currentRow)
            currentRow = mutableListOf()
            currentRowY = item.y
        }
        currentRow.add(item)
    }
    if (currentRow.isNotEmpty()) rows.add(currentRow)

    XWPFDocument().use { document ->
        val table = document.createTable(rows.size, columnCount)
        table.hideAllBorders()

        for ((rowIndex, rowItems) in rows.withIndex()) {
            val row = table.getRow(rowIndex)
            val colWidth = pageWidthPx.toDouble() / columnCount
            for (item in rowItems.sortedBy { it.x }) {
                val colIndex = (item.x / colWidth).toInt().coerceIn(0, columnCount - 1)
                val cell = row.getCell(colIndex)
                cell.removeParagraph(0)
                cell.addParagraph().createRun().setText(item.text)
            }
        }

        FileOutputStream(outputFile).use { document.write(it) }
    }
}
