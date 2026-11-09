package com.rozkaam.app.utils

import android.graphics.Bitmap
import android.graphics.Color
import androidx.camera.core.ImageProxy
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeWriter
import org.json.JSONObject

data class TaskQrPayload(
    val app: String,
    val taskId: String,
    val otp: String = "",
    val qrToken: String? = null,
    val purpose: String = PURPOSE_ARRIVAL
) {
    fun toJsonString(): String {
        val fields = mutableListOf(
            "\"app\":${app.toJsonStringValue()}",
            "\"taskId\":${taskId.toJsonStringValue()}",
            "\"purpose\":${purpose.toJsonStringValue()}"
        )
        if (otp.isNotBlank()) fields += "\"otp\":${otp.toJsonStringValue()}"
        qrToken?.let { fields += "\"qrToken\":${it.toJsonStringValue()}" }
        return fields.joinToString(prefix = "{", postfix = "}", separator = ",")
    }

    companion object {
        const val APP_IDENTIFIER = "rozkaam"
        const val PURPOSE_ARRIVAL = "arrival"
        const val PURPOSE_PROOF = "proof"

        fun fromJsonString(raw: String): TaskQrPayload? {
            return runCatching {
                val json = JSONObject(raw)
                val app = json.optString("app")
                if (app != APP_IDENTIFIER) return null
                val taskId = json.optString("taskId")
                val otp = json.optString("otp")
                val qrToken = json.optString("qrToken").takeIf { it.isNotBlank() }
                val purpose = json.optString("purpose", PURPOSE_ARRIVAL)
                if (taskId.isBlank()) return null
                when (purpose) {
                    PURPOSE_ARRIVAL -> if (!otp.matches(Regex("^\\d{4}$"))) return null
                    PURPOSE_PROOF -> if (qrToken?.matches(Regex("^[0-9a-fA-F]{64}$")) != true) return null
                    else -> return null
                }
                TaskQrPayload(
                    app = app,
                    taskId = taskId,
                    otp = otp,
                    qrToken = qrToken,
                    purpose = purpose
                )
            }.getOrNull()
        }
    }
}

private fun String.toJsonStringValue(): String = buildString {
    append('"')
    for (character in this@toJsonStringValue) {
        when (character) {
            '"' -> append("\\\"")
            '\\' -> append("\\\\")
            '\b' -> append("\\b")
            '\u000C' -> append("\\f")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            else -> if (character.code < 0x20) {
                append("\\u%04x".format(character.code))
            } else {
                append(character)
            }
        }
    }
    append('"')
}

object QrVerificationHelper {

    fun generateQrBitmap(
        payload: String,
        sizePx: Int = 512,
        darkColor: Int = Color.BLACK,
        lightColor: Int = Color.WHITE
    ): Bitmap {
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(payload, BarcodeFormat.QR_CODE, sizePx, sizePx)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) darkColor else lightColor)
            }
        }
        return bitmap
    }

    fun decodeQrBitmap(bitmap: Bitmap): TaskQrPayload? {
        return runCatching {
            val width = bitmap.width
            val height = bitmap.height
            val pixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

            val source = RGBLuminanceSource(width, height, pixels)
            val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
            val result = MultiFormatReader().decode(binaryBitmap)
            TaskQrPayload.fromJsonString(result.text)
        }.getOrNull()
    }

    fun decodeImageProxy(image: ImageProxy): TaskQrPayload? {
        val plane = image.planes.getOrNull(0) ?: return null
        return runCatching {
            val buffer = plane.buffer.duplicate()
            val width = image.width
            val height = image.height
            val bytes = ByteArray(width * height)
            for (row in 0 until height) {
                for (column in 0 until width) {
                    bytes[row * width + column] =
                        buffer.get(row * plane.rowStride + column * plane.pixelStride)
                }
            }
            val source = PlanarYUVLuminanceSource(
                bytes,
                width,
                height,
                0,
                0,
                width,
                height,
                false
            )
            val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
            val result = MultiFormatReader().decode(binaryBitmap)
            TaskQrPayload.fromJsonString(result.text)
        }.getOrNull()
    }
}
