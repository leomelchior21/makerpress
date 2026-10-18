package com.makerpress.app.core.rendering

import android.content.Context
import android.graphics.*
import android.net.Uri
import java.io.File
import java.util.UUID
import androidx.exifinterface.media.ExifInterface

class ImageImporter(private val context: Context) {
    fun import(uri: Uri): String {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, bounds) }
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Não foi possível abrir a imagem." }
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 1600) sample *= 2
        val decoded = resolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample }) } ?: error("Formato de imagem não suportado.")
        val exif = runCatching { resolver.openInputStream(uri).use { stream -> stream?.let { ExifInterface(it) } } }.getOrNull()
        val matrix = Matrix().apply { if (exif?.isFlipped == true) postScale(-1f, 1f); postRotate((exif?.rotationDegrees ?: 0).toFloat()) }
        val bitmap = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
        if (bitmap != decoded) decoded.recycle()
        val name = "${UUID.randomUUID()}.png"
        val dir = File(context.filesDir, "images").apply { mkdirs() }
        File(dir, name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        return name
    }
}
