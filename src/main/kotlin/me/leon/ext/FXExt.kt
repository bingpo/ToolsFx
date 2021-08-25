package me.leon.ext

import java.awt.Desktop
import java.awt.image.BufferedImage
import java.io.File
import java.net.URL
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.image.WritableImage
import javafx.scene.input.Clipboard
import javafx.scene.input.ClipboardContent
import javafx.stage.FileChooser
import javafx.stage.Window
import javax.imageio.ImageIO

fun String.copy() =
    Clipboard.getSystemClipboard().setContent(ClipboardContent().apply { putString(this@copy) })

fun clipboardText() = Clipboard.getSystemClipboard().string

fun Image.copy() =
    Clipboard.getSystemClipboard().setContent(ClipboardContent().apply { putImage(this@copy) })

fun String.openInBrowser() = Desktop.getDesktop().browse(URL(this).toURI())

fun BufferedImage.toFxImg(): Image {

    var wr: WritableImage? = null
    wr = WritableImage(width, height)
    val pw = wr.pixelWriter
    for (x in 0 until width) for (y in 0 until height) pw.setArgb(x, y, getRGB(x, y))
    return ImageView(wr).image
}

fun BufferedImage.writeFile(path: String = "E:/tmp.png", format: String = "png") {
    ImageIO.write(this, format, File(path))
}

fun Window.fileChooser() = FileChooser().apply { title = "请选择文件" }.showOpenDialog(this)
