package com.westkitty.dexwallpaper.core

/** Destinations are explicit. Nothing in the core represents an implicit "all screens". */
enum class Destination(val flags: Int, val label: String) {
    HOME(1, "Home screen"), LOCK(2, "Lock screen"), BOTH(3, "Both screens");
    companion object { fun parse(value: String) = entries.firstOrNull { it.name == value } ?: HOME }
}
enum class RotationMode { SHUFFLE, SEQUENTIAL, RANDOM }
enum class ScaleMode { FILL, FIT, STRETCH, ORIGINAL, FIT_HEIGHT }
enum class CapabilityState { SUPPORTED, SUPPORTED_WITH_PERMISSION, SUPPORTED_WITH_SETUP, SUPPORTED_WITH_LIMITATIONS, UNSUPPORTED }
data class Capability(val name: String, val state: CapabilityState, val explanation: String)
data class Candidate(val id: String, val favorite: Boolean = false, val excluded: Boolean = false, val valid: Boolean = true)
data class QueueState(val pending: List<String> = emptyList(), val recent: List<String> = emptyList(), val cycle: Long = 0)
data class Selection(val assetId: String, val queue: QueueState, val reason: String)
data class WallpaperSnapshot(val homeId: Int, val lockId: Int, val supported: Boolean = true, val allowed: Boolean = true)
sealed interface Outcome<out T> {
    data class Ok<T>(val value: T) : Outcome<T>
    data class Error(val message: String, val recoverable: Boolean = true) : Outcome<Nothing>
}

data class ImageEdit(
    val scale: ScaleMode = ScaleMode.FILL,
    val zoom: Float = 1f,
    val focalX: Float = .5f,
    val focalY: Float = .5f,
    val portraitX: Float = .5f,
    val portraitY: Float = .5f,
    val brightness: Float = 0f,
    val contrast: Float = 1f,
    val saturation: Float = 1f,
    val dim: Float = 0f,
    val vignette: Float = 0f,
    val blur: Float = 0f,
    val backgroundColor: Int = 0xff111114.toInt(),
    val safeTop: Float = 0f,
    val safeBottom: Float = 0f,
) {
    fun bounded() = copy(zoom = zoom.coerceIn(1f, 4f), focalX = focalX.coerceIn(0f, 1f), focalY = focalY.coerceIn(0f, 1f),
        portraitX = portraitX.coerceIn(0f, 1f), portraitY = portraitY.coerceIn(0f, 1f), brightness = brightness.coerceIn(-1f, 1f),
        contrast = contrast.coerceIn(0f, 2f), saturation = saturation.coerceIn(0f, 2f), dim = dim.coerceIn(0f, 1f),
        vignette = vignette.coerceIn(0f, 1f), blur = blur.coerceIn(0f, 25f), safeTop = safeTop.coerceIn(0f, .45f), safeBottom = safeBottom.coerceIn(0f, .45f))
}
