package com.westkitty.dexwallpaper.wallpaper

import android.graphics.*
import androidx.exifinterface.media.ExifInterface
import com.westkitty.dexwallpaper.core.*
import java.io.File

/** Owns decoded intermediates. Returned bitmaps belong to the caller; originals are never overwritten. */
class BitmapPipeline {
    fun decode(file: File, width: Int, height: Int): Bitmap {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Unreadable image" }
        val options = BitmapFactory.Options().apply {
            inSampleSize = AssetIntegrity.sampleSize(bounds.outWidth, bounds.outHeight, width, height)
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decoded = requireNotNull(BitmapFactory.decodeFile(file.path, options)) { "Image decoder rejected this file" }
        val orientation = try { ExifInterface(file).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL) }
            catch (_: java.io.IOException) { ExifInterface.ORIENTATION_NORMAL }
        val matrix = Matrix().apply {
            when (orientation) {
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> setScale(-1f, 1f)
                ExifInterface.ORIENTATION_ROTATE_180 -> setRotate(180f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> setScale(1f, -1f)
                ExifInterface.ORIENTATION_TRANSPOSE -> { setRotate(90f); postScale(-1f, 1f) }
                ExifInterface.ORIENTATION_ROTATE_90 -> setRotate(90f)
                ExifInterface.ORIENTATION_TRANSVERSE -> { setRotate(-90f); postScale(-1f, 1f) }
                ExifInterface.ORIENTATION_ROTATE_270 -> setRotate(-90f)
            }
        }
        if (matrix.isIdentity) return decoded
        return try { Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true).also { if (it !== decoded) decoded.recycle() } }
        catch (e: Exception) { decoded.recycle(); throw e }
    }

    fun render(file: File, width: Int, height: Int, raw: ImageEdit, safeMode: Boolean = false): Bitmap {
        require(width in 1..8192 && height in 1..8192 && width.toLong() * height <= 16_000_000) { "Output exceeds the 16MP safety limit" }
        val edit = raw.bounded()
        val source = decode(file, width, height)
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        try {
            val canvas = Canvas(output)
            canvas.drawColor(edit.backgroundColor)
            val place = FrameSense.place(source.width, source.height, width, height, edit)
            val cm = ColorMatrix().apply { setSaturation(edit.saturation) }
            val c = edit.contrast
            val offset = (1 - c) * 127.5f + edit.brightness * 255f
            cm.postConcat(ColorMatrix(floatArrayOf(c,0f,0f,0f,offset, 0f,c,0f,0f,offset, 0f,0f,c,0f,offset, 0f,0f,0f,1f,0f)))
            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply { colorFilter = ColorMatrixColorFilter(cm) }
            val blur = if (!safeMode) edit.blur else 0f
            var small: Bitmap? = null
            try {
                val drawable = if (blur > .1f) {
                    val divisor = 1f + blur * 2f
                    Bitmap.createScaledBitmap(source, (source.width / divisor).toInt().coerceAtLeast(1), (source.height / divisor).toInt().coerceAtLeast(1), true).also { small = it }
                } else source
                canvas.drawBitmap(drawable, null, RectF(place.x, place.y, place.x + place.width, place.y + place.height), paint)
            } finally { if (small !== source) small?.recycle() }
            if (edit.dim > 0) canvas.drawColor(Color.argb((edit.dim * 255).toInt(), 0, 0, 0))
            if (!safeMode && edit.vignette > 0) {
                val radius = kotlin.math.hypot(width.toFloat(), height.toFloat()) / 2f
                val shade = Paint().apply { shader = RadialGradient(width / 2f, height / 2f, radius,
                    intArrayOf(Color.TRANSPARENT, Color.argb((edit.vignette * 230).toInt(), 0, 0, 0)), floatArrayOf(.35f, 1f), Shader.TileMode.CLAMP) }
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), shade)
            }
            val safePaint = Paint().apply { color = Color.argb(110, 0, 0, 0) }
            canvas.drawRect(0f, 0f, width.toFloat(), height * edit.safeTop, safePaint)
            canvas.drawRect(0f, height * (1 - edit.safeBottom), width.toFloat(), height.toFloat(), safePaint)
            return output
        } catch (e: Exception) { output.recycle(); throw e }
        finally { source.recycle() }
    }

    fun perceptualHash(bitmap: Bitmap): Long {
        val small = Bitmap.createScaledBitmap(bitmap, 9, 8, true)
        var hash = 0L
        for (y in 0..7) for (x in 0..7) {
            fun luminance(pixel: Int) = Color.red(pixel) * 299 + Color.green(pixel) * 587 + Color.blue(pixel) * 114
            if (luminance(small.getPixel(x, y)) > luminance(small.getPixel(x + 1, y))) hash = hash or (1L shl (y * 8 + x))
        }
        if (small !== bitmap) small.recycle()
        return hash
    }
}
