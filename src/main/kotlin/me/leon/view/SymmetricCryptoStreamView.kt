package me.leon.view

import java.io.File
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleStringProperty
import javafx.geometry.Pos
import javafx.scene.control.Label
import javafx.scene.control.RadioButton
import javafx.scene.control.TextArea
import javafx.scene.control.TextField
import javafx.scene.image.Image
import me.leon.controller.SymmetricCryptoController
import me.leon.encode.base.base64Decode
import me.leon.ext.DEFAULT_SPACING
import me.leon.ext.cast
import me.leon.ext.clipboardText
import me.leon.ext.copy
import me.leon.ext.fileDraggedHandler
import me.leon.ext.hex2ByteArray
import tornadofx.FX.Companion.messages
import tornadofx.View
import tornadofx.action
import tornadofx.borderpane
import tornadofx.button
import tornadofx.checkbox
import tornadofx.combobox
import tornadofx.enableWhen
import tornadofx.get
import tornadofx.hbox
import tornadofx.imageview
import tornadofx.label
import tornadofx.paddingAll
import tornadofx.radiobutton
import tornadofx.textarea
import tornadofx.textfield
import tornadofx.togglegroup
import tornadofx.vbox

class SymmetricCryptoStreamView : View(messages["symmetricStream"]) {
    private val controller: SymmetricCryptoController by inject()
    override val closeable = SimpleBooleanProperty(false)
    private val isFile = SimpleBooleanProperty(false)
    private val isProcessing = SimpleBooleanProperty(false)
    private lateinit var input: TextArea
    private lateinit var key: TextField
    private lateinit var iv: TextField
    private var isEncrypt = true
    private lateinit var output: TextArea
    private val inputText: String
        get() = input.text
    private val outputText: String
        get() = output.text
    private val info
        get() = "Cipher: $cipher   charset: ${selectedCharset.get()}  file mode: ${isFile.get()} "
    private lateinit var infoLabel: Label
    private val keyByteArray
        get() =
            when (keyEncode) {
                "raw" -> key.text.toByteArray()
                "hex" -> key.text.hex2ByteArray()
                "base64" -> key.text.base64Decode()
                else -> byteArrayOf()
            }

    private var keyEncode = "raw"
    private var ivEncode = "raw"

    private val ivByteArray
        get() =
            when (ivEncode) {
                "raw" -> iv.text.toByteArray()
                "hex" -> iv.text.hex2ByteArray()
                "base64" -> iv.text.base64Decode()
                else -> byteArrayOf()
            }

    private val eventHandler = fileDraggedHandler {
        input.text =
            if (isFile.get())
                it.joinToString(System.lineSeparator(), transform = File::getAbsolutePath)
            else it.first().readText()
    }

    private val algs =
        mutableListOf(
            "RC4",
            "ChaCha",
            "VMPC",
            "HC128",
            "HC256",
            "Grainv1",
            "Grain128",
            "Salsa20",
            "XSalsa20",
            "Zuc-128",
            "Zuc-256",
        )
    private val selectedAlg = SimpleStringProperty(algs.first())

    private val cipher
        get() = selectedAlg.get()
    private val charsets = mutableListOf("UTF-8", "GBK", "GB2312", "GB18030", "ISO-8859-1", "BIG5")
    private val selectedCharset = SimpleStringProperty(charsets.first())

    private val centerNode = vbox {
        paddingAll = DEFAULT_SPACING
        spacing = DEFAULT_SPACING
        hbox {
            label(messages["input"])
            button(graphic = imageview(Image("/import.png"))) {
                action { input.text = clipboardText() }
            }
        }
        input =
            textarea {
                promptText = messages["inputHint"]
                isWrapText = true
                onDragEntered = eventHandler
            }
        hbox {
            alignment = Pos.CENTER_LEFT
            spacing = DEFAULT_SPACING
            label(messages["alg"])
            combobox(selectedAlg, algs) { cellFormat { text = it } }

            label("charset:")
            combobox(selectedCharset, charsets) { cellFormat { text = it } }
        }
        hbox {
            alignment = Pos.CENTER_LEFT
            label("key:")
            key = textfield { promptText = messages["keyHint"] }
            vbox {
                togglegroup {
                    spacing = DEFAULT_SPACING
                    paddingAll = DEFAULT_SPACING
                    radiobutton("raw") { isSelected = true }
                    radiobutton("hex")
                    radiobutton("base64")
                    selectedToggleProperty().addListener { _, _, new ->
                        keyEncode = new.cast<RadioButton>().text
                    }
                }
            }
            label("iv:")
            iv = textfield { promptText = messages["ivHint"] }
            vbox {
                togglegroup {
                    spacing = DEFAULT_SPACING
                    paddingAll = DEFAULT_SPACING
                    radiobutton("raw") { isSelected = true }
                    radiobutton("hex")
                    radiobutton("base64")
                    selectedToggleProperty().addListener { _, _, new ->
                        ivEncode = new.cast<RadioButton>().text
                    }
                }
            }
        }
        hbox {
            alignment = Pos.CENTER_LEFT
            togglegroup {
                spacing = DEFAULT_SPACING
                alignment = Pos.BASELINE_CENTER
                radiobutton(messages["encrypt"]) { isSelected = true }
                radiobutton(messages["decrypt"])
                selectedToggleProperty().addListener { _, _, new ->
                    isEncrypt = new.cast<RadioButton>().text == messages["encrypt"]
                    doCrypto()
                }
            }
            checkbox(messages["fileMode"], isFile)
            button(messages["run"], imageview(Image("/run.png"))) {
                enableWhen(!isProcessing)
                action { doCrypto() }
            }
        }
        hbox {
            label(messages["output"])
            spacing = DEFAULT_SPACING
            button(graphic = imageview(Image("/copy.png"))) { action { outputText.copy() } }
            button(graphic = imageview(Image("/up.png"))) {
                action {
                    input.text = outputText
                    output.text = ""
                }
            }
        }
        output =
            textarea {
                promptText = messages["outputHint"]
                isWrapText = true
            }
    }
    override val root = borderpane {
        center = centerNode
        bottom = hbox { infoLabel = label(info) }
    }

    private fun doCrypto() {
        runAsync {
            isProcessing.value = true
            if (isEncrypt)
                if (isFile.get())
                    inputText.split("\n|\r\n".toRegex()).joinToString("\n") {
                        controller.encryptByFile(keyByteArray, it, ivByteArray, cipher)
                    }
                else
                    controller.encrypt(
                        keyByteArray,
                        inputText,
                        ivByteArray,
                        cipher,
                        selectedCharset.get()
                    )
            else if (isFile.get())
                inputText.split("\n|\r\n".toRegex()).joinToString("\n") {
                    controller.decryptByFile(keyByteArray, it, ivByteArray, cipher)
                }
            else
                controller.decrypt(
                    keyByteArray,
                    inputText,
                    ivByteArray,
                    cipher,
                    selectedCharset.get()
                )
        } ui
            {
                isProcessing.value = false
                output.text = it
                infoLabel.text = info
            }
    }
}
