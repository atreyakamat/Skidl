package com.skidl.ui.components

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

@Composable
fun rememberQrCode(content: String, size: Int = 512): ImageBitmap? {
    return remember(content, size) {
        if (content.isBlank()) return@remember null
        val writer = QRCodeWriter()
        val matrix = try {
            writer.encode(content, BarcodeFormat.QR_CODE, size, size)
        } catch (t: Throwable) {
            return@remember null
        }
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        for (x in 0 until size) {
            for (y in 0 until size) {
                bmp.setPixel(x, y, if (matrix.get(x, y)) 0xFF000000.toInt() else 0xFFFFFFFF.toInt())
            }
        }
        bmp.asImageBitmap()
    }
}
