package com.westkitty.dexwallpaper.core

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import com.westkitty.dexwallpaper.data.AssetEntity
import java.io.File

/** Bounded, silent local MP4/WebM clips. Metadata is validated before library insertion. */
data class VideoClipInfo(val width: Int, val height: Int, val durationMs: Long)
object VideoClips {
    const val MAX_BYTES = 30L * 1024L * 1024L
    const val MAX_DURATION_MS = 15_000L
    fun suffixFor(mime: String?, name: String?): String? {
        val type = mime?.lowercase()?.substringBefore(';')
        return when (type) {
            "video/mp4" -> "mp4"
            "video/webm" -> "webm"
            "application/octet-stream", null, "" -> when (name?.substringAfterLast('.')?.lowercase()) {
                "mp4" -> "mp4"
                "webm" -> "webm"
                else -> null
            }
            else -> null
        }
    }
    fun isVideo(asset: AssetEntity) = asset.relativePath.endsWith(".mp4", true) ||
        asset.relativePath.endsWith(".webm", true)
    fun inspect(file: File): VideoClipInfo {
        require(file.isFile && file.length() in 1..MAX_BYTES) { "Clip is empty or exceeds 30 MB." }
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(file.absolutePath)
            require(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO) == "yes") {
                "Clip has no readable video track."
            }
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            require(duration in 200..MAX_DURATION_MS) { "Clips must be 0.2 to 15 seconds long." }
            val w = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
            val h = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
            require(w in 1..4096 && h in 1..4096 && w.toLong() * h <= 8_294_400L) {
                "Clip dimensions exceed the supported limit."
            }
            return VideoClipInfo(w, h, duration)
        } catch (e: IllegalArgumentException) {
            throw e
        } catch (e: RuntimeException) {
            throw IllegalArgumentException("Unsupported or corrupt MP4/WebM clip.", e)
        } finally {
            retriever.release()
        }
    }
    fun poster(file: File, maxW: Int = 320, maxH: Int = 320): Bitmap {
        inspect(file)
        val r = MediaMetadataRetriever()
        try {
            r.setDataSource(file.absolutePath)
            val frame = r.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: throw IllegalArgumentException("No decodable clip frame.")
            val scale = minOf(1f, maxW.toFloat() / frame.width, maxH.toFloat() / frame.height)
            if (scale >= 1f) return frame
            return try {
                Bitmap.createScaledBitmap(frame,(frame.width * scale).toInt().coerceAtLeast(1),
                    (frame.height * scale).toInt().coerceAtLeast(1),true)
            } finally { frame.recycle() }
        } finally { r.release() }
    }
}
