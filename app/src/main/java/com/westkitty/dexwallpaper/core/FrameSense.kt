package com.westkitty.dexwallpaper.core

import kotlin.math.max
import kotlin.math.min

data class Placement(val x: Float, val y: Float, val width: Float, val height: Float)
object FrameSense {
    fun place(sourceWidth: Int, sourceHeight: Int, targetWidth: Int, targetHeight: Int, raw: ImageEdit): Placement {
        require(sourceWidth > 0 && sourceHeight > 0 && targetWidth > 0 && targetHeight > 0)
        val edit = raw.bounded()
        val sx = targetWidth.toFloat() / sourceWidth
        val sy = targetHeight.toFloat() / sourceHeight
        if (edit.scale == ScaleMode.STRETCH) return Placement(0f, 0f, targetWidth.toFloat(), targetHeight.toFloat())
        val scale = when (edit.scale) {
            ScaleMode.FILL -> max(sx, sy)
            ScaleMode.FIT -> min(sx, sy)
            ScaleMode.FIT_HEIGHT -> sy
            ScaleMode.ORIGINAL -> 1f
            ScaleMode.STRETCH -> 1f
        } * edit.zoom
        val width = sourceWidth * scale
        val height = sourceHeight * scale
        val portrait = targetHeight > targetWidth
        val fx = if (portrait) edit.portraitX else edit.focalX
        val fy = if (portrait) edit.portraitY else edit.focalY
        return Placement((targetWidth - width) * fx, (targetHeight - height) * fy, width, height)
    }
}
object AssetIntegrity {
    fun hammingDistance(a: Long, b: Long): Int = java.lang.Long.bitCount(a xor b)
    fun nearDuplicate(a: Long, b: Long, threshold: Int = 5) = hammingDistance(a, b) <= threshold.coerceIn(0, 64)
    fun sampleSize(width: Int, height: Int, targetWidth: Int, targetHeight: Int): Int {
        require(width > 0 && height > 0 && targetWidth > 0 && targetHeight > 0)
        var sample = 1
        while (width / (sample * 2L) >= targetWidth && height / (sample * 2L) >= targetHeight && sample < 8192) sample *= 2
        // Also enforce a 16MP decode ceiling independently of aspect ratio.
        while (width.toLong() * height / sample / sample > 16_000_000 && sample < 8192) sample *= 2
        return sample
    }
}
