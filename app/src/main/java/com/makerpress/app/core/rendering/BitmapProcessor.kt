package com.makerpress.app.core.rendering

import android.graphics.*
import com.makerpress.app.domain.Dither

object BitmapProcessor {
    fun monochrome(source: Bitmap, mode: Dither, brightness: Float = 0f, contrast: Float = 1f): Bitmap {
        val w = source.width; val h = source.height
        require(w.toLong() * h <= 8_000_000) { "Imagem muito grande." }
        val pixels = IntArray(w * h); source.getPixels(pixels, 0, w, 0, 0, w, h)
        val values = FloatArray(pixels.size) { i ->
            val c = pixels[i]; val a = Color.alpha(c) / 255f
            val gray = (.299f * Color.red(c) + .587f * Color.green(c) + .114f * Color.blue(c)) * a + 255f * (1 - a)
            ((gray - 128) * contrast + 128 + brightness).coerceIn(0f, 255f)
        }
        fun diffuse(x: Int, y: Int, delta: Float) { if (x in 0 until w && y in 0 until h) values[y * w + x] += delta }
        for (y in 0 until h) for (x in 0 until w) {
            val i = y * w + x; val old = values[i]; val value = if (old < 128) 0f else 255f
            pixels[i] = if (value == 0f) Color.BLACK else Color.WHITE
            val error = old - value
            when (mode) {
                Dither.THRESHOLD -> Unit
                Dither.FLOYD_STEINBERG -> { diffuse(x + 1, y, error * 7 / 16); diffuse(x - 1, y + 1, error * 3 / 16); diffuse(x, y + 1, error * 5 / 16); diffuse(x + 1, y + 1, error / 16) }
                Dither.ATKINSON -> { val d = error / 8; diffuse(x + 1, y, d); diffuse(x + 2, y, d); diffuse(x - 1, y + 1, d); diffuse(x, y + 1, d); diffuse(x + 1, y + 1, d); diffuse(x, y + 2, d) }
            }
        }
        return Bitmap.createBitmap(pixels, w, h, Bitmap.Config.ARGB_8888)
    }
    fun pixels(bitmap: Bitmap): BooleanArray {
        val data = IntArray(bitmap.width * bitmap.height); bitmap.getPixels(data, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        return BooleanArray(data.size) { Color.red(data[it]) < 128 }
    }
}
