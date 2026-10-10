package com.westkitty.dexwallpaper.core

import com.westkitty.dexwallpaper.data.WorkspacePreferences
import org.json.JSONObject

/** Live content is NOT selected from static Home/Lock records. Static writes cannot rebind Lock engines. */
data class LiveConfiguration(val assetId: String,val edit: ImageEdit,val settings: WorkspacePreferences)
object LiveConfigCodec {
    fun encode(c: LiveConfiguration): String {
        require(c.assetId.matches(Regex("[A-Za-z0-9_-]{1,80}")))
        val e=c.edit.bounded();val s=c.settings
        return JSONObject().put("version",1).put("assetId",c.assetId).put("edit",JSONObject().put("scale",e.scale.name).put("zoom",e.zoom)
            .put("focalX",e.focalX).put("focalY",e.focalY).put("portraitX",e.portraitX).put("portraitY",e.portraitY).put("brightness",e.brightness)
            .put("contrast",e.contrast).put("saturation",e.saturation).put("dim",e.dim).put("vignette",e.vignette).put("blur",e.blur).put("safeTop",e.safeTop).put("safeBottom",e.safeBottom))
            .put("renderer",JSONObject().put("shader",s.shaderEffect).put("intensity",s.effectIntensity).put("transition",s.transition).put("duration",s.transitionMs)
                .put("frameRate",s.frameRate).put("clock",s.clockOverlay).put("battery",s.batteryOverlay).put("parallax",s.parallax).put("tilt",s.gyroParallax)
                .put("zoom",s.zoomAnimation).put("reducedMotion",s.reducedMotion)).toString()
    }
    fun decode(input: String): Outcome<LiveConfiguration> = try {
        require(input.length<=16*1024 && input.count { it=='{' || it=='[' }<=3) { "Live configuration exceeds structure budget" }
        val j=JSONObject(input);require(j.getInt("version")==1) { "Unsupported live configuration" }
        val id=j.getString("assetId");require(id.matches(Regex("[A-Za-z0-9_-]{1,80}"))) { "Invalid live asset identity" }
        val e=j.getJSONObject("edit");val s=j.getJSONObject("renderer")
        fun number(o: JSONObject,key: String,default: Double)=o.optDouble(key,default).also { require(it.isFinite()) { "Invalid live number" } }.toFloat()
        val edit=ImageEdit(scale=ScaleMode.valueOf(e.optString("scale","FILL")),zoom=number(e,"zoom",1.0),focalX=number(e,"focalX",.5),focalY=number(e,"focalY",.5),
            portraitX=number(e,"portraitX",.5),portraitY=number(e,"portraitY",.5),brightness=number(e,"brightness",0.0),contrast=number(e,"contrast",1.0),saturation=number(e,"saturation",1.0),
            dim=number(e,"dim",0.0),vignette=number(e,"vignette",0.0),blur=number(e,"blur",0.0),safeTop=number(e,"safeTop",0.0),safeBottom=number(e,"safeBottom",0.0)).bounded()
        val shader=ShaderEffect.valueOf(s.optString("shader","NONE"));val transition=Transition.valueOf(s.optString("transition","CROSSFADE"))
        val settings=WorkspacePreferences(frameRate=s.optInt("frameRate",30).coerceIn(5,60),clockOverlay=s.optBoolean("clock"),batteryOverlay=s.optBoolean("battery"),
            parallax=s.optBoolean("parallax"),shaderEffect=shader.name,effectIntensity=number(s,"intensity",.65).coerceIn(0f,1f),transition=transition.name,
            transitionMs=s.optInt("duration",700).coerceIn(100,5000),gyroParallax=s.optBoolean("tilt"),zoomAnimation=s.optBoolean("zoom"),reducedMotion=s.optBoolean("reducedMotion"))
        Outcome.Ok(LiveConfiguration(id,edit,settings))
    } catch(e: Exception) { Outcome.Error(e.message?.take(150) ?: "Invalid live configuration") }
}
object LiveDestinationPolicy {
    const val UNKNOWN=0
    fun mayUpdate(target: Destination,installedFlags: Collection<Int>): Boolean = when(target) {
        Destination.HOME -> 1 in installedFlags && installedFlags.all { it==1 || it==2 }
        Destination.LOCK -> 2 in installedFlags && installedFlags.all { it==1 || it==2 }
        Destination.BOTH -> installedFlags.isNotEmpty() // Explicitly affects ALL OS-assigned instances, not static wallpaper.
    }
    fun mayFinishPicker(target: Destination,installedFlags: Collection<Int>,before: WallpaperSnapshot,after: WallpaperSnapshot): Boolean {
        if(!after.supported || !after.allowed || !mayUpdate(target,installedFlags) || DestinationPolicy.check(target,before) !is Outcome.Ok || DestinationPolicy.verify(target,before,after) !is Outcome.Ok) return false
        return target!=Destination.BOTH || 0 in installedFlags || 3 in installedFlags || (1 in installedFlags && 2 in installedFlags)
    }
    fun source(flags: Int,home: String,lock: String,shared: String): String = when(flags) { 1 -> home;2 -> lock;else -> shared }
    // Deliberately no argument for static currentAssetId. It cannot influence a live source.
}
