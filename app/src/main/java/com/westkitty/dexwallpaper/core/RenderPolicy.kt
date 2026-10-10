package com.westkitty.dexwallpaper.core

/** Capability/visibility/power inputs, no performance or battery-saving percentages invented. */
data class QualityInput(val visible: Boolean, val animated: Boolean, val requestedFps: Int, val batterySaver: Boolean,
    val thermalSeverity: Int, val lowRam: Boolean, val reducedMotion: Boolean, val safeMode: Boolean, val glSupported: Boolean)
data class RenderQuality(val framesPerSecond: Int, val gpu: Boolean, val motion: Boolean, val effectScale: Float, val reason: String)
object AdaptiveQualityPolicy {
    fun evaluate(input: QualityInput): RenderQuality {
        if(!input.visible) return RenderQuality(0,false,false,0f,"Hidden: all animation and decoding suspended")
        if(input.safeMode) return RenderQuality(0,false,false,0f,"Safe mode: static Canvas fallback")
        if(input.reducedMotion || !input.animated) return RenderQuality(0,input.glSupported,false,
            when { input.thermalSeverity>=4 -> 0f;input.thermalSeverity>=3 -> .35f;input.batterySaver -> .5f;input.lowRam -> .75f;else -> 1f },"Static event-driven frames")
        val fps=input.requestedFps.coerceIn(5,60)
        val cap=when { input.thermalSeverity>=4 -> 5; input.thermalSeverity>=3 -> 12; input.batterySaver -> 12; input.lowRam -> 24; else -> 60 }
        val scale=when { input.thermalSeverity>=4 -> 0f; input.thermalSeverity>=3 -> .35f; input.batterySaver -> .5f; input.lowRam -> .75f; else -> 1f }
        return RenderQuality(minOf(fps,cap),input.glSupported,true,scale,when { input.thermalSeverity>=3 -> "Thermal cap"; input.batterySaver -> "Battery Saver cap"; input.lowRam -> "Low-RAM cap"; else -> "Configured frame cap" })
    }
}
enum class ShaderEffect(val title: String) {
    NONE("None"), ATMOSPHERE("Atmosphere"), REVERSE_ATMOSPHERE("Reverse atmosphere"), REEDED_GLASS("Reeded glass"), GLASS("Glass"),
    REVERSE_GLASS("Reverse glass"), COLOR_FILL("Color fill"), REVERSE_COLOR_FILL("Reverse color fill"), SKETCH("Canvas sketch"), REVERSE_SKETCH("Reverse sketch"),
    FROST("Frosted glass"), REVERSE_FROST("Reverse frost"), HALFTONE("Halftone"), REVERSE_HALFTONE("Reverse halftone"), GRAIN("Film grain")
}
enum class Transition(val title: String) { NONE("Off"), CROSSFADE("Crossfade"), SCALE("Scale"), SLIDE("Slide") }
