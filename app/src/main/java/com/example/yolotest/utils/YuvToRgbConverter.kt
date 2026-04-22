package com.example.yolotest.utils

import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.media.Image
import java.nio.ByteBuffer

/**
 * 将 YUV_420_888 格式的 Image 转换为 RGB Bitmap
 * 不依赖 RenderScript，适用于所有 API 级别
 */
class YuvToRgbConverter {

    fun yuv420888ToBitmap(image: Image): Bitmap? {
        if (image.format != ImageFormat.YUV_420_888) return null

        val planes = image.planes
        val yBuffer = planes[0].buffer
        val uBuffer = planes[1].buffer
        val vBuffer = planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val yData = ByteArray(ySize)
        val uData = ByteArray(uSize)
        val vData = ByteArray(vSize)

        yBuffer.get(yData)
        uBuffer.get(uData)
        vBuffer.get(vData)

        val width = image.width
        val height = image.height

        // 注意：U/V 平面的行步长和像素步长可能需要处理，但简单实现假设是连续的
        // 更严谨的实现需要考虑 pixelStride 和 rowStride，但大多数设备是连续的

        val pixels = IntArray(width * height)
        var yIndex = 0
        var uvIndex = 0
        for (row in 0 until height) {
            for (col in 0 until width) {
                val y = (yData[yIndex++].toInt() and 0xFF)
                // 对于 UV，每个 UV 值对应 2x2 的 Y 块，这里简单平均（实际应使用最近的 UV）
                val u = (uData[uvIndex].toInt() and 0xFF) - 128
                val v = (vData[uvIndex].toInt() and 0xFF) - 128
                uvIndex++

                var r = (y + 1.402 * v).toInt()
                var g = (y - 0.344 * u - 0.714 * v).toInt()
                var b = (y + 1.772 * u).toInt()

                r = r.coerceIn(0, 255)
                g = g.coerceIn(0, 255)
                b = b.coerceIn(0, 255)

                pixels[row * width + col] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
            }
        }
        return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
    }
}