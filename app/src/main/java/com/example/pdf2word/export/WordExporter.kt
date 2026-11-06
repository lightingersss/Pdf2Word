package com.example.pdf2word.export

import android.graphics.Bitmap
import org.apache.poi.util.Units
import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.apache.poi.xwpf.usermodel.XWPFTable
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBorder
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.math.BigInteger

fun XWPFTable.hideAllBorders() {
    val tblPr = this.ctTbl.tblPr ?: this.ctTbl.addNewTblPr()
    val borders = if (tblPr.isSetTblBorders) tblPr.tblBorders else tblPr.addNewTblBorders()

    fun CTBorder.none(): CTBorder = apply {
        this.`val` = STBorder.NONE
        this.sz = BigInteger.ZERO
        this.space = BigInteger.ZERO
    }

    if (borders.isSetTop) borders.top.none() else borders.addNewTop().none()
    if (borders.isSetBottom) borders.bottom.none() else borders.addNewBottom().none()
    if (borders.isSetLeft) borders.left.none() else borders.addNewLeft().none()
    if (borders.isSetRight) borders.right.none() else borders.addNewRight().none()
    if (borders.isSetInsideH) borders.insideH.none() else borders.addNewInsideH().none()
    if (borders.isSetInsideV) borders.insideV.none() else borders.addNewInsideV().none()
}

fun createWordWithTextAndImage(
    outputFile: File,
    text: String,
    bitmap: Bitmap,
    imageWidthPercent: Double = 0.8
) {
    XWPFDocument().use { document ->
        val p = document.addParagraph()
        val run = p.createRun()
        run.setText(text)

        val imgParagraph = document.addParagraph()
        val imgRun = imgParagraph.createRun()
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
        val bytes = baos.toByteArray()
        val pageWidthEmu = Units.toEMU(6.0 * 72.0)
        val targetWidthEmu = (pageWidthEmu * imageWidthPercent).toInt()
        imgRun.addPicture(
            bytes.inputStream(),
            XWPFDocument.PICTURE_TYPE_PNG,
            "image.png",
            targetWidthEmu,
            Units.toEMU(200.0)
        )

        FileOutputStream(outputFile).use { document.write(it) }
    }
}

fun createHiddenTableLayout(outputFile: File, bitmap: Bitmap, text: String) {
    XWPFDocument().use { document ->
        val table = document.createTable(3, 2)
        table.hideAllBorders()

        table.getRow(0).getCell(0).apply {
            removeParagraph(0)
            addParagraph().createRun().setText(text)
        }

        table.getRow(0).getCell(1).apply {
            removeParagraph(0)
            val p = addParagraph()
            val run = p.createRun()
            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
            run.addPicture(
                baos.toByteArray().inputStream(),
                XWPFDocument.PICTURE_TYPE_PNG,
                "img.png",
                Units.toEMU(200.0),
                Units.toEMU(120.0)
            )
        }

        table.getRow(1).getCell(0).apply {
            removeParagraph(0)
            addParagraph().createRun().setText("嵌套内容 / Nested")
        }

        FileOutputStream(outputFile).use { document.write(it) }
    }
}
