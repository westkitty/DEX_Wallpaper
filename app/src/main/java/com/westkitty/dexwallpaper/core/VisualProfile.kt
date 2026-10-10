package com.westkitty.dexwallpaper.core

import org.json.JSONObject

data class VisualProfile(val name: String,val collectionId: String="",val mode: RotationMode=RotationMode.SHUFFLE,val intervalMinutes: Long=60,
    val dim: Float=0f,val saturation: Float=1f,val brightness: Float=0f,val contrast: Float=1f,
    val reducedMotion: Boolean=false,val clockOverlay: Boolean=false,val shader: ShaderEffect=ShaderEffect.NONE,val parallax: Boolean=false,val frameRate: Int=30)
object DeskShift {
    val presets=listOf(
        VisualProfile("Work",dim=.12f,saturation=.85f,reducedMotion=true,clockOverlay=true,frameRate=15),
        VisualProfile("Art",intervalMinutes=180),
        VisualProfile("Gaming",saturation=1.15f,shader=ShaderEffect.GLASS,parallax=true),
        VisualProfile("Reading",dim=.25f,saturation=.15f,reducedMotion=true,frameRate=15),
        VisualProfile("Night",intervalMinutes=1440,dim=.6f,saturation=.35f,reducedMotion=true,frameRate=10),
        VisualProfile("Minimal",dim=.15f,saturation=0f,reducedMotion=true,clockOverlay=true,frameRate=15))
}
object ProfileCodec {
    fun encode(p: VisualProfile): String {
        validate(p)
        return JSONObject().put("format","dex-visual-profile").put("version",1).put("name",p.name).put("collectionId",p.collectionId)
            .put("mode",p.mode.name).put("intervalMinutes",p.intervalMinutes).put("dim",p.dim).put("saturation",p.saturation).put("brightness",p.brightness)
            .put("contrast",p.contrast).put("reducedMotion",p.reducedMotion).put("clockOverlay",p.clockOverlay).put("shader",p.shader.name)
            .put("parallax",p.parallax).put("frameRate",p.frameRate).toString(2)
    }
    fun decode(input: String): Outcome<VisualProfile> = try {
        require(input.toByteArray().size<=16*1024 && input.count { it=='{' || it=='[' }<=4) { "Profile exceeds size/structure budget" }
        val j=JSONObject(input)
        require(j.getString("format")=="dex-visual-profile" && j.getInt("version")==1) { "Unsupported profile" }
        val p=VisualProfile(j.getString("name"),j.optString("collectionId"),RotationMode.valueOf(j.optString("mode","SHUFFLE")),j.optLong("intervalMinutes",60),
            j.optDouble("dim",0.0).toFloat(),j.optDouble("saturation",1.0).toFloat(),j.optDouble("brightness",0.0).toFloat(),j.optDouble("contrast",1.0).toFloat(),
            j.optBoolean("reducedMotion"),j.optBoolean("clockOverlay"),ShaderEffect.valueOf(j.optString("shader","NONE")),j.optBoolean("parallax"),j.optInt("frameRate",30))
        validate(p); Outcome.Ok(p)
    } catch(e: Exception) { Outcome.Error(e.message?.take(200) ?: "Invalid profile") }
    private fun validate(p: VisualProfile) {
        require(p.name.length in 1..80 && (p.collectionId.isEmpty() || p.collectionId.matches(Regex("[A-Za-z0-9_-]{1,80}")))) { "Invalid profile identity" }
        require(p.intervalMinutes in 15..10080 && p.frameRate in 5..60 && listOf(p.dim,p.saturation,p.brightness,p.contrast).all { it.isFinite() }) { "Invalid profile settings" }
        require(p.dim in 0f..1f && p.saturation in 0f..2f && p.brightness in -1f..1f && p.contrast in 0f..2f) { "Profile effects out of bounds" }
    }
}
